package com.tqmane.filmsim.util

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages the LUT [LruCache] with:
 *   - Dynamic sizing based on device memory
 *   - Hit/miss statistics via [CacheStats]
 *   - Pre-loading helpers
 *   - Memory-pressure trimming
 *
 * Replaces the static cache inside [CubeLUTParser].
 */
class LutCacheManager(context: Context) {

    companion object {
        private const val TAG = "LutCacheManager"
    }

    val stats = CacheStats(TAG)

    private val cache: LruCache<String, CubeLUT>

    init {
        val maxSizeKb = computeCacheSizeKb(context)
        Log.d(TAG, "Initialising LUT cache: ${maxSizeKb / 1024} MB")

        cache = object : LruCache<String, CubeLUT>(maxSizeKb) {
            override fun sizeOf(key: String, value: CubeLUT): Int {
                val bytes = value.size * value.size * value.size * 3 * 4
                return (bytes / 1024).coerceAtLeast(1)
            }

            override fun entryRemoved(
                evicted: Boolean, key: String, oldValue: CubeLUT, newValue: CubeLUT?
            ) {
                if (evicted) stats.recordEviction()
            }
        }
    }

    // ----- public API -----

    fun get(key: String): CubeLUT? {
        val result = cache.get(key)
        if (result != null) {
            result.data.position(0)
            stats.recordHit()
        } else {
            stats.recordMiss()
        }
        return result
    }

    fun put(key: String, lut: CubeLUT) {
        lut.data.position(0)
        cache.put(key, lut)
    }

    /**
     * Evict a fraction (0..1) of entries.  1 = clear all.
     */
    fun trimBy(fraction: Float) {
        if (fraction >= 1f) {
            cache.evictAll()
        } else {
            val target = (cache.maxSize() * (1f - fraction)).toInt()
            cache.trimToSize(target)
        }
        stats.logReport()
    }

    fun clear() {
        cache.evictAll()
    }

    /**
     * Pre-load a list of LUT paths in the background.
     */
    suspend fun preload(context: Context, paths: List<String>) = withContext(Dispatchers.IO) {
        for (path in paths) {
            if (cache.get(path) != null) continue
            val lut = CubeLUTParser.parse(context, path)
            if (lut != null) {
                put(path, lut)
            }
        }
        Log.d(TAG, "Pre-loaded ${paths.size} LUTs")
    }

    // ----- internal -----

    private fun computeCacheSizeKb(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return 16 * 1024

        val memoryClassMb = am.memoryClass

        // Use 1/8 of available heap for LUT cache, clamped to 8–64 MB.
        val cacheSizeMb = when {
            memoryClassMb <= 64 -> memoryClassMb / 8
            memoryClassMb <= 128 -> 16
            memoryClassMb <= 256 -> 24
            else -> 32
        }.coerceIn(8, 64)

        Log.d(TAG, "Device memory class: ${memoryClassMb}MB → cache ${cacheSizeMb}MB")
        return cacheSizeMb * 1024
    }
}
