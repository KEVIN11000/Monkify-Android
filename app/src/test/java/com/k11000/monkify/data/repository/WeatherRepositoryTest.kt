package com.k11000.monkify.data.repository

import com.google.gson.Gson
import com.k11000.monkify.data.local.LocalWeatherStore
import com.k11000.monkify.data.location.LocationCoordinates
import com.k11000.monkify.data.location.LocationDataSource
import com.k11000.monkify.data.remote.api.OpenMeteoRemoteDataSource
import com.k11000.monkify.domain.model.CurrentWeather
import com.k11000.monkify.domain.model.WeatherCondition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class WeatherRepositoryTest {

    private lateinit var fakeLocalStore: FakeLocalWeatherStore
    private lateinit var fakeRemoteDataSource: FakeOpenMeteoRemoteDataSource
    private lateinit var fakeLocationDataSource: FakeLocationDataSource
    private lateinit var repository: WeatherRepositoryImpl

    @Before
    fun setUp() {
        fakeLocalStore = FakeLocalWeatherStore()
        fakeRemoteDataSource = FakeOpenMeteoRemoteDataSource()
        fakeLocationDataSource = FakeLocationDataSource()

        repository = WeatherRepositoryImpl(
            localWeatherStore = fakeLocalStore,
            remoteDataSource = fakeRemoteDataSource,
            locationDataSource = fakeLocationDataSource
        )
    }

    @Test
    fun returns_cached_weather_if_fresh_without_remote_call() = runBlocking {
        val now = System.currentTimeMillis()
        val freshCached = CurrentWeather(
            condition = WeatherCondition.SUNNY,
            weatherCode = 0,
            temperatureCelsius = 23f,
            fetchedAtEpochMs = now - (10 * 60 * 1000L) // 10 minutos
        )
        fakeLocalStore.saveWeather(freshCached)

        val result = repository.refreshWeatherIfNeeded(force = false)

        assertTrue(result.isSuccess)
        assertEquals(23f, result.getOrNull()?.temperatureCelsius)
        assertEquals(0, fakeRemoteDataSource.callCount) // No se llamó a la API
    }

    @Test
    fun fetches_and_saves_remote_weather_when_cache_is_stale() = runBlocking {
        val now = System.currentTimeMillis()
        val staleCached = CurrentWeather(
            condition = WeatherCondition.CLOUDY,
            weatherCode = 2,
            temperatureCelsius = 15f,
            fetchedAtEpochMs = now - (3 * 60 * 60 * 1000L) // 3 horas
        )
        fakeLocalStore.saveWeather(staleCached)

        fakeRemoteDataSource.resultToReturn = Result.success(
            CurrentWeather(
                condition = WeatherCondition.RAINY,
                weatherCode = 61,
                temperatureCelsius = 18f,
                fetchedAtEpochMs = now
            )
        )

        val result = repository.refreshWeatherIfNeeded(force = false)

        assertTrue(result.isSuccess)
        assertEquals(WeatherCondition.RAINY, result.getOrNull()?.condition)
        assertEquals(1, fakeRemoteDataSource.callCount)
        assertEquals(WeatherCondition.RAINY, fakeLocalStore.getLastWeather()?.condition)
    }

    @Test
    fun force_refresh_calls_remote_even_if_cache_is_fresh() = runBlocking {
        val now = System.currentTimeMillis()
        val freshCached = CurrentWeather(
            condition = WeatherCondition.SUNNY,
            weatherCode = 0,
            temperatureCelsius = 25f,
            fetchedAtEpochMs = now
        )
        fakeLocalStore.saveWeather(freshCached)

        fakeRemoteDataSource.resultToReturn = Result.success(
            CurrentWeather(
                condition = WeatherCondition.SNOWY,
                weatherCode = 71,
                temperatureCelsius = 2f,
                fetchedAtEpochMs = now
            )
        )

        val result = repository.refreshWeatherIfNeeded(force = true)

        assertTrue(result.isSuccess)
        assertEquals(WeatherCondition.SNOWY, result.getOrNull()?.condition)
        assertEquals(1, fakeRemoteDataSource.callCount)
    }

    @Test
    fun falls_back_to_default_coordinates_when_location_is_unavailable() = runBlocking {
        fakeLocationDataSource.locationToReturn = null // Permiso denegado o GPS apagado

        fakeRemoteDataSource.resultToReturn = Result.success(
            CurrentWeather(
                condition = WeatherCondition.SUNNY,
                weatherCode = 0,
                temperatureCelsius = 22f,
                fetchedAtEpochMs = System.currentTimeMillis()
            )
        )

        val result = repository.refreshWeatherIfNeeded(force = true)

        assertTrue(result.isSuccess)
        assertEquals(WeatherRepositoryImpl.DEFAULT_LATITUDE, fakeRemoteDataSource.lastLatitude!!, 0.001)
        assertEquals(WeatherRepositoryImpl.DEFAULT_LONGITUDE, fakeRemoteDataSource.lastLongitude!!, 0.001)
    }

    @Test
    fun returns_stale_cache_gracefully_when_remote_network_fails() = runBlocking {
        val stale = CurrentWeather(
            condition = WeatherCondition.HAZY,
            weatherCode = 45,
            temperatureCelsius = 19f,
            fetchedAtEpochMs = System.currentTimeMillis() - (5 * 60 * 60 * 1000L)
        )
        fakeLocalStore.saveWeather(stale)

        fakeRemoteDataSource.resultToReturn = Result.failure(IOException("No network connection"))

        val result = repository.refreshWeatherIfNeeded(force = true)

        assertTrue(result.isSuccess)
        assertEquals(WeatherCondition.HAZY, result.getOrNull()?.condition)
    }

    @Test
    fun generates_synthetic_weather_when_network_fails_and_cache_is_empty() = runBlocking {
        fakeRemoteDataSource.resultToReturn = Result.failure(IOException("No network"))

        val result = repository.refreshWeatherIfNeeded(force = true)

        assertTrue(result.isSuccess)
        val synthetic = result.getOrNull()
        assertNotNull(synthetic)
        assertEquals(WeatherCondition.SUNNY, synthetic?.condition)
        assertEquals(22f, synthetic?.temperatureCelsius)
    }
}

