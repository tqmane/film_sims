package com.tqmane.filmsim.domain

import android.content.Context
import com.tqmane.filmsim.util.SecurityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for environment trust checks.
 * Centralizes all calls to [SecurityManager.isEnvironmentTrusted] to avoid
 * scattering security logic across multiple layers.
 *
 * All security gating should call [isTrusted] rather than invoking
 * [SecurityManager] directly.
 */
@Singleton
class SecurityCheckUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isTrusted(): Boolean = SecurityManager.isEnvironmentTrusted(context)
}
