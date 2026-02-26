package com.kimapps.network

import com.google.gson.Gson
import com.kimapps.network.error.AppNetworkException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkClient @Inject constructor(
    val client: ApiClient,
    val gson: Gson
) {

    // Helper to safely deserialize or throw
    inline fun <reified T> deserialize(body: String?): T {
        if (body.isNullOrBlank()) {
            throw AppNetworkException.AppBodyResponseException("Response body is null or empty")
        }
        return gson.fromJson(body, T::class.java)
            ?: throw AppNetworkException.AppConvertResponseException("Gson returned null for type ${T::class.java.simpleName}")
    }

    suspend inline fun <reified T> get(
        url: String,
        queryParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): T {
        val response = client.get(url, queryParams, headers)
        return deserialize(response.body)
    }

    suspend inline fun <reified T> post(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): T {
        val response = client.post(url, body, headers)
        return deserialize(response.body)
    }

    suspend inline fun <reified T> put(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): T {
        val response = client.put(url, body, headers)
        return deserialize(response.body)
    }

    suspend inline fun <reified T> delete(
        url: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap()
    ): T {
        val response = client.delete(url, body ?: Any(), headers)
        return deserialize(response.body)
    }

    suspend inline fun <reified T> patch(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): T {
        val response = client.patch(url, body, headers)
        return deserialize(response.body)
    }
}
