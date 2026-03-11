package com.tqmane.filmsim.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLSurfaceView
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tqmane.filmsim.R
import com.tqmane.filmsim.gl.FilmSimRenderer
import com.tqmane.filmsim.gl.GlCommandExecutor
import com.tqmane.filmsim.gl.GlSurfaceViewExecutor
import com.tqmane.filmsim.gl.GpuExportRenderer
import com.tqmane.filmsim.ui.AuthViewModel
import com.tqmane.filmsim.ui.EditState
import com.tqmane.filmsim.ui.EditorViewModel
import com.tqmane.filmsim.ui.editor.panel.AdjustPanel
import com.tqmane.filmsim.ui.editor.panel.LutSelectorPanel
import com.tqmane.filmsim.ui.editor.dialog.SettingsDialog
import com.tqmane.filmsim.ui.UiEvent
import com.tqmane.filmsim.ui.editor.dialog.UpdateDialog
import com.tqmane.filmsim.ui.editor.panel.AdjustTab
import com.tqmane.filmsim.ui.ViewState
import com.tqmane.filmsim.ui.WatermarkState
import com.tqmane.filmsim.ui.component.AuroraBackground
import com.tqmane.filmsim.ui.component.EmptyState
import com.tqmane.filmsim.ui.component.LiquidButton
import com.tqmane.filmsim.ui.component.TopBar
import com.tqmane.filmsim.ui.theme.LiquidColors
import com.tqmane.filmsim.ui.theme.LiquidTheme
import com.tqmane.filmsim.util.WatermarkProcessor.WatermarkStyle
import kotlinx.coroutines.delay

