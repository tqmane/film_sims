package com.tqmane.filmsim.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.util.Log

class VivoFontManager(private val context: Context) {

    private val tag = "VivoFontManager"
    private val fontCache = mutableMapOf<Int, Typeface>()

    companion object {
        private const val FONT_PATH = "watermark/vivo/fonts/"
        private const val FONT_PATH_ALT = "vivo_watermark_full2/assets/fonts/"
        private val FONT_MAP = mapOf(
            0 to "Roboto-Bold.ttf",
            1 to "vivoCameraVF.ttf",
            2 to "vivotype-Heavy.ttf",
            3 to "vivo-Regular.otf",
            4 to "ZEISSFrutigerNextW1G-Bold.ttf",
            5 to "Roboto-Bold.ttf",
            6 to "IQOOTYPE-Bold.ttf",
            7 to "vivoSansExpVF.ttf",
            8 to "vivoCameraVF.ttf",
            9 to "IQOOTYPE-Bold.ttf",
            10 to "vivotypeSimple-Bold.ttf"
        )
    }

    fun getTypeface(typeface: Int): Typeface {
        return fontCache.getOrPut(typeface) {
            loadTypeface(typeface)
        }
    }

    private fun loadTypeface(typeface: Int): Typeface {
        return try {
            val fontFile = FONT_MAP[typeface]
            if (fontFile != null) {
                // Try primary path first, then alternative path
                try {
                    Typeface.createFromAsset(context.assets, FONT_PATH + fontFile)
                } catch (_: Exception) {
                    try {
                        Typeface.createFromAsset(context.assets, FONT_PATH_ALT + fontFile)
                    } catch (_: Exception) {
                        Typeface.create("sans-serif", Typeface.NORMAL)
                    }
                }
            } else {
                Typeface.create("sans-serif", Typeface.NORMAL)
            }
        } catch (e: Exception) {
            Typeface.create("sans-serif", Typeface.NORMAL)
        }
    }

    fun createPaint(
        typeface: Int = 0,
        weight: Int = 400,
        textSize: Float = 12f,
        color: Int = 0xFF000000.toInt(),
        letterSpacing: Float = 0f
    ): Paint {
        val baseTypeface = getTypeface(typeface)
        val finalTypeface = if (Build.VERSION.SDK_INT >= 28) {
            Typeface.create(baseTypeface, weight, false)
        } else {
            baseTypeface
        }
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = finalTypeface
            this.textSize = textSize
            this.color = color
            this.letterSpacing = letterSpacing
        }
    }

    fun parseColor(colorStr: String): Int {
        return try {
            when {
                colorStr.startsWith("#") -> {
                    val hex = colorStr.substring(1)
                    when (hex.length) {
                        6 -> {
                            val r = hex.substring(0, 2).toInt(16)
                            val g = hex.substring(2, 4).toInt(16)
                            val b = hex.substring(4, 6).toInt(16)
                            0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
                        }
                        8 -> {
                            val a = hex.substring(0, 2).toInt(16)
                            val r = hex.substring(2, 4).toInt(16)
                            val g = hex.substring(4, 6).toInt(16)
                            val b = hex.substring(6, 8).toInt(16)
                            (a shl 24) or (r shl 16) or (g shl 8) or b
                        }
                        else -> 0xFF000000.toInt()
                    }
                }
                else -> 0xFF000000.toInt()
            }
        } catch (e: Exception) {
            0xFF000000.toInt()
        }
    }

    fun getTextPaint(textParam: VivoTextParam, scaleFactor: Float = 1f): Paint {
        val color = parseColor(textParam.textcolor)
        val textSize = textParam.textsize * scaleFactor
        return createPaint(
            typeface = textParam.typeface,
            weight = textParam.textfontweight,
            textSize = textSize,
            color = color,
            letterSpacing = textParam.letterspacing
        )
    }

    fun clearCache() {
        fontCache.clear()
    }
}
