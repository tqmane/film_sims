package com.tqmane.filmsim.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.2f)
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
                .alpha(if (enabled) 1f else 0.55f)
                .heightIn(min = LiquidDimensions.ChipHeight, max = LiquidDimensions.ChipHeight)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(backgroundColor)
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .semantics {
                    role = Role.Button
                    this.selected = selected
                }
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
