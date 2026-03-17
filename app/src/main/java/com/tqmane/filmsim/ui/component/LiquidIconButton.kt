package com.tqmane.filmsim.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tqmane.filmsim.ui.theme.LiquidColors
import com.tqmane.filmsim.ui.theme.LiquidMotion

/**
 * Flat icon button with Liquid spring press animation and haptic feedback.
 * Use instead of Material3 IconButton to stay consistent with the Liquid design system.
 *
 * For icon actions that need a glass circle background, use [LiquidRoundButton] instead.
 */
@Composable
fun LiquidIconButton(
    iconRes: Int,
    contentDesc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LiquidColors.TextMediumEmphasis,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = LiquidMotion.SpringSpecFast,
        label = "icon_button_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(RoundedCornerShape(12.dp))
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
            modifier = Modifier.size(iconSize)
        )
    }
}
