package com.kimapps.network.retrofit

import com.kimapps.network.ApiClient
import com.kimapps.network.error.AppNetworkException
import com.kimapps.network.models.ApiResponse
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper implementation that adapts Retrofit's responses to [ApiResponse].
 * This makes Retrofit independent from the ApiClient interface contract.
 *
 * Throws [AppNetworkException] on failed requests for consistent error handling.
 *
 * @property retrofitService The Retrofit service interface
 */
@Singleton
class RetrofitApiClientImpl @Inject constructor(
    private val retrofitService: RetrofitApiService
) : ApiClient {

    override suspend fun get(
        url: String,
        queryParams: Map<String, String>,
        requestHeaders: Map<String, String>
    ): ApiResponse {
        return try {
            val response = retrofitService.get(url, queryParams, requestHeaders)
            response.toApiResponseOrThrow()
        } catch (e: Throwable) {
            throw RetrofitNetworkException.mapToAppException(e)
        }
    }

    override suspend fun post(
        url: String,
        body: Any,
        requestHeaders: Map<String, String>
    ): ApiResponse {
        return try {
            val response = retrofitService.post(url, body, requestHeaders)
            response.toApiResponseOrThrow()
        } catch (e: Throwable) {
            throw RetrofitNetworkException.mapToAppException(e)
        }
    }

    override suspend fun put(
        url: String,
        body: Any,
        requestHeaders: Map<String, String>
    ): ApiResponse {
        return try {
            val response = retrofitService.put(url, body, requestHeaders)
            response.toApiResponseOrThrow()
        } catch (e: Throwable) {
            throw RetrofitNetworkException.mapToAppException(e)
        }
    }

    override suspend fun delete(
        url: String,
        body: Any,
        requestHeaders: Map<String, String>
    ): ApiResponse {
        return try {
            val response = retrofitService.delete(url, body, requestHeaders)
            response.toApiResponseOrThrow()
        } catch (e: Throwable) {
            throw RetrofitNetworkException.mapToAppException(e)
        }
    }

    override suspend fun patch(
        url: String,
        body: Any,
        requestHeaders: Map<String, String>
    ): ApiResponse {
        return try {
            val response = retrofitService.patch(url, body, requestHeaders)
            response.toApiResponseOrThrow()
        } catch (e: Throwable) {
            throw RetrofitNetworkException.mapToAppException(e)
        }
    }

    /**
     * Converts Retrofit's [Response] to our common [ApiResponse].
     * Throws [AppNetworkException] if the response is not successful.
     */
    private fun Response<ResponseBody>.toApiResponseOrThrow(): ApiResponse {
        if (!isSuccessful) {
            // Map failed response to AppNetworkException and throw
            throw RetrofitNetworkException.mapResponseToAppException(this)
        }

        val bodyString = body()?.string()
        val headerMap = headers().toMap()

        return ApiResponse.success(
            code = code(),
            body = bodyString,
            headers = headerMap
        )
    }

    /**
     * Converts Retrofit headers to a Map.
     */
    private fun okhttp3.Headers.toMap(): Map<String, String> {
        return names().associateWith { name ->
            get(name) ?: ""
        }
    }
}

