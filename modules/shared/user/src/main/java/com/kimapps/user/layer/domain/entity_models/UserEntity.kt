package com.kimapps.user.layer.domain.entity_models

/**
 * Represents a user in the domain layer.
 * This entity contains the essential information about a user.
 *
 * @property id The unique identifier of the user.
 * @property name The name of the user.
 */
data class UserEntity(
    // The unique identifier for the user.
    val id: Int?,
    // The name of the user.
    val name: String?,
)
