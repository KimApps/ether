package com.example.withdraw.layer.data.data_sources

import com.example.withdraw.layer.data.dto_models.TransactionQuotationDto
import com.example.withdraw.layer.domain.request_models.GetQuotationRequest
import com.example.withdraw.layer.domain.request_models.SubmitWithdrawRequest
import com.kimapps.network.NetworkClient
import kotlinx.coroutines.delay
import java.util.Date
import javax.inject.Inject

class WithdrawRemoteDS @Inject constructor(private val client: NetworkClient) {

    suspend fun getChallenge(rq: GetQuotationRequest): TransactionQuotationDto {
        delay(2000)
        val now = System.currentTimeMillis()
        val oneMinute = 1 * 60 * 1000
        val expirationTime = now + oneMinute
        return TransactionQuotationDto(
            id = "1",
            amount = rq.amount.toString(),
            fee = "5",
            challenge = "mock_challenge",
            expiresAt = expirationTime,
        )
    }

    suspend fun submitWithdrawal(rq: SubmitWithdrawRequest): Boolean {
        delay(1000)
        return true
    }

}