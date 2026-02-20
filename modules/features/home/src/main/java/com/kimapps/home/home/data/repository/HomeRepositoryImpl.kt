package com.kimapps.home.home.data.repository

import com.kimapps.home.home.data.data_sources.HomeLocalDS
import com.kimapps.home.home.data.data_sources.HomeRemoteDS
import com.kimapps.home.home.domain.repository.HomeRepository
import javax.inject.Inject

/**
 * Implementation of [HomeRepository].
 */
class HomeRepositoryImpl @Inject constructor(
    private val local: HomeLocalDS,
    private val remote: HomeRemoteDS,
) : HomeRepository {

}