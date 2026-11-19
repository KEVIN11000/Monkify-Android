package com.k11000.monkify.data.remote.api

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import java.lang.reflect.Modifier

class RetrofitContractTest {

    private val httpAnnotations = listOf(
        GET::class.java,
        POST::class.java,
        PUT::class.java,
        PATCH::class.java,
        DELETE::class.java,
        HEAD::class.java,
        HTTP::class.java
    )

    @Test
    fun `all Retrofit endpoint methods are abstract without JVM default implementations`() {
        val apiInterfaces = listOf(
            MonkifyApi::class.java,
            GitHubReleaseApi::class.java
        )

        apiInterfaces.forEach { apiInterface ->
            val endpointMethods = apiInterface.declaredMethods.filter { method ->
                httpAnnotations.any { annotation -> method.isAnnotationPresent(annotation) }
            }
            assertTrue("${apiInterface.simpleName} must declare HTTP endpoint methods", endpointMethods.isNotEmpty())

            endpointMethods.forEach { method ->
                assertFalse(
                    "${apiInterface.simpleName}.${method.name} must not have a Kotlin/JVM default body (= ...); Retrofit skips default methods on API 26+",
                    method.isDefault
                )
                assertTrue(
                    "${apiInterface.simpleName}.${method.name} must be abstract",
                    Modifier.isAbstract(method.modifiers)
                )
            }
        }
    }

    @Test
    fun `retrofit eagerly validates MonkifyApi and GitHubReleaseApi without errors`() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://monkify.example.com/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .validateEagerly(true)
            .build()

        retrofit.create(MonkifyApi::class.java)
        retrofit.create(GitHubReleaseApi::class.java)
    }
}
