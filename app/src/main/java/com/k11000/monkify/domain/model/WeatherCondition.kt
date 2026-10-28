package com.k11000.monkify.domain.model

import java.util.Calendar

/**
 * Categorías meteorológicas universales normalizadas a partir de los códigos WMO (OMM).
 */
enum class WeatherCondition {
    SUNNY,
    CLOUDY,
    HAZY,
    RAINY,
    SNOWY;

    val displayName: String
        get() = when (this) {
            SUNNY -> "Despejado"
            CLOUDY -> "Nublado"
            HAZY -> "Neblina"
            RAINY -> "Lluvia"
            SNOWY -> "Nieve"
        }

    val assetTag: String
        get() = when (this) {
            SUNNY -> "sunny"
            CLOUDY -> "cloudy"
            HAZY -> "hazy"
            RAINY -> "rainy"
            SNOWY -> "snowy"
        }

    companion object {
        fun fromWmoCode(code: Int): WeatherCondition = when (code) {
            0, 1 -> SUNNY
            2, 3 -> CLOUDY
            45, 48 -> HAZY
            71, 73, 75, 77, 85, 86 -> SNOWY
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99 -> RAINY
            else -> CLOUDY
        }
    }
}

/**
 * Franjas horarias del ciclo Zen (correspondientes a los assets oficiales de Froggy).
 */
enum class ZenDayPart {
    DAWN,    // Morning / Amanecer
    DAY,     // Día pleno
    SUNSET,  // Atardecer / Crepúsculo
    NIGHT;   // Noche

    val assetTag: String
        get() = when (this) {
            DAWN -> "morning"
            DAY -> "day"
            SUNSET -> "sunset"
            NIGHT -> "night"
        }
}

/**
 * Escenarios ambientales para Froggy extraídos de Google Weather.
 */
enum class FroggyScenery(val prefix: String) {
    FIELDS("fields"),
    HILL("hill"),
    MUSHROOM("mushroom");

    companion object {
        fun fromPrefix(prefix: String?): FroggyScenery = when (prefix?.lowercase()) {
            "hill" -> HILL
            "mushroom" -> MUSHROOM
            else -> FIELDS
        }
    }
}

/**
 * Estado del clima actual validado y normalizado para Monkify Zen Engine.
 */
data class CurrentWeather(
    val condition: WeatherCondition,
    val weatherCode: Int,
    val temperatureCelsius: Float,
    val tempMaxCelsius: Float? = null,
    val tempMinCelsius: Float? = null,
    val isDay: Boolean = true,
    val sunriseEpochMs: Long? = null,
    val sunsetEpochMs: Long? = null,
    val fetchedAtEpochMs: Long = System.currentTimeMillis(),
    val cityName: String? = null
) {
    val isStale: Boolean
        get() = (System.currentTimeMillis() - fetchedAtEpochMs) > (2 * 60 * 60 * 1000L) // TTL 2 horas

    val temperatureSummary: String
        get() {
            val current = "${temperatureCelsius.toInt()}°"
            return if (tempMaxCelsius != null && tempMinCelsius != null) {
                "$current · ↑${tempMaxCelsius.toInt()}° ↓${tempMinCelsius.toInt()}°"
            } else {
                current
            }
        }

    /**
     * Resuelve la franja horaria precisa según los cálculos astronómicos de salida y puesta del sol,
     * o cae elegantemente a la hora local del dispositivo si no hay datos de coordenadas solares.
     */
    fun resolveDayPart(nowEpochMs: Long = System.currentTimeMillis()): ZenDayPart {
        if (sunriseEpochMs != null && sunsetEpochMs != null && sunsetEpochMs > sunriseEpochMs) {
            val morningEnd = sunriseEpochMs + (2.5 * 60 * 60 * 1000L).toLong() // Amanecer + 2h 30m
            val sunsetStart = sunsetEpochMs - (1 * 60 * 60 * 1000L)            // Puesta - 1h
            val sunsetEnd = sunsetEpochMs + (30 * 60 * 1000L)                  // Puesta + 30m

            return when {
                nowEpochMs < sunriseEpochMs -> ZenDayPart.NIGHT
                nowEpochMs < morningEnd -> ZenDayPart.DAWN
                nowEpochMs < sunsetStart -> ZenDayPart.DAY
                nowEpochMs < sunsetEnd -> ZenDayPart.SUNSET
                else -> ZenDayPart.NIGHT
            }
        }

        // Fallback por reloj del sistema si no hay datos astronómicos
        val cal = Calendar.getInstance().apply { timeInMillis = nowEpochMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..8 -> ZenDayPart.DAWN
            in 9..17 -> ZenDayPart.DAY
            in 18..20 -> ZenDayPart.SUNSET
            else -> ZenDayPart.NIGHT
        }
    }
}
