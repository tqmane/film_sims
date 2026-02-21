package com.tqmane.filmsim.ui

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.data.LutBrand
import com.tqmane.filmsim.data.LutItem
import com.tqmane.filmsim.gl.FilmSimRenderer
import com.tqmane.filmsim.ui.components.GlassBottomSheet
import com.tqmane.filmsim.ui.components.LiquidChip
import com.tqmane.filmsim.ui.components.LiquidIntensitySlider
import com.tqmane.filmsim.ui.components.LiquidLutCard
import com.tqmane.filmsim.ui.components.LiquidSectionHeader
import com.tqmane.filmsim.ui.theme.LiquidColors
import com.tqmane.filmsim.util.WatermarkProcessor
import com.tqmane.filmsim.util.WatermarkProcessor.WatermarkStyle

// ═══════════════════════════════════════════════════════════════════════════════
// GLASS CONTROL PANEL - Bottom sheet with controls
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun GlassControlPanel(
    viewModel: MainViewModel,
    editState: EditState,
    watermarkState: WatermarkState,
    viewState: ViewState,
    panelExpanded: Boolean,
    onTogglePanel: () -> Unit,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?,
    isWatermarkActive: Boolean,
    onRefreshWatermark: () -> Unit,
    onLutReselected: () -> Unit,
    squareTop: Boolean = false,
    modifier: Modifier = Modifier
) {
    GlassBottomSheet(
        modifier = modifier,
        squareTop = squareTop
    ) {
        LiquidSectionHeader(stringResource(R.string.header_camera))
        LiquidBrandGenreLutSection(
            viewModel.brands, viewModel, viewState, editState, watermarkState,
            glSurfaceView, renderer, isWatermarkActive, onRefreshWatermark,
            onLutReselected = onLutReselected
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// BRAND/GENRE/LUT SECTION
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LiquidBrandGenreLutSection(
    brands: List<LutBrand>,
    viewModel: MainViewModel,
    viewState: ViewState,
    editState: EditState,
    watermarkState: WatermarkState,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?,
    isWatermarkActive: Boolean,
    onRefreshWatermark: () -> Unit,
    onLutReselected: () -> Unit
) {
    val initialBrandIndex = remember(brands, watermarkState.brandName) {
        brands.indexOfFirst { it.name == watermarkState.brandName }.takeIf { it >= 0 } ?: 0
    }
    var selectedBrandIndex by rememberSaveable { mutableIntStateOf(initialBrandIndex) }
    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    val categories = remember(selectedBrandIndex) { brands.getOrNull(selectedBrandIndex)?.categories.orEmpty() }
    val lutItems = remember(selectedBrandIndex, selectedCategoryIndex) { categories.getOrNull(selectedCategoryIndex)?.items.orEmpty() }

    // Brand chips
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        itemsIndexed(brands) { index, brand ->
            LiquidChip(
                text = brand.displayName,
                selected = index == selectedBrandIndex,
                onClick = {
                    selectedBrandIndex = index
                    selectedCategoryIndex = 0
                    viewModel.updateWatermarkBrand(brand.name)
                }
            )
        }
    }

    LiquidSectionHeader(stringResource(R.string.header_style))

    // Category chips
    if (categories.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            itemsIndexed(categories) { index, cat ->
                LiquidChip(
                    text = cat.displayName,
                    selected = index == selectedCategoryIndex,
                    onClick = { selectedCategoryIndex = index }
                )
            }
        }
    }

    // Intensity slider removed — now in LiquidAdjustPanel

    LiquidSectionHeader(stringResource(R.string.header_presets))

    // LUT cards - re-tap selected card to open adjust panel
    LiquidLutRow(
        items = lutItems,
        thumbnailBitmap = (viewState as? ViewState.Content)?.thumbnailBitmap,
        onLutSelected = { item ->
            if (item.assetPath == editState.currentLutPath) {
                // Already selected → toggle adjust panel
                onLutReselected()
            } else {
                viewModel.applyLut(item)
            }
        },
        currentLutPath = editState.currentLutPath
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// LUT ROW
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LiquidLutRow(
    items: List<LutItem>,
    thumbnailBitmap: Bitmap?,
    onLutSelected: (LutItem) -> Unit,
    currentLutPath: String?
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(120.dp)
    ) {
        itemsIndexed(items) { index, item ->
            LiquidLutCard(
                item = item,
                thumbnailBitmap = thumbnailBitmap,
                selected = item.assetPath == currentLutPath,
                onClick = { onLutSelected(item) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LIQUID ADJUST PANEL - Slide-in panel with Intensity / Grain / Watermark tabs
// ═══════════════════════════════════════════════════════════════════════════════

private enum class AdjustTab { INTENSITY, GRAIN, WATERMARK }

@Composable
fun LiquidAdjustPanel(
    editState: EditState,
    watermarkState: WatermarkState,
    viewModel: MainViewModel,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?,
    isWatermarkActive: Boolean,
    onRefreshWatermark: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable {
        mutableStateOf(AdjustTab.INTENSITY.name)
    }
    val currentTab = try { AdjustTab.valueOf(selectedTab) } catch (_: Exception) { AdjustTab.INTENSITY }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(LiquidColors.SurfaceDark.copy(alpha = 0.92f))
            .padding(top = 12.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        // ─── Tab Bar (Pill UI) ─────────────────────────────────────────────
        LiquidTabBar(
            selectedTab = currentTab,
            onTabSelected = { selectedTab = it.name },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        // ─── Tab Content ───────────────────────────────────────────────────
        when (currentTab) {
            AdjustTab.INTENSITY -> {
                LiquidIntensitySlider(
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
            AdjustTab.GRAIN -> {
                LiquidGrainControls(
                    editState = editState,
                    viewModel = viewModel,
                    glSurfaceView = glSurfaceView,
                    renderer = renderer,
                    isWatermarkActive = isWatermarkActive,
                    onRefreshWatermark = onRefreshWatermark
                )
            }
            AdjustTab.WATERMARK -> {
                LiquidWatermarkControls(
                    watermarkState = watermarkState,
                    viewModel = viewModel,
                    onRefreshWatermark = onRefreshWatermark
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB BAR - Pill-style tab navigation
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LiquidTabBar(
    selectedTab: AdjustTab,
    onTabSelected: (AdjustTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val tabs = listOf(
        AdjustTab.INTENSITY to R.string.adjustments,
        AdjustTab.GRAIN to R.string.grain,
        AdjustTab.WATERMARK to R.string.watermark
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(LiquidColors.GlassSurfaceDark)
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { (tab, labelRes) ->
            val isSelected = selectedTab == tab
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) LiquidColors.AccentPrimary.copy(alpha = 0.25f)
                else Color.Transparent,
                animationSpec = tween(200),
                label = "tab_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) LiquidColors.AccentPrimary
                else LiquidColors.TextLowEmphasis,
                animationSpec = tween(200),
                label = "tab_text"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .clickable {
                        haptic.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                        )
                        onTabSelected(tab)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(labelRes),
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.02.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// GRAIN CONTROLS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LiquidGrainControls(
    editState: EditState,
    viewModel: MainViewModel,
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

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_grain),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.label_film_grain),
            color = LiquidColors.TextMediumEmphasis,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${(grainIntensity * 100).toInt()}%",
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.End,
            modifier = Modifier.width(42.dp).padding(end = 8.dp)
        )
        Checkbox(
            checked = grainEnabled,
            onCheckedChange = { on ->
                grainEnabled = on
                viewModel.setGrainEnabled(on)
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                if (!isWatermarkActive) {
                    glSurfaceView?.queueEvent {
                        renderer?.setGrainEnabled(on)
                        if (on) renderer?.setGrainIntensity(grainIntensity)
                        glSurfaceView.requestRender()
                    }
                }
                onRefreshWatermark()
            },
            colors = CheckboxDefaults.colors(checkedColor = LiquidColors.AccentPrimary),
            modifier = Modifier.size(24.dp)
        )
    }

    Slider(
        value = grainIntensity,
        onValueChange = { grainIntensity = it },
        onValueChangeFinished = {
            viewModel.setGrainIntensity(grainIntensity)
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            if (grainEnabled) {
                if (!isWatermarkActive) {
                    glSurfaceView?.queueEvent {
                        renderer?.setGrainIntensity(grainIntensity)
                        glSurfaceView.requestRender()
                    }
                }
                onRefreshWatermark()
            }
        },
        enabled = grainEnabled,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
            contentDescription = null,
            tint = LiquidColors.AccentSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.label_grain_style),
            color = LiquidColors.TextMediumEmphasis,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(end = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                glSurfaceView?.queueEvent {
                                    renderer?.setGrainStyle(key)
                                    glSurfaceView.requestRender()
                                }
                            }
                            onRefreshWatermark()
                        }
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// WATERMARK CONTROLS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LiquidWatermarkControls(
    watermarkState: WatermarkState,
    viewModel: MainViewModel,
    onRefreshWatermark: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val defaultTime = remember { WatermarkProcessor.getDefaultTimeString() }
    val selectedBrand = watermarkState.brandName
    val selectedStyle = watermarkState.style

    var deviceName by remember(watermarkState.deviceName) { mutableStateOf(watermarkState.deviceName) }
    var timeText by remember(watermarkState.timeText) { mutableStateOf(watermarkState.timeText.ifEmpty { defaultTime }) }
    var locationText by remember(watermarkState.locationText) { mutableStateOf(watermarkState.locationText) }
    var lensInfo by remember(watermarkState.lensInfo) { mutableStateOf(watermarkState.lensInfo) }
    
    val honorStyles = listOf(R.string.watermark_frame to WatermarkStyle.FRAME, R.string.watermark_text to WatermarkStyle.TEXT, R.string.watermark_frame_yg to WatermarkStyle.FRAME_YG, R.string.watermark_text_yg to WatermarkStyle.TEXT_YG)
    val meizuStyles = listOf(R.string.meizu_norm to WatermarkStyle.MEIZU_NORM, R.string.meizu_pro to WatermarkStyle.MEIZU_PRO, R.string.meizu_z1 to WatermarkStyle.MEIZU_Z1, R.string.meizu_z2 to WatermarkStyle.MEIZU_Z2, R.string.meizu_z3 to WatermarkStyle.MEIZU_Z3, R.string.meizu_z4 to WatermarkStyle.MEIZU_Z4, R.string.meizu_z5 to WatermarkStyle.MEIZU_Z5, R.string.meizu_z6 to WatermarkStyle.MEIZU_Z6, R.string.meizu_z7 to WatermarkStyle.MEIZU_Z7)
    val vivoStyles = listOf(R.string.vivo_zeiss to WatermarkStyle.VIVO_ZEISS, R.string.vivo_classic to WatermarkStyle.VIVO_CLASSIC, R.string.vivo_pro to WatermarkStyle.VIVO_PRO, R.string.vivo_iqoo to WatermarkStyle.VIVO_IQOO, R.string.vivo_zeiss_v1 to WatermarkStyle.VIVO_ZEISS_V1, R.string.vivo_zeiss_sonnar to WatermarkStyle.VIVO_ZEISS_SONNAR, R.string.vivo_zeiss_humanity to WatermarkStyle.VIVO_ZEISS_HUMANITY, R.string.vivo_iqoo_v1 to WatermarkStyle.VIVO_IQOO_V1, R.string.vivo_iqoo_humanity to WatermarkStyle.VIVO_IQOO_HUMANITY, R.string.vivo_zeiss_frame to WatermarkStyle.VIVO_ZEISS_FRAME, R.string.vivo_zeiss_overlay to WatermarkStyle.VIVO_ZEISS_OVERLAY, R.string.vivo_zeiss_center to WatermarkStyle.VIVO_ZEISS_CENTER, R.string.vivo_frame to WatermarkStyle.VIVO_FRAME, R.string.vivo_frame_time to WatermarkStyle.VIVO_FRAME_TIME, R.string.vivo_iqoo_frame to WatermarkStyle.VIVO_IQOO_FRAME, R.string.vivo_iqoo_frame_time to WatermarkStyle.VIVO_IQOO_FRAME_TIME, R.string.vivo_os to WatermarkStyle.VIVO_OS, R.string.vivo_os_corner to WatermarkStyle.VIVO_OS_CORNER, R.string.vivo_os_simple to WatermarkStyle.VIVO_OS_SIMPLE, R.string.vivo_event to WatermarkStyle.VIVO_EVENT)
    val tecnoStyles = listOf(R.string.tecno_1 to WatermarkStyle.TECNO_1, R.string.tecno_2 to WatermarkStyle.TECNO_2, R.string.tecno_3 to WatermarkStyle.TECNO_3, R.string.tecno_4 to WatermarkStyle.TECNO_4)

    val availableStyles = when (selectedBrand) { "Honor" -> honorStyles; "Meizu" -> meizuStyles; "Vivo" -> vivoStyles; "TECNO" -> tecnoStyles; else -> emptyList() }

    val noDeviceStyles = setOf(WatermarkStyle.MEIZU_Z6, WatermarkStyle.MEIZU_Z7, WatermarkStyle.VIVO_OS_CORNER, WatermarkStyle.VIVO_OS_SIMPLE)
    val noLensStyles = setOf(WatermarkStyle.FRAME_YG, WatermarkStyle.TEXT_YG, WatermarkStyle.VIVO_CLASSIC, WatermarkStyle.VIVO_ZEISS_HUMANITY, WatermarkStyle.VIVO_IQOO_HUMANITY, WatermarkStyle.VIVO_FRAME, WatermarkStyle.VIVO_IQOO_FRAME, WatermarkStyle.VIVO_OS_CORNER, WatermarkStyle.VIVO_OS_SIMPLE, WatermarkStyle.TECNO_1)
    val noTimeStyles = setOf(WatermarkStyle.FRAME_YG, WatermarkStyle.TEXT_YG, WatermarkStyle.VIVO_ZEISS_HUMANITY, WatermarkStyle.VIVO_IQOO_HUMANITY, WatermarkStyle.VIVO_FRAME, WatermarkStyle.VIVO_IQOO_FRAME, WatermarkStyle.VIVO_OS_CORNER, WatermarkStyle.VIVO_OS_SIMPLE)
    
    val showFields = selectedStyle != WatermarkStyle.NONE
    val showDevice = showFields && selectedStyle !in noDeviceStyles
    val showLens   = showFields && selectedStyle !in noLensStyles
    val showTime   = showFields && selectedStyle !in noTimeStyles

    Column {
        Text(
            stringResource(R.string.header_watermark).uppercase(),
            color = LiquidColors.AccentPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.15.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_watermark),
                contentDescription = null,
                tint = LiquidColors.AccentSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.label_watermark_brand),
                color = LiquidColors.TextMediumEmphasis,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(end = 12.dp)
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(R.string.brand_none to "None", R.string.brand_honor to "Honor", R.string.brand_meizu to "Meizu", R.string.brand_vivo to "Vivo", R.string.brand_tecno to "TECNO").forEach { (labelRes, brand) ->
                    LiquidChip(
                        text = stringResource(labelRes),
                        selected = selectedBrand == brand,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.updateWatermarkBrand(brand)
                            if (brand == "None") viewModel.updateWatermarkStyle(WatermarkStyle.NONE)
                            else viewModel.updateWatermarkStyle(when(brand){"Honor"->honorStyles;"Meizu"->meizuStyles;"Vivo"->vivoStyles;"TECNO"->tecnoStyles;else->emptyList()}.firstOrNull()?.second ?: WatermarkStyle.NONE)
                            onRefreshWatermark()
                        }
                    )
                }
            }
        }
        
        if (availableStyles.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.label_watermark_style),
                    color = LiquidColors.TextMediumEmphasis,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableStyles.forEach { (labelRes, style) ->
                        LiquidChip(
                            text = stringResource(labelRes),
                            selected = selectedStyle == style,
                            onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.updateWatermarkStyle(style)
                                onRefreshWatermark()
                            }
                        )
                    }
                }
            }
        }
        
        if (showDevice) LiquidWatermarkInputRow(stringResource(R.string.label_watermark_device), deviceName) { deviceName = it; viewModel.updateWatermarkFields(deviceName = it); onRefreshWatermark() }
        if (showLens) LiquidWatermarkInputRow(stringResource(R.string.label_watermark_lens), lensInfo) { lensInfo = it; viewModel.updateWatermarkFields(lensInfo = it); onRefreshWatermark() }
        if (showTime) {
            LiquidWatermarkInputRow(stringResource(R.string.label_watermark_time), timeText) { timeText = it; viewModel.updateWatermarkFields(timeText = it); onRefreshWatermark() }
            LiquidWatermarkInputRow(stringResource(R.string.label_watermark_location), locationText) { locationText = it; viewModel.updateWatermarkFields(locationText = it); onRefreshWatermark() }
        }
    }
}

@Composable
private fun LiquidWatermarkInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 30.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = LiquidColors.TextLowEmphasis,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.width(56.dp).padding(end = 8.dp)
        )
        Box(
            modifier = Modifier.weight(1f).height(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LiquidColors.GlassSurfaceDark)
                .border(1.dp, LiquidColors.GlassBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = LiquidColors.TextHighEmphasis
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(LiquidColors.AccentPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
