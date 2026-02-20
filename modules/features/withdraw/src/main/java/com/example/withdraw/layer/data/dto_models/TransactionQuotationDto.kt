package com.example.withdraw.layer.data.dto_models

data class TransactionQuotationDto(
    val id: String,
    val amount: String,
    val fee: String,
    val challenge: String,
    val expiresAt: Long
)

