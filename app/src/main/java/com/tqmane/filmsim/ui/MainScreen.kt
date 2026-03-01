package com.tqmane.filmsim.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLSurfaceView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {}
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

        // GpuExportRenderer is owned entirely by the UI layer so it is released
        // together with the GLSurfaceView, not leaked into the ViewModel.
        val gpuExportRendererRef = remember { arrayOfNulls<GpuExportRenderer>(1) }

        // Watermark preview bitmap
        var watermarkPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }

        // UI toggles
        var isImmersive by rememberSaveable { mutableStateOf(false) }
        var showAdjustPanel by rememberSaveable { mutableStateOf(false) }
        // Dialog state
        var showSettings by rememberSaveable { mutableStateOf(false) }
        var pendingUpdate by remember { mutableStateOf<com.tqmane.filmsim.util.ReleaseInfo?>(null) }

        val selectedBrandIndex by viewModel.selectedBrandIndex.collectAsState()
        val selectedCategoryIndex by viewModel.selectedCategoryIndex.collectAsState()

        // Track UI heights for preview offset
        var topBarHeightPx by remember { mutableFloatStateOf(0f) }
        var bottomPanelHeightPx by remember { mutableFloatStateOf(0f) }

        // Track GLSurfaceView actual measured size.
        var glViewWidthPx by remember { mutableIntStateOf(0) }
        var glViewHeightPx by remember { mutableIntStateOf(0) }

        // Track if initial offset was applied to the preview
        var initialOffsetApplied by remember(viewState, glViewWidthPx, glViewHeightPx) { mutableStateOf(false) }

        // ─── UI Event Handler ─────────────────────────────────────────────────
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
                    is UiEvent.ShowUpdateDialog -> pendingUpdate = event.release
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

        // ─── GL Sync Effects ──────────────────────────────────────────────────

        // 1. 画像読み込み時: プレビュービットマップをGLにセット
        LaunchedEffect(viewState) {
            val content = viewState as? ViewState.Content ?: return@LaunchedEffect
            val r = renderer ?: return@LaunchedEffect
            val gl = glSurfaceView ?: return@LaunchedEffect
            touchHandler?.resetZoom()
            initialOffsetApplied = false
            val bmp = content.previewBitmap
            val lut = editState.currentLut
            val intensity = if (editState.hasSelectedLut) editState.intensity else 0f
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
            val intensity = if (editState.hasSelectedLut) editState.intensity else 0f
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

        // 3. LUT/エフェクト変更時: GL更新 + 透かしプレビュー再生成（統合）
        LaunchedEffect(
            editState.lutVersion, editState.intensity,
            editState.grainEnabled, editState.grainIntensity, editState.grainStyle
        ) {
            val r = renderer ?: return@LaunchedEffect
            val gl = glSurfaceView ?: return@LaunchedEffect
            if (viewState !is ViewState.Content) return@LaunchedEffect
            val wmActive = watermarkPreviewBitmap != null
            val lut = editState.currentLut
            val intensity = if (editState.hasSelectedLut) editState.intensity else 0f
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
            // Refresh watermark preview if one is active
            viewModel.refreshWatermarkIfActive { bmp -> watermarkPreviewBitmap = bmp }
        }

        // 4. 透かしスタイル変更時
        LaunchedEffect(watermarkState.style) {
            if (watermarkState.style != WatermarkStyle.NONE) {
                viewModel.renderWatermarkPreview { bmp -> watermarkPreviewBitmap = bmp }
            } else {
                watermarkPreviewBitmap = null
            }
        }

        // 5. 初期表示用のプレビューオフセット調整
        LaunchedEffect(topBarHeightPx, bottomPanelHeightPx, viewState, glViewWidthPx, glViewHeightPx) {
            val content = viewState as? ViewState.Content
            if (content != null && !initialOffsetApplied
                && topBarHeightPx > 0f && bottomPanelHeightPx > 0f
                && glViewWidthPx > 0 && glViewHeightPx > 0
            ) {
                touchHandler?.updateInitialBounds(
                    content.previewBitmap.width,
                    content.previewBitmap.height,
                    topBarHeightPx,
                    bottomPanelHeightPx
                )
                initialOffsetApplied = true
            }
        }

        // 6. Immersive切替時のプレビュー位置再調整
        LaunchedEffect(isImmersive) {
            if (viewState is ViewState.Content && initialOffsetApplied) {
                val effectiveTopBar = if (isImmersive) 0f else topBarHeightPx
                val effectiveBottomPanel = if (isImmersive) 0f else bottomPanelHeightPx
                touchHandler?.updateForImmersiveChange(effectiveTopBar, effectiveBottomPanel)
            }
        }

        // ─── Root Frame with Living Background ───────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            LivingBackground(modifier = Modifier.fillMaxSize())

            // ─── GL Surface + State Overlays ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (showAdjustPanel) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showAdjustPanel = false }
                        } else Modifier
                    )
            ) {
                AndroidView(
                    factory = { ctx ->
                        GLSurfaceView(ctx).apply {
                            setEGLContextClientVersion(3)
                            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                            preserveEGLContextOnPause = true
                            val r = FilmSimRenderer(ctx)
                            setRenderer(r)
                            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                            renderer = r
                            glSurfaceView = this
                            glExecutor = GlSurfaceViewExecutor(this)
                            // GpuExportRenderer is owned here (UI layer), not in the ViewModel
                            queueEvent { gpuExportRendererRef[0] = GpuExportRenderer(ctx) }

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
                                            r.setIntensity(if (editState.hasSelectedLut) editState.intensity else 0f)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            glViewWidthPx = coords.size.width
                            glViewHeightPx = coords.size.height
                        },
                    update = { view ->
                        view.visibility = if (viewState is ViewState.Content) android.view.View.VISIBLE else android.view.View.GONE
                    }
                )

                // State overlays (Loading / Error / Empty)
                ImageStateContent(
                    viewState = viewState,
                    onPickImage = onPickImage
                )
            }

            // ─── Main vertical Layout ─────────────────────────────────────────
            Column(modifier = Modifier.fillMaxSize()) {

                // ─── Top Bar ──────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = !isImmersive,
                    enter = slideInVertically(animationSpec = tween(380, easing = FastOutSlowInEasing)) { -it } + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(animationSpec = tween(320, easing = FastOutSlowInEasing)) { -it } + fadeOut(animationSpec = tween(250))
                ) {
                    LiquidTopBar(
                        onPickImage = onPickImage,
                        onSettings = { showSettings = true },
                        onSave = {
                            val gl = glExecutor
                            if (viewState !is ViewState.Content) {
                                Toast.makeText(context, context.getString(R.string.select_image_first), Toast.LENGTH_SHORT).show()
                            } else if (gl != null) {
                                Toast.makeText(context, context.getString(R.string.exporting), Toast.LENGTH_SHORT).show()
                                viewModel.saveHighResImage(gl, gpuExportRendererRef[0]) {
                                    Toast.makeText(context, context.getString(R.string.cpu_processing), Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                            .onGloballyPositioned { topBarHeightPx = it.size.height.toFloat() }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // ─── Bottom Area ───────────────────────────────────────────────
                BottomControlArea(
                    viewModel = viewModel,
                    editState = editState,
                    watermarkState = watermarkState,
                    viewState = viewState,
                    glSurfaceView = glSurfaceView,
                    renderer = renderer,
                    isImmersive = isImmersive,
                    showAdjustPanel = showAdjustPanel,
                    onShowAdjustPanelChange = { showAdjustPanel = it },
                    isWatermarkActive = watermarkPreviewBitmap != null,
                    onRefreshWatermark = { refreshWatermarkPreview() },
                    isProUser = isProUser,
                    selectedBrandIndex = selectedBrandIndex,
                    selectedCategoryIndex = selectedCategoryIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { bottomPanelHeightPx = it.size.height.toFloat() }
                )
            }
        }

        // ─── Compose Dialogs ──────────────────────────────────────────────────
        if (showSettings) {
            SettingsDialog(
                viewModel = viewModel,
                authViewModel = authViewModel,
                onSignIn = onSignIn,
                onSignOut = onSignOut,
                onDismiss = { showSettings = false }
            )
        }

        pendingUpdate?.let { release ->
            UpdateDialog(
                release = release,
                onDismiss = { pendingUpdate = null },
                onUpdate = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                    )
                    pendingUpdate = null
                }
            )
        }

        // Release GpuExportRenderer when the composable leaves composition
        DisposableEffect(Unit) {
            onDispose {
                gpuExportRendererRef[0]?.release()
                gpuExportRendererRef[0] = null
            }
        }
    }
}

// ─── Image State Content (Loading / Error / Empty overlays) ──────────────────

@Composable
private fun ImageStateContent(
    viewState: ViewState,
    onPickImage: () -> Unit
) {
    when (val state = viewState) {
        is ViewState.Empty -> LiquidPlaceholderContent(onPickImage = onPickImage)
        is ViewState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        is ViewState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        else -> {}
    }
}

// ─── Bottom Control Area (Adjust Panel + Control Panel) ──────────────────────

@Composable
private fun BottomControlArea(
    viewModel: MainViewModel,
    editState: EditState,
    watermarkState: WatermarkState,
    viewState: ViewState,
    glSurfaceView: android.opengl.GLSurfaceView?,
    renderer: com.tqmane.filmsim.gl.FilmSimRenderer?,
    isImmersive: Boolean,
    showAdjustPanel: Boolean,
    onShowAdjustPanelChange: (Boolean) -> Unit,
    isWatermarkActive: Boolean,
    onRefreshWatermark: () -> Unit,
    isProUser: Boolean,
    selectedBrandIndex: Int,
    selectedCategoryIndex: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Adjust Panel
        androidx.compose.animation.AnimatedVisibility(
            visible = !isImmersive && showAdjustPanel && editState.hasSelectedLut,
            enter = androidx.compose.animation.expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
        ) {
            LiquidAdjustPanel(
                editState = editState,
                watermarkState = watermarkState,
                viewModel = viewModel,
                glSurfaceView = glSurfaceView,
                renderer = renderer,
                isWatermarkActive = isWatermarkActive,
                onRefreshWatermark = onRefreshWatermark,
                isProUser = isProUser,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Bottom Control Panel
        AnimatedVisibility(
            visible = !isImmersive && viewState is ViewState.Content,
            enter = slideInVertically(animationSpec = tween(380, easing = FastOutSlowInEasing)) { it } + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(animationSpec = tween(320, easing = FastOutSlowInEasing)) { it } + fadeOut(animationSpec = tween(250))
        ) {
            GlassControlPanel(
                viewModel = viewModel,
                editState = editState,
                watermarkState = watermarkState,
                viewState = viewState,
                glSurfaceView = glSurfaceView,
                renderer = renderer,
                isWatermarkActive = isWatermarkActive,
                onRefreshWatermark = onRefreshWatermark,
                onLutReselected = { onShowAdjustPanelChange(!showAdjustPanel) },
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

