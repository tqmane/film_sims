package com.tqmane.filmsim.di

import android.content.Context
import com.tqmane.filmsim.util.ReleaseInfo
import com.tqmane.filmsim.util.UpdateChecker as UpdateCheckerObj
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable wrapper around the static [UpdateCheckerObj].
 * Holds the application [Context] so that the ViewModel layer never needs it.
 */
@Singleton
class UpdateCheckerWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun checkForUpdate(force: Boolean = false): ReleaseInfo? =
        UpdateCheckerObj.checkForUpdate(context, force)

    fun skipVersion(version: String) =
        UpdateCheckerObj.skipVersion(context, version)
}
