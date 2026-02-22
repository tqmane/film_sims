package com.tqmane.filmsim.ui

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tqmane.filmsim.R
import com.tqmane.filmsim.gl.FilmSimRenderer
import com.tqmane.filmsim.gl.GlCommandExecutor
import com.tqmane.filmsim.gl.GlSurfaceViewExecutor
import com.tqmane.filmsim.gl.GpuExportRenderer
import com.tqmane.filmsim.ui.components.LiquidPlaceholderContent
import com.tqmane.filmsim.ui.components.LiquidTopBar
import com.tqmane.filmsim.ui.components.LivingBackground
import com.tqmane.filmsim.ui.theme.LiquidTheme
import com.tqmane.filmsim.util.WatermarkProcessor.WatermarkStyle

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN SCREEN - Entry Point with Liquid Theme
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onPickImage: () -> Unit,
    onShowSettings: () -> Unit,
    onShowUpdateDialog: (com.tqmane.filmsim.util.ReleaseInfo) -> Unit
) {
    LiquidTheme {
        val context = LocalContext.current
        val viewState by viewModel.viewState.collectAsState()
        val editState by viewModel.editState.collectAsState()
        val watermarkState by viewModel.watermarkState.collectAsState()
        val isProUser by authViewModel.isProUser.collectAsState()

        // GL references
        var glSurfaceView by remember { mutableStateOf<GLSurfaceView?>(null) }
        var renderer by remember { mutableStateOf<FilmSimRenderer?>(null) }
        var glExecutor by remember { mutableStateOf<GlCommandExecutor?>(null) }
        var touchHandler by remember { mutableStateOf<GlTouchHandler?>(null) }

        // Watermark preview bitmap
        var watermarkPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
        
        // UI toggles
        var isImmersive by rememberSaveable { mutableStateOf(false) }
        var panelExpanded by rememberSaveable { mutableStateOf(true) }
        var showAdjustPanel by rememberSaveable { mutableStateOf(false) }

        // Lifted brand/category selection state (survives immersive toggle)
        val selectedBrandIndex by viewModel.selectedBrandIndex.collectAsState()
        val selectedCategoryIndex by viewModel.selectedCategoryIndex.collectAsState()

        // Handle UI events
        LaunchedEffect(Unit) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is UiEvent.ShowToast -> {
                        val msg = if (event.formatArgs.isNotEmpty())
                            context.getString(event.messageResId, *event.formatArgs)
                        else context.getString(event.messageResId)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                    is UiEvent.ShowRawToast ->
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    is UiEvent.ShowUpdateDialog -> onShowUpdateDialog(event.release)
                    is UiEvent.ImageSaved -> {
                        val msg = context.getString(
                            R.string.image_saved,
                            "${event.width}x${event.height}", event.path, event.filename
                        )
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Watermark preview refresh helper
        fun refreshWatermarkPreview() {
            viewModel.refreshWatermarkIfActive { bmp -> watermarkPreviewBitmap = bmp }
        }

        // 1. 画像読み込み時: プレビュービットマップをGLにセット
        LaunchedEffect(viewState) {
            val content = viewState as? ViewState.Content ?: return@LaunchedEffect
            val r = renderer ?: return@LaunchedEffect
            val gl = glSurfaceView ?: return@LaunchedEffect
            touchHandler?.resetZoom()
            val bmp = content.previewBitmap
            val lut = editState.currentLut
            val intensity = editState.intensity
            val grainOn = editState.grainEnabled
            val grainVal = editState.grainIntensity
            val grainSty = editState.grainStyle
            gl.queueEvent {
                r.setImage(bmp)
                if (lut != null) r.setLut(lut)
                r.setIntensity(intensity)
                r.setGrainEnabled(grainOn)
                if (grainOn) r.setGrainIntensity(grainVal)
                r.setGrainStyle(grainSty)
                gl.requestRender()
            }
        }

        // 2. 透かしプレビュー変更時
        LaunchedEffect(watermarkPreviewBitmap) {
            val r = renderer ?: return@LaunchedEffect
            val gl = glSurfaceView ?: return@LaunchedEffect
            val wmBmp = watermarkPreviewBitmap
            val content = viewState as? ViewState.Content
            val lut = editState.currentLut
            val intensity = editState.intensity
            val grainOn = editState.grainEnabled
            val grainVal = editState.grainIntensity
            val grainSty = editState.grainStyle
            gl.queueEvent {
                if (wmBmp != null) {
                    r.setImage(wmBmp)
                    r.setIntensity(0f)
                    r.setGrainEnabled(false)
                } else if (content != null) {
                    r.setImage(content.previewBitmap)
                    if (lut != null) r.setLut(lut)
                    r.setIntensity(intensity)
                    r.setGrainEnabled(grainOn)
                    if (grainOn) r.setGrainIntensity(grainVal)
                    r.setGrainStyle(grainSty)
                }
                gl.requestRender()
            }
        }

        // 3. LUT/エフェクト変更時
        LaunchedEffect(
            editState.lutVersion, editState.intensity,
            editState.grainEnabled, editState.grainIntensity, editState.grainStyle
        ) {
            val r = renderer ?: return@LaunchedEffect
            val gl = glSurfaceView ?: return@LaunchedEffect
            if (viewState !is ViewState.Content) return@LaunchedEffect
            val wmActive = watermarkPreviewBitmap != null
            val lut = editState.currentLut
            val intensity = editState.intensity
            val grainOn = editState.grainEnabled
            val grainVal = editState.grainIntensity
            val grainSty = editState.grainStyle
            gl.queueEvent {
                if (lut != null) r.setLut(lut)
                if (!wmActive) {
                    r.setIntensity(intensity)
                    r.setGrainEnabled(grainOn)
                    if (grainOn) r.setGrainIntensity(grainVal)
                    r.setGrainStyle(grainSty)
                }
                gl.requestRender()
            }
        }

        // 4. 透かしモード中のLUT/エフェクト変更時
        LaunchedEffect(editState.lutVersion, editState.intensity, editState.grainEnabled, editState.grainIntensity, editState.grainStyle) {
            viewModel.refreshWatermarkIfActive { bmp -> watermarkPreviewBitmap = bmp }
        }

        // 5. 透かしスタイル変更時
        LaunchedEffect(watermarkState.style) {
            if (watermarkState.style != WatermarkStyle.NONE) {
                viewModel.renderWatermarkPreview { bmp -> watermarkPreviewBitmap = bmp }
            } else {
                watermarkPreviewBitmap = null
            }
        }

        // ─── Root Frame with Living Background ───────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            // Living Background (aurora + noise)
            LivingBackground(modifier = Modifier.fillMaxSize())

            // GLSurfaceView (full screen)
            AndroidView(
                factory = { ctx ->
                    GLSurfaceView(ctx).apply {
                        setEGLContextClientVersion(3)
                        preserveEGLContextOnPause = true
                        val r = FilmSimRenderer(ctx)
                        setRenderer(r)
                        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                        renderer = r
                        glSurfaceView = this
                        glExecutor = GlSurfaceViewExecutor(this)
                        queueEvent { viewModel.gpuExportRenderer = GpuExportRenderer(ctx) }

                        val th = GlTouchHandler(
                            this, r,
                            onSingleTap = { isImmersive = !isImmersive },
                            onLongPressStart = {
                                if (editState.hasSelectedLut && watermarkPreviewBitmap == null) {
                                    queueEvent {
                                        r.setIntensity(0f); r.setGrainEnabled(false)
                                        requestRender()
                                    }
                                }
                            },
                            onLongPressEnd = {
                                if (watermarkPreviewBitmap == null) {
                                    queueEvent {
                                        r.setIntensity(editState.intensity)
                                        r.setGrainEnabled(editState.grainEnabled)
                                        if (editState.grainEnabled) r.setGrainIntensity(editState.grainIntensity)
                                        requestRender()
                                    }
                                }
                            }
                        )
                        th.install()
                        touchHandler = th
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // ─── Main vertical Layout ─────────────────────────────────────────
            Column(modifier = Modifier.fillMaxSize()) {

                // ─── Top Bar ────────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = !isImmersive,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
                ) {
                    LiquidTopBar(
                        onPickImage = onPickImage,
                        onSettings = onShowSettings,
                        onSave = {
                            val gl = glExecutor
                            if (viewState !is ViewState.Content) {
                                Toast.makeText(context, context.getString(R.string.select_image_first), Toast.LENGTH_SHORT).show()
                            } else if (gl != null) {
                                Toast.makeText(context, context.getString(R.string.exporting), Toast.LENGTH_SHORT).show()
                                viewModel.saveHighResImage(gl) {
                                    Toast.makeText(context, context.getString(R.string.cpu_processing), Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    )
                }

                // ─── Content Area (weight=1) ────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (showAdjustPanel) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showAdjustPanel = false }
                            } else Modifier
                        )
                ) {
                    if (viewState is ViewState.Empty) {
                        LiquidPlaceholderContent(onPickImage = onPickImage)
                    }
                }

                // ─── Adjust Panel (slides in from bottom) ───────────────────────
                AnimatedVisibility(
                    visible = !isImmersive && showAdjustPanel && editState.hasSelectedLut,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    LiquidAdjustPanel(
                        editState = editState,
                        watermarkState = watermarkState,
                        viewModel = viewModel,
                        glSurfaceView = glSurfaceView,
                        renderer = renderer,
                        isWatermarkActive = watermarkPreviewBitmap != null,
                        onRefreshWatermark = { refreshWatermarkPreview() },
                        isProUser = isProUser,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ─── Bottom Control Panel (Glass Bottom Sheet) ──────────────────
                AnimatedVisibility(
                    visible = !isImmersive,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    GlassControlPanel(
                        viewModel = viewModel,
                        editState = editState,
                        watermarkState = watermarkState,
                        viewState = viewState,
                        panelExpanded = panelExpanded,
                        onTogglePanel = { panelExpanded = !panelExpanded },
                        glSurfaceView = glSurfaceView,
                        renderer = renderer,
                        isWatermarkActive = watermarkPreviewBitmap != null,
                        onRefreshWatermark = { refreshWatermarkPreview() },
                        onLutReselected = { showAdjustPanel = !showAdjustPanel },
                        isProUser = isProUser,
                        selectedBrandIndex = selectedBrandIndex,
                        onBrandIndexChanged = { viewModel.setSelectedBrandIndex(it) },
                        selectedCategoryIndex = selectedCategoryIndex,
                        onCategoryIndexChanged = { viewModel.setSelectedCategoryIndex(it) },
                        squareTop = showAdjustPanel && editState.hasSelectedLut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                    )
                }
            }
        }

        // Cleanup
        DisposableEffect(Unit) {
            onDispose {
                viewModel.gpuExportRenderer?.release()
                viewModel.gpuExportRenderer = null
            }
        }
    }
}

