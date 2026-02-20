package com.kimapps.localstorage.example

import com.kimapps.localstorage.storage.LocalStorageClient
import javax.inject.Inject

/**
 * Example usage of LocalStorageClient with Hilt injection.
 * This demonstrates how feature modules can inject and use LocalStorageClient
 * in their data layer repositories.
 */
class ExampleRepository @Inject constructor(
    private val localStorageClient: LocalStorageClient
) {

    fun saveUserData(userId: String, userName: String) {
        localStorageClient.saveString("user_id", userId)
        localStorageClient.saveString("user_name", userName)
    }

    fun getUserId(): String {
        return localStorageClient.getString("user_id", "")
    }

    fun getUserName(): String {
        return localStorageClient.getString("user_name", "")
    }

    fun savePreferences(theme: String, isNotificationEnabled: Boolean) {
        localStorageClient.saveString("theme", theme)
        localStorageClient.saveBoolean("notifications_enabled", isNotificationEnabled)
    }

    fun clearUserData() {
        localStorageClient.remove("user_id")
        localStorageClient.remove("user_name")
    }
}

