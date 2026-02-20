package com.example.withdraw.layer.data.mappers

import com.example.withdraw.layer.data.dto_models.TransactionQuotationDto
import com.example.withdraw.layer.domain.entity_models.TransactionQuotationEntity
import javax.inject.Inject

class TransactionQuotationMapper @Inject constructor() {
    fun map(model: TransactionQuotationDto): TransactionQuotationEntity {
        return TransactionQuotationEntity(
            id = model.id,
            amount = model.amount,
            fee = model.fee,
            challenge = model.challenge,
            expiresAt = model.expiresAt,
        )
    }
}