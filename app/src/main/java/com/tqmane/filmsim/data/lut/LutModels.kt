package com.tqmane.filmsim.data.lut

/**
 * Individual LUT item with name and asset path.
 */
data class LutItem(
    val name: String,
    val assetPath: String
)

/**
 * Category of LUTs within a brand (e.g. "Portrait", "Landscape").
 */
data class LutCategory(
    val name: String,
    val displayName: String,
    val items: List<LutItem>
)

/**
 * Brand containing multiple LUT categories (e.g. "OnePlus", "Samsung").
 */
data class LutBrand(
    val name: String,
    val displayName: String,
    val categories: List<LutCategory>
)
