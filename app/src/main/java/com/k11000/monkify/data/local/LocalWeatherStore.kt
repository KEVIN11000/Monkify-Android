package com.k11000.monkify.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.k11000.monkify.domain.model.CurrentWeather
import com.k11000.monkify.domain.model.WeatherCondition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class LocalWeatherStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_WEATHER_CONDITION = stringPreferencesKey("weather_condition")
        val KEY_WEATHER_CODE = intPreferencesKey("weather_code")
        val KEY_WEATHER_TEMP = floatPreferencesKey("weather_temp")
        val KEY_WEATHER_TEMP_MAX = floatPreferencesKey("weather_temp_max")
        val KEY_WEATHER_TEMP_MIN = floatPreferencesKey("weather_temp_min")
        val KEY_WEATHER_IS_DAY = booleanPreferencesKey("weather_is_day")
        val KEY_WEATHER_SUNRISE = longPreferencesKey("weather_sunrise")
        val KEY_WEATHER_SUNSET = longPreferencesKey("weather_sunset")
        val KEY_WEATHER_FETCHED_AT = longPreferencesKey("weather_fetched_at")
        val KEY_WEATHER_CITY = stringPreferencesKey("weather_city")
    }

    open val weatherFlow: Flow<CurrentWeather?> = dataStore.data.map { prefs ->
        val conditionStr = prefs[KEY_WEATHER_CONDITION] ?: return@map null
        val condition = try {
            WeatherCondition.valueOf(conditionStr)
        } catch (e: Exception) {
            WeatherCondition.CLOUDY
        }

        val code = prefs[KEY_WEATHER_CODE] ?: 0
        val temp = prefs[KEY_WEATHER_TEMP] ?: 20f
        val tempMax = prefs[KEY_WEATHER_TEMP_MAX]
        val tempMin = prefs[KEY_WEATHER_TEMP_MIN]
        val isDay = prefs[KEY_WEATHER_IS_DAY] ?: true
        val sunrise = prefs[KEY_WEATHER_SUNRISE]
        val sunset = prefs[KEY_WEATHER_SUNSET]
        val fetchedAt = prefs[KEY_WEATHER_FETCHED_AT] ?: 0L
        val city = prefs[KEY_WEATHER_CITY]

        CurrentWeather(
            condition = condition,
            weatherCode = code,
            temperatureCelsius = temp,
            tempMaxCelsius = tempMax,
            tempMinCelsius = tempMin,
            isDay = isDay,
            sunriseEpochMs = sunrise,
            sunsetEpochMs = sunset,
            fetchedAtEpochMs = fetchedAt,
            cityName = city
        )
    }

    open suspend fun saveWeather(weather: CurrentWeather) {
        dataStore.edit { prefs ->
            prefs[KEY_WEATHER_CONDITION] = weather.condition.name
            prefs[KEY_WEATHER_CODE] = weather.weatherCode
            prefs[KEY_WEATHER_TEMP] = weather.temperatureCelsius
            if (weather.tempMaxCelsius != null) {
                prefs[KEY_WEATHER_TEMP_MAX] = weather.tempMaxCelsius
            } else {
                prefs.remove(KEY_WEATHER_TEMP_MAX)
            }
            if (weather.tempMinCelsius != null) {
                prefs[KEY_WEATHER_TEMP_MIN] = weather.tempMinCelsius
            } else {
                prefs.remove(KEY_WEATHER_TEMP_MIN)
            }
            prefs[KEY_WEATHER_IS_DAY] = weather.isDay
            if (weather.sunriseEpochMs != null) {
                prefs[KEY_WEATHER_SUNRISE] = weather.sunriseEpochMs
            } else {
                prefs.remove(KEY_WEATHER_SUNRISE)
            }
            if (weather.sunsetEpochMs != null) {
                prefs[KEY_WEATHER_SUNSET] = weather.sunsetEpochMs
            } else {
                prefs.remove(KEY_WEATHER_SUNSET)
            }
            prefs[KEY_WEATHER_FETCHED_AT] = weather.fetchedAtEpochMs
            if (weather.cityName != null) {
                prefs[KEY_WEATHER_CITY] = weather.cityName
            } else {
                prefs.remove(KEY_WEATHER_CITY)
            }
        }
    }

    open suspend fun getLastWeather(): CurrentWeather? {
        return weatherFlow.firstOrNull()
    }

    open suspend fun clearWeather() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_WEATHER_CONDITION)
            prefs.remove(KEY_WEATHER_CODE)
            prefs.remove(KEY_WEATHER_TEMP)
            prefs.remove(KEY_WEATHER_TEMP_MAX)
            prefs.remove(KEY_WEATHER_TEMP_MIN)
            prefs.remove(KEY_WEATHER_IS_DAY)
            prefs.remove(KEY_WEATHER_SUNRISE)
            prefs.remove(KEY_WEATHER_SUNSET)
            prefs.remove(KEY_WEATHER_FETCHED_AT)
            prefs.remove(KEY_WEATHER_CITY)
        }
    }
}
