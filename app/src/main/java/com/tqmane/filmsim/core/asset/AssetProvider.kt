package com.tqmane.filmsim.core.asset

import android.content.Context
import android.graphics.Typeface
import java.io.InputStream

/**
 * Contract for asset access. Abstracts encrypted/unencrypted asset loading.
 */
interface AssetProvider {
    fun openAsset(path: String): InputStream
    fun loadTypeface(path: String): Typeface
}
