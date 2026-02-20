package com.kimapps.network.retrofit

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.QueryMap
import retrofit2.http.Url

/**
 * Retrofit service interface for making network requests.
 * This is the internal Retrofit interface used by [RetrofitApiClientImpl].
 *
 * Note: This doesn't extend ApiClient - it returns Retrofit's Response type.
 * The adapter layer (RetrofitApiClientImpl) converts these to ApiResponse.
 */
interface RetrofitApiService {
    /**
     * Performs a GET request.
     *
     * @param url The endpoint URL for the request.
     * @param queryParams A map of query parameters to be appended to the URL.
     * @param headers A map of custom headers to be added to the request.
     * @return A Retrofit [retrofit2.Response] containing the [okhttp3.ResponseBody].
     */
    @GET
    suspend fun get(
        @Url url: String,
        @QueryMap queryParams: Map<String, String> = emptyMap(),
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    /**
     * Performs a POST request.
     *
     * @param url The endpoint URL for the request.
     * @param body The request body.
     * @param headers A map of custom headers to be added to the request.
     * @return A Retrofit [Response] containing the [ResponseBody].
     */
    @POST
    suspend fun post(
        @Url url: String,
        @Body body: Any,
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    /**
     * Performs a PUT request.
     *
     * @param url The endpoint URL for the request.
     * @param body The request body.
     * @param headers A map of custom headers to be added to the request.
     * @return A Retrofit [Response] containing the [ResponseBody].
     */
    @PUT
    suspend fun put(
        @Url url: String,
        @Body body: Any,
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    /**
     * Performs a DELETE request.
     *
     * @param url The endpoint URL for the request.
     * @param body The request body.
     * @param headers A map of custom headers to be added to the request.
     * @return A Retrofit [Response] containing the [ResponseBody].
     */
    @DELETE
    suspend fun delete(
        @Url url: String,
        @Body body: Any,
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    /**
     * Performs a PATCH request.
     *
     * @param url The endpoint URL for the request.
     * @param body The request body.
     * @param headers A map of custom headers to be added to the request.
     * @return A Retrofit [Response] containing the [ResponseBody].
     */
    @PATCH
    suspend fun patch(
        @Url url: String,
        @Body body: Any,
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Response<ResponseBody>
}