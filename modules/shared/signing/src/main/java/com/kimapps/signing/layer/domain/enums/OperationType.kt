package com.kimapps.signing.layer.domain.enums

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class OperationType : Parcelable {
    WITHDRAWAL,
    TRANSFER,
    SWAP
}