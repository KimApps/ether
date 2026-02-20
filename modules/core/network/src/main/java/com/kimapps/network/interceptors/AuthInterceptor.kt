package com.kimapps.network.interceptors

import okhttp3.Interceptor

/**
 * OkHttp interceptor that adds authentication token to all HTTP requests.
 *
 * This interceptor is called for EVERY network request. It retrieves the current
 * authentication token dynamically via [tokenProvider] and adds it as a Bearer token
 * in the Authorization header.
 *
 * ## How tokenProvider Works:
 *
 * Instead of passing a static token string, we pass a **lambda function** that returns
 * the token. This allows the interceptor to always fetch the latest token, even if it
 * changes (e.g., after login, logout, or token refresh).
 *
 * ## Execution Flow:
 * 1. App makes a network request (e.g., `networkClient.get<User>("users/profile")`)
 * 2. OkHttp intercepts the request BEFORE sending it
 * 3. This interceptor calls `tokenProvider()` to get the current token
 * 4. If token exists → adds "Authorization: Bearer {token}" header
 * 5. If no token → sends request without Authorization header
 * 6. Request proceeds to the server
 *
 * ## Example Usage in DI:
 * ```kotlin
 * // In NetworkModule
 * @Provides
 * fun provideOkHttpClient(tokenManager: TokenManager): OkHttpClient {
 *     val authInterceptor = AuthInterceptor(
 *         tokenProvider = { tokenManager.getToken() }
 *     )
 *     return OkHttpClient.Builder()
 *         .addInterceptor(authInterceptor)
 *         .build()
 * }
 * ```
 *
 * ## Why Use a Lambda Instead of a String?
 * - **Dynamic**: Token can change (login/logout) but interceptor is created only once
 * - **Lazy**: Token is fetched only when needed (per request)
 * - **Up-to-date**: Always gets the latest token from storage
 *
 * @param tokenProvider Lambda function that returns the current auth token or null.
 *                      Called on EVERY request to get the latest token.
 */
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        // Fetch the current token by calling the lambda function
        val token = tokenProvider()

        // Build the request with or without Authorization header
        val request = if (token != null) {
            // Token exists → add Authorization header with Bearer token
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            // No token → proceed with original request (no auth header)
            chain.request()
        }

        // Continue with the modified (or original) request
        return chain.proceed(request)
    }
}