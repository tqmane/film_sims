package com.tqmane.filmsim.domain

import android.content.Context
import com.tqmane.filmsim.util.CubeLUT
import com.tqmane.filmsim.util.CubeLUTParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use-case for parsing and applying LUTs.
 */
class LutApplyUseCase @Inject constructor() {

    /**
     * Parse a LUT file from assets.
     * Returns null on failure.
     */
    suspend fun parseLut(context: Context, assetPath: String): CubeLUT? =
        withContext(Dispatchers.IO) {
            CubeLUTParser.parse(context, assetPath)
        }
}
