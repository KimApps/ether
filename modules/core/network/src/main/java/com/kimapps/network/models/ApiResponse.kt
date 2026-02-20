package com.kimapps.network.models

/**
 * Common HTTP response wrapper that's independent of Retrofit or Ktor.
 * Both implementations will convert their responses to this format.
 *
 * @property code HTTP status code
 * @property body Response body as string
 * @property headers Response headers
 * @property isSuccessful Whether the request was successful (2xx status code)
 */
data class ApiResponse(
    val code: Int,
    val body: String?,
    val headers: Map<String, String>,
    val isSuccessful: Boolean
) {
    companion object {
        /**
         * Creates a successful response.
         */
        fun success(
            code: Int = 200,
            body: String?,
            headers: Map<String, String> = emptyMap()
        ): ApiResponse {
            return ApiResponse(
                code = code,
                body = body,
                headers = headers,
                isSuccessful = true
            )
        }

        /**
         * Creates an error response.
         */
        fun error(
            code: Int,
            body: String?,
            headers: Map<String, String> = emptyMap()
        ): ApiResponse {
            return ApiResponse(
                code = code,
                body = body,
                headers = headers,
                isSuccessful = false
            )
        }
    }
}

