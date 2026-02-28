package com.kimapps.signing.layer.domain.request_models

import com.kimapps.signing.layer.domain.enums.OperationType

data class SigningRequest(
    val challenge: String,
    val operationType: OperationType
)