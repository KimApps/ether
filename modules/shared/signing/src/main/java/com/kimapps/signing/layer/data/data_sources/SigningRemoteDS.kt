package com.kimapps.signing.layer.data.data_sources

import com.kimapps.signing.layer.data.dto_models.SigningResultDto
import com.kimapps.signing.layer.domain.request_models.SigningRequest
import kotlinx.coroutines.delay
import javax.inject.Inject

class SigningRemoteDS @Inject constructor() {
    suspend fun signChallenge(rq: SigningRequest): SigningResultDto {
        // mock api request
        delay(2000)
        val signature = if (rq.challenge.isEmpty()) {
            // if challenge is empty, return empty signature
            // it will be mapped to the error message
            ""
        } else {
            // mock signature generation
            "${rq.challenge}_signed"
        }
        return SigningResultDto(signature)
    }
}