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
