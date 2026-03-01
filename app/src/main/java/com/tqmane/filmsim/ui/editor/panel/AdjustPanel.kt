package com.tqmane.filmsim.ui.editor.panel

import android.opengl.GLSurfaceView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.gl.FilmSimRenderer
import com.tqmane.filmsim.ui.EditState
import com.tqmane.filmsim.ui.EditorViewModel
import com.tqmane.filmsim.ui.WatermarkState
import com.tqmane.filmsim.ui.component.LiquidIntensitySlider
import com.tqmane.filmsim.ui.component.LiquidTabBar
import com.tqmane.filmsim.ui.theme.LiquidColors

internal enum class AdjustTab { INTENSITY, GRAIN, WATERMARK }

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
        AdjustTab.GRAIN to stringResource(R.string.grain),
        AdjustTab.WATERMARK to (
            if (!isProUser) "${stringResource(R.string.watermark)} 🔒"
            else stringResource(R.string.watermark)
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
