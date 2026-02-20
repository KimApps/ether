package com.kimapps.signing.layer.data.repository

import com.kimapps.signing.layer.data.data_sourses.SigningRemoteDS
import com.kimapps.signing.layer.domain.entity_models.SigningResultEntity
import com.kimapps.signing.layer.domain.repository.SigningRepository
import com.kimapps.signing.layer.domain.request_models.SigningRequest
import javax.inject.Inject

class SigningRepositoryImpl @Inject constructor(
    private val remote: SigningRemoteDS
) : SigningRepository {
    override suspend fun signChallenge(rq: SigningRequest): SigningResultEntity {
        TODO("Not yet implemented")
    }
}