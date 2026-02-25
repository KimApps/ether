package com.kimapps.network.ktor

import com.google.gson.Gson
import com.kimapps.network.ApiClient
import com.kimapps.network.models.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ktor-based implementation of [ApiClient].
 * Completely independent of Retrofit - uses [ApiResponse] for responses.
 * Throws [com.kimapps.network.error.AppNetworkException] on failed requests.
 *
 * @property httpClient Ktor's HTTP client instance
 * @property gson Gson instance for JSON serialization
 */
@Singleton
class KtorApiClient @Inject constructor(
    private val httpClient: HttpClient,
    private val gson: Gson
) : ApiClient {

    /**
     * Performs a GET request using Ktor and returns an [ApiResponse].
     * Throws [com.kimapps.network.error.AppNetworkException] on error.
     */
    override suspend fun get(
        url: String,
        queryParams: Map<String, String>,
        requestHeaders: Map<String, String>
    ): ApiResponse {
        return try {
            val response: HttpResponse = httpClient.get(url) {
                // Add query parameters
                queryParams.forEach { (key, value) ->
                    parameter(key, value)
                }
                // Add custom headers
                headers {
                    requestHeaders.forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
            response.toApiResponseOrThrow()
        } catch (e: Throwable) {
            throw KtorNetworkException.mapToAppException(e)
        }
    }

    /**
     * Performs a POST request using Ktor and returns an [ApiResponse].
     * Throws [com.kimapps.network.error.AppNetworkException] on error.
     */
    override suspend fun post(
        url: String,
        body: Any,
        requestHeaders: Map<String, String>
    ): ApiResponse {
        return try {
            val response: HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(body))
                // Add custom headers
                headers {
                    requestHeaders.forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
            response.toApiResponseOrThrow()
        } catch (e: Throwable) {
            throw KtorNetworkException.mapToAppException(e)
        }
    }

    /**
     * Performs a PUT request using Ktor and returns an [ApiResponse].
     * Throws [com.kimapps.network.error.AppNetworkException] on error.
     */
    override suspend fun put(
        url: String,
        body: Any,
        requestHeaders: Map<String, String>
    ): ApiResponse {
        return try {
            val response: HttpResponse = httpClient.put(url) {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(body))
                // Add custom headers
                headers {
                    requestHeaders.forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
            response.toApiResponseOrThrow()
        } catch (e: Throwable) {
            throw KtorNetworkException.mapToAppException(e)
        }
    }

    /**
     * Performs a DELETE request using Ktor and returns an [ApiResponse].
     * Throws [com.kimapps.network.error.AppNetworkException] on error.
     */
    override suspend fun delete(
        url: String,
        body: Any,
        requestHeaders: Map<String, String>
    ): ApiResponse {
        return try {
            val response: HttpResponse = httpClient.delete(url) {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(body))
                // Add custom headers
                headers {
                    requestHeaders.forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
            response.toApiResponseOrThrow()
        } catch (e: Throwable) {
            throw KtorNetworkException.mapToAppException(e)
        }
    }

    /**
     * Performs a PATCH request using Ktor and returns an [ApiResponse].
     * Throws [com.kimapps.network.error.AppNetworkException] on error.
     */
    override suspend fun patch(
        url: String,
        body: Any,
        requestHeaders: Map<String, String>
    ): ApiResponse {
        return try {
            val response: HttpResponse = httpClient.patch(url) {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(body))
                // Add custom headers
                headers {
                    requestHeaders.forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
            response.toApiResponseOrThrow()
        } catch (e: Throwable) {
            throw KtorNetworkException.mapToAppException(e)
        }
    }

    /**
     * Converts a Ktor [HttpResponse] to an [ApiResponse].
     * Throws [com.kimapps.network.error.AppNetworkException] if the response is not successful.
     */
    private suspend fun HttpResponse.toApiResponseOrThrow(): ApiResponse {
        if (status.value !in 200..299) {
            // Map failed response to AppNetworkException and throw
            throw KtorNetworkException.mapResponseToAppException(this)
        }

        val bodyText = bodyAsText()

        // Convert Ktor headers to Map
        val headerMap = headers.entries().associate { entry ->
            entry.key to entry.value.joinToString(", ")
        }

        return ApiResponse.success(
            code = status.value,
            body = bodyText,
            headers = headerMap
        )
    }
}

