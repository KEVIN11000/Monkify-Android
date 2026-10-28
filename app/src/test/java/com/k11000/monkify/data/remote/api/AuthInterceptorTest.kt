package com.k11000.monkify.data.remote.api

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class AuthInterceptorTest {

    private class FakeChain(private val initialRequest: Request) : Interceptor.Chain {
        var proceedRequest: Request? = null

        override fun request(): Request = initialRequest

        override fun proceed(request: Request): Response {
            proceedRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        override fun connection(): Connection? = null
        override fun call(): Call = error("unused")
        override fun connectTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }

    @Test
    fun intercept_adds_token_to_monkify_workers_host() {
        val interceptor = AuthInterceptor()
        interceptor.setToken("secret_token_123")

        val chain = FakeChain(
            Request.Builder()
                .url("https://monkify-backend.monkify.workers.dev/api/v1/tracks")
                .build()
        )

        val response = interceptor.intercept(chain)
        assertEquals(200, response.code)

        val processed = chain.proceedRequest
        assertEquals("Bearer secret_token_123", processed?.header("Authorization"))
        assertEquals("secret_token_123", processed?.header("X-Monkify-Token"))
    }

    @Test
    fun intercept_does_not_add_token_to_github_api() {
        val interceptor = AuthInterceptor()
        interceptor.setToken("secret_token_123")

        val chain = FakeChain(
            Request.Builder()
                .url("https://api.github.com/repos/KEVIN11000/Monkify-Android/releases/latest")
                .build()
        )

        interceptor.intercept(chain)

        val processed = chain.proceedRequest
        assertNull(processed?.header("Authorization"))
        assertNull(processed?.header("X-Monkify-Token"))
    }

    @Test
    fun intercept_does_not_add_token_to_open_meteo_api() {
        val interceptor = AuthInterceptor()
        interceptor.setToken("secret_token_123")

        val chain = FakeChain(
            Request.Builder()
                .url("https://api.open-meteo.com/v1/forecast?latitude=0&longitude=0")
                .build()
        )

        interceptor.intercept(chain)

        val processed = chain.proceedRequest
        assertNull(processed?.header("Authorization"))
        assertNull(processed?.header("X-Monkify-Token"))
    }

    @Test
    fun intercept_does_not_add_headers_when_token_is_blank() {
        val interceptor = AuthInterceptor()
        interceptor.setToken("   ")

        val chain = FakeChain(
            Request.Builder()
                .url("https://monkify-backend.monkify.workers.dev/api/v1/tracks")
                .build()
        )

        interceptor.intercept(chain)

        val processed = chain.proceedRequest
        assertNull(processed?.header("Authorization"))
        assertNull(processed?.header("X-Monkify-Token"))
    }

    @Test
    fun token_provider_dynamically_provides_token() {
        var dynamicToken: String? = "initial_token"
        val interceptor = AuthInterceptor(tokenProvider = { dynamicToken })

        val chain1 = FakeChain(
            Request.Builder().url("https://monkify-backend.monkify.workers.dev/api/v1/tracks").build()
        )
        interceptor.intercept(chain1)
        assertEquals("Bearer initial_token", chain1.proceedRequest?.header("Authorization"))

        dynamicToken = "updated_token_999"

        val chain2 = FakeChain(
            Request.Builder().url("https://monkify-backend.monkify.workers.dev/api/v1/tracks").build()
        )
        interceptor.intercept(chain2)
        assertEquals("Bearer updated_token_999", chain2.proceedRequest?.header("Authorization"))
    }

    @Test
    fun is_trusted_monkify_host_validation() {
        val interceptor = AuthInterceptor()
        assertTrue(interceptor.isTrustedMonkifyHost("monkify-backend.monkify.workers.dev"))
        assertTrue(interceptor.isTrustedMonkifyHost("api.monkify.tudominio.com"))
        assertTrue(interceptor.isTrustedMonkifyHost("10.0.2.2"))
        assertTrue(interceptor.isTrustedMonkifyHost("localhost"))
        assertTrue(interceptor.isTrustedMonkifyHost("127.0.0.1"))

        assertFalse(interceptor.isTrustedMonkifyHost("api.github.com"))
        assertFalse(interceptor.isTrustedMonkifyHost("api.open-meteo.com"))
        assertFalse(interceptor.isTrustedMonkifyHost("google.com"))
        assertFalse(interceptor.isTrustedMonkifyHost("evil-monkify.workers.dev.attacker.com"))
    }
}
