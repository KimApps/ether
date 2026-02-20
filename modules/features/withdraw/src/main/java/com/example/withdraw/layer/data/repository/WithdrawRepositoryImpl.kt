package com.example.withdraw.layer.data.repository

import com.example.withdraw.layer.data.data_sources.WithdrawRemoteDS
import com.example.withdraw.layer.data.mappers.TransactionQuotationMapper
import com.example.withdraw.layer.domain.entity_models.TransactionQuotationEntity
import com.example.withdraw.layer.domain.repository.WithdrawRepository
import com.example.withdraw.layer.domain.request_models.GetQuotationRequest
import com.example.withdraw.layer.domain.request_models.SubmitWithdrawRequest
import javax.inject.Inject

class WithdrawRepositoryImpl @Inject constructor(
    private val remote: WithdrawRemoteDS,
    private val transactionQuotationMapper: TransactionQuotationMapper
) : WithdrawRepository {
    override suspend fun getChallenge(rq: GetQuotationRequest): TransactionQuotationEntity {
        val result = remote.getChallenge(rq)
        return transactionQuotationMapper.map(result)
    }

    override suspend fun submitWithdrawal(rq: SubmitWithdrawRequest): Boolean {
        val result = remote.submitWithdrawal(rq)
        return result
    }
}