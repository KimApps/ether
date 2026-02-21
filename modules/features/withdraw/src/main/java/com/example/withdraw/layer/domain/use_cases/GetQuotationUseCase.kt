package com.example.withdraw.layer.domain.use_cases

import com.example.error_logger.ErrorLoggerService
import com.example.withdraw.layer.domain.entity_models.TransactionQuotationEntity
import com.example.withdraw.layer.domain.repository.WithdrawRepository
import com.example.withdraw.layer.domain.request_models.GetQuotationRequest
import javax.inject.Inject

class GetQuotationUseCase @Inject constructor(
    private val repository: WithdrawRepository,
    private val errorLogger: ErrorLoggerService
) {
    suspend operator fun invoke(rq: GetQuotationRequest): TransactionQuotationEntity {
        try {
            val result = repository.getChallenge(rq)
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