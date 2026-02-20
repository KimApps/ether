package com.kimapps.signing.layer.data.mapper

import com.kimapps.signing.layer.data.dto_models.SigningResultDto
import com.kimapps.signing.layer.domain.entity_models.SigningResultEntity
import javax.inject.Inject

class SigningResultMapper @Inject constructor() {
    fun map(model: SigningResultDto): SigningResultEntity {
        // Map the SigningResultDto to SigningResultEntity here
        return if (model.signature.isNullOrEmpty()) {
            SigningResultEntity.Error(message = "Signature is empty or null")
        } else {
            SigningResultEntity.Signed(signature = model.signature)
        }
    }
}