package com.tqmane.filmsim.data.lut.parser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.tqmane.filmsim.util.CubeLUT
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Parses image-based LUT formats: PNG, WEBP, JPEG, Samsung .sel (HALD), strip, 1D.
 */
class ImageLutParser @Inject constructor() : LutParser {

    companion object {
        private const val TAG = "ImageLutParser"
        private const val INV_255 = 0.003921569f
    }

    override fun parse(bytes: ByteArray, assetPath: String): CubeLUT? {
        try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode image: $assetPath")
                return null
            }

            val w = bitmap.width
            val h = bitmap.height
            Log.d(TAG, "Image LUT: $assetPath, size=${w}x${h}")

            return when {
                h in 1..3 && w >= 16 -> parse1DLut(bitmap)
                w > h && h > 0 && w == h * h -> parseStripLut(bitmap, h)
                h > w && w > 0 && h == w * w -> parseVerticalStripLut(bitmap, w)
                w == 512 && h == 512 -> parseHaldLut(bitmap, 64)
                else -> {
                    val lutSize = detectLutSize(w, h)
                    if (lutSize != null) parseHaldLut(bitmap, lutSize)
                    else { Log.e(TAG, "Unknown layout ${w}x${h}"); bitmap.recycle(); null }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing image LUT: $assetPath", e)
            return null
        }
    }

    // ── HALD ────────────────────────────────────────────

    private fun parseHaldLut(bitmap: Bitmap, lutSize: Int): CubeLUT? {
        try {
            val w = bitmap.width; val h = bitmap.height
            val tilesPerRow = kotlin.math.sqrt(lutSize.toDouble()).toInt()
            val tileWidth = w / tilesPerRow; val tileHeight = h / tilesPerRow

            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            bitmap.recycle()

            val entryCount = lutSize * lutSize * lutSize
            val fb = allocateFloatBuffer(entryCount * 3)

            for (b in 0 until lutSize) {
                val tileX = b % tilesPerRow; val tileY = b / tilesPerRow
                for (g in 0 until lutSize) {
                    val pixelY = tileY * tileHeight + g
                    if (pixelY >= h) continue
                    val rowOff = pixelY * w; val baseX = tileX * tileWidth
                    for (r in 0 until lutSize) {
                        val pixelX = baseX + r
                        if (pixelX >= w) continue
                        putPixel(fb, pixels[rowOff + pixelX])
                    }
                }
            }
            fb.position(0)
            return CubeLUT(lutSize, fb)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing HALD LUT", e); return null
        }
    }

    // ── Horizontal strip ────────────────────────────────

    private fun parseStripLut(bitmap: Bitmap, lutSize: Int): CubeLUT? {
        try {
            val w = bitmap.width; val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            bitmap.recycle()

            val sample = pixels[lutSize - 1]
            val sR = (sample ushr 16) and 0xFF; val sB = sample and 0xFF
            val isMeizu = sB > sR && sB > ((sample ushr 8) and 0xFF)
            Log.d(TAG, "Strip axis: meizu=$isMeizu")

            val entryCount = lutSize * lutSize * lutSize
            val fb = allocateFloatBuffer(entryCount * 3)

            if (isMeizu) {
                for (b in 0 until lutSize) for (g in 0 until lutSize) for (r in 0 until lutSize) {
                    val px = g * lutSize + b; val py = r
                    if (px >= w || py >= h) { fb.put(0f); fb.put(0f); fb.put(0f); continue }
                    putPixel(fb, pixels[py * w + px])
                }
            } else {
                for (b in 0 until lutSize) for (g in 0 until lutSize) for (r in 0 until lutSize) {
                    val px = b * lutSize + r; val py = g
                    if (px >= w || py >= h) { fb.put(0f); fb.put(0f); fb.put(0f); continue }
                    putPixel(fb, pixels[py * w + px])
                }
            }

            fb.position(0)
            return CubeLUT(lutSize, fb)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing strip LUT", e); return null
        }
    }

    // ── Vertical strip ──────────────────────────────────

    private fun parseVerticalStripLut(bitmap: Bitmap, lutSize: Int): CubeLUT? {
        try {
            val w = bitmap.width; val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            bitmap.recycle()

            val entryCount = lutSize * lutSize * lutSize
            val fb = allocateFloatBuffer(entryCount * 3)

            for (b in 0 until lutSize) {
                val baseY = b * lutSize
                for (g in 0 until lutSize) {
                    val py = baseY + g
                    if (py >= h) continue
                    val rowOff = py * w
                    for (r in 0 until lutSize) {
                        if (r >= w) { fb.put(0f); fb.put(0f); fb.put(0f); continue }
                        putPixel(fb, pixels[rowOff + r])
                    }
                }
            }

            fb.position(0)
            return CubeLUT(lutSize, fb)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing vertical strip LUT", e); return null
        }
    }

    // ── 1D LUT ──────────────────────────────────────────

    private fun parse1DLut(bitmap: Bitmap): CubeLUT? {
        try {
            val w = bitmap.width; val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            bitmap.recycle()

            val rCurve = FloatArray(w); val gCurve = FloatArray(w); val bCurve = FloatArray(w)
            for (i in 0 until w) {
                val p = pixels[i]
                rCurve[i] = ((p ushr 16) and 0xFF) / 255f
                gCurve[i] = ((p ushr 8) and 0xFF) / 255f
                bCurve[i] = (p and 0xFF) / 255f
            }

            val lutSize = 32
            val entryCount = lutSize * lutSize * lutSize
            val fb = allocateFloatBuffer(entryCount * 3)

            for (b in 0 until lutSize) {
                val bi = (b * (w - 1) / (lutSize - 1)).coerceIn(0, w - 1)
                for (g in 0 until lutSize) {
                    val gi = (g * (w - 1) / (lutSize - 1)).coerceIn(0, w - 1)
                    for (r in 0 until lutSize) {
                        val ri = (r * (w - 1) / (lutSize - 1)).coerceIn(0, w - 1)
                        fb.put(rCurve[ri]); fb.put(gCurve[gi]); fb.put(bCurve[bi])
                    }
                }
            }

            fb.position(0)
            return CubeLUT(lutSize, fb)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing 1D LUT", e); return null
        }
    }

    // ── Helpers ──────────────────────────────────────────

    private fun detectLutSize(width: Int, height: Int): Int? {
        if (width > height && height > 0 && width == height * height) return height
        if (width == height) {
            for (size in listOf(64, 36, 25, 16, 9, 4)) {
                val sqrt = kotlin.math.sqrt(size.toDouble()).toInt()
                if (sqrt * sqrt == size && size * sqrt == width) return size
            }
        }
        return null
    }

    private fun allocateFloatBuffer(floatCount: Int) =
        ByteBuffer.allocateDirect(floatCount * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private fun putPixel(fb: java.nio.FloatBuffer, pixel: Int) {
        fb.put(((pixel ushr 16) and 0xFF) * INV_255)
        fb.put(((pixel ushr 8) and 0xFF) * INV_255)
        fb.put((pixel and 0xFF) * INV_255)
    }
}
