package com.k11000.monkify

import android.app.Application
import com.k11000.monkify.util.AppLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MonkifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
    }
}
