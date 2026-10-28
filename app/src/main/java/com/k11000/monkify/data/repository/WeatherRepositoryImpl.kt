package com.k11000.monkify.data.repository

import com.k11000.monkify.data.local.LocalWeatherStore
import com.k11000.monkify.data.location.LocationDataSource
import com.k11000.monkify.data.remote.api.OpenMeteoRemoteDataSource
import com.k11000.monkify.domain.model.CurrentWeather
import com.k11000.monkify.domain.model.WeatherCondition
import com.k11000.monkify.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val localWeatherStore: LocalWeatherStore,
    private val remoteDataSource: OpenMeteoRemoteDataSource,
    private val locationDataSource: LocationDataSource
) : WeatherRepository {

    companion object {
        // Coordenadas de respaldo por defecto (Buenos Aires / templado)
        const val DEFAULT_LATITUDE = -34.6037
        const val DEFAULT_LONGITUDE = -58.3816
        const val DEFAULT_CITY_NAME = "Buenos Aires"
    }

    private var manualLatitude: Double? = null
    private var manualLongitude: Double? = null
    private var manualCityName: String? = null

    override fun getCurrentWeatherFlow(): Flow<CurrentWeather?> {
        return localWeatherStore.weatherFlow
    }

    override suspend fun refreshWeatherIfNeeded(force: Boolean): Result<CurrentWeather> {
        val cached = localWeatherStore.getLastWeather()

        // 1. Si los datos cacheados siguen vigentes (< 2h) y no se fuerza, retornarlos directamente
        if (!force && cached != null && !cached.isStale) {
            return Result.success(cached)
        }

        // 2. Determinar coordenadas geográficas
        val (lat, lon, city) = resolveCoordinates()

        // 3. Consultar proveedor remoto Open-Meteo
        val remoteResult = remoteDataSource.fetchWeather(lat, lon, city)

        return if (remoteResult.isSuccess) {
            val freshWeather = remoteResult.getOrThrow()
            localWeatherStore.saveWeather(freshWeather)
            Result.success(freshWeather)
        } else {
            // 4. Degradación elegante en caso de fallo de red
            if (cached != null) {
                Result.success(cached)
            } else {
                // Generar clima sintético seguro si no hay nada en caché
                val synthetic = CurrentWeather(
                    condition = WeatherCondition.SUNNY,
                    weatherCode = 0,
                    temperatureCelsius = 22f,
                    tempMaxCelsius = 26f,
                    tempMinCelsius = 15f,
                    isDay = true,
                    fetchedAtEpochMs = System.currentTimeMillis(),
                    cityName = city ?: DEFAULT_CITY_NAME
                )
                localWeatherStore.saveWeather(synthetic)
                Result.success(synthetic)
            }
        }
    }

    override suspend fun setManualLocation(latitude: Double, longitude: Double, cityName: String) {
        manualLatitude = latitude
        manualLongitude = longitude
        manualCityName = cityName
        refreshWeatherIfNeeded(force = true)
    }

    private suspend fun resolveCoordinates(): Triple<Double, Double, String?> {
        if (manualLatitude != null && manualLongitude != null) {
            return Triple(manualLatitude!!, manualLongitude!!, manualCityName)
        }

        val gpsLocation = locationDataSource.getLastKnownLocation()
        if (gpsLocation != null) {
            return Triple(gpsLocation.latitude, gpsLocation.longitude, null)
        }

        return Triple(DEFAULT_LATITUDE, DEFAULT_LONGITUDE, DEFAULT_CITY_NAME)
    }
}
