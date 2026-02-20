package com.kimapps.user.layer.data.repository

import com.kimapps.user.layer.data.data_sources.UserLocalDS
import com.kimapps.user.layer.data.data_sources.UserRemoteDS
import com.kimapps.user.layer.data.mappers.UserMapper
import com.kimapps.user.layer.domain.entity_models.UserEntity
import com.kimapps.user.layer.domain.repository.UserRepository
import com.kimapps.user.layer.domain.request_models.GetUserRequest
import javax.inject.Inject

/**
 * Implementation of [UserRepository].
 */
class UserRepositoryImpl @Inject constructor(
    private val local: UserLocalDS,
    private val remote: UserRemoteDS,
    private val userMapper: UserMapper
) : UserRepository {
    /**
     * Get user by id.
     */
    override suspend fun getUser(rq: GetUserRequest): UserEntity {
        TODO("Not yet implemented")
    }
}