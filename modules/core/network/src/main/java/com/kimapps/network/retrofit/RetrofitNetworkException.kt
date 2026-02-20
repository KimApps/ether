package com.kimapps.network.retrofit

import com.kimapps.network.error.AppNetworkException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Utility class for mapping Retrofit exceptions and error responses to [AppNetworkException].
 * This class provides a standardized way to handle different types of network errors.
 */
object RetrofitNetworkException {

    /**
     * Maps a [Throwable] to [AppNetworkException].
     * Handles various types of exceptions including [HttpException], [IOException], etc.
     *
     * @param throwable The exception to map
     * @return The corresponding [AppNetworkException]
     */
    fun mapToAppException(throwable: Throwable): AppNetworkException {
        return when (throwable) {
            is HttpException -> {
                // HTTP error responses (4xx, 5xx)
                mapHttpException(throwable)
            }

            is UnknownHostException -> {
                // No internet connection or DNS resolution failure
                AppNetworkException.AppConnectionException(errorMessage = "No internet connection $throwable")
            }

            is SocketTimeoutException -> {
                // Request timeout
                AppNetworkException.AppTimeoutException(errorMessage = "Connection timeout $throwable")
            }

            is IOException -> {
                // Other IO errors (network issues, etc.)
                AppNetworkException.AppUnknownException(
                    errorMessage = "IOException:  $throwable"
                )
            }

            else -> {
                // Any other unexpected exception
                AppNetworkException.AppUnknownException(
                    errorMessage = "Unknown:  $throwable"
                )
            }
        }
    }

    /**
     * Maps a Retrofit [Response] to [AppNetworkException].
     * Useful when you have a Response object and want to convert it to an exception.
     *
     * @param response The Retrofit response
     * @return The corresponding [AppNetworkException]
     */
    fun <T> mapResponseToAppException(response: Response<T>): AppNetworkException {
        val code = response.code()
        val errorBody = response.errorBody()?.string() ?: response.message()

        return mapStatusCode(code, errorBody)
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
            401 -> {
                // Unauthorized - authentication required
                AppNetworkException.AppUnauthorizedException(
                    errorMessage = parseErrorMessage(message, "Please login")
                )
            }

            403 -> {
                // Forbidden - user doesn't have permission
                AppNetworkException.AppForbiddenException(
                    errorMessage = parseErrorMessage(message, "Access denied")
                )
            }

            in 400..499 -> {
                // Other client errors (bad request, not found, etc.)
                AppNetworkException.AppBadResponseException(
                    code = code,
                    errorMessage = parseErrorMessage(message, "Client error")
                )
            }

            in 500..599 -> {
                // Server errors
                AppNetworkException.AppServerException(
                    code = code,
                    errorMessage = parseErrorMessage(message, "Server error")
                )
            }

            else -> {
                // Unknown status code
                AppNetworkException.AppUnknownException(
                    errorMessage = "HTTP $code: ${parseErrorMessage(message, "Unknown error")}"
                )
            }
        }
    }

    /**
     * Maps [HttpException] to [AppNetworkException].
     *
     * @param exception The HTTP exception from Retrofit
     * @return The corresponding [AppNetworkException]
     */
    private fun mapHttpException(exception: HttpException): AppNetworkException {
        val code = exception.code()
        val errorBody = try {
            exception.response()?.errorBody()?.string() ?: exception.message()
        } catch (e: Exception) {
            exception.message()
        } ?: "Unknown error"

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

    /**
     * Wraps a suspend function call and maps any exceptions to [AppNetworkException].
     * Use this to safely execute Retrofit API calls.
     *
     * Example:
     * ```
     * val result = RetrofitNetworkException.safeApiCall {
     *     apiService.getUser(userId)
     * }
     * ```
     *
     * @param apiCall The suspend function to execute
     * @return Result containing either the success value or [AppNetworkException]
     */
    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(
                        AppNetworkException.AppUnknownException(errorMessage = "Response body is null")
                    )
                }
            } else {
                Result.failure(mapResponseToAppException(response))
            }
        } catch (throwable: Throwable) {
            Result.failure(mapToAppException(throwable))
        }
    }
}