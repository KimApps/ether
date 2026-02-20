package com.kimapps.network

import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple network helper that performs HTTP calls via [ApiClient] and maps JSON
 * responses to Kotlin objects using Gson.
 *
 * This client is agnostic to the underlying HTTP implementation (Retrofit or Ktor).
 * It accepts any [ApiClient] implementation through dependency injection.
 */
@Singleton
class NetworkClient @Inject constructor(
    val client: ApiClient,
    val gson: Gson
) {

    // Perform GET and map the JSON body to T
    suspend inline fun <reified T> get(
        url: String,
        queryParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): T {
        // execute the request through the lower-level client
        val response = client.get(url, queryParams, headers)
        // read response body as string or fail
        val jsonString = response.body
        // map JSON to the requested type T
        return gson.fromJson(jsonString, T::class.java)
    }

    // Perform POST and map response to T
    suspend inline fun <reified T> post(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): T {
        val response = client.post(url, body, headers)
        val jsonString = response.body
        return gson.fromJson(jsonString, T::class.java)
    }

    // Perform PUT and map response to T
    suspend inline fun <reified T> put(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): T {
        val response = client.put(url, body, headers)
        val jsonString = response.body
        return gson.fromJson(jsonString, T::class.java)
    }

    // Perform DELETE and map response to T
    suspend inline fun <reified T> delete(
        url: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap()
    ): T {
        // some servers don't accept bodies for DELETE
        val response = client.delete(url, body ?: Any(), headers)
        val jsonString = response.body
        return gson.fromJson(jsonString, T::class.java)
    }

    // Perform PATCH and map response to T
    suspend inline fun <reified T> patch(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): T {
        val response = client.patch(url, body, headers)
        val jsonString = response.body
        return gson.fromJson(jsonString, T::class.java)
    }
}