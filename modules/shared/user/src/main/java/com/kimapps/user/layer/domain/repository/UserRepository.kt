package com.kimapps.user.layer.domain.repository

import com.kimapps.user.layer.domain.entity_models.UserEntity
import com.kimapps.user.layer.domain.request_models.GetUserRequest

/**
 * Repository interface for home feature.
 */
interface UserRepository {
    /**
     * Get user by id.
     */
    suspend fun getUser(rq: GetUserRequest): UserEntity
}