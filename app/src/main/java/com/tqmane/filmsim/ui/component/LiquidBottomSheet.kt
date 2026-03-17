package com.tqmane.filmsim.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.ui.theme.LiquidColors

/**
 * Glassmorphic bottom sheet with surface gradient and top highlight.
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
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            LiquidColors.AccentPrimary.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 1.5.dp.toPx()),
                    cornerRadius = CornerRadius(topRadius.toPx(), topRadius.toPx())
                )

                drawLine(
                    color = Color(0x38FFFFFF),
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

/**
 * Styled section header with accent color.
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
