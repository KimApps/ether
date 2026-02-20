package com.kimapps.user.layer.data.data_sources

import com.kimapps.localstorage.storage.LocalStorageClient
import javax.inject.Inject

class UserLocalDS @Inject constructor(private val client: LocalStorageClient) {
}