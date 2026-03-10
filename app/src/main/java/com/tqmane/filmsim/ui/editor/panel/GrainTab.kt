package com.tqmane.filmsim.ui.editor.panel

import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.tqmane.filmsim.gl.FilmSimRenderer
import com.tqmane.filmsim.ui.EditState
import com.tqmane.filmsim.ui.EditorViewModel
import com.tqmane.filmsim.ui.component.LiquidChip
import com.tqmane.filmsim.ui.theme.LiquidColors

@Composable
internal fun GrainTab(
    editState: EditState,
    viewModel: EditorViewModel,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?,
    isWatermarkActive: Boolean,
    onRefreshWatermark: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var grainEnabled by remember { mutableStateOf(editState.grainEnabled) }
    var grainIntensity by remember { mutableFloatStateOf(editState.grainIntensity) }
    var selectedStyle by remember { mutableStateOf(editState.grainStyle) }
    val accent = if (grainEnabled) LiquidColors.AccentPrimary else LiquidColors.TextLowEmphasis

    Column {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_grain),
            contentDescription = stringResource(R.string.label_film_grain),
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.label_film_grain),
            color = LiquidColors.TextMediumEmphasis,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${(grainIntensity * 100).toInt()}%",
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.End,
            modifier = Modifier.width(46.dp).padding(end = 8.dp)
        )
        Switch(
            checked = grainEnabled,
            onCheckedChange = { on ->
                grainEnabled = on
                viewModel.setGrainEnabled(on)
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                if (!isWatermarkActive) {
                    glSurfaceView?.let { glView ->
                        glView.queueEvent {
                            renderer?.setGrainEnabled(on)
                            if (on) renderer?.setGrainIntensity(grainIntensity)
                            glView.requestRender()
                        }
                    }
                }
                onRefreshWatermark()
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0C0C10),
                checkedTrackColor = LiquidColors.AccentPrimary,
                uncheckedThumbColor = LiquidColors.TextLowEmphasis,
                uncheckedTrackColor = Color(0x22FFFFFF),
                uncheckedBorderColor = Color(0x30FFFFFF)
            )
        )
    }

    Slider(
        value = grainIntensity,
        onValueChange = { value ->
            grainIntensity = value
            viewModel.setGrainIntensity(value)
            if (grainEnabled) {
                if (!isWatermarkActive) {
                    glSurfaceView?.let { glView ->
                        glView.queueEvent {
                            renderer?.setGrainIntensity(value)
                            glView.requestRender()
                        }
                    }
                }
                onRefreshWatermark()
            }
        },
        onValueChangeFinished = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        },
        enabled = grainEnabled,
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        colors = SliderDefaults.colors(
            thumbColor = accent,
            activeTrackColor = accent,
            inactiveTrackColor = LiquidColors.GlassSurfaceDark,
            disabledThumbColor = LiquidColors.TextLowEmphasis,
            disabledActiveTrackColor = LiquidColors.TextLowEmphasis,
            disabledInactiveTrackColor = LiquidColors.GlassSurfaceDark
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth().alpha(if (grainEnabled) 1f else 0.4f).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_grain),
            contentDescription = stringResource(R.string.label_grain_style),
            tint = LiquidColors.AccentSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.label_grain_style),
            color = LiquidColors.TextMediumEmphasis,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(end = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Xiaomi" to R.string.grain_style_xiaomi, "OnePlus" to R.string.grain_style_oneplus).forEach { (key, labelRes) ->
                LiquidChip(
                    text = stringResource(labelRes),
                    selected = selectedStyle == key,
                    enabled = grainEnabled,
                    onClick = {
                        if (grainEnabled) {
                            selectedStyle = key
                            viewModel.setGrainStyle(key)
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            if (!isWatermarkActive) {
                                glSurfaceView?.let { glView ->
                                    glView.queueEvent {
                                        renderer?.setGrainStyle(key)
                                        glView.requestRender()
                                    }
                                }
                            }
                            onRefreshWatermark()
                        }
                    }
                )
            }
        }
    }
    } // Column
}
