package com.tqmane.filmsim

import android.app.Application
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.tqmane.filmsim.util.BitmapPool
import com.tqmane.filmsim.util.MemoryMonitor
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FilmSimsApplication : Application() {

    private lateinit var memoryMonitor: MemoryMonitor

    override fun onCreate() {
        super.onCreate()
        Firebase.analytics.setAnalyticsCollectionEnabled(true)
        memoryMonitor = MemoryMonitor(BitmapPool())
        registerComponentCallbacks(memoryMonitor)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        memoryMonitor.onTrimMemory(level)
    }
}
