package com.kimapps.signing.layer.domain.entity_models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// This sealed class represents all possible outcomes of the signing flow.
@Parcelize
sealed class SigningResultEntity : Parcelable {
    // Represents a successful signing result.
    @Parcelize
    data class Signed(val signature: String) : SigningResultEntity()

    // Represents a cancellation of the signing process.
    @Parcelize
    data object Cancelled : SigningResultEntity()

    // Represents an error during the signing process.
    @Parcelize
    data class Error(val message: String) : SigningResultEntity()
}
