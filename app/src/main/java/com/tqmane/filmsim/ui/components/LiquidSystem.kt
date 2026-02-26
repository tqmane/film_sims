package com.tqmane.filmsim.ui.components

import android.graphics.Bitmap
import android.graphics.Shader
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.data.LutItem
import com.tqmane.filmsim.ui.theme.LiquidColors
import com.tqmane.filmsim.ui.theme.LiquidDimensions
import com.tqmane.filmsim.util.CubeLUTParser
import com.tqmane.filmsim.util.LutBitmapProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════════════════════════════
// LIVING BACKGROUND - Aurora Mesh Gradient with Noise
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Animated background with aurora mesh gradient and film grain noise overlay.
 * Creates an immersive "Liquid Film Noir" atmosphere.
 */
@Composable
fun LivingBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    
    // Animate aurora positions with slow, breathing movement
    val amberOffsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amber_x"
    )
    
    val amberOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amber_y"
    )
    
    val cyanOffsetX by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cyan_x"
    )
    
    val cyanOffsetY by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cyan_y"
    )
    
    val purpleOffsetX by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "purple_x"
    )
    
    val purpleOffsetY by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "purple_y"
    )
    
    // Scale pulsation for "breathing" effect
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_pulse"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LiquidColors.Background)
            .drawBehind {
                // Draw aurora lights
                drawAuroraLights(
                    amberOffsetX = amberOffsetX,
                    amberOffsetY = amberOffsetY,
                    cyanOffsetX = cyanOffsetX,
                    cyanOffsetY = cyanOffsetY,
                    purpleOffsetX = purpleOffsetX,
                    purpleOffsetY = purpleOffsetY,
                    scalePulse = scalePulse
                )
            }
    ) {
        // Noise overlay (film grain simulation)
        NoiseOverlay(
            opacity = 0.025f,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun DrawScope.drawAuroraLights(
    amberOffsetX: Float,
    amberOffsetY: Float,
    cyanOffsetX: Float,
    cyanOffsetY: Float,
    purpleOffsetX: Float,
    purpleOffsetY: Float,
    scalePulse: Float
) {
    val width = size.width
    val height = size.height
    
    // Amber light (top-left area)
    val amberBrush = RadialGradient(
        colors = listOf(LiquidColors.AmbientAmber.copy(alpha = 0.10f), LiquidColors.AmbientAmber.copy(alpha = 0f)),
        center = Offset(width * 0.2f * amberOffsetX + width * 0.1f, height * 0.3f * amberOffsetY + height * 0.1f),
        radius = (width * 0.4f) * scalePulse
    )
    drawCircle(brush = amberBrush, radius = (width * 0.4f) * scalePulse, center = Offset(width * 0.2f * amberOffsetX + width * 0.1f, height * 0.3f * amberOffsetY + height * 0.1f))
    
    // Cyan light (bottom-right area)
    val cyanBrush = RadialGradient(
        colors = listOf(LiquidColors.AmbientCyan.copy(alpha = 0.08f), LiquidColors.AmbientCyan.copy(alpha = 0f)),
        center = Offset(width * 0.8f * cyanOffsetX + width * 0.1f, height * 0.7f * cyanOffsetY + height * 0.1f),
        radius = (width * 0.35f) * scalePulse
    )
    drawCircle(brush = cyanBrush, radius = (width * 0.35f) * scalePulse, center = Offset(width * 0.8f * cyanOffsetX + width * 0.1f, height * 0.7f * cyanOffsetY + height * 0.1f))
    
    // Purple light (center-bottom area)
    val purpleBrush = RadialGradient(
        colors = listOf(LiquidColors.AmbientPurple.copy(alpha = 0.06f), LiquidColors.AmbientPurple.copy(alpha = 0f)),
        center = Offset(width * 0.5f * purpleOffsetX + width * 0.3f, height * 0.8f * purpleOffsetY + height * 0.1f),
        radius = (width * 0.3f) * scalePulse
    )
    drawCircle(brush = purpleBrush, radius = (width * 0.3f) * scalePulse, center = Offset(width * 0.5f * purpleOffsetX + width * 0.3f, height * 0.8f * purpleOffsetY + height * 0.1f))
}

/**
 * Simple radial gradient helper
 */
private fun RadialGradient(
    colors: List<Color>,
    center: Offset,
    radius: Float
): Brush {
    return object : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            return RadialGradientShader(
                center = center,
                radius = radius,
                colors = colors
            )
        }
    }
}

