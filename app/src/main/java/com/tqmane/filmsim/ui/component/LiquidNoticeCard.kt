package com.tqmane.filmsim.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.ui.theme.LiquidColors
import com.tqmane.filmsim.ui.theme.LiquidDimensions

@Composable
fun LiquidNoticeCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    accentColor: Color = LiquidColors.AccentPrimary
) {
    val shape = RoundedCornerShape(LiquidDimensions.CornerLarge)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LiquidColors.SurfaceElevated.copy(alpha = 0.94f),
                        LiquidColors.SurfaceMedium.copy(alpha = 0.98f)
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.18f), shape)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        label?.let {
            Text(
                text = it.uppercase(),
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Text(
            text = title,
            color = LiquidColors.TextHighEmphasis,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.01.sp
        )

        Text(
            text = message,
            color = LiquidColors.TextMediumEmphasis,
            fontSize = 12.5.sp,
            fontFamily = FontFamily.SansSerif,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
