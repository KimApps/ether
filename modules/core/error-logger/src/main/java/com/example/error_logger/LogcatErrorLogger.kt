package com.example.error_logger

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogcatErrorLogger @Inject constructor() : ErrorLoggerService {
    override fun logException(
        featureName: String,
        errorTitle: String,
        exception: Throwable,
        stackTrace: Array<StackTraceElement>?
    ) {
        // In a real project, I would swap this for CrashlyticsErrorLogger
        // but for this assignment, we use Logcat to avoid Firebase setup overhead.
        Log.e("ErrorLogger --->", "[$featureName] $errorTitle", exception)
    }
}