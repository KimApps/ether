package com.kimapps.network.error

/**
 * Base sealed class for all network-related exceptions in the app.
 * Extends [Throwable] to allow use with Result and standard exception handling.
 */
sealed class AppNetworkException(message: String? = null) : Throwable(message) {

    /**
     * Client error - for status codes in the range 400..499
     */
    data class AppBadResponseException(val code: Int, val errorMessage: String) :
        AppNetworkException(message = "HTTP $code: $errorMessage")

    /**
     * Server error - for status codes in the range 500..599
     */
    data class AppServerException(val code: Int, val errorMessage: String) :
        AppNetworkException(message = "Server error $code: $errorMessage")

    /**
     * Unauthorized - for code 401
     */
    data class AppUnauthorizedException(val errorMessage: String) :
        AppNetworkException(message = "Unauthorized - $errorMessage")

    /**
     * Forbidden - for code 403
     */
    data class AppForbiddenException(val errorMessage: String) :
        AppNetworkException(message = "Forbidden - $errorMessage")

    /**
     * Timeout
     */
    data class AppTimeoutException(val errorMessage: String) :
        AppNetworkException(message = "Timeout - $errorMessage")

    /**
     * Connection error
     */
    data class AppConnectionException(val errorMessage: String) :
        AppNetworkException(message = "Connection error - $errorMessage")

    /**
     * Unknown error - for any other exception
     */
    data class AppUnknownException(val errorMessage: String) :
        AppNetworkException(message = errorMessage)
}