// Fakes para test unitario rápido y desacoplado
class FakeLocalWeatherStore : LocalWeatherStore(
    // Pasamos un DataStore falso o sobreescribimos métodos
    dataStore = FakeDataStore()
) {
    private var stored: CurrentWeather? = null
    private val flow = MutableStateFlow<CurrentWeather?>(null)

    override suspend fun getLastWeather(): CurrentWeather? = stored

    override suspend fun saveWeather(weather: CurrentWeather) {
        stored = weather
        flow.value = weather
    }

    override suspend fun clearWeather() {
        stored = null
        flow.value = null
    }
}

class FakeDataStore : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
    override val data: Flow<androidx.datastore.preferences.core.Preferences> = kotlinx.coroutines.flow.emptyFlow()
    override suspend fun updateData(transform: suspend (t: androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences): androidx.datastore.preferences.core.Preferences {
        return androidx.datastore.preferences.core.emptyPreferences()
    }
}

class FakeOpenMeteoRemoteDataSource : OpenMeteoRemoteDataSource(
    okHttpClient = OkHttpClient(),
    gson = Gson()
) {
    var resultToReturn: Result<CurrentWeather>? = null
    var callCount = 0
    var lastLatitude: Double? = null
    var lastLongitude: Double? = null

    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        cityName: String?
    ): Result<CurrentWeather> {
        callCount++
        lastLatitude = latitude
        lastLongitude = longitude
        return resultToReturn ?: Result.success(
            CurrentWeather(
                condition = WeatherCondition.SUNNY,
                weatherCode = 0,
                temperatureCelsius = 20f
            )
        )
    }
}

class FakeLocationDataSource : LocationDataSource {
    var locationToReturn: LocationCoordinates? = null

    override suspend fun getLastKnownLocation(): LocationCoordinates? {
        return locationToReturn
    }
}
