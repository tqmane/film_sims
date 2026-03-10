package com.tqmane.filmsim.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
                contentDescription = stringResource(R.string.label_intensity),
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
