package com.tqmane.filmsim.core.security

import android.content.Context

/**
 * Contract for environment trust verification.
 * Enables mocking in tests without relying on static SecurityManager.
 */
interface SecurityChecker {
    fun isEnvironmentTrusted(context: Context): Boolean
    fun invalidateCache()
}
