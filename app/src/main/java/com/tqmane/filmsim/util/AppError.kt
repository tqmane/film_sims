package com.tqmane.filmsim.util

/**
 * Unified error hierarchy for the FilmSims application.
 * Each error carries a user-facing message and a technical log message.
 */
sealed class AppError {
    abstract val userMessage: String
    abstract val logMessage: String
    abstract val cause: Throwable?

    data class NetworkError(
        override val userMessage: String,
        override val logMessage: String,
        override val cause: Throwable? = null
    ) : AppError()

    data class StorageError(
        override val userMessage: String,
        override val logMessage: String,
        override val cause: Throwable? = null
    ) : AppError()

    data class ProcessingError(
        override val userMessage: String,
        override val logMessage: String,
        override val cause: Throwable? = null
    ) : AppError()

    data class MemoryError(
        override val userMessage: String,
        override val logMessage: String,
        override val cause: Throwable? = null
    ) : AppError()
}
