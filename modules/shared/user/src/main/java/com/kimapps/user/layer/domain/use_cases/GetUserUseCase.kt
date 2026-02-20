package com.kimapps.user.layer.domain.use_cases

import com.kimapps.user.layer.domain.repository.UserRepository
import javax.inject.Inject

class GetUserUseCase @Inject constructor(private val repository: UserRepository) {
}