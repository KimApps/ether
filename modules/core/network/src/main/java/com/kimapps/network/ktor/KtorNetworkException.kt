package com.kimapps.network.ktor

import com.kimapps.network.error.AppNetworkException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Utility class for mapping Ktor exceptions and error responses to [AppNetworkException].
 * This class provides a standardized way to handle different types of network errors.
 */
object KtorNetworkException {

    /**
     * Maps a [Throwable] to [AppNetworkException].
     * Handles various types of exceptions including Ktor-specific exceptions, [IOException], etc.
     *
     * @param throwable The exception to map
     * @return The corresponding [AppNetworkException]
     */
    fun mapToAppException(throwable: Throwable): AppNetworkException {
        return when (throwable) {
            is ClientRequestException -> {
                // 4xx errors (client errors)
                mapClientException(throwable)
            }
            is ServerResponseException -> {
                // 5xx errors (server errors)
                mapServerException(throwable)
            }
            is RedirectResponseException -> {
                // 3xx errors (redirects)
                AppNetworkException.AppUnknownException(
                    errorMessage = "Redirect error: $throwable"
                )
            }
            is ResponseException -> {
                // Other HTTP response errors
                mapResponseException(throwable)
            }
            is UnknownHostException -> {
                // No internet connection or DNS resolution failure
                AppNetworkException.AppConnectionException(
                    errorMessage = "No internet connection: $throwable"
                )
            }
            is SocketTimeoutException -> {
                // Request timeout
                AppNetworkException.AppTimeoutException(
                    errorMessage = "Connection timeout: $throwable"
                )
            }
            is IOException -> {
                // Other IO errors (network issues, etc.)
                AppNetworkException.AppConnectionException(
                    errorMessage = "IOException: $throwable"
                )
            }
            else -> {
                // Any other unexpected exception
                AppNetworkException.AppUnknownException(
                    errorMessage = "Unknown error: $throwable"
                )
            }
        }
    }

    /**
     * Maps a Ktor [HttpResponse] to [AppNetworkException].
     * Useful when you have an HttpResponse object and want to convert it to an exception.
     *
     * @param response The Ktor HttpResponse
     * @param errorBody Optional error body string
     * @return The corresponding [AppNetworkException]
     */
    suspend fun mapResponseToAppException(
        response: HttpResponse,
        errorBody: String? = null
    ): AppNetworkException {
        val statusCode = response.status.value
        val message = errorBody ?: try {
            response.bodyAsText()
        } catch (e: Exception) {
            response.status.description
        }

        return mapStatusCode(statusCode, message)
    }

    /**
     * Maps an HTTP status code to [AppNetworkException].
     *
     * @param code HTTP status code
     * @param message Error message or response body
     * @return The corresponding [AppNetworkException]
     */
    fun mapStatusCode(code: Int, message: String): AppNetworkException {
        return when (code) {
            HttpStatusCode.Unauthorized.value -> {
                // 401 - Unauthorized - authentication required
                AppNetworkException.AppUnauthorizedException(
                    errorMessage = parseErrorMessage(message, "Unauthorized: $message")
                )
            }
            HttpStatusCode.Forbidden.value -> {
                // 403 - Forbidden - user doesn't have permission
                AppNetworkException.AppForbiddenException(
                    errorMessage = parseErrorMessage(message, "Access denied: $message")
                )
            }
            in 400..499 -> {
                // Other client errors (bad request, not found, etc.)
                AppNetworkException.AppBadResponseException(
                    code = code,
                    errorMessage = parseErrorMessage(message, "Client error: $message")
                )
            }
            in 500..599 -> {
                // Server errors
                AppNetworkException.AppServerException(
                    code = code,
                    errorMessage = parseErrorMessage(message, "Server error: $message")
                )
            }
            else -> {
                // Unknown status code
                AppNetworkException.AppUnknownException(
                    errorMessage = "HTTP $code: ${parseErrorMessage(message, "Unknown: $message")}"
                )
            }
        }
    }

    /**
     * Maps [ClientRequestException] (4xx) to [AppNetworkException].
     */
    private fun mapClientException(exception: ClientRequestException): AppNetworkException {
        val code = exception.response.status.value
        val errorBody = try {
            // Note: bodyAsText() is suspend, but we can't call it here
            // The exception message usually contains the status description
            exception.message
        } catch (e: Exception) {
            "Client error"
        }

        return mapStatusCode(code, errorBody)
    }

    /**
     * Maps [ServerResponseException] (5xx) to [AppNetworkException].
     */
    private fun mapServerException(exception: ServerResponseException): AppNetworkException {
        val code = exception.response.status.value
        val errorBody = exception.message

        return AppNetworkException.AppServerException(
            code = code,
            errorMessage = parseErrorMessage(errorBody, "Server error")
        )
    }

    /**
     * Maps [ResponseException] to [AppNetworkException].
     */
    private fun mapResponseException(exception: ResponseException): AppNetworkException {
        val code = exception.response.status.value
        val errorBody = exception.message ?: "HTTP error"

        return mapStatusCode(code, errorBody)
    }

    /**
     * Parses error message from response body.
     * Attempts to extract meaningful error messages from JSON or HTML responses.
     *
     * @param rawMessage The raw error message/body
     * @param defaultMessage Default message if parsing fails
     * @return Parsed or default error message
     */
    private fun parseErrorMessage(rawMessage: String?, defaultMessage: String): String {
        if (rawMessage.isNullOrBlank()) {
            return defaultMessage
        }

        return try {
            // Try to extract message from JSON
            // Simple pattern matching for {"message": "..."} or {"error": "..."}
            val messagePattern = """"message"\s*:\s*"([^"]+)"""".toRegex()
            val errorPattern = """"error"\s*:\s*"([^"]+)"""".toRegex()

            messagePattern.find(rawMessage)?.groupValues?.get(1)
                ?: errorPattern.find(rawMessage)?.groupValues?.get(1)
                ?: rawMessage.take(200) // Limit message length
        } catch (e: Exception) {
            rawMessage.take(200)
        }
    }
}