package com.tqmane.filmsim.core.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for environment trust checks.
 * Centralizes all security gating through [SecurityChecker] interface.
 */
@Singleton
class SecurityCheckUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityChecker: SecurityChecker
) {
    fun isTrusted(): Boolean = securityChecker.isEnvironmentTrusted(context)
}