/**
 * Root editor screen — replaces MainScreen.
 * Uses [EditorPanelState] sealed interface instead of scattered boolean flags.
 */
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
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
        val showPanelHints by viewModel.showPanelHints.collectAsState()
        val isProUser by authViewModel.isProUser.collectAsState()
        val isSaving by viewModel.isSaving.collectAsState()

        // GL references
        var glSurfaceView by remember { mutableStateOf<GLSurfaceView?>(null) }
        var renderer by remember { mutableStateOf<FilmSimRenderer?>(null) }
        var glExecutor by remember { mutableStateOf<GlCommandExecutor?>(null) }
        var gestureHandler by remember { mutableStateOf<ImageGestureHandler?>(null) }
        val gpuExportRendererRef = remember { arrayOfNulls<GpuExportRenderer>(1) }

        // Watermark preview bitmap
        var watermarkPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }

        // ─── Panel state (replaces scattered booleans) ────────────────────────
        var panelState by rememberSaveable { mutableStateOf<String>("LutSelector") }
        var selectedAdjustTabName by rememberSaveable { mutableStateOf(AdjustTab.INTENSITY.name) }
        var isSelectingOverlay by rememberSaveable { mutableStateOf(false) }
        var showSettings by rememberSaveable { mutableStateOf(false) }
        var pendingUpdate by remember { mutableStateOf<com.tqmane.filmsim.util.ReleaseInfo?>(null) }
        var savedBanner by remember { mutableStateOf<UiEvent.ImageSaved?>(null) }
        var compareEnabled by rememberSaveable { mutableStateOf(false) }
        var comparePosition by rememberSaveable { mutableFloatStateOf(0.5f) }
        var compareVertical by rememberSaveable { mutableStateOf(true) }

        // Derived state helpers
        val isImmersive = panelState == "Immersive"
        val showAdjustPanel = panelState == "Adjustments"
        val selectedAdjustTab = remember(selectedAdjustTabName) {
            runCatching { AdjustTab.valueOf(selectedAdjustTabName) }.getOrElse { AdjustTab.INTENSITY }
        }

        val selectedBrandIndex by viewModel.selectedBrandIndex.collectAsState()
        val selectedCategoryIndex by viewModel.selectedCategoryIndex.collectAsState()

        // Track UI heights for preview offset
        var topBarHeightPx by remember { mutableFloatStateOf(0f) }
        var bottomPanelHeightPx by remember { mutableFloatStateOf(0f) }
        var glViewWidthPx by remember { mutableIntStateOf(0) }
        var glViewHeightPx by remember { mutableIntStateOf(0) }
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
                        savedBanner = event
                    }
                }
            }
        }

        fun refreshWatermarkPreview() {
            viewModel.refreshWatermarkIfActive { bmp -> watermarkPreviewBitmap = bmp }
        }

        // Auto-dismiss save success banner after 3 seconds
        LaunchedEffect(savedBanner) {
            if (savedBanner != null) {
                delay(3000)
                savedBanner = null
            }
        }

        // ─── GL Sync Effects ──────────────────────────────────────────────────

        LaunchedEffect(viewState) {
            val content = viewState as? ViewState.Content ?: return@LaunchedEffect
            val r = renderer ?: return@LaunchedEffect
            val gl = glSurfaceView ?: return@LaunchedEffect
            gestureHandler?.resetZoom()
            initialOffsetApplied = false
            val bmp = content.previewBitmap
            val lut = editState.currentLut
            val overlayLut = editState.overlayLut
            val intensity = if (editState.hasSelectedLut) editState.intensity else 0f
            val overlayIntensity = if (overlayLut != null) editState.overlayIntensity else 0f
            val grainOn = editState.grainEnabled
            val grainVal = editState.grainIntensity
            val grainSty = editState.grainStyle
            val exp = editState.exposure
            val con = editState.contrast
            val hl = editState.highlights
            val sh = editState.shadows
            val ct = editState.colorTemp
            val hue = editState.hue
            val sat = editState.saturation
            val lum = editState.luminance
            gl.queueEvent {
                r.setImage(bmp)
                if (lut != null) r.setLut(lut)
                if (overlayLut != null) r.setOverlayLut(overlayLut)
                r.setIntensity(intensity)
                r.setOverlayIntensity(overlayIntensity)
                r.setGrainEnabled(grainOn)
                if (grainOn) r.setGrainIntensity(grainVal)
                r.setGrainStyle(grainSty)
                r.setExposure(exp)
                r.setContrast(con)
                r.setHighlights(hl)
                r.setShadows(sh)
                r.setColorTemp(ct)
                r.setHue(hue)
                r.setSaturation(sat)
                r.setLuminance(lum)
                gl.requestRender()
            }
        }

        LaunchedEffect(watermarkPreviewBitmap) {
            val r = renderer ?: return@LaunchedEffect
            val gl = glSurfaceView ?: return@LaunchedEffect
            val wmBmp = watermarkPreviewBitmap
            val content = viewState as? ViewState.Content
            val lut = editState.currentLut
            val overlayLut = editState.overlayLut
            val intensity = if (editState.hasSelectedLut) editState.intensity else 0f
            val overlayIntensity = if (overlayLut != null) editState.overlayIntensity else 0f
            val grainOn = editState.grainEnabled
            val grainVal = editState.grainIntensity
            val grainSty = editState.grainStyle
            val exp = editState.exposure
            val con = editState.contrast
            val hl = editState.highlights
            val sh = editState.shadows
            val ct = editState.colorTemp
            val hue = editState.hue
            val sat = editState.saturation
            val lum = editState.luminance
            gl.queueEvent {
                if (wmBmp != null) {
                    r.setImage(wmBmp)
                    r.setIntensity(0f)
                    r.setOverlayIntensity(0f)
                    r.setGrainEnabled(false)
                    r.setExposure(exp)
                    r.setContrast(con)
                    r.setHighlights(hl)
                    r.setShadows(sh)
                    r.setColorTemp(ct)
                    r.setHue(hue)
                    r.setSaturation(sat)
                    r.setLuminance(lum)
                } else if (content != null) {
                    r.setImage(content.previewBitmap)
                    if (lut != null) r.setLut(lut)
                    if (overlayLut != null) r.setOverlayLut(overlayLut)
                    r.setIntensity(intensity)
                    r.setOverlayIntensity(overlayIntensity)
                    r.setGrainEnabled(grainOn)
                    if (grainOn) r.setGrainIntensity(grainVal)
                    r.setGrainStyle(grainSty)
                    r.setExposure(exp)
                    r.setContrast(con)
                    r.setHighlights(hl)
                    r.setShadows(sh)
                    r.setColorTemp(ct)
                    r.setHue(hue)
                    r.setSaturation(sat)
                    r.setLuminance(lum)
                }
                gl.requestRender()
            }
        }

        LaunchedEffect(compareEnabled, comparePosition, compareVertical, watermarkPreviewBitmap, renderer, glSurfaceView) {
            val r = renderer ?: return@LaunchedEffect
            val gl = glSurfaceView ?: return@LaunchedEffect
            val isPreviewCompareEnabled = compareEnabled && watermarkPreviewBitmap == null
            gl.queueEvent {
                r.setCompareEnabled(isPreviewCompareEnabled)
                r.setCompareSplit(comparePosition)
                r.setCompareVertical(compareVertical)
                gl.requestRender()
            }
        }

        LaunchedEffect(
            editState.lutVersion, editState.intensity,
            editState.overlayIntensity,
            editState.grainEnabled, editState.grainIntensity, editState.grainStyle,
            editState.exposure, editState.contrast, editState.highlights,
            editState.shadows, editState.colorTemp,
            editState.hue, editState.saturation, editState.luminance
        ) {
            val r = renderer ?: return@LaunchedEffect
            val gl = glSurfaceView ?: return@LaunchedEffect
            if (viewState !is ViewState.Content) return@LaunchedEffect
            val wmActive = watermarkPreviewBitmap != null
            val lut = editState.currentLut
            val overlayLut = editState.overlayLut
            val intensity = if (editState.hasSelectedLut) editState.intensity else 0f
            val overlayIntensity = if (overlayLut != null) editState.overlayIntensity else 0f
            val grainOn = editState.grainEnabled
            val grainVal = editState.grainIntensity
            val grainSty = editState.grainStyle
            val exp = editState.exposure
            val con = editState.contrast
            val hl = editState.highlights
            val sh = editState.shadows
            val ct = editState.colorTemp
            val hue = editState.hue
            val sat = editState.saturation
            val lum = editState.luminance
            gl.queueEvent {
                if (lut != null) r.setLut(lut)
                if (overlayLut != null) r.setOverlayLut(overlayLut)
                if (!wmActive) {
                    r.setIntensity(intensity)
                    r.setOverlayIntensity(overlayIntensity)
                    r.setGrainEnabled(grainOn)
                    if (grainOn) r.setGrainIntensity(grainVal)
                    r.setGrainStyle(grainSty)
                }
                r.setExposure(exp)
                r.setContrast(con)
                r.setHighlights(hl)
                r.setShadows(sh)
                r.setColorTemp(ct)
                r.setHue(hue)
                r.setSaturation(sat)
                r.setLuminance(lum)
                gl.requestRender()
            }
            viewModel.refreshWatermarkIfActive { bmp -> watermarkPreviewBitmap = bmp }
        }

        LaunchedEffect(watermarkState.style) {
            if (watermarkState.style != WatermarkStyle.NONE) {
                viewModel.renderWatermarkPreview { bmp -> watermarkPreviewBitmap = bmp }
            } else {
                watermarkPreviewBitmap = null
            }
        }

        LaunchedEffect(topBarHeightPx, bottomPanelHeightPx, viewState, glViewWidthPx, glViewHeightPx) {
            val content = viewState as? ViewState.Content
            if (content != null && !initialOffsetApplied
                && topBarHeightPx > 0f && bottomPanelHeightPx > 0f
                && glViewWidthPx > 0 && glViewHeightPx > 0
            ) {
                gestureHandler?.updateInitialBounds(
                    content.previewBitmap.width,
                    content.previewBitmap.height,
                    topBarHeightPx,
                    bottomPanelHeightPx
                )
                initialOffsetApplied = true
            }
        }

        LaunchedEffect(isImmersive) {
            if (viewState is ViewState.Content && initialOffsetApplied) {
                val effectiveTopBar = if (isImmersive) 0f else topBarHeightPx
                val effectiveBottomPanel = if (isImmersive) 0f else bottomPanelHeightPx
                gestureHandler?.updateForImmersiveChange(effectiveTopBar, effectiveBottomPanel)
            }
        }

        // ─── Root Frame ───────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            AuroraBackground(modifier = Modifier.fillMaxSize())

            // GL Surface + State Overlays
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (showAdjustPanel) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { panelState = "LutSelector" }
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
                            queueEvent { gpuExportRendererRef[0] = GpuExportRenderer(ctx) }

                            val gh = ImageGestureHandler(
                                this, r,
                                onSingleTap = {
                                    panelState = if (panelState == "Immersive") "LutSelector" else "Immersive"
                                },
                                onLongPressStart = {
                                    if (editState.hasSelectedLut && watermarkPreviewBitmap == null) {
                                        queueEvent {
                                            r.setIntensity(0f)
                                            r.setOverlayIntensity(0f)
                                            r.setGrainEnabled(false)
                                            requestRender()
                                        }
                                    }
                                },
                                onLongPressEnd = {
                                    if (watermarkPreviewBitmap == null) {
                                        queueEvent {
                                            r.setIntensity(if (editState.hasSelectedLut) editState.intensity else 0f)
                                            r.setOverlayIntensity(
                                                if (editState.overlayLut != null) editState.overlayIntensity else 0f
                                            )
                                            r.setGrainEnabled(editState.grainEnabled)
                                            if (editState.grainEnabled) r.setGrainIntensity(editState.grainIntensity)
                                            requestRender()
                                        }
                                    }
                                }
                            )
                            gh.install()
                            gestureHandler = gh
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

                ImageStateContent(
                    viewState = viewState,
                    onPickImage = onPickImage,
                    onRetry = {
                        val content = viewState
                        if (content is ViewState.Error) {
                            // Trigger image picker again as retry
                            onPickImage()
                        }
                    }
                )

                if (viewState is ViewState.Content && compareEnabled && watermarkPreviewBitmap == null) {
                    ComparePreviewOverlay(
                        split = comparePosition,
                        vertical = compareVertical,
                        onSplitChange = { comparePosition = it }
                    )
                }
            }

            // Main vertical layout
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                AnimatedVisibility(
                    visible = !isImmersive,
                    enter = slideInVertically(animationSpec = tween(380, easing = FastOutSlowInEasing)) { -it } + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(animationSpec = tween(320, easing = FastOutSlowInEasing)) { -it } + fadeOut(animationSpec = tween(250))
                ) {
                    TopBar(
                        onPickImage = onPickImage,
                        onSettings = { showSettings = true },
                        onSave = {
                            val gl = glExecutor
                            if (viewState !is ViewState.Content) {
                                Toast.makeText(context, context.getString(R.string.select_image_first), Toast.LENGTH_SHORT).show()
                            } else if (gl != null) {
                                viewModel.saveHighResImage(gl, gpuExportRendererRef[0]) {
                                    Toast.makeText(context, context.getString(R.string.cpu_processing), Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        canSave = viewState is ViewState.Content && !isSaving,
                        isSaving = isSaving,
                        canCompare = viewState is ViewState.Content && editState.hasSelectedLut,
                        compareEnabled = compareEnabled,
                        onCompareToggle = { compareEnabled = !compareEnabled },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                            .onGloballyPositioned { topBarHeightPx = it.size.height.toFloat() }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Area
                BottomControlArea(
                    viewModel = viewModel,
                    editState = editState,
                    watermarkState = watermarkState,
                    viewState = viewState,
                    glSurfaceView = glSurfaceView,
                    renderer = renderer,
                    isImmersive = isImmersive,
                    showAdjustPanel = showAdjustPanel,
                    onShowAdjustPanelChange = { show ->
                        panelState = if (show) "Adjustments" else "LutSelector"
                    },
                    isWatermarkActive = watermarkPreviewBitmap != null,
                    onRefreshWatermark = { refreshWatermarkPreview() },
                    isProUser = isProUser,
                    selectedBrandIndex = selectedBrandIndex,
                    selectedCategoryIndex = selectedCategoryIndex,
                    selectedAdjustTab = selectedAdjustTab,
                    onAdjustTabSelected = { selectedAdjustTabName = it.name },
                    showPanelHints = showPanelHints,
                    compareEnabled = compareEnabled,
                    comparePosition = comparePosition,
                    compareVertical = compareVertical,
                    onComparePositionChange = { comparePosition = it },
                    onCompareVerticalChange = { compareVertical = it },
                    isSelectingOverlay = isSelectingOverlay,
                    onStartOverlaySelection = {
                        selectedAdjustTabName = AdjustTab.INTENSITY.name
                        isSelectingOverlay = true
                    },
                    onFinishOverlaySelection = { isSelectingOverlay = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { bottomPanelHeightPx = it.size.height.toFloat() }
                )
            }
        }

        // Dialogs
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

        // Save success banner overlay (floats above all content)
        SaveSuccessBanner(
            event = savedBanner,
            onDismiss = { savedBanner = null }
        )

        DisposableEffect(Unit) {
            onDispose {
                gpuExportRendererRef[0]?.release()
                gpuExportRendererRef[0] = null
            }
        }
    }
}

// ─── Image State Content ─────────────────────────────────────────────────────

@Composable
private fun ImageStateContent(
    viewState: ViewState,
    onPickImage: () -> Unit,
    onRetry: () -> Unit
) {
    when (val state = viewState) {
        is ViewState.Empty -> EmptyState(onPickImage = onPickImage)
        is ViewState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = LiquidColors.AccentPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.loading_image),
                        color = LiquidColors.TextMediumEmphasis,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
        is ViewState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    // Error icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x1AFF6B6B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "!",
                            color = Color(0xFFFF6B6B),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.error_title),
                        color = LiquidColors.TextHighEmphasis,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = state.message,
                        color = LiquidColors.TextLowEmphasis,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 20.sp,
                        modifier = Modifier.widthIn(max = 280.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LiquidButton(
                            onClick = onRetry,
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text(
                                stringResource(R.string.error_pick_another),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }
        }
        else -> {}
    }
}

// ─── Bottom Control Area ─────────────────────────────────────────────────────

@Composable
private fun BottomControlArea(
    viewModel: EditorViewModel,
    editState: EditState,
    watermarkState: WatermarkState,
    viewState: ViewState,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?,
    isImmersive: Boolean,
    showAdjustPanel: Boolean,
    onShowAdjustPanelChange: (Boolean) -> Unit,
    isWatermarkActive: Boolean,
    onRefreshWatermark: () -> Unit,
    isProUser: Boolean,
    selectedBrandIndex: Int,
    selectedCategoryIndex: Int,
    selectedAdjustTab: AdjustTab,
    onAdjustTabSelected: (AdjustTab) -> Unit,
    showPanelHints: Boolean,
    compareEnabled: Boolean,
    comparePosition: Float,
    compareVertical: Boolean,
    onComparePositionChange: (Float) -> Unit,
    onCompareVerticalChange: (Boolean) -> Unit,
    isSelectingOverlay: Boolean,
    onStartOverlaySelection: () -> Unit,
    onFinishOverlaySelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    AnimatedVisibility(
        visible = !isImmersive && viewState is ViewState.Content,
        modifier = modifier,
        enter = slideInVertically(animationSpec = tween(380, easing = FastOutSlowInEasing)) { it } + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(animationSpec = tween(320, easing = FastOutSlowInEasing)) { it } + fadeOut(animationSpec = tween(250))
    ) {
        AnimatedContent(
            targetState = showAdjustPanel && editState.hasSelectedLut,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            modifier = Modifier.fillMaxWidth()
        ) { showAdj ->
            if (showAdj) {
                AdjustPanel(
                    editState = editState,
                    watermarkState = watermarkState,
                    viewModel = viewModel,
                    glSurfaceView = glSurfaceView,
                    renderer = renderer,
                    isWatermarkActive = isWatermarkActive,
                    onRefreshWatermark = onRefreshWatermark,
                    selectedTab = selectedAdjustTab,
                    onTabSelected = onAdjustTabSelected,
                    showPanelHints = showPanelHints,
                    onSelectOverlayFilter = {
                        onStartOverlaySelection()
                        onShowAdjustPanelChange(false)
                    },
                    compareEnabled = compareEnabled,
                    comparePosition = comparePosition,
                    compareVertical = compareVertical,
                    onComparePositionChange = onComparePositionChange,
                    onCompareVerticalChange = onCompareVerticalChange,
                    isProUser = isProUser,
                    onClose = { onShowAdjustPanelChange(false) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = navPadding)
                )
            } else {
                LutSelectorPanel(
                    viewModel = viewModel,
                    editState = editState,
                    watermarkState = watermarkState,
                    viewState = viewState,
                    glSurfaceView = glSurfaceView,
                    renderer = renderer,
                    isWatermarkActive = isWatermarkActive,
                    onRefreshWatermark = onRefreshWatermark,
                    onLutReselected = { onShowAdjustPanelChange(true) },
                    showPanelHints = showPanelHints,
                    isSelectingOverlay = isSelectingOverlay,
                    onCancelOverlaySelection = {
                        onFinishOverlaySelection()
                        onShowAdjustPanelChange(true)
                    },
                    onOverlaySelectionComplete = {
                        onFinishOverlaySelection()
                        onShowAdjustPanelChange(true)
                    },
                    isProUser = isProUser,
                    selectedBrandIndex = selectedBrandIndex,
                    onBrandIndexChanged = { viewModel.setSelectedBrandIndex(it) },
                    selectedCategoryIndex = selectedCategoryIndex,
                    onCategoryIndexChanged = { viewModel.setSelectedCategoryIndex(it) },
                    squareTop = false,
                    modifier = Modifier.fillMaxWidth().padding(bottom = navPadding)
                )
            }
        }
    }
}

@Composable
private fun ComparePreviewOverlay(
    split: Float,
    vertical: Boolean,
    onSplitChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var dragActive by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 20.dp)
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val dragThresholdPx = with(density) { 28.dp.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val indicatorOffset = if (vertical) (maxWidth * split) - 1.dp else (maxHeight * split) - 1.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(split, vertical, maxWidthPx, maxHeightPx) {
                    val availablePx = if (vertical) maxWidthPx else maxHeightPx
                    if (availablePx <= 0f) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            val indicator = split * availablePx
                            val touchAxis = if (vertical) offset.x else offset.y
                            dragActive = kotlin.math.abs(touchAxis - indicator) <= dragThresholdPx
                            if (dragActive) {
                                onSplitChange((touchAxis / availablePx).coerceIn(0f, 1f))
                            }
                        },
                        onDragEnd = {
                            dragActive = false
                        },
                        onDragCancel = {
                            dragActive = false
                        },
                        onDrag = { change, _ ->
                            if (!dragActive) return@detectDragGestures
                            val dragAxis = if (vertical) change.position.x else change.position.y
                            onSplitChange((dragAxis / availablePx).coerceIn(0f, 1f))
                        }
                    )
                }
        )

        if (vertical) {
            Text(
                text = stringResource(R.string.compare_after),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )

            Text(
                text = stringResource(R.string.compare_before),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        } else {
            Text(
                text = stringResource(R.string.compare_after),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )

            Text(
                text = stringResource(R.string.compare_before),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        if (vertical) {
            // Divider line
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = indicatorOffset)
                    .fillMaxHeight()
                    .widthIn(min = 2.dp, max = 2.dp)
                    .background(Color.White.copy(alpha = 0.92f))
            )
            // Drag handle pill
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = indicatorOffset - 3.dp)
                    .size(width = 6.dp, height = 44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
            )
        } else {
            // Divider line
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = indicatorOffset)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.92f))
            )
            // Drag handle pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = indicatorOffset - 3.dp)
                    .size(width = 44.dp, height = 6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
            )
        }
    }
}

// ─── Save Success Banner ─────────────────────────────────────────────────────

@Composable
private fun SaveSuccessBanner(
    event: UiEvent.ImageSaved?,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 88.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = event != null,
            enter = slideInVertically(
                animationSpec = tween(340, easing = FastOutSlowInEasing)
            ) { it / 2 } + fadeIn(animationSpec = tween(280)),
            exit = slideOutVertically(
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) { it / 2 } + fadeOut(animationSpec = tween(200))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LiquidColors.SurfaceMedium.copy(alpha = 0.95f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2ECC71).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_save),
                        contentDescription = null,
                        tint = Color(0xFF2ECC71),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event?.filename ?: "",
                        color = LiquidColors.TextHighEmphasis,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = event?.let { "${it.width}×${it.height}" } ?: "",
                        color = LiquidColors.TextLowEmphasis,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}
