package com.example.withdraw.layer.domain.use_cases

import com.example.error_logger.ErrorLoggerService
import com.example.withdraw.layer.domain.repository.WithdrawRepository
import com.example.withdraw.layer.domain.request_models.SubmitWithdrawRequest
import javax.inject.Inject

class SubmitWithdrawUseCase @Inject constructor(
    private val repository: WithdrawRepository,
    private val errorLogger: ErrorLoggerService
) {
    suspend operator fun invoke(rq: SubmitWithdrawRequest): Boolean {
        try {
            val result = repository.submitWithdrawal(rq)
            return result
        } catch (e: Exception) {
            //Log error to a crashlytics or analytics tool.
            errorLogger.logException(
                featureName = "withdraw",
                errorTitle = "Failed in ${this::class.java.simpleName}",
                exception = e
            )
            // Re-throws the exception to be handled by the caller (e.g., a ViewModel).
            throw e
        }

    }

}