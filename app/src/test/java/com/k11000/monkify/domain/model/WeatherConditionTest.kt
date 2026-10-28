package com.k11000.monkify.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class WeatherConditionTest {

    @Test
    fun wmo_codes_map_to_expected_weather_conditions() {
        // Soleado / Despejado
        assertEquals(WeatherCondition.SUNNY, WeatherCondition.fromWmoCode(0))
        assertEquals(WeatherCondition.SUNNY, WeatherCondition.fromWmoCode(1))

        // Nublado
        assertEquals(WeatherCondition.CLOUDY, WeatherCondition.fromWmoCode(2))
        assertEquals(WeatherCondition.CLOUDY, WeatherCondition.fromWmoCode(3))

        // Niebla / Neblina
        assertEquals(WeatherCondition.HAZY, WeatherCondition.fromWmoCode(45))
        assertEquals(WeatherCondition.HAZY, WeatherCondition.fromWmoCode(48))

        // Lluvia / Tormentas
        assertEquals(WeatherCondition.RAINY, WeatherCondition.fromWmoCode(51)) // Drizzle light
        assertEquals(WeatherCondition.RAINY, WeatherCondition.fromWmoCode(61)) // Rain light
        assertEquals(WeatherCondition.RAINY, WeatherCondition.fromWmoCode(65)) // Heavy rain
        assertEquals(WeatherCondition.RAINY, WeatherCondition.fromWmoCode(80)) // Showers
        assertEquals(WeatherCondition.RAINY, WeatherCondition.fromWmoCode(95)) // Thunderstorm
        assertEquals(WeatherCondition.RAINY, WeatherCondition.fromWmoCode(99)) // Thunderstorm with hail

        // Nieve
        assertEquals(WeatherCondition.SNOWY, WeatherCondition.fromWmoCode(71)) // Light snow
        assertEquals(WeatherCondition.SNOWY, WeatherCondition.fromWmoCode(75)) // Heavy snow
        assertEquals(WeatherCondition.SNOWY, WeatherCondition.fromWmoCode(85)) // Snow showers

        // Códigos desconocidos caen en Nublado por defecto seguro
        assertEquals(WeatherCondition.CLOUDY, WeatherCondition.fromWmoCode(999))
    }

    @Test
    fun asset_tags_and_display_names_are_correct() {
        assertEquals("sunny", WeatherCondition.SUNNY.assetTag)
        assertEquals("Despejado", WeatherCondition.SUNNY.displayName)

        assertEquals("cloudy", WeatherCondition.CLOUDY.assetTag)
        assertEquals("Nublado", WeatherCondition.CLOUDY.displayName)

        assertEquals("rainy", WeatherCondition.RAINY.assetTag)
        assertEquals("Lluvia", WeatherCondition.RAINY.displayName)

        assertEquals("snowy", WeatherCondition.SNOWY.assetTag)
        assertEquals("Nieve", WeatherCondition.SNOWY.displayName)

        assertEquals("hazy", WeatherCondition.HAZY.assetTag)
        assertEquals("Neblina", WeatherCondition.HAZY.displayName)

        // DayPart asset tags
        assertEquals("morning", ZenDayPart.DAWN.assetTag)
        assertEquals("day", ZenDayPart.DAY.assetTag)
        assertEquals("sunset", ZenDayPart.SUNSET.assetTag)
        assertEquals("night", ZenDayPart.NIGHT.assetTag)
    }

    @Test
    fun froggy_scenery_resolves_correctly() {
        assertEquals(FroggyScenery.FIELDS, FroggyScenery.fromPrefix("fields"))
        assertEquals(FroggyScenery.HILL, FroggyScenery.fromPrefix("hill"))
        assertEquals(FroggyScenery.MUSHROOM, FroggyScenery.fromPrefix("mushroom"))
        assertEquals(FroggyScenery.FIELDS, FroggyScenery.fromPrefix("unknown_or_null"))
    }

    @Test
    fun current_weather_ttl_expiration_works() {
        val now = System.currentTimeMillis()
        val freshWeather = CurrentWeather(
            condition = WeatherCondition.SUNNY,
            weatherCode = 0,
            temperatureCelsius = 24.5f,
            tempMaxCelsius = 28f,
            tempMinCelsius = 16f,
            fetchedAtEpochMs = now - (30 * 60 * 1000L) // 30 minutos de antigüedad
        )
        assertFalse(freshWeather.isStale)
        assertEquals("24° · ↑28° ↓16°", freshWeather.temperatureSummary)

        val staleWeather = freshWeather.copy(
            fetchedAtEpochMs = now - (3 * 60 * 60 * 1000L) // 3 horas de antigüedad
        )
        assertTrue(staleWeather.isStale)
    }

    @Test
    fun solar_calculations_resolve_precise_day_part() {
        val sunrise = 1700000000000L // T0
        val sunset = sunrise + (12 * 60 * 60 * 1000L) // T0 + 12h

        val weather = CurrentWeather(
            condition = WeatherCondition.SUNNY,
            weatherCode = 0,
            temperatureCelsius = 20f,
            sunriseEpochMs = sunrise,
            sunsetEpochMs = sunset
        )

        // Antes del amanecer -> Noche
        assertEquals(ZenDayPart.NIGHT, weather.resolveDayPart(sunrise - 1000L))

        // 1 hora después del amanecer -> Amanecer / Mañana
        assertEquals(ZenDayPart.DAWN, weather.resolveDayPart(sunrise + (1 * 60 * 60 * 1000L)))

        // Mediodía (6 horas después del amanecer) -> Día pleno
        assertEquals(ZenDayPart.DAY, weather.resolveDayPart(sunrise + (6 * 60 * 60 * 1000L)))

        // 30 minutos antes de la puesta de sol -> Atardecer / Crepúsculo
        assertEquals(ZenDayPart.SUNSET, weather.resolveDayPart(sunset - (30 * 60 * 1000L)))

        // 1 hora después de la puesta de sol -> Noche
        assertEquals(ZenDayPart.NIGHT, weather.resolveDayPart(sunset + (60 * 60 * 1000L)))
    }

    @Test
    fun fallback_calendar_hour_resolves_day_part_without_solar_times() {
        val weatherNoSolar = CurrentWeather(
            condition = WeatherCondition.CLOUDY,
            weatherCode = 2,
            temperatureCelsius = 18f,
            sunriseEpochMs = null,
            sunsetEpochMs = null
        )

        fun createTimestampForHour(hour: Int): Long {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
        }

        assertEquals(ZenDayPart.DAWN, weatherNoSolar.resolveDayPart(createTimestampForHour(7)))
        assertEquals(ZenDayPart.DAY, weatherNoSolar.resolveDayPart(createTimestampForHour(12)))
        assertEquals(ZenDayPart.SUNSET, weatherNoSolar.resolveDayPart(createTimestampForHour(19)))
        assertEquals(ZenDayPart.NIGHT, weatherNoSolar.resolveDayPart(createTimestampForHour(23)))
        assertEquals(ZenDayPart.NIGHT, weatherNoSolar.resolveDayPart(createTimestampForHour(3)))
    }
}
