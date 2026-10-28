package com.k11000.monkify.data.remote

import com.google.gson.Gson
import com.k11000.monkify.data.remote.dto.OpenMeteoForecastResponse
import com.k11000.monkify.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoDtoTest {

    private val gson = Gson()

    @Test
    fun deserializes_open_meteo_json_response_correctly() {
        val json = """
        {
          "latitude": -34.6037,
          "longitude": -58.3816,
          "generationtime_ms": 0.042,
          "utc_offset_seconds": -10800,
          "timezone": "America/Argentina/Buenos_Aires",
          "timezone_abbreviation": "-03",
          "elevation": 25.0,
          "current": {
            "time": "2026-10-27T15:00",
            "interval": 900,
            "temperature_2m": 22.4,
            "weather_code": 1,
            "is_day": 1
          },
          "daily": {
            "time": ["2026-10-27"],
            "weather_code": [1],
            "temperature_2m_max": [25.1],
            "temperature_2m_min": [14.8],
            "sunrise": ["2026-10-27T06:05"],
            "sunset": ["2026-10-27T19:22"]
          }
        }
        """.trimIndent()

        val response = gson.fromJson(json, OpenMeteoForecastResponse::class.java)

        assertNotNull(response)
        assertEquals(-34.6037, response.latitude!!, 0.0001)
        assertEquals(-58.3816, response.longitude!!, 0.0001)
        assertEquals(-10800, response.utcOffsetSeconds)
        assertEquals("America/Argentina/Buenos_Aires", response.timezone)

        val domain = response.toDomain(cityName = "Buenos Aires")

        assertEquals(WeatherCondition.SUNNY, domain.condition)
        assertEquals(1, domain.weatherCode)
        assertEquals(22.4f, domain.temperatureCelsius, 0.01f)
        assertEquals(25.1f, domain.tempMaxCelsius!!, 0.01f)
        assertEquals(14.8f, domain.tempMinCelsius!!, 0.01f)
        assertTrue(domain.isDay)
        assertEquals("Buenos Aires", domain.cityName)
        assertNotNull(domain.sunriseEpochMs)
        assertNotNull(domain.sunsetEpochMs)
        assertTrue(domain.sunsetEpochMs!! > domain.sunriseEpochMs!!)
    }

    @Test
    fun tolerant_handling_of_empty_or_partial_json() {
        val json = """
        {
          "latitude": 0.0,
          "longitude": 0.0
        }
        """.trimIndent()

        val response = gson.fromJson(json, OpenMeteoForecastResponse::class.java)
        val domain = response.toDomain()

        assertEquals(WeatherCondition.SUNNY, domain.condition)
        assertEquals(20.0f, domain.temperatureCelsius, 0.01f)
        assertNull(domain.tempMaxCelsius)
        assertNull(domain.tempMinCelsius)
        assertNull(domain.sunriseEpochMs)
        assertNull(domain.sunsetEpochMs)
    }

    @Test
    fun parse_iso_epoch_handles_invalid_formats_safely() {
        assertNull(OpenMeteoForecastResponse.parseIsoToEpochMs(null, null))
        assertNull(OpenMeteoForecastResponse.parseIsoToEpochMs("", null))
        assertNull(OpenMeteoForecastResponse.parseIsoToEpochMs("not-a-date", null))

        val parsed = OpenMeteoForecastResponse.parseIsoToEpochMs("2026-10-27T06:05", -10800)
        assertNotNull(parsed)
        assertTrue(parsed!! > 0L)
    }
}
