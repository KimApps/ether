package com.kimapps.signing.layer.domain.entity_models


// This sealed class represents all possible outcomes of the signing flow.

sealed class SigningResultEntity {
    // Represents a successful signing result.

    data class Signed(val signature: String) : SigningResultEntity()

    // Represents a cancellation of the signing process.

    data object Cancelled : SigningResultEntity()

    // Represents an error during the signing process.

    data class Error(val message: String) : SigningResultEntity()
}
