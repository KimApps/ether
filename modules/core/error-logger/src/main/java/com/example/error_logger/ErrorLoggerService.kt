package com.example.error_logger

/**
 * Interface for observability and error reporting.
 * Defined in the core module to be accessible by all feature domain layers.
 */
interface ErrorLoggerService {
    fun logException(
        featureName: String,
        errorTitle: String,
        exception: Throwable,
        stackTrace: Array<StackTraceElement>? = null
    )
}