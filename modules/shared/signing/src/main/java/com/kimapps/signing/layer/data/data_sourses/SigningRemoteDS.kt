package com.kimapps.signing.layer.data.data_sourses

import com.kimapps.signing.layer.data.dto_models.SigningResultDto
import com.kimapps.signing.layer.domain.request_models.SigningRequest
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    suspend fun signWithWallet(challenge: String): SigningResultDto = suspendCancellableCoroutine { continuation ->
        // 1. Find the active session to respond to
        val activeSession = WalletKit.getListOfActiveSessions().firstOrNull()
            ?: run {
                continuation.resumeWithException(IllegalStateException("No active session. Please connect first."))
                return@suspendCancellableCoroutine
            }

        val mockSignature = "0x_walletkit_signed_${challenge.take(8)}"

        // 2. Correct Reown Params (Wallet.Params, NOT com.google...)
        val response = Wallet.Params.SessionRequestResponse(
            sessionTopic = activeSession.topic,
            jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(
                id = 1L, // Note: In a real app, this matches the SessionRequest ID
                result = mockSignature
            )
        )

        // 3. Respond via WalletKit
        WalletKit.respondSessionRequest(
            params = response,
            onSuccess = {
                continuation.resume(SigningResultDto(mockSignature))
            },
            onError = { error ->
                continuation.resumeWithException(error.throwable)
            }
        )
    }
}