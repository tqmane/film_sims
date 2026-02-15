package com.tqmane.filmsim.util

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.util.Log

/**
 * Monitors system memory pressure via [ComponentCallbacks2]
 * and triggers cache/pool trimming.
 *
 * Register once in [android.app.Application.onCreate] or in the Activity.
 */
class MemoryMonitor(
    private val bitmapPool: BitmapPool,
    private val onTrimLutCache: (fraction: Float) -> Unit = {}
) : ComponentCallbacks2 {

    companion object {
        private const val TAG = "MemoryMonitor"
    }

    override fun onTrimMemory(level: Int) {
        Log.d(TAG, "onTrimMemory level=$level")
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                // App is in the background and the system is running low.
                bitmapPool.clear()
                onTrimLutCache(1f) // clear all
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // UI hidden: trim 30 %
                bitmapPool.trimBy(0.3f)
                onTrimLutCache(0.3f)
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                // Still in foreground but memory is low.
                bitmapPool.trimBy(0.5f)
                onTrimLutCache(0.5f)
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                bitmapPool.trimBy(0.25f)
                onTrimLutCache(0.25f)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) { /* no-op */ }

    override fun onLowMemory() {
        bitmapPool.clear()
        onTrimLutCache(1f)
    }

    /**
     * Convenience: returns available memory in bytes.
     */
    fun availableMemoryBytes(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.availMem
    }
}
