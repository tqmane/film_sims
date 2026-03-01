package com.tqmane.filmsim.ui.components

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.ui.theme.LiquidColors
import com.tqmane.filmsim.ui.theme.LiquidDimensions

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
