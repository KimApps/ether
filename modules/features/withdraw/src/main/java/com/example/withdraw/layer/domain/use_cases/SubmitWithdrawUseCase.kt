package com.example.withdraw.layer.domain.use_cases

import com.example.withdraw.layer.domain.repository.WithdrawRepository
import com.example.withdraw.layer.domain.request_models.SubmitWithdrawRequest
import javax.inject.Inject

class SubmitWithdrawUseCase @Inject constructor(private val repository: WithdrawRepository) {
    suspend operator fun invoke(rq: SubmitWithdrawRequest): Boolean {
        try {
            val result = repository.submitWithdrawal(rq)
            return result
        } catch (e: Exception) {
            // TODO: Log error to a crashlytics or analytics tool.
            // Re-throws the exception to be handled by the caller (e.g., a ViewModel).
            throw e
        }

    }

}