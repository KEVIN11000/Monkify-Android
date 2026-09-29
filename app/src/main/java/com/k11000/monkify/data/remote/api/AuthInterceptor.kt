package com.k11000.monkify.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    // TODO: Obtain token from DataStore/SharedPreferences
    private var authToken: String? = null

    fun setToken(token: String?) {
        authToken = token
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        
        authToken?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }
        
        val response = chain.proceed(requestBuilder.build())
        
        if (response.code == 401) {
            // TODO: Handle token refresh logic
        }
        
        return response
    }
}
