package com.example.withdraw.layer.domain.entity_models

data class TransactionQuotationEntity(
    val id: String,
    val amount: String,
    val fee: String,
    val challenge: String,
    val expiresAt: Long
)
