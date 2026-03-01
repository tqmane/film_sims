package com.tqmane.filmsim.ui

import android.opengl.GLSurfaceView
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.gl.FilmSimRenderer
import com.tqmane.filmsim.ui.components.LiquidIntensitySlider
import com.tqmane.filmsim.ui.theme.LiquidColors

// ═══════════════════════════════════════════════════════════════════════════════
// LIQUID ADJUST PANEL - Slide-in panel with Intensity / Grain / Watermark tabs
// ═══════════════════════════════════════════════════════════════════════════════

internal enum class AdjustTab { INTENSITY, GRAIN, WATERMARK }

@Composable
fun LiquidAdjustPanel(
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
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {}
            .padding(top = 14.dp, bottom = 10.dp, start = 18.dp, end = 18.dp)
    ) {
        // ─── Tab Bar (Pill UI) ─────────────────────────────────────────────
        LiquidTabBar(
            selectedTab = currentTab,
            onTabSelected = {
                if (it == AdjustTab.WATERMARK && !isProUser) {
                    Toast.makeText(context, context.getString(R.string.pro_watermark_locked), Toast.LENGTH_SHORT).show()
                    return@LiquidTabBar
                }
                selectedTab = it.name
            },
            isProUser = isProUser,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
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
internal fun LiquidTabBar(
    selectedTab: AdjustTab,
    onTabSelected: (AdjustTab) -> Unit,
    isProUser: Boolean = false,
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
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x12FFFFFF))
            .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(22.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { (tab, labelRes) ->
            val isSelected = selectedTab == tab
            val bgAlpha by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFFFAB60) else Color.Transparent,
                animationSpec = tween(250),
                label = "tab_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF0C0C10) else LiquidColors.TextLowEmphasis,
                animationSpec = tween(250),
                label = "tab_text"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(bgAlpha)
                    .clickable {
                        haptic.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                        )
                        onTabSelected(tab)
                    }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                val label = stringResource(labelRes)
                val displayLabel = if (tab == AdjustTab.WATERMARK && !isProUser) "$label 🔒" else label
                Text(
                    displayLabel,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.01.sp
                )
            }
        }
    }
}
