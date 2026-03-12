package com.tqmane.filmsim.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.ui.theme.LiquidColors
import com.tqmane.filmsim.ui.theme.LiquidDimensions

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
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Breathing glow effect
        if (selected) {
            val infiniteTransition = rememberInfiniteTransition(label = "breathing")
            val glowScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow_scale"
            )
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow_alpha"
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .scale(glowScale)
                    .alpha(glowAlpha)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                LiquidColors.AccentPrimary.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
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
}
