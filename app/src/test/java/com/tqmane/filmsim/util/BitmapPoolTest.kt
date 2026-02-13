package com.tqmane.filmsim.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [CacheStats] (pure-logic, no Android deps).
 */
class CacheStatsTest {

    private lateinit var stats: CacheStats

    @BeforeEach
    fun setUp() {
        stats = CacheStats()
    }

    @Nested
    @DisplayName("Hit rate calculation")
    inner class HitRate {

        @Test
        fun `hit rate is zero with no accesses`() {
            assertEquals(0f, stats.hitRate, 0.001f)
        }

        @Test
        fun `100% hit rate when all are hits`() {
            repeat(10) { stats.recordHit() }
            assertEquals(1f, stats.hitRate, 0.001f)
        }

        @Test
        fun `0% hit rate when all are misses`() {
            repeat(10) { stats.recordMiss() }
            assertEquals(0f, stats.hitRate, 0.001f)
        }

        @Test
        fun `50% hit rate with equal hits and misses`() {
            repeat(5) { stats.recordHit() }
            repeat(5) { stats.recordMiss() }
            assertEquals(0.5f, stats.hitRate, 0.001f)
        }
    }

    @Nested
    @DisplayName("Counter accuracy")
    inner class Counters {

        @Test
        fun `evictions are tracked separately`() {
            stats.recordEviction()
            stats.recordEviction()
            // Hit rate should still be 0 (evictions don't count as access)
            assertEquals(0f, stats.hitRate, 0.001f)
        }

        @Test
        fun `reset clears all counters`() {
            stats.recordHit()
            stats.recordMiss()
            stats.recordEviction()
            stats.reset()
            assertEquals(0f, stats.hitRate, 0.001f)
        }
    }
}
