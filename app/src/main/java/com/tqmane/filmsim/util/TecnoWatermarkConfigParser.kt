package com.tqmane.filmsim.util

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parser for TECNO TranssionWM.json watermark configuration.
 * 
 * This parses the JSON configuration file that defines:
 * - Mode layouts (Mode_1a through Mode_4c for portrait/landscape)
 * - Bar dimensions and colors
 * - Text profiles with font, size, color, and position
 * - Icon profiles for logos and dots
 * - Backdrop/texture configuration
 */
class TecnoWatermarkConfigParser(private val context: Context) {

    private val tag = "TecnoConfigParser"

    fun parseConfig(assetPath: String = "watermark/TECNO/TranssionWM.json"): TecnoWatermarkTemplate? {
        return try {
            val inputStream = context.assets.open(assetPath)
            val content = inputStream.bufferedReader().use { it.readText() }
            parseContent(content)
        } catch (e: Exception) {
            Log.e(tag, "Error parsing TECNO config: $assetPath", e)
            null
        }
    }

    private fun parseContent(content: String): TecnoWatermarkTemplate? {
        return try {
            val root = JSONObject(content)
            val watermark = root.getJSONObject("WATERMARK")
            
            val layouts = watermark.getJSONArray("WM_LAYOUTS")
            val portraitModes = parseModeList(layouts.getJSONArray(0))
            val landscapeModes = parseModeList(layouts.getJSONArray(1))
            
            TecnoWatermarkTemplate(
                portraitModes = portraitModes,
                landscapeModes = landscapeModes
            )
        } catch (e: Exception) {
            Log.e(tag, "Error parsing TECNO JSON content", e)
            null
        }
    }

    private fun parseModeList(array: JSONArray): List<TecnoMode> {
        val modes = mutableListOf<TecnoMode>()
        for (i in 0 until array.length()) {
            val modeName = array.getString(i)
            modes.add(TecnoMode(name = modeName))
        }
        return modes
    }

