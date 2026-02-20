package com.kimapps.user.layer.data.dto_models

/**
 * Represents the user data model as it is structured in the data layer.
 *
 * @property id The unique identifier for the user.
 * @property name The name of the user.
 */
data class UserDto(
    // The unique identifier for the user.
    val id: Int?,
    // The name of the user.
    val name: String?,
)
