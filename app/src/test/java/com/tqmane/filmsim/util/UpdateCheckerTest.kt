package com.tqmane.filmsim.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Unit tests for [UpdateChecker].
 * Focuses on the pure logic of [UpdateChecker.isNewerVersion], which is
 * marked `internal` for testability.
 */
class UpdateCheckerTest {

    @Nested
    @DisplayName("isNewerVersion")
    inner class VersionComparison {

        @ParameterizedTest(name = "new={0} vs current={1} → {2}")
        @CsvSource(
            "1.0.1, 1.0.0, true",
            "2.0.0, 1.9.9, true",
            "1.1.0, 1.0.9, true",
            "0.2.0, 0.1.0, true",
            "1.0.0, 1.0.0, false",
            "1.0.0, 1.0.1, false",
            "1.0.0, 2.0.0, false",
            "0.9.0, 1.0.0, false"
        )
        fun `semantic version comparison`(newVer: String, curVer: String, expected: Boolean) {
            assertEquals(expected, UpdateChecker.isNewerVersion(newVer, curVer))
        }

        @Test
        fun `handles different segment lengths`() {
            assertTrue(UpdateChecker.isNewerVersion("1.0.1", "1.0"))
            assertFalse(UpdateChecker.isNewerVersion("1.0", "1.0.1"))
        }

        @Test
        fun `handles single segment versions`() {
            assertTrue(UpdateChecker.isNewerVersion("2", "1"))
            assertFalse(UpdateChecker.isNewerVersion("1", "2"))
            assertFalse(UpdateChecker.isNewerVersion("1", "1"))
        }

        @Test
        fun `handles non-numeric segments gracefully`() {
            // Non-numeric segments are treated as 0
            assertFalse(UpdateChecker.isNewerVersion("abc", "1.0.0"))
        }

        @Test
        fun `handles empty strings gracefully`() {
            assertFalse(UpdateChecker.isNewerVersion("", ""))
        }

        @Test
        fun `handles four-segment versions`() {
            assertTrue(UpdateChecker.isNewerVersion("1.0.0.1", "1.0.0.0"))
            assertFalse(UpdateChecker.isNewerVersion("1.0.0.0", "1.0.0.1"))
        }
    }
}
