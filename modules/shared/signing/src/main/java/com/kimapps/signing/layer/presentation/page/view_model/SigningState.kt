package com.kimapps.signing.layer.presentation.page.view_model

import com.kimapps.signing.layer.domain.enums.OperationType

data class SigningState(
    val challenge: String = "",
    val operationType: OperationType = OperationType.WITHDRAWAL,
    val isLoading: Boolean = false,
    val error: String? = null,
)