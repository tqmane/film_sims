package com.tqmane.filmsim.ui.component

import android.graphics.Bitmap
import android.graphics.Shader
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.tqmane.filmsim.ui.theme.LiquidColors

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalContext
import com.tqmane.filmsim.util.SensorUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring

/**
 * Animated background with aurora mesh gradient and film grain noise overlay.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tiltData by remember { SensorUtils.observeTilt(context) }.collectAsState(initial = SensorUtils.TiltData(0f, 0f))

    // Smooth out sensor jitter with a spring animation
    val smoothPitch by animateFloatAsState(targetValue = tiltData.pitch, animationSpec = spring(dampingRatio = 0.8f, stiffness = 50f), label = "pitch")
    val smoothRoll by animateFloatAsState(targetValue = tiltData.roll, animationSpec = spring(dampingRatio = 0.8f, stiffness = 50f), label = "roll")

    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val amberOffsetX by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "amber_x"
    )
    val amberOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "amber_y"
    )
    val cyanOffsetX by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(12000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cyan_x"
    )
    val cyanOffsetY by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cyan_y"
    )
    val purpleOffsetX by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "purple_x"
    )
    val purpleOffsetY by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "purple_y"
    )
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LiquidColors.Background)
            .drawBehind {
                drawAuroraLights(
                    amberOffsetX = amberOffsetX, amberOffsetY = amberOffsetY,
                    cyanOffsetX = cyanOffsetX, cyanOffsetY = cyanOffsetY,
                    purpleOffsetX = purpleOffsetX, purpleOffsetY = purpleOffsetY,
                    scalePulse = scalePulse,
                    parallaxX = smoothRoll,
                    parallaxY = smoothPitch
                )
            }
    ) {
        NoiseOverlay(opacity = 0.025f, modifier = Modifier.fillMaxSize())
    }
}

private fun DrawScope.drawAuroraLights(
    amberOffsetX: Float, amberOffsetY: Float,
    cyanOffsetX: Float, cyanOffsetY: Float,
    purpleOffsetX: Float, purpleOffsetY: Float,
    scalePulse: Float,
    parallaxX: Float,
    parallaxY: Float
) {
    val w = size.width; val h = size.height
    
    // Parallax multipliers - different depths for different colors
    val pXAmber = parallaxX * w * 0.15f; val pYAmber = parallaxY * h * 0.15f
    val pXCyan  = parallaxX * w * -0.1f; val pYCyan  = parallaxY * h * -0.1f
    val pXPurple = parallaxX * w * 0.05f; val pYPurple = parallaxY * h * 0.05f

    val amberCenter = Offset(w * 0.2f * amberOffsetX + w * 0.1f + pXAmber, h * 0.3f * amberOffsetY + h * 0.1f + pYAmber)

    val amberRadius = (w * 0.4f) * scalePulse
    drawCircle(
        brush = RadialGradient(listOf(LiquidColors.AmbientAmber.copy(alpha = 0.10f), LiquidColors.AmbientAmber.copy(alpha = 0f)), amberCenter, amberRadius),
        radius = amberRadius, center = amberCenter
    )

    val cyanCenter = Offset(w * 0.8f * cyanOffsetX + w * 0.1f + pXCyan, h * 0.7f * cyanOffsetY + h * 0.1f + pYCyan)
    val cyanRadius = (w * 0.35f) * scalePulse
    drawCircle(
        brush = RadialGradient(listOf(LiquidColors.AmbientCyan.copy(alpha = 0.08f), LiquidColors.AmbientCyan.copy(alpha = 0f)), cyanCenter, cyanRadius),
        radius = cyanRadius, center = cyanCenter
    )

    val purpleCenter = Offset(w * 0.5f * purpleOffsetX + w * 0.3f + pXPurple, h * 0.8f * purpleOffsetY + h * 0.1f + pYPurple)
    val purpleRadius = (w * 0.3f) * scalePulse
    drawCircle(
        brush = RadialGradient(listOf(LiquidColors.AmbientPurple.copy(alpha = 0.06f), LiquidColors.AmbientPurple.copy(alpha = 0f)), purpleCenter, purpleRadius),
        radius = purpleRadius, center = purpleCenter
    )
}

private fun RadialGradient(colors: List<Color>, center: Offset, radius: Float): Brush {
    return object : ShaderBrush() {
        override fun createShader(size: Size): android.graphics.Shader {
            return RadialGradientShader(center = center, radius = radius, colors = colors)
        }
    }
}

@Composable
private fun NoiseOverlay(opacity: Float, modifier: Modifier = Modifier) {
    val noiseBitmap = remember { generateNoiseBitmap(256, 256) }
    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    shader = android.graphics.BitmapShader(noiseBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                    this.alpha = opacity
                    blendMode = BlendMode.Overlay
                }
                canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint.asFrameworkPaint())
            }
        }
    )
}

private fun generateNoiseBitmap(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    var seed = 12345L
    for (i in pixels.indices) {
        seed = (seed * 1103515245 + 12345) and 0xFFFFFFFF
        val gray = ((seed % 256) - 128).toInt().coerceIn(0, 255)
        pixels[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}