/**
 * Noise overlay for film grain effect
 */
@Composable
private fun NoiseOverlay(
    opacity: Float,
    modifier: Modifier = Modifier
) {
    // Generate noise bitmap once
    val noiseBitmap = remember { generateNoiseBitmap(256, 256) }
    
    Box(
        modifier = modifier
            .drawWithContent {
                drawContent()
                // Draw noise with overlay blend
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        shader = android.graphics.BitmapShader(
                            noiseBitmap,
                            Shader.TileMode.REPEAT,
                            Shader.TileMode.REPEAT
                        )
                        this.alpha = opacity
                        blendMode = BlendMode.Overlay
                    }
                    canvas.nativeCanvas.drawRect(
                        0f, 0f, size.width, size.height,
                        paint.asFrameworkPaint()
                    )
                }
            }
    )
}

/**
 * Generate Perlin-like noise bitmap
 */
private fun generateNoiseBitmap(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    
    // Simple noise generation (for production, consider using a proper Perlin noise)
    var seed = 12345L
    for (i in pixels.indices) {
        seed = (seed * 1103515245 + 12345) and 0xFFFFFFFF
        val gray = ((seed % 256) - 128).toInt().coerceIn(0, 255)
        pixels[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
    }
    
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

// ═══════════════════════════════════════════════════════════════════════════════
// LIQUID INTENSITY SLIDER - Enhanced slider with label and percentage display
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Styled intensity slider with label, icon, and percentage readout.
 */
@Composable
fun LiquidIntensitySlider(
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(intensity) { mutableFloatStateOf(intensity) }
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_opacity),
                contentDescription = null,
                tint = LiquidColors.AccentPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.label_intensity),
                color = LiquidColors.TextMediumEmphasis,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${(sliderValue * 100).toInt()}%",
                color = LiquidColors.AccentPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.End,
                modifier = Modifier.width(42.dp).padding(end = 8.dp)
            )
        }

        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onIntensityChange(it)
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                )
            },
            onValueChangeFinished = { },
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            colors = SliderDefaults.colors(
                thumbColor = LiquidColors.AccentPrimary,
                activeTrackColor = LiquidColors.AccentPrimary,
                inactiveTrackColor = Color(0x14FFFFFF)
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LIQUID BUTTON - With oil film effect and ripple
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Liquid-style button with "oil film on water" appearance.
 * Features press animation with scale and ripple effects.
 */
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .height(LiquidDimensions.ButtonHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        LiquidColors.GradientAccentStart,
                        LiquidColors.GradientAccentEnd
                    )
                )
            )
            .drawBehind {
                // Subtle inner shadow for depth
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.12f)
                        )
                    ),
                    size = size
                )
            }
            .border(
                1.dp,
                Color(0x25FFFFFF),
                RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                )
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Round glass button for icon actions
 */
