package com.tqmane.filmsim.ui.editor.panel

import android.opengl.GLSurfaceView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.gl.FilmSimRenderer
import com.tqmane.filmsim.ui.EditState
import com.tqmane.filmsim.ui.EditorViewModel
import com.tqmane.filmsim.ui.Preset
import com.tqmane.filmsim.ui.WatermarkState
import com.tqmane.filmsim.ui.component.LiquidIntensitySlider
import com.tqmane.filmsim.ui.component.LiquidTabBar
import com.tqmane.filmsim.ui.theme.LiquidColors

internal enum class AdjustTab { INTENSITY, ADJUST, GRAIN, WATERMARK, PRESETS }

@Composable
fun AdjustPanel(
    editState: EditState,
    watermarkState: WatermarkState,
    viewModel: EditorViewModel,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?,
    isWatermarkActive: Boolean,
    onRefreshWatermark: () -> Unit,
    isProUser: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable {
        mutableStateOf(AdjustTab.INTENSITY.name)
    }
    val currentTab = try { AdjustTab.valueOf(selectedTab) } catch (_: Exception) { AdjustTab.INTENSITY }

    val tabs = listOf(
        AdjustTab.INTENSITY to stringResource(R.string.adjustments),
        AdjustTab.ADJUST to (
            if (!isProUser) "${stringResource(R.string.tab_adjust)} 🔒"
            else stringResource(R.string.tab_adjust)
        ),
        AdjustTab.GRAIN to stringResource(R.string.grain),
        AdjustTab.WATERMARK to (
            if (!isProUser) "${stringResource(R.string.watermark)} 🔒"
            else stringResource(R.string.watermark)
        ),
        AdjustTab.PRESETS to (
            if (!isProUser) "${stringResource(R.string.tab_presets)} 🔒"
            else stringResource(R.string.tab_presets)
        )
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        LiquidColors.SurfaceMedium.copy(alpha = 0.95f),
                        LiquidColors.SurfaceDark.copy(alpha = 0.97f)
                    )
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {}
            .padding(top = 14.dp, bottom = 10.dp, start = 18.dp, end = 18.dp)
    ) {
        LiquidTabBar(
            tabs = tabs,
            selectedTab = currentTab,
            onTabSelected = { tab ->
                if (tab == AdjustTab.WATERMARK && !isProUser) {
                    Toast.makeText(context, context.getString(R.string.pro_watermark_locked), Toast.LENGTH_SHORT).show()
                    return@LiquidTabBar
                }
                if (tab == AdjustTab.ADJUST && !isProUser) {
                    Toast.makeText(context, context.getString(R.string.preset_pro_locked), Toast.LENGTH_SHORT).show()
                    return@LiquidTabBar
                }
                if (tab == AdjustTab.PRESETS && !isProUser) {
                    Toast.makeText(context, context.getString(R.string.preset_pro_locked), Toast.LENGTH_SHORT).show()
                    return@LiquidTabBar
                }
                selectedTab = tab.name
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        )

        when (currentTab) {
            AdjustTab.INTENSITY -> {
                IntensityTab(
                    intensity = editState.intensity,
                    onIntensityChange = { value ->
                        viewModel.setIntensity(value)
                        if (!isWatermarkActive) {
                            glSurfaceView?.queueEvent {
                                renderer?.setIntensity(value)
                                glSurfaceView.requestRender()
                            }
                        }
                        onRefreshWatermark()
                    }
                )
            }
            AdjustTab.ADJUST -> {
                BasicAdjustTab(
                    editState = editState,
                    viewModel = viewModel,
                    glSurfaceView = glSurfaceView,
                    renderer = renderer,
                    isWatermarkActive = isWatermarkActive,
                    onRefreshWatermark = onRefreshWatermark
                )
            }
            AdjustTab.GRAIN -> {
                GrainTab(
                    editState = editState,
                    viewModel = viewModel,
                    glSurfaceView = glSurfaceView,
                    renderer = renderer,
                    isWatermarkActive = isWatermarkActive,
                    onRefreshWatermark = onRefreshWatermark
                )
            }
            AdjustTab.WATERMARK -> {
                WatermarkTab(
                    watermarkState = watermarkState,
                    viewModel = viewModel,
                    onRefreshWatermark = onRefreshWatermark
                )
            }
            AdjustTab.PRESETS -> {
                PresetsTab(
                    viewModel = viewModel
                )
            }
        }
    }
}

