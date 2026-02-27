package com.kimapps.network.di

import com.google.gson.Gson
import com.kimapps.localstorage.storage.EncryptedStorage
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
     * Provides a singleton [TokenManager] instance.
     *
     * [TokenManager] persists access and refresh tokens via [EncryptedStorage],
     * which writes AES-256-GCM ciphertext to Jetpack DataStore — ensuring tokens
     * are never stored in plaintext on disk.
     *
     * @param encryptedStorage Encrypted DataStore-backed storage from `core:local-storage`.
     * @return A singleton [TokenManager] instance.
     */
    @Provides
    @Singleton
    fun provideTokenManager(encryptedStorage: EncryptedStorage): TokenManager {
        return TokenManager(encryptedStorage)
    }

    /**
     * Provides a singleton [RefreshTokenInterceptor] for automatic token refresh on 401 responses.
     *
     * When OkHttp receives a 401, this interceptor calls the refresh endpoint, saves the
     * new tokens via [TokenManager], and transparently retries the original request —
     * the caller never needs to handle token expiry manually.
     *
     * @param tokenManager Reads and writes tokens from/to [EncryptedStorage].
     * @param gson Parses the JSON refresh response to extract the new token pair.
     * @return A configured [RefreshTokenInterceptor] instance.
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
     * Provides a singleton [OkHttpClient] used by the Retrofit implementation.
     *
     * Currently only the logging interceptor is active. [AuthInterceptor] and
     * [RefreshTokenInterceptor] are wired up but commented out pending integration
     * testing against a live auth endpoint.
     *
     * Interceptor order when enabled:
     * 1. [AuthInterceptor]          — attaches `Authorization: Bearer <token>` to every request.
     * 2. [RefreshTokenInterceptor]  — intercepts 401 responses, refreshes tokens, retries.
     * 3. [HttpLoggingInterceptor]   — logs full request/response bodies (DEBUG builds only).
     *
     * @param tokenManager Supplies the current access token to [AuthInterceptor].
     * @param refreshTokenInterceptor Handles 401 responses and token refresh.
     * @return A configured [OkHttpClient] instance.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenManager: TokenManager,
        refreshTokenInterceptor: RefreshTokenInterceptor
    ): OkHttpClient {
        // AuthInterceptor reads the token synchronously via getTokenBlocking() because
        // OkHttp interceptors run on a background thread and cannot call suspend functions.
        val authInterceptor = AuthInterceptor(
            tokenProvider = { tokenManager.getTokenBlocking() }
        )

        // Log full request/response bodies in DEBUG; suppress entirely in release.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // TODO: enable authInterceptor and refreshTokenInterceptor once tested
        //       against a live auth endpoint.
        return OkHttpClient.Builder()
            //.addInterceptor(authInterceptor)            // 1. Add Authorization header
            //.addInterceptor(refreshTokenInterceptor)    // 2. Handle 401 & refresh tokens
            .addInterceptor(loggingInterceptor)         // 3. Log requests/responses
            .connectTimeout(NetworkConstant.timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(NetworkConstant.timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(NetworkConstant.timeoutSeconds, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provides a singleton [Retrofit] instance backed by [OkHttpClient].
     *
     * Base URL is read from [ApiEndpoint.baseUrl]. JSON serialization uses
     * [GsonConverterFactory] — the same [Gson] instance provided by [provideGson].
     *
     * @param okHttpClient The [OkHttpClient] configured with interceptors and timeouts.
     * @return A configured [Retrofit] instance.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiEndpoint.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides a singleton [RetrofitApiService] by creating an implementation
     * of the interface from the [Retrofit] instance.
     *
     * @param retrofit The configured [Retrofit] instance.
     * @return A generated [RetrofitApiService] implementation.
     */
    @Provides
    @Singleton
    fun provideRetrofitApiService(retrofit: Retrofit): RetrofitApiService {
        return retrofit.create(RetrofitApiService::class.java)
    }

    /**
     * Provides a singleton [RetrofitApiClientImpl] that wraps [RetrofitApiService]
     * and adapts it to the common [ApiClient] interface.
     *
     * @param retrofitService The generated Retrofit service interface.
     * @return An [ApiClient]-compatible [RetrofitApiClientImpl] instance.
     */
    @Provides
    @Singleton
    fun provideRetrofitApiClient(retrofitService: RetrofitApiService): RetrofitApiClientImpl {
        return RetrofitApiClientImpl(retrofitService)
    }

    /**
     * Provides a singleton [KtorApiClient] that wraps Ktor's [HttpClient]
     * and adapts it to the common [ApiClient] interface.
     *
     * @param httpClient The configured Ktor [HttpClient].
     * @param gson [Gson] instance used for JSON deserialization inside [KtorApiClient].
     * @return An [ApiClient]-compatible [KtorApiClient] instance.
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
     * Binds the active [ApiClient] implementation used throughout the app.
     *
     * **To switch HTTP library, change the return value here — one line, zero call-site changes:**
     * - `return ktorApiClient`      ← Ktor (current)
     * - `return retrofitApiClient`  ← Retrofit
     *
     * Both implementations are fully constructed by Hilt regardless of which one
     * is returned, so the unused one adds no runtime overhead beyond instantiation.
     *
     * @param retrofitApiClient Retrofit-backed implementation (available but inactive).
     * @param ktorApiClient Ktor-backed implementation (currently active).
     * @return The active [ApiClient] implementation.
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
     * Provides a singleton [Gson] instance for JSON serialization and deserialization.
     * Used by [RetrofitApiClientImpl] (via [GsonConverterFactory]) and [KtorApiClient].
     *
     * @return A default [Gson] instance.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    /**
     * Provides a singleton Ktor [HttpClient] configured for production-style use.
     *
     * Plugins installed:
     * - **DefaultRequest** — sets [ApiEndpoint.baseUrl] as the base URL for all requests
     *   and attaches `Content-Type: application/json` by default.
     * - **ContentNegotiation** — kotlinx.serialization JSON with lenient parsing and
     *   unknown-key tolerance, so API additions don't break existing DTOs.
     * - **Logging** — full body logging on DEBUG builds; silent on release.
     * - **Auth (bearer)** — `loadTokens` reads the current token pair from [TokenManager]
     *   before each request; `refreshTokens` delegates to [RefreshTokenInterceptor] on
     *   a 401 response and retries with the new token pair automatically.
     * - **HttpTimeout** — applies [NetworkConstant.timeoutSeconds] to request, connect,
     *   and socket timeouts.
     *
     * @param tokenManager Supplies access and refresh tokens stored in [EncryptedStorage].
     * @param refreshTokenInterceptor Performs the token refresh call on 401 responses.
     * @return A fully configured Ktor [HttpClient] instance.
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
