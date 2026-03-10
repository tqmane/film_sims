package com.tqmane.filmsim.domain

import com.tqmane.filmsim.data.lut.parser.LutParserFactory
import com.tqmane.filmsim.di.IoDispatcher
import com.tqmane.filmsim.util.CubeLUT
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Contract for LUT parsing operations. */
interface LutApplyUseCase {
    suspend fun parseLut(assetPath: String): CubeLUT?
}

class LutApplyUseCaseImpl @Inject constructor(
    private val lutParserFactory: LutParserFactory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : LutApplyUseCase {

    override suspend fun parseLut(assetPath: String): CubeLUT? =
        withContext(ioDispatcher) {
            lutParserFactory.parse(assetPath)
        }
}
