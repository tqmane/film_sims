package com.tqmane.filmsim.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tqmane.filmsim.ui.theme.LiquidColors
import com.tqmane.filmsim.ui.theme.LiquidDimensions
import com.tqmane.filmsim.ui.theme.LiquidMotion

/**
 * Liquid-style button with gradient background, elastic press animation, and subtle glow.
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
    val backgroundColors = if (enabled) {
        listOf(
            LiquidColors.GradientAccentStart,
            LiquidColors.GradientAccentEnd
        )
    } else {
        listOf(
            LiquidColors.SurfaceElevated,
            LiquidColors.SurfaceMedium
        )
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = LiquidMotion.SpringSpecElastic,
        label = "button_scale"
    )
    
    // Dynamic glow alpha based on press state
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.4f else 0.15f,
        animationSpec = tween(durationMillis = LiquidMotion.DurationQuick, easing = LiquidMotion.EasingEmphasized),
        label = "button_glow"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.56f)
            .sizeIn(minHeight = 48.dp)
            .height(LiquidDimensions.ButtonHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors = backgroundColors))
            .semantics { role = Role.Button }
            .drawBehind {
                if (enabled) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                LiquidColors.AccentPrimary.copy(alpha = glowAlpha),
                                Color.Transparent
                            ),
                            radius = size.width * 0.8f
                        )
                    )
                }

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f)
                        )
                    ),
                    size = size
                )
            }
            .border(
                1.dp,
                if (enabled) Color(0x35FFFFFF) else Color(0x12FFFFFF),
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
 * Round glass button for icon actions with fluid press state.
 */
@Composable
fun LiquidRoundButton(
    iconRes: Int,
    contentDesc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LiquidColors.TextMediumEmphasis
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = LiquidMotion.SpringSpecElastic,
        label = "round_button_scale"
    )

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .size(48.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .background(Color(0x12FFFFFF))
            .border(1.dp, Color(0x14FFFFFF), CircleShape)
            .semantics { role = Role.Button }
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
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}
