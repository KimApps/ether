package com.kimapps.signing.layer.domain.enums

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class OperationType : Parcelable {
    WITHDRAWAL,
    TRANSFER,
    SWAP;

    companion object {
        // Safely converts a string to an OperationType.
        fun fromString(value: String?): OperationType? {
            if (value == null) return null
            // Use .name which is the string representation of the enum constant.
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }

}