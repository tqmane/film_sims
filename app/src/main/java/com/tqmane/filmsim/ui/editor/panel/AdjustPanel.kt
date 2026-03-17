package com.tqmane.filmsim.ui.editor.panel

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.gl.FilmSimRenderer
import com.tqmane.filmsim.ui.EditState
import com.tqmane.filmsim.ui.EditorViewModel
import com.tqmane.filmsim.ui.WatermarkState
import com.tqmane.filmsim.ui.component.LiquidNoticeCard
import com.tqmane.filmsim.ui.component.LiquidTabBar
import com.tqmane.filmsim.ui.theme.LiquidColors

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
    val currentLutName = remember(editState.currentLutPath) {
        viewModel.resolveLutDisplayName(editState.currentLutPath)
    } ?: stringResource(R.string.adjustments)

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
            .padding(top = 14.dp, bottom = 10.dp, start = 18.dp, end = 18.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
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
                .heightIn(min = 260.dp, max = 420.dp)
        ) {
            when (currentTab) {
                AdjustTab.INTENSITY -> {
                    IntensityTab(
                        intensity = editState.intensity,
                        overlayLutName = viewModel.resolveLutDisplayName(editState.overlayLutPath),
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
                        showHints = showPanelHints,
                    )
                }
                AdjustTab.ADJUST -> {
                    ColorAdjustTab(
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
                    PresetsTab(viewModel = viewModel, showHints = showPanelHints)
                }
            }
        }
    }
}
