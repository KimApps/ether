package com.kimapps.network

import com.kimapps.network.models.ApiResponse

/**
 * Common interface for API clients (Retrofit and Ktor).
 * This abstraction allows NetworkClient to work with either implementation.
 *
 * Note: Uses [ApiResponse] instead of Retrofit-specific classes to maintain
 * independence between implementations.
 */
interface ApiClient {
    /**
     * Performs a GET request.
     *
     * @param url The endpoint URL for the request.
     * @param queryParams A map of query parameters to be appended to the URL.
     * @param headers A map of custom headers to be added to the request.
     * @return An [ApiResponse] containing the response data.
     */
    suspend fun get(
        url: String,
        queryParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): ApiResponse

    /**
     * Performs a POST request.
     *
     * @param url The endpoint URL for the request.
     * @param body The request body.
     * @param headers A map of custom headers to be added to the request.
     * @return An [ApiResponse] containing the response data.
     */
    suspend fun post(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse

    /**
     * Performs a PUT request.
     *
     * @param url The endpoint URL for the request.
     * @param body The request body.
     * @param headers A map of custom headers to be added to the request.
     * @return An [ApiResponse] containing the response data.
     */
    suspend fun put(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse

    /**
     * Performs a DELETE request.
     *
     * @param url The endpoint URL for the request.
     * @param body The request body (optional).
     * @param headers A map of custom headers to be added to the request.
     * @return An [ApiResponse] containing the response data.
     */
    suspend fun delete(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse

    /**
     * Performs a PATCH request.
     *
     * @param url The endpoint URL for the request.
     * @param body The request body.
     * @param headers A map of custom headers to be added to the request.
     * @return An [ApiResponse] containing the response data.
     */
    suspend fun patch(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse
}

