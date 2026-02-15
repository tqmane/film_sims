package com.tqmane.filmsim.domain

import android.content.Context
import com.tqmane.filmsim.di.IoDispatcher
import com.tqmane.filmsim.util.CubeLUT
import com.tqmane.filmsim.util.CubeLUTParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Contract for LUT parsing operations. */
interface LutApplyUseCase {
    suspend fun parseLut(assetPath: String): CubeLUT?
}

class LutApplyUseCaseImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : LutApplyUseCase {

    override suspend fun parseLut(assetPath: String): CubeLUT? =
        withContext(ioDispatcher) {
            CubeLUTParser.parse(context, assetPath)
        }
}
