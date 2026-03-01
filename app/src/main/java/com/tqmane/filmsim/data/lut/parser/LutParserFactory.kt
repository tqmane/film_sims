package com.tqmane.filmsim.data.lut.parser

import android.content.Context
import android.util.Log
import com.tqmane.filmsim.core.asset.AssetProvider
import com.tqmane.filmsim.data.lut.cache.LutCacheManager
import com.tqmane.filmsim.util.CubeLUT
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes LUT parsing to the correct format-specific parser and manages cache.
 */
@Singleton
class LutParserFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assetProvider: AssetProvider,
    private val cache: LutCacheManager,
    private val cubeLutParser: CubeLutParser,
    private val binLutParser: BinLutParser,
    private val imageLutParser: ImageLutParser
) {
    companion object {
        private const val TAG = "LutParserFactory"

        private val IMAGE_EXTENSIONS = setOf("png", "webp", "jpg", "jpeg", "sel")
        private val BIN_EXTENSIONS = setOf("bin", "dat")
    }

    fun parse(assetPath: String): CubeLUT? {
        cache.get(assetPath)?.let { return it }

        val fileName = assetPath.substringAfterLast('/')
        val stripped = if (fileName.endsWith(".enc", ignoreCase = true))
            fileName.removeSuffix(".enc").removeSuffix(".ENC") else fileName
        val ext = stripped.substringAfterLast('.', "").lowercase()
        val hasExtension = stripped.contains('.')

        val parser: LutParser = when {
            ext in IMAGE_EXTENSIONS -> imageLutParser
            ext == "cube" -> cubeLutParser
            ext in BIN_EXTENSIONS -> binLutParser
            !hasExtension -> binLutParser
            else -> {
                Log.w(TAG, "Unknown LUT extension: $ext ($assetPath)")
                return null
            }
        }

        val bytes = try {
            assetProvider.openAsset(assetPath).use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read asset: $assetPath", e)
            return null
        }

        val result = parser.parse(bytes, assetPath)
        if (result != null) cache.put(assetPath, result)
        return result
    }
}
