package com.tqmane.filmsim.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for the error handling framework ([AppError], [ErrorHandler]).
 */
class ErrorHandlerTest {

    @Nested
    @DisplayName("AppError sealed class")
    inner class AppErrorTests {

        @Test
        fun `NetworkError carries user and log messages`() {
            val original = java.io.IOException("timeout")
            val error = AppError.NetworkError(
                userMessage = "No internet",
                logMessage = "HTTP timeout at api.github.com",
                cause = original
            )
            assertEquals("No internet", error.userMessage)
            assertEquals("HTTP timeout at api.github.com", error.logMessage)
            assertSame(original, error.cause)
        }

        @Test
        fun `StorageError works without cause`() {
            val error = AppError.StorageError(
                userMessage = "Save failed",
                logMessage = "MediaStore insert returned null"
            )
            assertEquals("Save failed", error.userMessage)
            assertNull(error.cause)
        }

        @Test
        fun `ProcessingError captures exception`() {
            val cause = IllegalStateException("bad LUT")
            val error = AppError.ProcessingError(
                userMessage = "Filter error",
                logMessage = "LUT parse failed",
                cause = cause
            )
            assertTrue(error.cause is IllegalStateException)
        }

        @Test
        fun `MemoryError preserves OOM cause`() {
            val oom = OutOfMemoryError("heap exhausted")
            val error = AppError.MemoryError(
                userMessage = "Out of memory",
                logMessage = "OOM during bitmap decode",
                cause = oom
            )
            assertTrue(error.cause is OutOfMemoryError)
        }
    }

    @Nested
    @DisplayName("defaultErrorMapper")
    inner class MapperTests {

        @Test
        fun `maps IOException to NetworkError`() {
            val error = defaultErrorMapper(java.io.IOException("connection reset"))
            assertTrue(error is AppError.NetworkError)
        }

        @Test
        fun `maps OutOfMemoryError to MemoryError`() {
            val error = defaultErrorMapper(OutOfMemoryError("heap"))
            assertTrue(error is AppError.MemoryError)
        }

        @Test
        fun `maps generic Exception to ProcessingError`() {
            val error = defaultErrorMapper(IllegalArgumentException("bad input"))
            assertTrue(error is AppError.ProcessingError)
        }

        @Test
        fun `maps SecurityException to StorageError`() {
            val error = defaultErrorMapper(SecurityException("no permission"))
            assertTrue(error is AppError.StorageError)
        }
    }
}
