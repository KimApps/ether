package com.example.withdraw.layer.domain.repository

import com.example.withdraw.layer.domain.entity_models.TransactionQuotationEntity
import com.example.withdraw.layer.domain.request_models.GetQuotationRequest
import com.example.withdraw.layer.domain.request_models.SubmitWithdrawRequest

interface WithdrawRepository {
    suspend fun getChallenge(rq: GetQuotationRequest): TransactionQuotationEntity
    suspend fun submitWithdrawal(rq: SubmitWithdrawRequest): Boolean
}