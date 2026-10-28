# Contrato de Interfaz: Weather API & Local Weather Store

**Feature:** `v043-rive-weather-zen`  
**Módulo:** `domain/weather` & `data/weather`

---

## 1. Contrato Externo: Open-Meteo Forecast Endpoint

### 1.1 Solicitud HTTP
* **Método:** `GET`
* **URL:** `https://api.open-meteo.com/v1/forecast`
* **Parámetros de Consulta:**
  * `latitude` (Float, ej. `-34.6037`): Latitud geográfica.
  * `longitude` (Float, ej. `-58.3816`): Longitud geográfica.
  * `current` (String): `weather_code,temperature_2m,is_day`
  * `daily` (String): `weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset`
  * `timezone` (String): `auto` (Calcula la zona horaria del meridiano correspondiente).
  * `forecast_days` (Int): `1`

### 1.2 Respuesta HTTP Exitosa (`200 OK`)
```json
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
```

---

## 2. Contratos de Dominio (Kotlin)

### 2.1 Modelos y Tipos
```kotlin
package com.k11000.monkify.domain.model

import java.time.LocalDateTime

enum class WeatherCondition {
    SUNNY,
    CLOUDY,
    HAZY,
    RAINY,
    SNOWY;

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

enum class ZenDayPart {
    MORNING,
    DAY,
    SUNSET,
    NIGHT
}

data class CurrentWeather(
    val condition: WeatherCondition,
    val weatherCode: Int,
    val temperatureCelsius: Float,
    val tempMaxCelsius: Float?,
    val tempMinCelsius: Float?,
    val isDay: Boolean,
    val sunrise: LocalDateTime?,
    val sunset: LocalDateTime?,
    val fetchedAtEpochMs: Long,
    val cityName: String? = null
) {
    val isStale: Boolean
        get() = (System.currentTimeMillis() - fetchedAtEpochMs) > (2 * 60 * 60 * 1000L) // 2 horas
}
```

### 2.2 Contrato del Repositorio
```kotlin
package com.k11000.monkify.domain.repository

import com.k11000.monkify.domain.model.CurrentWeather
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    /**
     * Retorna el flujo reactivo del clima actual cacheado.
     */
    fun getCurrentWeatherFlow(): Flow<CurrentWeather?>

    /**
     * Fuerza o verifica la actualización del clima si ha expirado el TTL (2h).
     * Si no hay ubicación o permisos, utiliza la última conocida o un fallback seguro.
     */
    suspend fun refreshWeatherIfNeeded(force: Boolean = false): Result<CurrentWeather>

    /**
     * Actualiza la ubicación fija manual si el usuario prefiere no usar GPS.
     */
    suspend fun setManualLocation(latitude: Double, longitude: Double, cityName: String)
}
```

---

## 3. Contrato de Almacenamiento Local (DataStore / SharedPreferences)
* Claves de persistencia:
  * `weather_condition`: String (`SUNNY`, `CLOUDY`, `HAZY`, `RAINY`, `SNOWY`)
  * `weather_code`: Int
  * `weather_temp`: Float
  * `weather_temp_max`: Float
  * `weather_temp_min`: Float
  * `weather_is_day`: Boolean
  * `weather_sunrise_iso`: String
  * `weather_sunset_iso`: String
  * `weather_fetched_at`: Long
  * `weather_city_name`: String
