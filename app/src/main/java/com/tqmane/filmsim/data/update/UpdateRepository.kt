package com.tqmane.filmsim.data.update

import com.tqmane.filmsim.util.ReleaseInfo

/**
 * Contract for checking application updates.
 */
interface UpdateRepository {
    suspend fun checkForUpdate(force: Boolean = false): ReleaseInfo?
}
