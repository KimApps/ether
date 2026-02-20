package com.kimapps.user.layer.domain.request_models

/**
 * Represents the request data for getting a user.
 *
 * @property userId The user's identifier.
 */
data class GetUserRequest(
    // The user's identifier.
    val userId: Int,
)
