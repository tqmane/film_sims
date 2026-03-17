package com.tqmane.filmsim.ui.editor.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.ui.component.LiquidChip
import com.tqmane.filmsim.ui.component.LiquidIntensitySlider
import com.tqmane.filmsim.ui.component.LiquidNoticeCard
import com.tqmane.filmsim.ui.component.LiquidSectionHeader
import com.tqmane.filmsim.ui.theme.LiquidColors

@Composable
internal fun IntensityTab(
    intensity: Float,
    overlayLutName: String?,
    overlayIntensity: Float,
    onIntensityChange: (Float) -> Unit,
    onOverlayIntensityChange: (Float) -> Unit,
    onSelectOverlayFilter: () -> Unit,
    onClearOverlay: () -> Unit,
    compareEnabled: Boolean,
    comparePosition: Float,
    compareVertical: Boolean,
    onComparePositionChange: (Float) -> Unit,
    onCompareVerticalChange: (Boolean) -> Unit,
    showHints: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier.verticalScroll(scrollState)) {
        if (showHints) {
            LiquidNoticeCard(
                title = stringResource(R.string.intensity_strength_title),
                message = stringResource(R.string.intensity_strength_hint),
                label = "${(intensity * 100).toInt()}%",
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        LiquidIntensitySlider(
            intensity = intensity,
            onIntensityChange = onIntensityChange
        )

        Spacer(modifier = Modifier.height(4.dp))
        LiquidSectionHeader(text = stringResource(R.string.overlay_filter))
        Text(
            text = overlayLutName ?: stringResource(R.string.overlay_filter_none),
            color = if (overlayLutName != null) LiquidColors.TextHighEmphasis else LiquidColors.TextMediumEmphasis,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif
        )
        Text(
            text = stringResource(
                if (overlayLutName == null) R.string.overlay_filter_hint_empty else R.string.overlay_filter_hint_active
            ),
            color = LiquidColors.TextLowEmphasis,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 12.dp, bottom = if (overlayLutName != null) 10.dp else 0.dp)
        ) {
            LiquidChip(
                text = stringResource(if (overlayLutName == null) R.string.overlay_pick else R.string.overlay_change),
                selected = false,
                onClick = onSelectOverlayFilter
            )
            if (overlayLutName != null) {
                LiquidChip(
                    text = stringResource(R.string.overlay_remove),
                    selected = false,
                    onClick = onClearOverlay
                )
            }
        }

        if (showHints && overlayLutName != null) {
            LiquidNoticeCard(
                title = stringResource(R.string.overlay_active_title, overlayLutName),
                message = stringResource(R.string.overlay_active_hint),
                label = stringResource(R.string.overlay_filter),
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        if (overlayLutName != null) {
            IntensityAdjustSlider(
                label = stringResource(R.string.overlay_blend),
                value = overlayIntensity,
                range = 0f..1f,
                onValueChange = onOverlayIntensityChange,
                valueFormatter = { "${(it * 100).toInt()}%" }
            )
        }

        if (compareEnabled) {
            Spacer(modifier = Modifier.height(6.dp))
            if (showHints) {
                LiquidNoticeCard(
                    title = stringResource(R.string.compare_active_title),
                    message = stringResource(R.string.compare_active_hint),
                    label = stringResource(R.string.compare_preview),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            LiquidSectionHeader(text = stringResource(R.string.compare_preview))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                LiquidChip(
                    text = stringResource(R.string.compare_vertical),
                    selected = compareVertical,
                    onClick = { onCompareVerticalChange(true) }
                )
                LiquidChip(
                    text = stringResource(R.string.compare_horizontal),
                    selected = !compareVertical,
                    onClick = { onCompareVerticalChange(false) }
                )
            }
            IntensityAdjustSlider(
                label = stringResource(R.string.compare_split),
                value = comparePosition,
                range = 0f..1f,
                onValueChange = onComparePositionChange,
                valueFormatter = { "${(it * 100).toInt()}%" }
            )
        }
    }
}

/** Minimal slider row used within IntensityTab (compare/overlay blend). */
@Composable
internal fun IntensityAdjustSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String = {
        val v = (it * 100).toInt()
        "${if (v > 0) "+" else ""}$v"
    },
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = LiquidColors.TextMediumEmphasis,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it)
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                )
            },
            valueRange = range,
            modifier = Modifier.weight(2f),
            colors = SliderDefaults.colors(
                thumbColor = LiquidColors.AccentPrimary,
                activeTrackColor = LiquidColors.AccentPrimary,
                inactiveTrackColor = Color(0x14FFFFFF)
            )
        )
        Text(
            valueFormatter(sliderValue),
            color = LiquidColors.AccentPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
