package com.k11000.monkify.ui.components

import androidx.annotation.DrawableRes
import com.k11000.monkify.R
import com.k11000.monkify.domain.model.FroggyScenery
import com.k11000.monkify.domain.model.WeatherCondition
import com.k11000.monkify.domain.model.ZenDayPart

/**
 * Catálogo exhaustivo y seguro en tiempo de compilación para los 60 fondos escénicos oficiales
 * de Froggy (Google Weather) clasificados por escenario, franja horaria y condición meteorológica.
 */
object FroggyAssetCatalog {

    @DrawableRes
    fun getBackgroundRes(
        scenery: FroggyScenery,
        dayPart: ZenDayPart,
        condition: WeatherCondition
    ): Int {
        return when (scenery) {
            FroggyScenery.FIELDS -> getFieldsBackground(dayPart, condition)
            FroggyScenery.HILL -> getHillBackground(dayPart, condition)
            FroggyScenery.MUSHROOM -> getMushroomBackground(dayPart, condition)
        }
    }

    @DrawableRes
    private fun getFieldsBackground(dayPart: ZenDayPart, condition: WeatherCondition): Int {
        return when (dayPart) {
            ZenDayPart.DAWN -> when (condition) {
                WeatherCondition.SUNNY -> R.drawable.fields_morning_sunny_bg
                WeatherCondition.CLOUDY -> R.drawable.fields_morning_cloudy_bg
                WeatherCondition.HAZY -> R.drawable.fields_morning_hazy_bg
                WeatherCondition.RAINY -> R.drawable.fields_morning_rainy_bg
                WeatherCondition.SNOWY -> R.drawable.fields_morning_snowy_bg
            }
            ZenDayPart.DAY -> when (condition) {
                WeatherCondition.SUNNY -> R.drawable.fields_day_sunny_bg
                WeatherCondition.CLOUDY -> R.drawable.fields_day_cloudy_bg
                WeatherCondition.HAZY -> R.drawable.fields_day_hazy_bg
                WeatherCondition.RAINY -> R.drawable.fields_day_rainy_bg
                WeatherCondition.SNOWY -> R.drawable.fields_day_snowy_bg
            }
            ZenDayPart.SUNSET -> when (condition) {
                WeatherCondition.SUNNY -> R.drawable.fields_sunset_sunny_bg
                WeatherCondition.CLOUDY -> R.drawable.fields_sunset_cloudy_bg
                WeatherCondition.HAZY -> R.drawable.fields_sunset_hazy_bg
                WeatherCondition.RAINY -> R.drawable.fields_sunset_rainy_bg
                WeatherCondition.SNOWY -> R.drawable.fields_sunset_snowy_bg
            }
            ZenDayPart.NIGHT -> when (condition) {
                WeatherCondition.SUNNY -> R.drawable.fields_night_sunny_bg
                WeatherCondition.CLOUDY -> R.drawable.fields_night_cloudy_bg
                WeatherCondition.HAZY -> R.drawable.fields_night_hazy_bg
                WeatherCondition.RAINY -> R.drawable.fields_night_rainy_bg
                WeatherCondition.SNOWY -> R.drawable.fields_night_snowy_bg
            }
        }
    }

    @DrawableRes
    private fun getHillBackground(dayPart: ZenDayPart, condition: WeatherCondition): Int {
        return when (dayPart) {
            ZenDayPart.DAWN -> when (condition) {
                WeatherCondition.SUNNY -> R.drawable.hill_morning_sunny_bg
                WeatherCondition.CLOUDY -> R.drawable.hill_morning_cloudy_bg
                WeatherCondition.HAZY -> R.drawable.hill_morning_hazy_bg
                WeatherCondition.RAINY -> R.drawable.hill_morning_rainy_bg
                WeatherCondition.SNOWY -> R.drawable.hill_morning_snowy_bg
            }
            ZenDayPart.DAY -> when (condition) {
                WeatherCondition.SUNNY -> R.drawable.hill_day_sunny_bg
                WeatherCondition.CLOUDY -> R.drawable.hill_day_cloudy_bg
                WeatherCondition.HAZY -> R.drawable.hill_day_hazy_bg
                WeatherCondition.RAINY -> R.drawable.hill_day_rainy_bg
                WeatherCondition.SNOWY -> R.drawable.hill_day_snowy_bg
            }
            ZenDayPart.SUNSET -> when (condition) {
                WeatherCondition.SUNNY -> R.drawable.hill_sunset_sunny_bg
                WeatherCondition.CLOUDY -> R.drawable.hill_sunset_cloudy_bg
                WeatherCondition.HAZY -> R.drawable.hill_sunset_hazy_bg
                WeatherCondition.RAINY -> R.drawable.hill_sunset_rainy_bg
                WeatherCondition.SNOWY -> R.drawable.hill_sunset_snowy_bg
            }
            ZenDayPart.NIGHT -> when (condition) {
                WeatherCondition.SUNNY -> R.drawable.hill_night_sunny_bg
                WeatherCondition.CLOUDY -> R.drawable.hill_night_cloudy_bg
                WeatherCondition.HAZY -> R.drawable.hill_night_hazy_bg
                WeatherCondition.RAINY -> R.drawable.hill_night_rainy_bg
                WeatherCondition.SNOWY -> R.drawable.hill_night_snowy_bg
            }
        }
    }

    @DrawableRes
    private fun getMushroomBackground(dayPart: ZenDayPart, condition: WeatherCondition): Int {
        return when (dayPart) {
            ZenDayPart.DAWN -> when (condition) {
                WeatherCondition.SUNNY -> R.drawable.mushroom_morning_sunny_bg
                WeatherCondition.CLOUDY -> R.drawable.mushroom_morning_cloudy_bg
                WeatherCondition.HAZY -> R.drawable.mushroom_morning_hazy_bg
                WeatherCondition.RAINY -> R.drawable.mushroom_morning_rainy_bg
                WeatherCondition.SNOWY -> R.drawable.mushroom_morning_snowy_bg
            }
            ZenDayPart.DAY -> when (condition) {
                WeatherCondition.SUNNY -> R.drawable.mushroom_day_sunny_bg
                WeatherCondition.CLOUDY -> R.drawable.mushroom_day_cloudy_bg
                WeatherCondition.HAZY -> R.drawable.mushroom_day_hazy_bg
                WeatherCondition.RAINY -> R.drawable.mushroom_day_rainy_bg
                WeatherCondition.SNOWY -> R.drawable.mushroom_day_snowy_bg
            }
            ZenDayPart.SUNSET -> when (condition) {
                WeatherCondition.SUNNY -> R.drawable.mushroom_sunset_sunny_bg
                WeatherCondition.CLOUDY -> R.drawable.mushroom_sunset_cloudy_bg
                WeatherCondition.HAZY -> R.drawable.mushroom_sunset_hazy_bg
                WeatherCondition.RAINY -> R.drawable.mushroom_sunset_rainy_bg
                WeatherCondition.SNOWY -> R.drawable.mushroom_sunset_snowy_bg
            }
            ZenDayPart.NIGHT -> when (condition) {
                WeatherCondition.SUNNY -> R.drawable.mushroom_night_sunny_bg
                WeatherCondition.CLOUDY -> R.drawable.mushroom_night_cloudy_bg
                WeatherCondition.HAZY -> R.drawable.mushroom_night_hazy_bg
                WeatherCondition.RAINY -> R.drawable.mushroom_night_rainy_bg
                WeatherCondition.SNOWY -> R.drawable.mushroom_night_snowy_bg
            }
        }
    }
}
