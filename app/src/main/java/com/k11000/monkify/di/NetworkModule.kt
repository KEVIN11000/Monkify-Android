package com.k11000.monkify.di

import com.google.gson.Gson
import com.k11000.monkify.BuildConfig
import com.k11000.monkify.data.remote.api.AuthInterceptor
import com.k11000.monkify.data.remote.api.GitHubReleaseApi
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideServerStatusMonitor(): com.k11000.monkify.data.remote.api.ServerStatusMonitor =
        com.k11000.monkify.data.remote.api.ServerStatusMonitor.shared


    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(settingsRepository: SettingsRepository): AuthInterceptor {
        // Mantiene caliente el caché en memoria ante cambios reactivos en DataStore
        networkScope.launch {
            settingsRepository.serverAccessToken.collect { /* actualiza cachedServerToken en SettingsRepository */ }
        }
        return AuthInterceptor(tokenProvider = { settingsRepository.getServerAccessTokenImmediate() })
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("X-Monkify-Token")
            redactHeader("Cookie")
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideMonkifyApi(retrofit: Retrofit): MonkifyApi {
        return retrofit.create(MonkifyApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGitHubReleaseApi(okHttpClient: OkHttpClient, gson: Gson): GitHubReleaseApi {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(GitHubReleaseApi::class.java)
    }
}
