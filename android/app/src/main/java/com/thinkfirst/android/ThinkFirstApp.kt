package com.thinkfirst.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ThinkFirstApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize app-wide components here
    }
}

