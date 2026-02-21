package com.kimapps.signing.layer.domain.repository

import com.kimapps.signing.layer.domain.entity_models.SigningResultEntity
import com.kimapps.signing.layer.domain.request_models.SigningRequest

interface SigningRepository {
    suspend fun signChallenge(rq: SigningRequest): SigningResultEntity
}