@Composable
fun LiquidRoundButton(
    iconRes: Int,
    contentDesc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "round_button_scale"
    )
    
    Box(
        modifier = modifier
            .size(46.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color(0x12FFFFFF))
            .border(1.dp, Color(0x14FFFFFF), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                )
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDesc,
            tint = LiquidColors.TextMediumEmphasis,
            modifier = Modifier.size(21.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LIQUID CHIP- Glass chip with selection states
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Liquid-style chip with glass morphism and animated selection state.
 */
@Composable
fun LiquidChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) LiquidColors.ChipSelected else LiquidColors.ChipUnselected,
        animationSpec = tween(300),
        label = "chip_bg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (selected) LiquidColors.AccentPrimary.copy(alpha = 0.5f) else Color(0x10FFFFFF),
        animationSpec = tween(300),
        label = "chip_border"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (selected) LiquidColors.ChipSelectedText else LiquidColors.TextMediumEmphasis,
        animationSpec = tween(300),
        label = "chip_text"
    )
    
    Box(
        modifier = modifier
            .height(LiquidDimensions.ChipHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                )
                onClick()
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.01.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LIQUID CARD - For LUT items with selection glow
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Liquid-style card for LUT preview items.
 * Features animated selection state with glow effect.
 */
@Composable
fun LiquidLutCard(
    item: LutItem,
    thumbnailBitmap: Bitmap?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Async LUT preview — avoids blocking the Compose render thread
    var previewBitmap by remember(item.assetPath, thumbnailBitmap) {
        mutableStateOf<Bitmap?>(null)
    }
    var isLoadingPreview by remember(item.assetPath, thumbnailBitmap) {
        mutableStateOf(thumbnailBitmap != null)
    }
    LaunchedEffect(item.assetPath, thumbnailBitmap) {
        if (thumbnailBitmap != null) {
            isLoadingPreview = true
            previewBitmap = withContext(Dispatchers.IO) {
                try {
                    val lut = CubeLUTParser.parse(context, item.assetPath)
                    if (lut != null) LutBitmapProcessor.applyLutToBitmap(thumbnailBitmap, lut) else null
                } catch (e: Exception) { null }
            }
            isLoadingPreview = false
        } else {
            previewBitmap = null
            isLoadingPreview = false
        }
    }
    
    val borderColor by animateColorAsState(
        targetValue = if (selected) LiquidColors.AccentPrimary else Color.Transparent,
        animationSpec = tween(300),
        label = "card_border"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(start = 2.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)
            .clickable {
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                )
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .size(94.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LiquidColors.SurfaceMedium)
                .then(
                    if (selected) {
                        Modifier.border(2.5.dp, borderColor, RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    }
                )
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.5f)
                )
            }

            // Shimmer overlay while LUT preview is being generated
            if (isLoadingPreview) {
                val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
                val shimmerAlpha by shimmerTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = 0.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "shimmer_alpha"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LiquidColors.GlassSurface.copy(alpha = shimmerAlpha))
                )
            }
            
            // Selection glow overlay
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    LiquidColors.AccentPrimary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Adjust hint overlay — bottom scrim with sliders icon + label
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.60f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Sliders icon drawn with Canvas (matching sketch)
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(13.dp)) {
                            val w = size.width
                            val h = size.height
                            val stroke = 1.6.dp.toPx()
                            val knobR = 2.4.dp.toPx()
                            // Top slider line
                            drawLine(
                                color = Color.White,
                                start = Offset(0f, h * 0.28f),
                                end = Offset(w, h * 0.28f),
                                strokeWidth = stroke,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            // Top slider knob (filled circle)
                            drawCircle(
                                color = Color.White,
                                radius = knobR,
                                center = Offset(w * 0.68f, h * 0.28f)
                            )
                            // Top knob inner (dark fill to make it a ring)
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.6f),
                                radius = knobR * 0.45f,
                                center = Offset(w * 0.68f, h * 0.28f)
                            )
                            // Bottom slider line
                            drawLine(
                                color = Color.White,
                                start = Offset(0f, h * 0.72f),
                                end = Offset(w, h * 0.72f),
                                strokeWidth = stroke,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            // Bottom slider knob (filled circle)
                            drawCircle(
                                color = Color.White,
                                radius = knobR,
                                center = Offset(w * 0.32f, h * 0.72f)
                            )
                            // Bottom knob inner
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.6f),
                                radius = knobR * 0.45f,
                                center = Offset(w * 0.32f, h * 0.72f)
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = stringResource(R.string.adjustments),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.sp
                        )
                    }
                }
            }
        }
        
        Text(
            item.name,
            color = if (selected) LiquidColors.TextHighEmphasis else LiquidColors.TextMediumEmphasis,
            fontSize = 10.sp,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.01.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Clip,
            lineHeight = 12.sp,
            modifier = Modifier
                .width(94.dp)
                .padding(top = 6.dp, bottom = 2.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// GLASS BOTTOM SHEET - For LUT/Brand selection
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Glassmorphic bottom sheet with blur effect.
 * Uses RenderEffect for background blur on Android 12+.
 */
@Composable
fun GlassBottomSheet(
    modifier: Modifier = Modifier,
    squareTop: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val topRadius = if (squareTop) 0.dp else 22.dp
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = topRadius, topEnd = topRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LiquidColors.SurfaceMedium.copy(alpha = 0.95f),
                        LiquidColors.SurfaceDark.copy(alpha = 0.97f)
                    )
                )
            )
            .drawBehind {
                // Subtle top border highlight
                drawLine(
                    color = Color(0x28FFFFFF),
                    start = Offset(topRadius.toPx(), 0f),
                    end = Offset(size.width - topRadius.toPx(), 0f),
                    strokeWidth = 1f
                )
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {}
            .padding(top = 10.dp, bottom = 16.dp, start = 18.dp, end = 18.dp)
    ) {
        // Drag handle
        if (!squareTop) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 14.dp)
                    .width(44.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x50FFFFFF))
            )
        }
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION HEADER - Styled text header
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Styled section header with accent color
 */
