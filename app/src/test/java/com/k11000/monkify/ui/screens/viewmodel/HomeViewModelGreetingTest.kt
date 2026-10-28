package com.k11000.monkify.ui.screens.viewmodel

import com.k11000.monkify.domain.model.CurrentWeather
import com.k11000.monkify.domain.model.WeatherCondition
import com.k11000.monkify.domain.model.ZenDayPart
import com.k11000.monkify.domain.model.ZenPhraseCatalog
import com.k11000.monkify.domain.repository.TrackRepository
import com.k11000.monkify.domain.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelGreetingTest {

    private val weatherFlow = MutableStateFlow<CurrentWeather?>(null)

    private val fakeWeatherRepository = object : WeatherRepository {
        override fun getCurrentWeatherFlow(): Flow<CurrentWeather?> = weatherFlow
        override suspend fun refreshWeatherIfNeeded(force: Boolean): Result<CurrentWeather> {
            val weather = weatherFlow.value ?: CurrentWeather(
                condition = WeatherCondition.SUNNY,
                weatherCode = 0,
                temperatureCelsius = 22f
            )
            return Result.success(weather)
        }
        override suspend fun setManualLocation(latitude: Double, longitude: Double, cityName: String) {}
    }

    private val fakeTrackRepository = object : TrackRepository {
        override fun getRecentTracks() = flowOf(emptyList<com.k11000.monkify.domain.model.Track>())
        override fun getMadeForYouTracks() = flowOf(emptyList<com.k11000.monkify.domain.model.Track>())
        override suspend fun getTracks(limit: Int, offset: Int, query: String?) = emptyList<com.k11000.monkify.domain.model.Track>()
        override suspend fun getLatestUploadedTracks(limit: Int) = emptyList<com.k11000.monkify.domain.model.Track>()
        override suspend fun recordTrackPlayed(trackId: String) {}
        override suspend fun requestIngest(url: String) = error("unused")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        weatherFlow.value = null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `greeting updates reactively when rainy weather is emitted`() {
        val now = System.currentTimeMillis()
        val rainyWeather = CurrentWeather(
            condition = WeatherCondition.RAINY,
            weatherCode = 61,
            temperatureCelsius = 16f,
            sunriseEpochMs = now - 3600000L * 4, // 4 hours ago
            sunsetEpochMs = now + 3600000L * 6   // 6 hours in future -> DAY
        )
        weatherFlow.value = rainyWeather

        val viewModel = HomeViewModel(
            trackRepository = fakeTrackRepository,
            weatherRepository = fakeWeatherRepository
        )

        val greeting = viewModel.greeting.value
        assertEquals("Buenos días", greeting.title)
        val expectedRainyPhrases = ZenPhraseCatalog.getPhrases(WeatherCondition.RAINY, ZenDayPart.DAY)
        assertTrue(
            "Greeting phrase '${greeting.phrase}' should be in catalog for RAINY DAY",
            expectedRainyPhrases.contains(greeting.phrase)
        )
    }

    @Test
    fun `greeting title and phrase adapt to night and snowy conditions`() {
        val now = System.currentTimeMillis()
        val snowyNightWeather = CurrentWeather(
            condition = WeatherCondition.SNOWY,
            weatherCode = 71,
            temperatureCelsius = -2f,
            sunriseEpochMs = now - 3600000L * 14,
            sunsetEpochMs = now - 3600000L * 2    // Sunset was 2 hours ago -> NIGHT
        )
        weatherFlow.value = snowyNightWeather

        val viewModel = HomeViewModel(
            trackRepository = fakeTrackRepository,
            weatherRepository = fakeWeatherRepository
        )

        val greeting = viewModel.greeting.value
        assertEquals("Buenas noches", greeting.title)
        val expectedSnowyNightPhrases = ZenPhraseCatalog.getPhrases(WeatherCondition.SNOWY, ZenDayPart.NIGHT)
        assertTrue(
            "Greeting phrase '${greeting.phrase}' should be in catalog for SNOWY NIGHT",
            expectedSnowyNightPhrases.contains(greeting.phrase)
        )
    }

    @Test
    fun `cycleNextPhrase rotates to next phrase in catalog for current weather`() {
        val now = System.currentTimeMillis()
        val sunnyDay = CurrentWeather(
            condition = WeatherCondition.SUNNY,
            weatherCode = 0,
            temperatureCelsius = 24f,
            sunriseEpochMs = now - 3600000L * 4,
            sunsetEpochMs = now + 3600000L * 6
        )
        weatherFlow.value = sunnyDay

        val viewModel = HomeViewModel(
            trackRepository = fakeTrackRepository,
            weatherRepository = fakeWeatherRepository
        )

        val phrases = ZenPhraseCatalog.getPhrases(WeatherCondition.SUNNY, ZenDayPart.DAY)
        val phrase0 = viewModel.greeting.value.phrase
        viewModel.cycleNextPhrase()
        val phrase1 = viewModel.greeting.value.phrase

        assertEquals(phrases[0], phrase0)
        assertEquals(phrases[1], phrase1)
    }

    @Test
    fun `fallback greeting is safe and valid when weatherRepository is null`() {
        val viewModel = HomeViewModel(trackRepository = fakeTrackRepository)
        val greeting = viewModel.greeting.value
        assertNotNull(greeting.title)
        assertNotNull(greeting.phrase)
        assertTrue(greeting.title.isNotBlank())
        assertTrue(greeting.phrase.isNotBlank())
    }
}
