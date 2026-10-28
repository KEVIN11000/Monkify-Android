package com.k11000.monkify.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: (() -> String?)? = null
) : Interceptor {
    private var explicitToken: String? = null

    fun setToken(token: String?) {
        explicitToken = token
    }

    private fun getCurrentToken(): String? {
        val token = explicitToken ?: tokenProvider?.invoke()
        return token?.trim()?.takeIf { it.isNotEmpty() }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()
        val host = request.url.host

        // SEC-04: Only attach Authorization and X-Monkify-Token to trusted Monkify backend hosts
        if (isTrustedMonkifyHost(host)) {
            getCurrentToken()?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
                requestBuilder.header("X-Monkify-Token", token)
            }
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401 && isTrustedMonkifyHost(host)) {
            // Manejo de refresh futuro o re-autenticación
        }

        return response
    }

    fun isTrustedMonkifyHost(host: String): Boolean {
        val normalized = host.lowercase()
        return normalized.endsWith("monkify.workers.dev") ||
               normalized.endsWith("monkify.tudominio.com") ||
               normalized == "10.0.2.2" ||
               normalized == "localhost" ||
               normalized == "127.0.0.1"
    }
}
