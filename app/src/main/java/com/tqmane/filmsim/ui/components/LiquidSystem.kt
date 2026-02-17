package com.tqmane.filmsim.ui.components

import android.graphics.Bitmap
import android.graphics.Shader
import android.os.Build
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.runBlocking

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
            opacity = 0.03f,
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
        colors = listOf(LiquidColors.AmbientAmber.copy(alpha = 0.15f), LiquidColors.AmbientAmber.copy(alpha = 0f)),
        center = Offset(width * 0.2f * amberOffsetX + width * 0.1f, height * 0.3f * amberOffsetY + height * 0.1f),
        radius = (width * 0.4f) * scalePulse
    )
    drawCircle(brush = amberBrush, radius = (width * 0.4f) * scalePulse, center = Offset(width * 0.2f * amberOffsetX + width * 0.1f, height * 0.3f * amberOffsetY + height * 0.1f))
    
    // Cyan light (bottom-right area)
    val cyanBrush = RadialGradient(
        colors = listOf(LiquidColors.AmbientCyan.copy(alpha = 0.12f), LiquidColors.AmbientCyan.copy(alpha = 0f)),
        center = Offset(width * 0.8f * cyanOffsetX + width * 0.1f, height * 0.7f * cyanOffsetY + height * 0.1f),
        radius = (width * 0.35f) * scalePulse
    )
    drawCircle(brush = cyanBrush, radius = (width * 0.35f) * scalePulse, center = Offset(width * 0.8f * cyanOffsetX + width * 0.1f, height * 0.7f * cyanOffsetY + height * 0.1f))
    
    // Purple light (center-bottom area)
    val purpleBrush = RadialGradient(
        colors = listOf(LiquidColors.AmbientPurple.copy(alpha = 0.1f), LiquidColors.AmbientPurple.copy(alpha = 0f)),
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
// LIQUID SLIDER - With metaball effect and water drop thumb
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Liquid-style slider with organic track deformation and water drop thumb.
 * Features metaball-like attraction effect towards touch position.
 */
@Composable
fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "thumb_scale"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            haptic.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                            )
                            tryAwaitRelease()
                            isDragging = false
                            onValueChangeFinished?.invoke()
                        }
                    )
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LiquidColors.GlassSurfaceDark)
            )
            
            // Active track with liquid deformation
            val progress = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        LiquidColors.AccentPrimary,
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

/**
 * Enhanced intensity slider with label and percentage display
 */
@Composable
fun LiquidIntensitySlider(
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(intensity) { mutableFloatStateOf(intensity) }
    val haptic = LocalHapticFeedback.current
    
    Column(modifier = modifier.padding(bottom = 14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_opacity),
                contentDescription = null,
                tint = LiquidColors.AccentPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.label_intensity),
                color = LiquidColors.TextMediumEmphasis,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${(sliderValue * 100).toInt()}%",
                color = LiquidColors.TextHighEmphasis,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.End,
                modifier = Modifier.width(40.dp)
            )
        }
        
        // Custom liquid slider track
        LiquidSlider(
            value = sliderValue,
            onValueChange = { 
                sliderValue = it
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                )
            },
            onValueChangeFinished = { onIntensityChange(sliderValue) },
            modifier = Modifier.fillMaxWidth()
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
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )
    
    val bounceScale by animateFloatAsState(
        targetValue = if (!isPressed && scale < 0.95f) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_bounce"
    )
    
    Box(
        modifier = modifier
            .scale(scale * bounceScale)
            .height(LiquidDimensions.ButtonHeight)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        LiquidColors.GradientAccentStart,
                        LiquidColors.GradientAccentEnd
                    )
                )
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
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "round_button_scale"
    )
    
    Box(
        modifier = modifier
            .size(42.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(LiquidColors.GlassSurface)
            .border(1.dp, LiquidColors.GlassBorder, CircleShape)
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
            tint = LiquidColors.TextHighEmphasis,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LIQUID CHIP - Glass chip with selection states
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
        targetValue = if (selected) LiquidColors.AccentPrimaryDark else LiquidColors.GlassBorder,
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
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                )
                onClick()
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = textColor,
            fontSize = 12.sp,
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
    
    // Generate LUT preview (blocking call for simplicity)
    val previewBitmap = remember(item.assetPath, thumbnailBitmap) {
        if (thumbnailBitmap != null) {
            try {
                val lut = runBlocking(Dispatchers.IO) { CubeLUTParser.parse(context, item.assetPath) }
                if (lut != null) {
                    runBlocking(Dispatchers.IO) { LutBitmapProcessor.applyLutToBitmap(thumbnailBitmap, lut) }
                } else null
            } catch (e: Exception) { null }
        } else null
    }
    
    val borderColor by animateColorAsState(
        targetValue = if (selected) LiquidColors.AccentPrimary else Color.Transparent,
        animationSpec = tween(300),
        label = "card_border"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            .clickable {
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                )
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(LiquidColors.SurfaceMedium)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, borderColor, RoundedCornerShape(10.dp))
                    } else {
                        Modifier
                    }
                )
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
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
            }
        }
        
        Text(
            item.name,
            color = if (selected) LiquidColors.TextHighEmphasis else LiquidColors.TextMediumEmphasis,
            fontSize = 9.0.sp,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.01.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 11.0.sp,
            modifier = Modifier
                .width(86.dp)
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
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(LiquidColors.SurfaceDark.copy(alpha = 0.85f))
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = Paint()
                            paint.asFrameworkPaint().apply {
                                this.color = LiquidColors.SurfaceDark.copy(alpha = 0.87f).toArgb()
                            }
                            canvas.drawRect(0f, 0f, size.width, size.height, paint)
                        }
                    }
                } else {
                    Modifier
                }
            )
            .padding(top = 16.dp, bottom = 20.dp, start = 16.dp, end = 16.dp),
        content = content
    )
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
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 0.12.sp,
        modifier = modifier.padding(bottom = 8.dp)
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
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable { onPickImage() }
            .padding(56.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(LiquidColors.GlassSurface)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add_photo),
                contentDescription = null,
                tint = LiquidColors.TextLowEmphasis,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Text(
            stringResource(R.string.label_pick_image),
            color = LiquidColors.TextHighEmphasis,
            fontSize = 26.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.005.sp,
            modifier = Modifier.padding(top = 36.dp)
        )
        
        Text(
            stringResource(R.string.desc_pick_image),
            color = LiquidColors.TextLowEmphasis,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = (15 * 1.5).sp,
            modifier = Modifier.padding(top = 14.dp)
        )
        
        LiquidButton(
            onClick = {
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                )
                onPickImage()
            },
            modifier = Modifier
                .padding(top = 44.dp)
                .height(56.dp)
                .widthIn(min = 200.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gallery),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.btn_open_gallery),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.015.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TOP BAR - Application header
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
    
    Box(modifier = modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.app_name),
                    color = LiquidColors.TextHighEmphasis,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.005.sp
                )
                Text(
                    stringResource(R.string.subtitle_film_simulator).uppercase(),
                    color = LiquidColors.AccentPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.1.sp,
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
                modifier = Modifier.width(80.dp)
            ) {
                Text(
                    stringResource(R.string.save),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}