    fun getMode(template: TecnoWatermarkTemplate, modeName: String, isLandscape: Boolean): TecnoModeConfig? {
        return try {
            val inputStream = context.assets.open("watermark/TECNO/TranssionWM.json")
            val content = inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(content)
            val watermark = root.getJSONObject("WATERMARK")
            
            if (watermark.has(modeName)) {
                parseModeConfig(watermark.getJSONObject(modeName))
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting mode $modeName", e)
            null
        }
    }

    private fun parseModeConfig(mode: JSONObject): TecnoModeConfig {
        // Parse bar config
        val barColor = parseColorArray(mode.optJSONArray("BAR_COLOR") ?: JSONArray("[255, 255, 255]"))
        val barSize = parseIntArray(mode.optJSONArray("BAR_SIZE") ?: JSONArray("[1080, 113]"))
        
        // Parse backdrop
        val backdropValid = mode.optBoolean("BACKDROP_IS_VALID", true)
        val backdrop = if (mode.has("BACKDROP_PROFILE")) {
            parseBackdropProfile(mode.getJSONObject("BACKDROP_PROFILE"))
        } else null

        // Parse brand profile
        val brand = if (mode.has("BRAND_PROFILE")) {
            parseBrandProfile(mode.getJSONObject("BRAND_PROFILE"))
        } else null

        // Parse icon profiles
        val icons = mutableListOf<TecnoIconProfile>()
        if (mode.has("ICON_PROFILES")) {
            val iconArray = mode.getJSONArray("ICON_PROFILES")
            for (i in 0 until iconArray.length()) {
                icons.add(parseIconProfile(iconArray.getJSONObject(i)))
            }
        }

        // Parse text profiles
        val texts = mutableListOf<TecnoTextProfile>()
        if (mode.has("TEXT_PROFILES")) {
            val textArray = mode.getJSONArray("TEXT_PROFILES")
            for (i in 0 until textArray.length()) {
                texts.add(parseTextProfile(textArray.getJSONObject(i)))
            }
        }

        return TecnoModeConfig(
            barColor = barColor,
            barWidth = barSize.getOrElse(0) { 1080 },
            barHeight = barSize.getOrElse(1) { 113 },
            backdropValid = backdropValid,
            backdrop = backdrop,
            brand = brand,
            brandName = brand?.textBrandName ?: "", // Extract brand name for fallback
            iconProfiles = icons,
            textProfiles = texts
        )
    }

    private fun parseColorArray(array: JSONArray): Int {
        val r = array.optInt(0, 255)
        val g = array.optInt(1, 255)
        val b = array.optInt(2, 255)
        return android.graphics.Color.rgb(r, g, b)
    }

    private fun parseIntArray(array: JSONArray): List<Int> {
        val result = mutableListOf<Int>()
        for (i in 0 until array.length()) {
            result.add(array.getInt(i))
        }
        return result
    }

    private fun parseBackdropProfile(obj: JSONObject): TecnoBackdropProfile {
        return TecnoBackdropProfile(
            iconFileName = obj.optString("ICON_FILE_NAME", ""),
            iconCoordinate = parseFloatPair(obj.optJSONArray("ICON_COORDINATE")),
            iconSize = parseFloatPair(obj.optJSONArray("ICON_SIZE")),
            tuningCoordinate = parseFloatPair(obj.optJSONArray("TUNING_COORDINATE"))
        )
    }

    private fun parseBrandProfile(obj: JSONObject): TecnoBrandProfile {
        val isText = obj.optBoolean("TYPE_TEXT", true)
        val brandName = if (isText) {
            obj.optString("TEXT_BRAND_NAME", "TECNO")
        } else ""
        
        return TecnoBrandProfile(
            typeText = isText,
            textBrandName = brandName
        )
    }

    private fun parseIconProfile(obj: JSONObject): TecnoIconProfile {
        val relyProfile = if (obj.has("RELY_PROFILE")) {
            parseRelyProfile(obj.getJSONObject("RELY_PROFILE"))
        } else null
        
        return TecnoIconProfile(
            iconFileName = obj.optString("ICON_FILE_NAME", ""),
            iconCoordinate = parseFloatPair(obj.optJSONArray("ICON_COORDINATE")),
            iconSize = parseFloatPair(obj.optJSONArray("ICON_SIZE")),
            tuningCoordinate = parseFloatPair(obj.optJSONArray("TUNING_COORDINATE")),
            relyOnElem = obj.optBoolean("RELY_ON_ELEM", false),
            relyProfile = relyProfile
        )
    }
    
    private fun parseRelyProfile(obj: JSONObject): TecnoRelyProfile {
        return TecnoRelyProfile(
            relyType = obj.optInt("RELY_TYPE", 0),
            relyIndex = obj.optInt("RELY_INDEX", 0),
            reltOnLeftX = obj.optBoolean("RELT_ON_LEFT_X", false)
        )
    }
    
    private fun parseTextProfile(obj: JSONObject): TecnoTextProfile {
        val fontProfile = if (obj.has("FONT_PROFILE")) {
            parseFontProfile(obj.getJSONObject("FONT_PROFILE"))
        } else null
        
        return TecnoTextProfile(
            fontProfile = fontProfile,
            spaceRatio = obj.optDouble("SPACE_RATIO", 0.42).toFloat(),
            characterDistanceRatio = obj.optDouble("CHARACTER_DISTANCE_RATIO", 0.0).toFloat(),
            textCoordinate = parseFloatPair(obj.optJSONArray("TEXT_COORDINATE")),
            tuningCoordinate = parseFloatPair(obj.optJSONArray("TUNING_COORDINATE")),
            renderDirection = obj.optInt("RENDER_DIRECTION", 0),
            relyOnElem = obj.optBoolean("RELY_ON_ELEM", false),
            relyProfile = if (obj.has("RELY_PROFILE")) parseRelyProfile(obj.getJSONObject("RELY_PROFILE")) else null
        )
    }
    
    private fun parseFontProfile(obj: JSONObject): TecnoFontProfile {
        return TecnoFontProfile(
            fontFileName = obj.optString("FONT_FILE_NAME", ""),
            fontSize = obj.optDouble("FONT_SIZE", 29.0).toFloat(),
            fontColor = parseColorArray(obj.optJSONArray("FONT_COLOR") ?: JSONArray("[0, 0, 0]")),
            fontIntensity = obj.optDouble("FONT_INTENSITY", 1.0).toFloat()
        )
    }
    
    private fun parseFloatPair(array: JSONArray?): Pair<Float, Float> {
        if (array == null || array.length() < 2) return Pair(0f, 0f)
        return Pair(array.optDouble(0, 0.0).toFloat(), array.optDouble(1, 0.0).toFloat())
    }
}
