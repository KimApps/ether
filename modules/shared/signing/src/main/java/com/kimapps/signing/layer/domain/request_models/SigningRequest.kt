package com.kimapps.signing.layer.domain.request_models

import android.os.Parcelable
import com.kimapps.signing.layer.domain.enums.OperationType
import kotlinx.parcelize.Parcelize

@Parcelize
data class SigningRequest(
    val challenge: String,
    val operationType: OperationType
) : Parcelable