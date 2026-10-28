package com.k11000.monkify.data.remote.api

import com.google.gson.Gson
import com.k11000.monkify.data.remote.dto.OpenMeteoForecastResponse
import com.k11000.monkify.domain.model.CurrentWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class OpenMeteoRemoteDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    }

    open suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        cityName: String? = null
    ): Result<CurrentWeather> = withContext(Dispatchers.IO) {
        try {
            val url = HttpUrl.Builder()
                .scheme("https")
                .host("api.open-meteo.com")
                .addPathSegments("v1/forecast")
                .addQueryParameter("latitude", String.format(Locale.US, "%.4f", latitude))
                .addQueryParameter("longitude", String.format(Locale.US, "%.4f", longitude))
                .addQueryParameter("current", "weather_code,temperature_2m,is_day")
                .addQueryParameter("daily", "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset")
                .addQueryParameter("timezone", "auto")
                .addQueryParameter("forecast_days", "1")
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Open-Meteo respondió con código HTTP ${response.code}")
                    )
                }

                val bodyString = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Cuerpo de respuesta vacío"))

                val parsed = gson.fromJson(bodyString, OpenMeteoForecastResponse::class.java)
                val domainWeather = parsed.toDomain(cityName)
                Result.success(domainWeather)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
