package com.kimapps.network.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.kimapps.network.ApiClient
import com.kimapps.network.constants.ApiEndpoint
import com.kimapps.network.constants.NetworkConstant
import com.kimapps.network.interceptors.AuthInterceptor
import com.kimapps.network.interceptors.RefreshTokenInterceptor
import com.kimapps.network.ktor.KtorApiClient
import com.kimapps.network.retrofit.RetrofitApiClientImpl
import com.kimapps.network.retrofit.RetrofitApiService
import com.kimapps.network.storage.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.kimapps.network.BuildConfig

/**
 * Dagger Hilt module for providing network-related dependencies.
 * This module is installed in the [SingletonComponent], making the provided dependencies
 * available as singletons throughout the application.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /**
     * Provides a singleton TokenManager instance.
     * TokenManager uses DataStore for secure, asynchronous token storage.
     *
     * @param dataStore DataStore instance for persistence
     * @return TokenManager instance
     */
    @Provides
    @Singleton
    fun provideTokenManager(dataStore: DataStore<Preferences>): TokenManager {
        return TokenManager(dataStore)
    }

    /**
     * Provides RefreshTokenInterceptor for automatic token refresh on 401 errors.
     * Configure the refreshTokenPath based on your API's refresh endpoint.
     *
     * @param tokenManager Manages token storage and retrieval
     * @param gson For parsing JSON responses
     * @return RefreshTokenInterceptor instance
     */
    @Provides
    @Singleton
    fun provideRefreshTokenInterceptor(
        tokenManager: TokenManager,
        gson: Gson
    ): RefreshTokenInterceptor {
        return RefreshTokenInterceptor(
            tokenManager = tokenManager,
            gson = gson
        )
    }

    /**
     * Provides a singleton instance of [OkHttpClient].
     * The client is configured with:
     * - AuthInterceptor: Automatically adds "Authorization: Bearer {token}" header to requests
     * - RefreshTokenInterceptor: Automatically refreshes expired tokens on 401 errors
     * - LoggingInterceptor: Logs request and response bodies for debugging
     * - Timeouts: Connection, read, and write timeouts
     *
     * @param tokenManager Provides the authentication token for requests
     * @param refreshTokenInterceptor Handles automatic token refresh
     * @return A configured [OkHttpClient] instance.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenManager: TokenManager,
        refreshTokenInterceptor: RefreshTokenInterceptor
    ): OkHttpClient {
        // Create auth interceptor that retrieves token dynamically on each request
        // Using getTokenBlocking() since interceptors cannot suspend functions
        val authInterceptor = AuthInterceptor(
            tokenProvider = { tokenManager.getTokenBlocking() }
        )

        // Create a logging interceptor to view network traffic in the logcat.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // Build the OkHttpClient with interceptors and timeouts.
        // Order matters: Auth first, then refresh, then logging
        return OkHttpClient.Builder()
            // TODO authInterceptor and refreshTokenInterceptor need to be tested
            //.addInterceptor(authInterceptor)            // 1. Add Authorization header
            //.addInterceptor(refreshTokenInterceptor)    // 2. Handle 401 & refresh tokens
            .addInterceptor(loggingInterceptor)         // 3. Log requests/responses
            .connectTimeout(NetworkConstant.timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(NetworkConstant.timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(NetworkConstant.timeoutSeconds, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provides a singleton instance of [Retrofit].
     * The Retrofit instance is configured with a base URL, the [OkHttpClient] provided by
     * [provideOkHttpClient], and a [GsonConverterFactory] for JSON serialization/deserialization.
     *
     * @param okHttpClient The [OkHttpClient] to be used by Retrofit.
     * @return A configured [Retrofit] instance.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // Build the Retrofit instance with the base URL, OkHttpClient, and Gson converter.
        return Retrofit.Builder()
            .baseUrl(ApiEndpoint.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides a singleton instance of [RetrofitApiService] (Retrofit interface).
     * This method creates an implementation of the [RetrofitApiService] interface using the provided [Retrofit] instance.
     *
     * @param retrofit The [Retrofit] instance to use for creating the API service.
     * @return An instance of [RetrofitApiService].
     */
    @Provides
    @Singleton
    fun provideRetrofitApiService(retrofit: Retrofit): RetrofitApiService {
        // Create the API service from the Retrofit instance.
        return retrofit.create(RetrofitApiService::class.java)
    }

    /**
     * Provides a singleton instance of [RetrofitApiClientImpl].
     * This wraps the Retrofit service and adapts it to the [ApiClient] interface.
     *
     * @param retrofitService The Retrofit service interface
     * @return An instance of [RetrofitApiClientImpl] that implements [ApiClient]
     */
    @Provides
    @Singleton
    fun provideRetrofitApiClient(retrofitService: RetrofitApiService): RetrofitApiClientImpl {
        return RetrofitApiClientImpl(retrofitService)
    }

    /**
     * Provides a singleton instance of [KtorApiClient].
     *
     * @param httpClient Ktor's HttpClient instance
     * @param gson Gson instance for JSON serialization
     * @return An instance of [KtorApiClient]
     */
    @Provides
    @Singleton
    fun provideKtorApiClient(
        httpClient: HttpClient,
        gson: Gson
    ): KtorApiClient {
        return KtorApiClient(httpClient, gson)
    }

    /**
     * Provides the main [ApiClient] implementation.
     *
     * **SWITCH BETWEEN RETROFIT AND KTOR HERE**
     *
     * Current: Using Ktor
     * To switch to Retrofit: Change the parameter from `ktorApiClient` to `retrofitApiClient`
     *
     * @param retrofitApiClient Retrofit implementation (available)
     * @param ktorApiClient Ktor implementation (currently active)
     * @return The active [ApiClient] implementation
     */
    @Provides
    @Singleton
    fun provideApiClient(
        retrofitApiClient: RetrofitApiClientImpl,
        ktorApiClient: KtorApiClient
    ): ApiClient {
        // Switch implementation here:
        // return retrofitApiClient  // For Retrofit
        return ktorApiClient  // For Ktor (current)
    }

    /**
     * Provides a singleton instance of [Gson] for JSON serialization and deserialization.
     *
     * @return A [Gson] instance.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        // Return a new Gson instance.
        return Gson()
    }

    /**
     * Provides a singleton instance of [HttpClient] (Ktor).
     * The client is configured with:
     * - DefaultRequest: Sets base URL (like Retrofit's baseUrl) and common headers
     * - ContentNegotiation: JSON serialization/deserialization
     * - Auth: Bearer token authentication with automatic token refresh
     * - Logging: Request/response logging for debugging
     * - HttpTimeout: Connection and request timeouts
     *
     * **Important**: The base URL is configured using the DefaultRequest plugin.
     * This allows data sources to use relative URLs (e.g., "character/2")
     * which will be resolved to full URLs (e.g., "https://rickandmortyapi.com/api/character/2").
     *
     * @param tokenManager Provides the authentication token for requests
     * @param refreshTokenInterceptor Handles automatic token refresh
     * @return A configured [HttpClient] instance.
     */
    @Provides
    @Singleton
    fun provideKtorHttpClient(
        tokenManager: TokenManager,
        refreshTokenInterceptor: RefreshTokenInterceptor
    ): HttpClient {
        return HttpClient(Android) {
            // Default request configuration (base URL, common headers)
            install(DefaultRequest) {
                // Set base URL for all requests (like Retrofit's baseUrl)
                url.takeFrom(ApiEndpoint.baseUrl)

                // Set default content type
                header(HttpHeaders.ContentType, ContentType.Application.Json)
            }

            // JSON content negotiation
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                })
            }

            // Request/Response logging
            install(Logging) {
                logger = Logger.ANDROID
                level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
            }

            // Bearer token authentication with automatic refresh
            install(Auth) {
                bearer {
                    // Load tokens for each request
                    loadTokens {
                        val token = tokenManager.getToken()
                        val refreshToken = tokenManager.getRefreshToken()
                        if (token != null) {
                            BearerTokens(token, refreshToken ?: "")
                        } else {
                            null
                        }
                    }

                    // Refresh tokens when receiving 401 Unauthorized
                    refreshTokens {
                        val refreshSuccess = refreshTokenInterceptor.refreshToken()

                        if (refreshSuccess) {
                            // After a successful refresh, get the NEW tokens from storage
                            val newAccessToken = tokenManager.getToken()
                            val newRefreshToken = tokenManager.getRefreshToken()
                            if (newAccessToken != null) {
                                BearerTokens(newAccessToken, newRefreshToken ?: "")
                            } else {
                                null
                            }
                        } else {
                            // Refresh failed, clear everything
                            tokenManager.clearTokens()
                            null
                        }
                    }
                }
            }

            // Connection and request timeouts
            install(HttpTimeout) {
                requestTimeoutMillis = NetworkConstant.timeoutSeconds * 1000L
                connectTimeoutMillis = NetworkConstant.timeoutSeconds * 1000L
                socketTimeoutMillis = NetworkConstant.timeoutSeconds * 1000L
            }
        }
    }
}
