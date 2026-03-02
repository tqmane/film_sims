package com.tqmane.filmsim.data.lut.parser

import com.tqmane.filmsim.util.CubeLUT

/**
 * Contract for parsing LUT files of a specific format.
 */
interface LutParser {
    fun parse(bytes: ByteArray, assetPath: String): CubeLUT?
}