@Composable
fun LiquidSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text.uppercase(),
        color = LiquidColors.AccentPrimary,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 0.15.sp,
        modifier = modifier.padding(bottom = 10.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// PLACEHOLDER CONTENT - Empty state display
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Placeholder content shown when no image is loaded
 */
@Composable
fun LiquidPlaceholderContent(
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Breathing animation for placeholder icon
    val infiniteTransition = rememberInfiniteTransition(label = "placeholder_breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable { onPickImage() }
            .padding(56.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pulsing icon with glow ring
        Box(contentAlignment = Alignment.Center) {
            // Outer glow ring
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(breathScale)
                    .alpha(breathAlpha * 0.4f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(LiquidColors.AccentPrimary.copy(alpha = 0.12f))
            )
            // Icon container
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(breathScale)
                    .alpha(breathAlpha)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0x22FFFFFF),
                                Color(0x10FFFFFF)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        LiquidColors.AccentPrimary.copy(alpha = breathAlpha * 0.3f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_photo),
                    contentDescription = null,
                    tint = LiquidColors.AccentPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        Text(
            stringResource(R.string.label_pick_image),
            color = LiquidColors.TextHighEmphasis,
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.005.sp,
            modifier = Modifier.padding(top = 32.dp)
        )
        
        Text(
            stringResource(R.string.desc_pick_image),
            color = LiquidColors.TextLowEmphasis,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
        
        LiquidButton(
            onClick = {
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                )
                onPickImage()
            },
            modifier = Modifier
                .padding(top = 40.dp)
                .height(58.dp)
                .widthIn(min = 220.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gallery),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.btn_open_gallery),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.01.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TOP BAR- Application header
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Application top bar with title and action buttons
 */
@Composable
fun LiquidTopBar(
    onPickImage: () -> Unit,
    onSettings: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Box(modifier = modifier
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    LiquidColors.SurfaceDark.copy(alpha = 0.75f),
                    Color(0xFF0C0C11).copy(alpha = 0.5f),
                    Color.Transparent
                )
            )
        )
        .padding(horizontal = 24.dp, vertical = 16.dp)){
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.app_name),
                    color = LiquidColors.TextHighEmphasis,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.005.sp
                )
                Text(
                    stringResource(R.string.subtitle_film_simulator).uppercase(),
                    color = LiquidColors.AccentPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.15.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            
            LiquidRoundButton(
                iconRes = R.drawable.ic_add,
                contentDesc = stringResource(R.string.btn_open_gallery),
                onClick = onPickImage,
                modifier = Modifier.padding(end = 8.dp)
            )
            
            LiquidRoundButton(
                iconRes = R.drawable.ic_settings,
                contentDesc = stringResource(R.string.title_settings),
                onClick = onSettings,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            LiquidButton(
                onClick = {
                    haptic.performHapticFeedback(
                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                    )
                    onSave()
                },
                modifier = Modifier.width(94.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_save),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    stringResource(R.string.save),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}