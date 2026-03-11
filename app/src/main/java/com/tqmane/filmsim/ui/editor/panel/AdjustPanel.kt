package com.tqmane.filmsim.ui.editor.panel

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.tqmane.filmsim.ui.component.LiquidChip
import com.tqmane.filmsim.ui.component.LiquidIntensitySlider
import com.tqmane.filmsim.ui.component.LiquidNoticeCard
import com.tqmane.filmsim.ui.component.LiquidSectionHeader
import com.tqmane.filmsim.ui.component.LiquidTabBar
import com.tqmane.filmsim.ui.theme.LiquidColors

internal enum class AdjustTab { INTENSITY, ADJUST, GRAIN, WATERMARK, PRESETS }

@Composable
internal fun AdjustPanel(
    editState: EditState,
    watermarkState: WatermarkState,
    viewModel: EditorViewModel,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?,
    isWatermarkActive: Boolean,
    onRefreshWatermark: () -> Unit,
    selectedTab: AdjustTab,
    onTabSelected: (AdjustTab) -> Unit,
    showPanelHints: Boolean,
    onSelectOverlayFilter: () -> Unit,
    compareEnabled: Boolean,
    comparePosition: Float,
    compareVertical: Boolean,
    onComparePositionChange: (Float) -> Unit,
    onCompareVerticalChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    isProUser: Boolean = false,
    modifier: Modifier = Modifier
) {
    val lockedFeatureMessageResState = rememberSaveable {
        mutableIntStateOf(R.string.pro_adjust_tools_hint)
    }

    // Stable lambdas to prevent unnecessary recomposition of child Composables
    val currentGlSurfaceView by rememberUpdatedState(glSurfaceView)
    val currentRenderer by rememberUpdatedState(renderer)
    val currentIsWatermarkActive by rememberUpdatedState(isWatermarkActive)
    val currentEditState by rememberUpdatedState(editState)
    val currentOnRefreshWatermark by rememberUpdatedState(onRefreshWatermark)
    val stableOnIntensityChange = remember(viewModel) {
        { value: Float ->
            viewModel.setIntensity(value)
            if (!currentIsWatermarkActive) {
                currentGlSurfaceView?.let { glView ->
                    glView.queueEvent {
                        currentRenderer?.setIntensity(value)
                        glView.requestRender()
                    }
                }
            }
            currentOnRefreshWatermark()
        }
    }
    val stableOnOverlayIntensityChange = remember(viewModel) {
        { value: Float ->
            viewModel.setOverlayIntensity(value)
            if (!currentIsWatermarkActive) {
                currentGlSurfaceView?.let { glView ->
                    glView.queueEvent {
                        currentRenderer?.setOverlayIntensity(
                            if (currentEditState.overlayLutPath != null) value else 0f
                        )
                        glView.requestRender()
                    }
                }
            }
            currentOnRefreshWatermark()
        }
    }

    val currentTab = if (!isProUser && selectedTab in setOf(AdjustTab.ADJUST, AdjustTab.WATERMARK, AdjustTab.PRESETS)) {
        AdjustTab.INTENSITY
    } else {
        selectedTab
    }
    val currentTabLabel = when (currentTab) {
        AdjustTab.INTENSITY -> stringResource(R.string.adjustments)
        AdjustTab.ADJUST -> stringResource(R.string.tab_adjust)
        AdjustTab.GRAIN -> stringResource(R.string.grain)
        AdjustTab.WATERMARK -> stringResource(R.string.watermark)
        AdjustTab.PRESETS -> stringResource(R.string.tab_presets)
    }
    val currentHintMessage = when (currentTab) {
        AdjustTab.INTENSITY -> stringResource(R.string.adjust_hint_intensity)
        AdjustTab.ADJUST -> stringResource(R.string.adjust_hint_basic)
        AdjustTab.GRAIN -> stringResource(R.string.adjust_hint_grain)
        AdjustTab.WATERMARK -> stringResource(R.string.adjust_hint_watermark)
        AdjustTab.PRESETS -> stringResource(R.string.adjust_hint_presets)
    }
    val currentLutName = editState.currentLutPath
        ?.substringAfterLast("/")
        ?.substringBeforeLast(".")
        ?: stringResource(R.string.adjustments)

    val tabs = listOf(
        AdjustTab.INTENSITY to stringResource(R.string.adjustments),
        AdjustTab.ADJUST to (
            if (!isProUser) "${stringResource(R.string.tab_adjust)} ${stringResource(R.string.locked_indicator)}"
            else stringResource(R.string.tab_adjust)
        ),
        AdjustTab.GRAIN to stringResource(R.string.grain),
        AdjustTab.WATERMARK to (
            if (!isProUser) "${stringResource(R.string.watermark)} ${stringResource(R.string.locked_indicator)}"
            else stringResource(R.string.watermark)
        ),
        AdjustTab.PRESETS to (
            if (!isProUser) "${stringResource(R.string.tab_presets)} ${stringResource(R.string.locked_indicator)}"
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onClose) {
                Text(
                    stringResource(R.string.back_to_lut),
                    color = LiquidColors.TextMediumEmphasis,
                    fontSize = 13.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                )
            }
            Text(
                text = currentTabLabel.uppercase(),
                color = LiquidColors.AccentPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        if (showPanelHints) {
            LiquidNoticeCard(
                title = currentLutName,
                message = currentHintMessage,
                label = currentTabLabel,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }

        if (showPanelHints && !isProUser) {
            LiquidNoticeCard(
                title = stringResource(R.string.more_tools_title),
                message = stringResource(lockedFeatureMessageResState.intValue),
                label = stringResource(R.string.label_pro),
                accentColor = LiquidColors.AccentSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        LiquidTabBar(
            tabs = tabs,
            selectedTab = currentTab,
            onTabSelected = { tab ->
                if (tab == AdjustTab.WATERMARK && !isProUser) {
                    lockedFeatureMessageResState.intValue = R.string.pro_watermark_locked
                    return@LiquidTabBar
                }
                if (tab == AdjustTab.ADJUST && !isProUser) {
                    lockedFeatureMessageResState.intValue = R.string.pro_adjust_locked
                    return@LiquidTabBar
                }
                if (tab == AdjustTab.PRESETS && !isProUser) {
                    lockedFeatureMessageResState.intValue = R.string.preset_pro_locked
                    return@LiquidTabBar
                }
                onTabSelected(tab)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
        ) {
            when (currentTab) {
                AdjustTab.INTENSITY -> {
                    IntensityTab(
                        intensity = editState.intensity,
                        overlayLutName = editState.overlayLutPath
                            ?.substringAfterLast("/")
                            ?.substringBeforeLast("."),
                        overlayIntensity = editState.overlayIntensity,
                        onIntensityChange = stableOnIntensityChange,
                        onOverlayIntensityChange = stableOnOverlayIntensityChange,
                        onSelectOverlayFilter = onSelectOverlayFilter,
                        onClearOverlay = {
                            viewModel.clearOverlayLut()
                            onRefreshWatermark()
                        },
                        compareEnabled = compareEnabled,
                        comparePosition = comparePosition,
                        compareVertical = compareVertical,
                        onComparePositionChange = onComparePositionChange,
                        onCompareVerticalChange = onCompareVerticalChange,
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
}

/**
 * Intensity adjustment tab — wraps the slider component.
 */
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
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier.verticalScroll(scrollState)) {
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

        if (overlayLutName != null) {
            AdjustSlider(
                label = stringResource(R.string.overlay_blend),
                value = overlayIntensity,
                range = 0f..1f,
                onValueChange = onOverlayIntensityChange,
                valueFormatter = { "${(it * 100).toInt()}%" }
            )
        }

        if (compareEnabled) {
            Spacer(modifier = Modifier.height(6.dp))
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
            AdjustSlider(
                label = stringResource(R.string.compare_split),
                value = comparePosition,
                range = 0f..1f,
                onValueChange = onComparePositionChange,
                valueFormatter = { "${(it * 100).toInt()}%" }
            )
        }
    }
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
                        glView.queueEvent {
                            renderer?.setExposure(value)
                            glView.requestRender()
                        }
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
                        glView.queueEvent {
                            renderer?.setContrast(value)
                            glView.requestRender()
                        }
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
                        glView.queueEvent {
                            renderer?.setHighlights(value)
                            glView.requestRender()
                        }
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
                        glView.queueEvent {
                            renderer?.setShadows(value)
                            glView.requestRender()
                        }
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
                        glView.queueEvent {
                            renderer?.setColorTemp(value)
                            glView.requestRender()
                        }
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
                        glView.queueEvent {
                            renderer?.setHue(value)
                            glView.requestRender()
                        }
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
                        glView.queueEvent {
                            renderer?.setSaturation(value)
                            glView.requestRender()
                        }
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
                        glView.queueEvent {
                            renderer?.setLuminance(value)
                            glView.requestRender()
                        }
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
    } // Box
}

@Composable
private fun AdjustSlider(
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
                contentDescription = stringResource(R.string.cd_delete_preset),
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
