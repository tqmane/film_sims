package com.tqmane.filmsim.ui.editor.panel

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.gl.FilmSimRenderer
import com.tqmane.filmsim.ui.EditState
import com.tqmane.filmsim.ui.EditorViewModel
import com.tqmane.filmsim.ui.theme.LiquidColors

@Composable
internal fun ColorAdjustTab(
    editState: EditState,
    viewModel: EditorViewModel,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?,
    isWatermarkActive: Boolean,
    onRefreshWatermark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    viewModel.resetAdjustments()
                    glSurfaceView?.let { glView ->
                        glView.queueEvent {
                            renderer?.setExposure(0f)
                            renderer?.setContrast(0f)
                            renderer?.setHighlights(0f)
                            renderer?.setShadows(0f)
                            renderer?.setColorTemp(0f)
                            renderer?.setHue(0f)
                            renderer?.setSaturation(0f)
                            renderer?.setLuminance(0f)
                            glView.requestRender()
                        }
                    }
                    onRefreshWatermark()
                }) {
                    Text(
                        stringResource(R.string.btn_reset_adjustments),
                        color = LiquidColors.AccentPrimary,
                        fontSize = 12.sp
                    )
                }
            }

            AdjustSlider(
                label = stringResource(R.string.label_exposure),
                value = editState.exposure,
                range = -2f..2f,
                onValueChange = { value ->
                    viewModel.setExposure(value)
                    if (!isWatermarkActive) {
                        glSurfaceView?.let { glView ->
                            glView.queueEvent { renderer?.setExposure(value); glView.requestRender() }
                        }
                    }
                    onRefreshWatermark()
                }
            )
            AdjustSlider(
                label = stringResource(R.string.label_contrast),
                value = editState.contrast,
                range = -1f..1f,
                onValueChange = { value ->
                    viewModel.setContrast(value)
                    if (!isWatermarkActive) {
                        glSurfaceView?.let { glView ->
                            glView.queueEvent { renderer?.setContrast(value); glView.requestRender() }
                        }
                    }
                    onRefreshWatermark()
                }
            )
            AdjustSlider(
                label = stringResource(R.string.label_highlights),
                value = editState.highlights,
                range = -1f..1f,
                onValueChange = { value ->
                    viewModel.setHighlights(value)
                    if (!isWatermarkActive) {
                        glSurfaceView?.let { glView ->
                            glView.queueEvent { renderer?.setHighlights(value); glView.requestRender() }
                        }
                    }
                    onRefreshWatermark()
                }
            )
            AdjustSlider(
                label = stringResource(R.string.label_shadows),
                value = editState.shadows,
                range = -1f..1f,
                onValueChange = { value ->
                    viewModel.setShadows(value)
                    if (!isWatermarkActive) {
                        glSurfaceView?.let { glView ->
                            glView.queueEvent { renderer?.setShadows(value); glView.requestRender() }
                        }
                    }
                    onRefreshWatermark()
                }
            )
            AdjustSlider(
                label = stringResource(R.string.label_color_temp),
                value = editState.colorTemp,
                range = -1f..1f,
                onValueChange = { value ->
                    viewModel.setColorTemp(value)
                    if (!isWatermarkActive) {
                        glSurfaceView?.let { glView ->
                            glView.queueEvent { renderer?.setColorTemp(value); glView.requestRender() }
                        }
                    }
                    onRefreshWatermark()
                }
            )
            AdjustSlider(
                label = stringResource(R.string.label_hue),
                value = editState.hue,
                range = -1f..1f,
                onValueChange = { value ->
                    viewModel.setHue(value)
                    if (!isWatermarkActive) {
                        glSurfaceView?.let { glView ->
                            glView.queueEvent { renderer?.setHue(value); glView.requestRender() }
                        }
                    }
                    onRefreshWatermark()
                }
            )
            AdjustSlider(
                label = stringResource(R.string.label_saturation),
                value = editState.saturation,
                range = -1f..1f,
                onValueChange = { value ->
                    viewModel.setSaturation(value)
                    if (!isWatermarkActive) {
                        glSurfaceView?.let { glView ->
                            glView.queueEvent { renderer?.setSaturation(value); glView.requestRender() }
                        }
                    }
                    onRefreshWatermark()
                }
            )
            AdjustSlider(
                label = stringResource(R.string.label_luminance),
                value = editState.luminance,
                range = -1f..1f,
                onValueChange = { value ->
                    viewModel.setLuminance(value)
                    if (!isWatermarkActive) {
                        glSurfaceView?.let { glView ->
                            glView.queueEvent { renderer?.setLuminance(value); glView.requestRender() }
                        }
                    }
                    onRefreshWatermark()
                }
            )
        }
        if (scrollState.canScrollForward) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, LiquidColors.SurfaceDark.copy(alpha = 0.92f))
                        )
                    )
            )
        }
    }
}

@Composable
internal fun AdjustSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String = {
        val displayValue = (it * 100).toInt()
        "${if (displayValue > 0) "+" else ""}$displayValue"
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
            modifier = Modifier.width(72.dp)
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
            modifier = Modifier.weight(1f),
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
            modifier = Modifier.width(40.dp)
        )
    }
}
