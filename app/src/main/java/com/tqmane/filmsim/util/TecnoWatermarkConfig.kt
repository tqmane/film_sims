package com.tqmane.filmsim.util

/**
 * Data classes for TECNO watermark configuration parsed from TranssionWM.json
 */

data class TecnoWatermarkTemplate(
    val portraitModes: List<TecnoMode>,
    val landscapeModes: List<TecnoMode>
)

data class TecnoMode(
    val name: String
)

data class TecnoModeConfig(
    val barColor: Int,
    val barWidth: Int,
    val barHeight: Int,
    val backdropValid: Boolean,
    val backdrop: TecnoBackdropProfile?,
    val brand: TecnoBrandProfile?,
    val brandName: String = "", // Extracted from brand profile for use in rendering
    val iconProfiles: List<TecnoIconProfile>,
    val textProfiles: List<TecnoTextProfile>
)

data class TecnoBackdropProfile(
    val iconFileName: String,
    val iconCoordinate: Pair<Float, Float>,
    val iconSize: Pair<Float, Float>,
    val tuningCoordinate: Pair<Float, Float>
)

data class TecnoBrandProfile(
    val typeText: Boolean,
    val textBrandName: String
)

data class TecnoIconProfile(
    val iconFileName: String,
    val iconCoordinate: Pair<Float, Float>,
    val iconSize: Pair<Float, Float>,
    val tuningCoordinate: Pair<Float, Float>,
    val relyOnElem: Boolean,
    val relyProfile: TecnoRelyProfile?
)

data class TecnoRelyProfile(
    val relyType: Int,
    val relyIndex: Int,
    val reltOnLeftX: Boolean
)

data class TecnoTextProfile(
    val fontProfile: TecnoFontProfile?,
    val spaceRatio: Float,
    val characterDistanceRatio: Float,
    val textCoordinate: Pair<Float, Float>,
    val tuningCoordinate: Pair<Float, Float>,
    val renderDirection: Int,
    val relyOnElem: Boolean,
    val relyProfile: TecnoRelyProfile?
)

data class TecnoFontProfile(
    val fontFileName: String,
    val fontSize: Float,
    val fontColor: Int,
    val fontIntensity: Float
)