/**
 * Intensity adjustment tab — wraps the slider component.
 */
@Composable
internal fun IntensityTab(
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidIntensitySlider(
        intensity = intensity,
        onIntensityChange = onIntensityChange,
        modifier = modifier
    )
}

// ─── Basic Adjust Tab ────────────────────────────────────────

@Composable
internal fun BasicAdjustTab(
    editState: EditState,
    viewModel: EditorViewModel,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?,
    isWatermarkActive: Boolean,
    onRefreshWatermark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                viewModel.resetAdjustments()
                glSurfaceView?.queueEvent {
                    renderer?.setExposure(0f)
                    renderer?.setContrast(0f)
                    renderer?.setHighlights(0f)
                    renderer?.setShadows(0f)
                    renderer?.setColorTemp(0f)
                    glSurfaceView.requestRender()
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
                    glSurfaceView?.queueEvent {
                        renderer?.setExposure(value)
                        glSurfaceView.requestRender()
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
                    glSurfaceView?.queueEvent {
                        renderer?.setContrast(value)
                        glSurfaceView.requestRender()
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
                    glSurfaceView?.queueEvent {
                        renderer?.setHighlights(value)
                        glSurfaceView.requestRender()
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
                    glSurfaceView?.queueEvent {
                        renderer?.setShadows(value)
                        glSurfaceView.requestRender()
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
                    glSurfaceView?.queueEvent {
                        renderer?.setColorTemp(value)
                        glSurfaceView.requestRender()
                    }
                }
                onRefreshWatermark()
            }
        )
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }
    val haptic = LocalHapticFeedback.current
    val displayValue = (sliderValue * 100).toInt()

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
            "${if (displayValue > 0) "+" else ""}$displayValue",
            color = LiquidColors.AccentPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
    }
}

// ─── Presets Tab ─────────────────────────────────────────────

@Composable
internal fun PresetsTab(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val presets by viewModel.presets.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showSaveDialog = true }) {
                Text(
                    stringResource(R.string.preset_save),
                    color = LiquidColors.AccentPrimary,
                    fontSize = 12.sp
                )
            }
        }

        if (presets.isEmpty()) {
            Text(
                stringResource(R.string.preset_empty),
                color = LiquidColors.TextMediumEmphasis,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(160.dp)
            ) {
                items(presets, key = { it.id }) { preset ->
                    PresetItem(
                        preset = preset,
                        onLoad = { viewModel.loadPreset(preset) },
                        onDelete = { viewModel.deletePreset(preset.id) }
                    )
                }
            }
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.savePreset(name)
                showSaveDialog = false
            }
        )
    }
}

@Composable
private fun PresetItem(
    preset: Preset,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onLoad)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                preset.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val lutName = preset.lutPath?.substringAfterLast("/")?.substringBeforeLast(".") ?: "—"
            Text(
                lutName,
                color = LiquidColors.TextMediumEmphasis,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_delete),
                contentDescription = null,
                tint = LiquidColors.TextMediumEmphasis,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.preset_save_title),
                color = Color.White
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = {
                    Text(
                        stringResource(R.string.preset_name_hint),
                        color = LiquidColors.TextMediumEmphasis
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = LiquidColors.AccentPrimary,
                    unfocusedBorderColor = LiquidColors.TextMediumEmphasis,
                    cursorColor = LiquidColors.AccentPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save), color = LiquidColors.AccentPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = LiquidColors.TextMediumEmphasis)
            }
        },
        containerColor = LiquidColors.SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    )
}
