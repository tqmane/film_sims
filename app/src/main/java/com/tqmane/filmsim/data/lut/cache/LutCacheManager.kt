package com.tqmane.filmsim.data.lut.cache

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import android.util.LruCache
import com.tqmane.filmsim.util.CubeLUT
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LUT cache using LruCache with dynamic sizing based on device memory.
 */
@Singleton
class LutCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LutCacheManager"
    }

    private val cache: LruCache<String, CubeLUT> by lazy {
        val sizeKb = calculateCacheSizeKb()
        object : LruCache<String, CubeLUT>(sizeKb) {
            override fun sizeOf(key: String, value: CubeLUT): Int {
                val bytes = value.size * value.size * value.size * 3 * 4
                return (bytes / 1024).coerceAtLeast(1)
            }
        }
    }

    fun get(key: String): CubeLUT? {
        return synchronized(cache) {
            cache.get(key)?.also { it.data.position(0) }
        }
    }

    fun put(key: String, lut: CubeLUT) {
        lut.data.position(0)
        synchronized(cache) {
            cache.put(key, lut)
        }
    }

    private fun calculateCacheSizeKb(): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return 16 * 1024
        val memoryClassMb = am.memoryClass
        val cacheSizeMb = when {
            memoryClassMb <= 64 -> memoryClassMb / 8
            memoryClassMb <= 128 -> 16
            memoryClassMb <= 256 -> 24
            else -> 32
        }.coerceIn(8, 64)
        Log.d(TAG, "Device memory class: ${memoryClassMb}MB, Cache size: ${cacheSizeMb}MB")
        return cacheSizeMb * 1024
    }
}
