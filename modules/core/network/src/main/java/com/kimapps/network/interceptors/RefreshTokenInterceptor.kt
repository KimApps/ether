package com.kimapps.network.interceptors

import com.google.gson.Gson
import com.kimapps.network.constants.ApiEndpoint
import com.kimapps.network.storage.TokenManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
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
    @Volatile
    private var isRefreshing = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip refresh logic for the refresh token endpoint itself
        if (originalRequest.url.encodedPath.contains(ApiEndpoint.refreshTokens)) {
            return chain.proceed(originalRequest)
        }

        // Execute the original request
        val response = chain.proceed(originalRequest)

        // Check if response is 401 Unauthorized
        if (response.code == 401) {
            response.close() // Close the original response before retrying

            // Attempt to refresh the token
            synchronized(lock) {
                val currentToken = tokenManager.getTokenBlocking()

                // If token changed while waiting, it means another thread refreshed it
                // Retry the request with the new token
                if (currentToken != null && currentToken != extractToken(originalRequest)) {
                    return retryRequestWithNewToken(chain, originalRequest, currentToken)
                }

                // Attempt to refresh the token
                val refreshSuccess = refreshToken()

                if (refreshSuccess) {
                    // Token refreshed successfully, retry the original request
                    val newToken = tokenManager.getTokenBlocking()
                    if (newToken != null) {
                        return retryRequestWithNewToken(chain, originalRequest, newToken)
                    }
                }
            }

            // Refresh failed or no refresh token available
            // Clear tokens and return 401 (app should navigate to login)
            tokenManager.clearTokenBlocking()
            return response
        }

        return response
    }

    /**
     * Attempts to refresh the authentication token using the refresh token.
     *
     * @return true if refresh was successful, false otherwise
     */
     fun refreshToken(): Boolean {
        if (isRefreshing) {
            // Already refreshing in another thread
            return false
        }

        return try {
            isRefreshing = true

            val refreshToken = tokenManager.getRefreshTokenBlocking()
                ?: // No refresh token available, cannot refresh
                return false

            // Build the refresh request
            val refreshRequest = buildRefreshRequest(refreshToken)

            // Create a new OkHttpClient without interceptors to avoid infinite loops
            val client = okhttp3.OkHttpClient()
            val response = client.newCall(refreshRequest).execute()

            if (response.isSuccessful) {
                val responseBody = response.body.string()
                run {
                    // Parse the new tokens from response
                    val tokenResponse =
                        gson.fromJson(responseBody, RefreshTokenResponse::class.java)

                    // Save the new tokens
                    tokenManager.saveTokensBlocking(
                        token = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken ?: refreshToken
                    )

                    true
                }
            } else {
                // Refresh failed (e.g., refresh token expired)
                false
            }
        } catch (e: IOException) {
            // Network error during refresh
            false
        } catch (e: Exception) {
            // Parsing or other error
            false
        } finally {
            isRefreshing = false
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

