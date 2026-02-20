package com.kimapps.user.layer.data.mappers

import com.kimapps.user.layer.data.dto_models.UserDto
import com.kimapps.user.layer.domain.entity_models.UserEntity
import javax.inject.Inject


/**
 * Mapper class to convert [UserDto] from the data layer to [UserEntity] in the domain layer.
 */
class UserMapper @Inject constructor() {

    /**
     * Maps a [UserDto] to a [UserEntity].
     *
     * @param model The [UserDto] to be mapped.
     * @return The resulting [UserEntity].
     */
    fun map(model: UserDto): UserEntity {
        // Creates a UserEntity from the UserDto
        return UserEntity(
            id = model.id,
            name = model.name,
        )
    }
}
