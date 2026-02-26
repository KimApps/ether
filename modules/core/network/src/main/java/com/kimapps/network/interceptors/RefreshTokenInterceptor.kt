package com.kimapps.network.interceptors

import com.google.gson.Gson
import com.kimapps.network.constants.ApiEndpoint
import com.kimapps.network.storage.TokenManager
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that automatically refreshes expired authentication tokens.
 *
 * When a request receives a 401 Unauthorized response, this interceptor:
 * 1. Retrieves the refresh token from TokenManager
 * 2. Calls the refresh token endpoint
 * 3. Saves the new tokens
 * 4. Retries the original request with the new access token
 *
 * If token refresh fails:
 * - Clears all tokens (logs out user)
 * - Returns the 401 response (app should navigate to login)
 *
 * Thread Safety:
 * - Uses synchronized block to prevent multiple simultaneous refresh attempts
 * - Only one thread refreshes at a time; others wait and use the new token
 *
 * Usage:
 * This interceptor should be added AFTER AuthInterceptor in the OkHttpClient chain.
 *
 * Example in NetworkModule:
 * ```
 * @Provides
 * fun provideRefreshTokenInterceptor(
 *     tokenManager: TokenManager,
 *     gson: Gson
 * ): RefreshTokenInterceptor {
 *     return RefreshTokenInterceptor(
 *         tokenManager = tokenManager,
 *         baseUrl = ApiEndpoint.baseUrl,
 *         refreshTokenPath = "auth/refresh",  // Your API endpoint
 *         gson = gson
 *     )
 * }
 *
 * @Provides
 * fun provideOkHttpClient(
 *     authInterceptor: AuthInterceptor,
 *     refreshTokenInterceptor: RefreshTokenInterceptor,
 *     loggingInterceptor: HttpLoggingInterceptor
 * ): OkHttpClient {
 *     return OkHttpClient.Builder()
 *         .addInterceptor(authInterceptor)
 *         .addInterceptor(refreshTokenInterceptor)  // Add AFTER auth
 *         .addInterceptor(loggingInterceptor)
 *         .build()
 * }
 * ```
 *
 * @param tokenManager Manages token storage and retrieval
 * @param gson For parsing JSON responses
 */
@Singleton
class RefreshTokenInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val gson: Gson
) : Interceptor {

    // Lock object for synchronized access during token refresh
    private val lock = Any()

    // Track if we're currently refreshing to avoid multiple simultaneous refreshes
    private var isRefreshing = java.util.concurrent.atomic.AtomicBoolean(false)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (originalRequest.url.encodedPath.contains(ApiEndpoint.refreshTokens)) {
            return chain.proceed(originalRequest)
        }

        val response = chain.proceed(originalRequest)

        if (response.code == 401) {
            // FIX #5: Capture the protocol BEFORE closing
            val protocol = response.protocol
            response.close()

            synchronized(lock) {
                val currentToken = tokenManager.getTokenBlocking()

                if (currentToken != null && currentToken != extractToken(originalRequest)) {
                    return retryRequestWithNewToken(chain, originalRequest, currentToken)
                }

                if (refreshToken()) {
                    val newToken = tokenManager.getTokenBlocking()
                    if (newToken != null) {
                        return retryRequestWithNewToken(chain, originalRequest, newToken)
                    }
                }
            }

            // FIX #5: Create a NEW response instead of returning the closed one
            tokenManager.clearTokensBlocking()
            return Response.Builder()
                .request(originalRequest)
                .protocol(protocol) // Use captured protocol
                .code(401)
                .message("Unauthorized (Refresh Failed)")
                .body("".toResponseBody("application/json".toMediaTypeOrNull()))
                .build()
        }

        return response
    }

    /**
     * Attempts to refresh the authentication token using the refresh token.
     *
     * @return true if refresh was successful, false otherwise
     */
    fun refreshToken(): Boolean {
        // compareAndSet(expected, new) is atomic.
        // It returns true ONLY if it successfully changed false to true.
        if (!isRefreshing.compareAndSet(false, true)) {
            // Someone else is already refreshing.
            // We return true here because the synchronized(lock) in intercept()
            // will make this thread wait anyway.
            return true
        }

        return try {
            val refreshToken = tokenManager.getRefreshTokenBlocking() ?: return false
            val client = okhttp3.OkHttpClient()
            val response = client.newCall(buildRefreshRequest(refreshToken)).execute()

            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val tokenResponse = gson.fromJson(responseBody, RefreshTokenResponse::class.java)

                tokenManager.saveTokensBlocking(
                    token = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken ?: refreshToken
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            // Reset the flag so future 401s can trigger a refresh
            isRefreshing.set(false)
        }
    }

    /**
     * Builds the HTTP request to refresh the token.
     *
     * @param refreshToken The refresh token to send
     * @return The refresh request
     */
    private fun buildRefreshRequest(refreshToken: String): Request {
        // Build JSON request body
        val requestBody = gson.toJson(
            mapOf("refreshToken" to refreshToken)
        ).toRequestBody()

        // Construct the full URL from baseUrl and refreshTokenPath
        val fullUrl = if (ApiEndpoint.baseUrl.endsWith("/")) {
            "${ApiEndpoint.baseUrl}${ApiEndpoint.refreshTokens}"
        } else {
            "${ApiEndpoint.baseUrl}/${ApiEndpoint.refreshTokens}"
        }


        return Request.Builder()
            .url(fullUrl)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()
    }

    /**
     * Retries the original request with a new authentication token.
     *
     * @param chain The interceptor chain
     * @param originalRequest The original request that failed
     * @param newToken The new access token
     * @return The response from the retried request
     */
    private fun retryRequestWithNewToken(
        chain: Interceptor.Chain,
        originalRequest: Request,
        newToken: String
    ): Response {
        val newRequest = originalRequest.newBuilder()
            .removeHeader("Authorization")
            .addHeader("Authorization", "Bearer $newToken")
            .build()

        return chain.proceed(newRequest)
    }

    /**
     * Extracts the token from the Authorization header.
     *
     * @param request The request
     * @return The token, or null if not present
     */
    private fun extractToken(request: Request): String? {
        val authHeader = request.header("Authorization")
        return authHeader?.removePrefix("Bearer ")
    }

    /**
     * Data class for parsing the refresh token API response.
     * Adjust field names based on your actual API response.
     */
    private data class RefreshTokenResponse(
        val accessToken: String,
        val refreshToken: String? = null // Optional: some APIs return a new refresh token
    )
}

