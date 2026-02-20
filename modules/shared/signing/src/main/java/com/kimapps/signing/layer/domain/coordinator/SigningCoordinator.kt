package com.kimapps.signing.layer.domain.coordinator

import com.kimapps.signing.layer.domain.entity_models.SigningResultEntity
import com.kimapps.signing.layer.domain.request_models.SigningRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SigningCoordinator @Inject constructor() {
    // Flow to emit signing requests
    private val _signingRequest = MutableSharedFlow<SigningRequest>(extraBufferCapacity = 1)

    // for external classes to subscribe to the flow
    val signingRequest = _signingRequest.asSharedFlow()

    // map challenge to its specific deferred object
    private val activeRequests = mutableMapOf<String, CompletableDeferred<SigningResultEntity>>()

    /** called by features modules
     * This function SUSPENDS the caller's coroutine until [provideResult] is called
     */
    suspend fun requestSignature(rq: SigningRequest): SigningResultEntity {
        // Creating a new wait object for this specific request
        val deferred = CompletableDeferred<SigningResultEntity>()
        // store using the challenge as the key
        synchronized(activeRequests) {
            activeRequests[rq.challenge] = deferred
        }
        // emit the signing request to the flow
        _signingRequest.tryEmit(rq)
        // await until deferred completes in [provideResult]
        return try {
            deferred.await()
        } finally {
            // remove deferred from the map
            synchronized(activeRequests) {
                activeRequests.remove(rq.challenge)
            }
        }
    }

    /**
     * Called by the SigningViewModel when the process is done or user cancel it
     */
    fun provideResult(challenge: String, result: SigningResultEntity) {
        // complete the deferred object with the result
        synchronized(activeRequests) {
            activeRequests[challenge]?.complete(result)
        }
    }
}