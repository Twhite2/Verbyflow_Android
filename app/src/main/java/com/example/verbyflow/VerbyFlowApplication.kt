package com.example.verbyflow

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VerbyFlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide components here
    }
}
