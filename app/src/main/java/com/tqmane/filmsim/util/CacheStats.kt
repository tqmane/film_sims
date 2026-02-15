package com.tqmane.filmsim.util

import android.util.Log
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe hit/miss statistics for the LUT cache.
 * Only reports in DEBUG builds (calls are no-ops in release thanks to R8 stripping Log.d).
 */
class CacheStats(private val tag: String = "LutCache") {

    private val hits = AtomicLong(0)
    private val misses = AtomicLong(0)
    private val evictions = AtomicLong(0)

    fun recordHit() { hits.incrementAndGet() }
    fun recordMiss() { misses.incrementAndGet() }
    fun recordEviction() { evictions.incrementAndGet() }

    val hitCount: Long get() = hits.get()
    val missCount: Long get() = misses.get()
    val evictionCount: Long get() = evictions.get()
    val totalRequests: Long get() = hitCount + missCount

    val hitRate: Float
        get() {
            val total = totalRequests
            return if (total == 0L) 0f else hitCount.toFloat() / total
        }

    fun logReport() {
        val total = totalRequests
        if (total == 0L) return
        Log.d(
            tag,
            "Cache stats – hits: $hitCount, misses: $missCount, evictions: $evictionCount, " +
                    "hit rate: ${"%.1f".format(hitRate * 100)}%"
        )
    }

    fun reset() {
        hits.set(0)
        misses.set(0)
        evictions.set(0)
    }
}
