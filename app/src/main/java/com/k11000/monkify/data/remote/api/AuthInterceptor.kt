package com.k11000.monkify.data.remote.api

import com.k11000.monkify.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.net.URI

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

    fun isTrustedMonkifyHost(host: String, isDebug: Boolean = BuildConfig.DEBUG): Boolean =
        Companion.isTrustedHost(host, isDebug)

    companion object {
        private val configuredHost: String? by lazy {
            runCatching { URI(BuildConfig.BASE_URL).host?.lowercase()?.trim() }.getOrNull()
        }

        private val configuredRootDomain: String? by lazy {
            val host = configuredHost ?: return@lazy null
            val parts = host.split(".")
            if (parts.size >= 3) {
                "." + parts.takeLast(3).joinToString(".")
            } else {
                null
            }
        }

        // INVARIANT: Only attach Bearer/X-Monkify-Token to verified Monkify subdomains. Ref: SYSTEM_MAP #INV-N02
        fun isTrustedHost(host: String, isDebug: Boolean = BuildConfig.DEBUG): Boolean {
            val normalized = host.lowercase().trim()
            if (normalized.isEmpty()) return false
            if (!configuredHost.isNullOrEmpty() && normalized == configuredHost) return true
            if (!configuredRootDomain.isNullOrEmpty() && normalized.endsWith(configuredRootDomain!!)) return true
            if (isDebug && (normalized == "10.0.2.2" || normalized == "localhost" || normalized == "127.0.0.1")) {
                return true
            }
            return normalized == "monkify.tudominio.com" ||
                normalized.endsWith(".monkify.tudominio.com")
        }
    }
}
