package com.kimapps.signing.layer.domain.use_cases

import com.kimapps.signing.layer.domain.entity_models.SigningResultEntity
import com.kimapps.signing.layer.domain.repository.SigningRepository
import com.kimapps.signing.layer.domain.request_models.SigningRequest
import javax.inject.Inject

class SignWithWalletUseCase @Inject constructor(
    private val repository: SigningRepository
) {
    suspend operator fun invoke(challenge: String): SigningResultEntity {
        try {
            // Delegates the login call to the repository.
            return repository.signWithWallet(challenge)
        } catch (e: Exception) {
            //TODO: Log error to a crashlytics or analytics tool.
            // Re-throws the exception to be handled by the caller (e.g., a ViewModel).
            throw e
        }

    }
}