package com.tqmane.filmsim.util

import android.graphics.Bitmap
import android.util.Log
import java.util.LinkedList

/**
 * Simple pool for reusing [Bitmap] instances via [android.graphics.BitmapFactory.Options.inBitmap].
 *
 * Keeps a bounded set of recycled bitmaps keyed by (width, height, config).
 * When a matching bitmap is available it is returned instead of allocating a new one.
 *
 * Thread-safe.
 */
class BitmapPool(
    /** Maximum aggregated byte size of pooled bitmaps. */
    private val maxPoolSizeBytes: Long = DEFAULT_POOL_SIZE
) {

    companion object {
        /** Default pool: 32 MB */
        const val DEFAULT_POOL_SIZE: Long = 32L * 1024 * 1024
        private const val TAG = "BitmapPool"
    }

    private val pool = LinkedList<Bitmap>()
    private var currentSizeBytes: Long = 0
    private val lock = Any()

    /**
     * Try to retrieve a reusable bitmap that can be used as `inBitmap`
     * for a decode of [width]x[height] with [config].
     *
     * Returns `null` if no matching bitmap is available.
     */
    fun get(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
        synchronized(lock) {
            val iterator = pool.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                // For API 19+ inBitmap only requires that the byte count is >= the needed size.
                if (candidate.allocationByteCount >= width * height * bytesPerPixel(config) &&
                    !candidate.isRecycled
                ) {
                    iterator.remove()
                    currentSizeBytes -= candidate.allocationByteCount
                    // Reconfigure the bitmap to the requested dimensions
                    try {
                        candidate.reconfigure(width, height, config)
                        return candidate
                    } catch (_: Exception) {
                        candidate.recycle()
                    }
                }
            }
        }
        return null
    }

    /**
     * Return a bitmap to the pool for future reuse.
     * If the pool is full, the bitmap is recycled immediately.
     */
    fun put(bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        synchronized(lock) {
            if (currentSizeBytes + bitmap.allocationByteCount > maxPoolSizeBytes) {
                bitmap.recycle()
                return
            }
            pool.addFirst(bitmap)
            currentSizeBytes += bitmap.allocationByteCount
        }
    }

    /**
     * Evict [fraction] (0..1) of pooled bitmaps (oldest first).
     */
    fun trimBy(fraction: Float) {
        synchronized(lock) {
            val target = (currentSizeBytes * (1f - fraction.coerceIn(0f, 1f))).toLong()
            while (currentSizeBytes > target && pool.isNotEmpty()) {
                val removed = pool.removeLast()
                currentSizeBytes -= removed.allocationByteCount
                removed.recycle()
            }
            Log.d(TAG, "Pool trimmed by ${(fraction * 100).toInt()}%  – remaining ${currentSizeBytes / 1024}KB")
        }
    }

    /**
     * Recycle every bitmap in the pool.
     */
    fun clear() {
        synchronized(lock) {
            for (bmp in pool) bmp.recycle()
            pool.clear()
            currentSizeBytes = 0
            Log.d(TAG, "Pool cleared")
        }
    }

    private fun bytesPerPixel(config: Bitmap.Config): Int = when (config) {
        Bitmap.Config.ARGB_8888 -> 4
        Bitmap.Config.RGB_565 -> 2
        Bitmap.Config.ALPHA_8 -> 1
        else -> 4
    }
}
