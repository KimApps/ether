package com.kimapps.signing.layer.data.repository

import com.kimapps.signing.layer.data.data_sourses.SigningRemoteDS
import com.kimapps.signing.layer.data.mapper.SigningResultMapper
import com.kimapps.signing.layer.domain.entity_models.SigningResultEntity
import com.kimapps.signing.layer.domain.repository.SigningRepository
import com.kimapps.signing.layer.domain.request_models.SigningRequest
import javax.inject.Inject

class SigningRepositoryImpl @Inject constructor(
    private val remote: SigningRemoteDS,
    private val signingResultMapper: SigningResultMapper

) : SigningRepository {
    override suspend fun signChallenge(rq: SigningRequest): SigningResultEntity {
        val result = remote.signChallenge(rq)
        // map result to entity
        return signingResultMapper.map(result)

    }

    override suspend fun signWithWallet(challenge: String): SigningResultEntity {
        val result = remote.signWithWallet(challenge)
        // map result to entity
        return signingResultMapper.map(result)
    }
}