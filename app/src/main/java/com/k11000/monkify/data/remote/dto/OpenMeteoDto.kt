package com.k11000.monkify.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.k11000.monkify.domain.model.CurrentWeather
import com.k11000.monkify.domain.model.WeatherCondition
import java.time.LocalDateTime
import java.time.ZoneOffset

data class OpenMeteoForecastResponse(
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("current") val current: CurrentDto? = null,
    @SerializedName("daily") val daily: DailyDto? = null
) {
    fun toDomain(cityName: String? = null): CurrentWeather {
        val curr = current ?: CurrentDto()
        val code = curr.weatherCode ?: 0
        val condition = WeatherCondition.fromWmoCode(code)
        val temp = curr.temperature2m?.toFloat() ?: 20f
        val isDay = (curr.isDay ?: 1) == 1

        val tempMax = daily?.temperature2mMax?.firstOrNull()?.toFloat()
        val tempMin = daily?.temperature2mMin?.firstOrNull()?.toFloat()

        val sunriseEpoch = parseIsoToEpochMs(daily?.sunrise?.firstOrNull(), utcOffsetSeconds)
        val sunsetEpoch = parseIsoToEpochMs(daily?.sunset?.firstOrNull(), utcOffsetSeconds)

        return CurrentWeather(
            condition = condition,
            weatherCode = code,
            temperatureCelsius = temp,
            tempMaxCelsius = tempMax,
            tempMinCelsius = tempMin,
            isDay = isDay,
            sunriseEpochMs = sunriseEpoch,
            sunsetEpochMs = sunsetEpoch,
            fetchedAtEpochMs = System.currentTimeMillis(),
            cityName = cityName
        )
    }

    companion object {
        fun parseIsoToEpochMs(isoString: String?, utcOffsetSeconds: Int?): Long? {
            if (isoString.isNullOrBlank()) return null
            return try {
                val ldt = LocalDateTime.parse(isoString)
                val offset = if (utcOffsetSeconds != null) {
                    ZoneOffset.ofTotalSeconds(utcOffsetSeconds)
                } else {
                    ZoneOffset.UTC
                }
                ldt.toInstant(offset).toEpochMilli()
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class CurrentDto(
    @SerializedName("time") val time: String? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null,
    @SerializedName("temperature_2m") val temperature2m: Double? = null,
    @SerializedName("is_day") val isDay: Int? = null
)

data class DailyDto(
    @SerializedName("time") val time: List<String>? = null,
    @SerializedName("weather_code") val weatherCode: List<Int>? = null,
    @SerializedName("temperature_2m_max") val temperature2mMax: List<Double>? = null,
    @SerializedName("temperature_2m_min") val temperature2mMin: List<Double>? = null,
    @SerializedName("sunrise") val sunrise: List<String>? = null,
    @SerializedName("sunset") val sunset: List<String>? = null
)
