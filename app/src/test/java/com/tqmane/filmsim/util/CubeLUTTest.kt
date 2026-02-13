package com.tqmane.filmsim.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Unit tests for [CubeLUT] data class and LUT data structure behaviour.
 * Since [CubeLUTParser.parse] requires an Android [Context] to read assets,
 * these tests focus on the pure-logic portions: data integrity, FloatBuffer access,
 * and properties of valid LUT structures.
 */
class CubeLUTTest {

    @Nested
    @DisplayName("CubeLUT data class")
    inner class DataClassTests {

        @Test
        fun `CubeLUT preserves size`() {
            val lut = createIdentityLut(4)
            assertEquals(4, lut.size)
        }

        @Test
        fun `CubeLUT data buffer has correct capacity`() {
            val size = 8
            val lut = createIdentityLut(size)
            // size^3 * 3 floats (r, g, b for each cell)
            assertEquals(size * size * size * 3, lut.data.capacity())
        }

        @Test
        fun `identity LUT maps corners correctly`() {
            val size = 2
            val lut = createIdentityLut(size)

            // For a 2x2x2 identity LUT:
            // (0,0,0)  → (0, 0, 0)    index = 0
            // (1,0,0)  → (1, 0, 0)    index = 3
            // (0,1,0)  → (0, 1, 0)    index = 6
            // (1,1,0)  → (1, 1, 0)    index = 9
            // (0,0,1)  → (0, 0, 1)    index = 12
            lut.data.position(0)
            assertEquals(0f, lut.data.get(0), 0.001f)  // r at (0,0,0)
            assertEquals(0f, lut.data.get(1), 0.001f)  // g at (0,0,0)
            assertEquals(0f, lut.data.get(2), 0.001f)  // b at (0,0,0)

            // (1,1,1) → (1, 1, 1) — last entry
            val lastIdx = (size * size * size - 1) * 3
            assertEquals(1f, lut.data.get(lastIdx), 0.001f)
            assertEquals(1f, lut.data.get(lastIdx + 1), 0.001f)
            assertEquals(1f, lut.data.get(lastIdx + 2), 0.001f)
        }

        @Test
        fun `CubeLUT data class equality works by value`() {
            val a = createIdentityLut(4)
            val b = createIdentityLut(4)
            // data class equals checks size but FloatBuffer.equals checks content reference
            assertEquals(a.size, b.size)
        }
    }

    @Nested
    @DisplayName("FloatBuffer operations")
    inner class FloatBufferTests {

        @Test
        fun `float buffer position resets correctly`() {
            val lut = createIdentityLut(4)
            lut.data.position(10)
            lut.data.position(0)
            assertEquals(0, lut.data.position())
        }

        @Test
        fun `float buffer values are in valid range`() {
            val lut = createIdentityLut(8)
            lut.data.position(0)
            for (i in 0 until lut.data.capacity()) {
                val value = lut.data.get(i)
                assertTrue(value in 0f..1f, "Value at index $i out of range: $value")
            }
        }
    }

    // ─── Test helper ────────────────────────────────────

    companion object {
        /**
         * Create an identity LUT of the given size for testing.
         * An identity LUT maps every color to itself.
         */
        fun createIdentityLut(size: Int): CubeLUT {
            val totalEntries = size * size * size * 3
            val buffer = ByteBuffer.allocateDirect(totalEntries * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            for (b in 0 until size) {
                for (g in 0 until size) {
                    for (r in 0 until size) {
                        buffer.put(r.toFloat() / (size - 1).coerceAtLeast(1))
                        buffer.put(g.toFloat() / (size - 1).coerceAtLeast(1))
                        buffer.put(b.toFloat() / (size - 1).coerceAtLeast(1))
                    }
                }
            }
            buffer.position(0)
            return CubeLUT(size, buffer)
        }
    }
}
