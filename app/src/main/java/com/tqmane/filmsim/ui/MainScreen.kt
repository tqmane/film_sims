package com.tqmane.filmsim.ui

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tqmane.filmsim.R
import com.tqmane.filmsim.data.LutBrand
import com.tqmane.filmsim.data.LutItem
import com.tqmane.filmsim.gl.FilmSimRenderer
import com.tqmane.filmsim.gl.GlCommandExecutor
import com.tqmane.filmsim.gl.GlSurfaceViewExecutor
import com.tqmane.filmsim.gl.GpuExportRenderer
import com.tqmane.filmsim.ui.theme.AccentDark
import com.tqmane.filmsim.ui.theme.AccentPrimary
import com.tqmane.filmsim.ui.theme.AccentSecondary
import com.tqmane.filmsim.ui.theme.TextPrimary
import com.tqmane.filmsim.ui.theme.TextSecondary
import com.tqmane.filmsim.ui.theme.TextTertiary
import com.tqmane.filmsim.util.WatermarkProcessor
import com.tqmane.filmsim.util.WatermarkProcessor.WatermarkStyle

// ─── Colors matching the XML resources faithfully ───────

// bg_liquid_glass_panel base
private val PanelBg = Color(0xDD111114)
// bg_liquid_glass_top_bar gradient
private val TopBarGradientStart = Color(0xE8111214)
private val TopBarGradientCenter = Color(0xE50D0D10)
private val TopBarGradientEnd = Color.Transparent
// bg_accent_gradient_button
private val AccentGradientStart = Color(0xFFFF8A50)  // accent_start
private val AccentGradientEnd   = Color(0xFFFFC06A)  // accent_end
// bg_round_button_static
private val RoundButtonBg     = Color(0x30FFFFFF)
private val RoundButtonBorder = Color(0x20FFFFFF)
// chip_glow_bg selector
private val ChipBgUnselected     = Color(0x30FFFFFF)
private val ChipBgSelected       = AccentPrimary        // FFAB60
// chip_glow_stroke selector
private val ChipStrokeUnselected = Color(0x18FFFFFF)
private val ChipStrokeSelected   = AccentDark            // E07830
// simple_chip_text selector
private val ChipTextUnselected   = Color(0xDDFFFFFF)
private val ChipTextSelected     = Color(0xFF121212)
// bg_liquid_glass_input
private val InputBg     = Color(0x15FFFFFF)
private val InputBorder = Color(0x25FFFFFF)
// divider
private val DividerColor = Color(0x15FFFFFF)
// bg_glass_panel (placeholder icon box)
private val GlassPanelBg = Color(0x35FFFFFF)
// root background
private val RootBg = Color(0xFF050508)

// ─── Main Screen ────────────────────────────────────────

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPickImage: () -> Unit,
    onShowSettings: () -> Unit,
    onShowUpdateDialog: (com.tqmane.filmsim.util.ReleaseInfo) -> Unit
) {
    val context = LocalContext.current
    val viewState by viewModel.viewState.collectAsState()
    val editState by viewModel.editState.collectAsState()
    val watermarkState by viewModel.watermarkState.collectAsState()

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
        if (watermarkState.style == WatermarkStyle.NONE) {
            watermarkPreviewBitmap = null
            return
        }
        viewModel.renderWatermarkPreview { bmp -> watermarkPreviewBitmap = bmp }
    }

    // Sync edit state to GL when LUT changes
    LaunchedEffect(editState.currentLut) {
        val r = renderer ?: return@LaunchedEffect
        val gl = glSurfaceView ?: return@LaunchedEffect
        editState.currentLut?.let { lut ->
            gl.queueEvent {
                r.setIntensity(editState.intensity)
                r.setLut(lut)
                gl.requestRender()
            }
            refreshWatermarkPreview()
        }
    }

    // Load image into GL when Content state arrives
    LaunchedEffect(viewState) {
        val state = viewState
        if (state is ViewState.Content) {
            val r = renderer ?: return@LaunchedEffect
            val gl = glSurfaceView ?: return@LaunchedEffect
            touchHandler?.resetZoom()
            gl.queueEvent {
                r.setImage(state.previewBitmap)
                gl.requestRender()
            }
        }
    }

    // ─── Root FrameLayout ───────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(RootBg)) {

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
                            if (editState.hasSelectedLut) {
                                queueEvent {
                                    r.setIntensity(0f); r.setGrainEnabled(false)
                                    requestRender()
                                }
                            }
                        },
                        onLongPressEnd = {
                            queueEvent {
                                r.setIntensity(editState.intensity)
                                r.setGrainEnabled(editState.grainEnabled)
                                if (editState.grainEnabled) r.setGrainIntensity(editState.grainIntensity)
                                requestRender()
                            }
                        }
                    )
                    th.install()
                    touchHandler = th
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Watermark preview overlay
        watermarkPreviewBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ─── Main vertical LinearLayout ─────────────────
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Top Bar ────────────────────────────────
            AnimatedVisibility(
                visible = !isImmersive,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                TopBar(
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

            // ─── Content Area (weight=1) ────────────────
            Box(modifier = Modifier.weight(1f)) {
                if (viewState is ViewState.Empty) {
                    PlaceholderContent(onPickImage = onPickImage)
                }
            }

            // ─── Bottom Control Panel ───────────────────
            AnimatedVisibility(
                visible = !isImmersive,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                ControlPanel(
                    viewModel = viewModel,
                    editState = editState,
                    watermarkState = watermarkState,
                    viewState = viewState,
                    panelExpanded = panelExpanded,
                    onTogglePanel = { panelExpanded = !panelExpanded },
                    glSurfaceView = glSurfaceView,
                    renderer = renderer,
                    onRefreshWatermark = { refreshWatermarkPreview() },
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

// ═══════════════════════════════════════════════════════
// Top Bar
// Matches bg_liquid_glass_top_bar gradient, paddingH=24dp, paddingV=24dp
// ═══════════════════════════════════════════════════════

@Composable
private fun TopBar(
    onPickImage: () -> Unit,
    onSettings: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(TopBarGradientStart, TopBarGradientCenter, TopBarGradientEnd)
                )
            )
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App title + subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.app_name),
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.005.sp
                )
                Text(
                    stringResource(R.string.subtitle_film_simulator).uppercase(),
                    color = AccentPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.1.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            // Change-photo round button 42dp
            RoundGlassButton(
                iconRes = R.drawable.ic_add,
                contentDesc = stringResource(R.string.btn_open_gallery),
                onClick = onPickImage,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Settings round button 42dp
            RoundGlassButton(
                iconRes = R.drawable.ic_settings,
                contentDesc = stringResource(R.string.title_settings),
                onClick = onSettings,
                modifier = Modifier.padding(end = 12.dp)
            )

            // Save / Export – accent gradient button
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                modifier = Modifier
                    .height(44.dp)
                    .background(
                        Brush.linearGradient(listOf(AccentGradientStart, AccentGradientEnd)),
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Text(
                    stringResource(R.string.save),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

@Composable
private fun RoundGlassButton(
    iconRes: Int,
    contentDesc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(RoundButtonBg)
            .border(1.dp, RoundButtonBorder, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDesc,
            tint = TextPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════
// Placeholder – matches the old centered design
// ═══════════════════════════════════════════════════════

@Composable
private fun PlaceholderContent(onPickImage: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RootBg)
            .clickable { onPickImage() }
            .padding(56.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 100dp glass-panel icon box
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GlassPanelBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add_photo),
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            stringResource(R.string.label_pick_image),
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.005.sp,
            modifier = Modifier.padding(top = 36.dp)
        )

        Text(
            stringResource(R.string.desc_pick_image),
            color = TextTertiary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = (15 * 1.5).sp,
            modifier = Modifier.padding(top = 14.dp)
        )

        // "Open Gallery" accent-gradient button
        Button(
            onClick = onPickImage,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(28.dp),
            contentPadding = PaddingValues(horizontal = 32.dp),
            modifier = Modifier
                .padding(top = 44.dp)
                .height(56.dp)
                .background(
                    Brush.linearGradient(listOf(AccentGradientStart, AccentGradientEnd)),
                    RoundedCornerShape(28.dp)
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gallery),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.btn_open_gallery),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.015.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// Control Panel
// bg_liquid_glass_panel (#DD111114, rounded 20dp top)
// paddingTop=16, paddingBottom=20, paddingH=16
// Order: adjustment header → sliders (grain+watermark) →
//        CAMERA → brands → STYLE → genres → intensity → PRESETS → LUTs
// ═══════════════════════════════════════════════════════

@Composable
private fun ControlPanel(
    viewModel: MainViewModel,
    editState: EditState,
    watermarkState: WatermarkState,
    viewState: ViewState,
    panelExpanded: Boolean,
    onTogglePanel: () -> Unit,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?,
    onRefreshWatermark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(PanelBg)
            .padding(top = 16.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
    ) {
        // ── Adjustment Header ("EFFECTS") ───────────────
        // Visible only after first LUT selection; toggles slider panel
        AnimatedVisibility(
            visible = editState.hasSelectedLut,
            enter = fadeIn() + scaleIn(initialScale = 0.95f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTogglePanel() }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.header_grain).uppercase(),
                    color = AccentPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.15.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_expand_less),
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(if (panelExpanded) 0f else 180f)
                )
            }
        }

        // ── Slider Panel (grain + watermark, collapsible) ──
        AnimatedVisibility(visible = panelExpanded && editState.hasSelectedLut) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                GrainControls(
                    editState = editState,
                    viewModel = viewModel,
                    glSurfaceView = glSurfaceView,
                    renderer = renderer
                )

                // Divider
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(1.dp)
                        .background(DividerColor)
                )

                WatermarkControls(
                    watermarkState = watermarkState,
                    viewModel = viewModel,
                    onRefreshWatermark = onRefreshWatermark
                )
            }
        }

        // ── CAMERA ──────────────────────────────────────
        SectionHeader(stringResource(R.string.header_camera))

        BrandGenreLutSection(
            brands = viewModel.brands,
            viewModel = viewModel,
            viewState = viewState,
            editState = editState,
            glSurfaceView = glSurfaceView,
            renderer = renderer,
            onRefreshWatermark = onRefreshWatermark
        )
    }
}

// ─── Section Header (CAMERA / STYLE / PRESETS) ──────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        color = AccentPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 0.12.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// ═══════════════════════════════════════════════════════
// Brand → Genre → Intensity → LUT
// ═══════════════════════════════════════════════════════

@Composable
private fun BrandGenreLutSection(
    brands: List<LutBrand>,
    viewModel: MainViewModel,
    viewState: ViewState,
    editState: EditState,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?,
    onRefreshWatermark: () -> Unit
) {
    var selectedBrandIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    val categories = remember(selectedBrandIndex) {
        brands.getOrNull(selectedBrandIndex)?.categories.orEmpty()
    }
    val lutItems = remember(selectedBrandIndex, selectedCategoryIndex) {
        categories.getOrNull(selectedCategoryIndex)?.items.orEmpty()
    }

    // Brand chips (horizontal)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        itemsIndexed(brands) { index, brand ->
            GlowChip(
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

    // STYLE header
    SectionHeader(stringResource(R.string.header_style))

    // Genre/category chips (horizontal)
    if (categories.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            itemsIndexed(categories) { index, cat ->
                GlowChip(
                    text = cat.displayName,
                    selected = index == selectedCategoryIndex,
                    onClick = { selectedCategoryIndex = index }
                )
            }
        }
    }

    // Intensity slider (visible after first LUT selection)
    AnimatedVisibility(
        visible = editState.hasSelectedLut,
        enter = fadeIn() + scaleIn(initialScale = 0.95f)
    ) {
        QuickIntensitySlider(
            intensity = editState.intensity,
            onIntensityChange = { value ->
                viewModel.setIntensity(value)
                glSurfaceView?.queueEvent {
                    renderer?.setIntensity(value)
                    glSurfaceView.requestRender()
                }
                onRefreshWatermark()
            }
        )
    }

    // PRESETS header
    SectionHeader(stringResource(R.string.header_presets))

    // LUT card row (120dp height)
    LutRow(
        items = lutItems,
        thumbnailBitmap = (viewState as? ViewState.Content)?.thumbnailBitmap,
        onLutSelected = { viewModel.applyLut(it) }
    )
}

// ═══════════════════════════════════════════════════════
// Glow Chip  (matches chip_glow_bg / chip_glow_stroke / simple_chip_text)
// 32dp height, cornerRadius 16dp, strokeWidth 1.5dp
// ═══════════════════════════════════════════════════════

@Composable
private fun GlowChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val bg = if (selected) ChipBgSelected else ChipBgUnselected
    val stroke = if (selected) ChipStrokeSelected else ChipStrokeUnselected
    val textCol = if (selected) ChipTextSelected else ChipTextUnselected

    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, stroke, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = textCol,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.01.sp
        )
    }
}

// ═══════════════════════════════════════════════════════
// LUT row  (matches item_lut.xml: 80×80dp card + name)
// ═══════════════════════════════════════════════════════

@Composable
private fun LutRow(
    items: List<LutItem>,
    thumbnailBitmap: Bitmap?,
    onLutSelected: (LutItem) -> Unit
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(-1) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(120.dp)
    ) {
        itemsIndexed(items) { index, item ->
            LutCard(
                item = item,
                selected = index == selectedIndex,
                onClick = {
                    selectedIndex = index
                    onLutSelected(item)
                }
            )
        }
    }
}

@Composable
private fun LutCard(
    item: LutItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            .clickable { onClick() }
    ) {
        // 80dp card  (bg #151519, cornerRadius 10dp)
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF151519))
                .then(
                    if (selected)
                        Modifier.border(2.dp, AccentPrimary, RoundedCornerShape(10.dp))
                    else Modifier
                )
        )

        // Name label
        Text(
            item.name,
            color = TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.01.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 11.sp,
            modifier = Modifier
                .width(80.dp)
                .padding(top = 6.dp, bottom = 2.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════
// Quick Intensity Slider
// ic_opacity + "Intensity" + value% + SeekBar
// ═══════════════════════════════════════════════════════

@Composable
private fun QuickIntensitySlider(
    intensity: Float,
    onIntensityChange: (Float) -> Unit
) {
    var sliderValue by remember(intensity) { mutableFloatStateOf(intensity) }

    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_opacity),
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.label_intensity),
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${(sliderValue * 100).toInt()}%",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.End,
                modifier = Modifier.width(40.dp)
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onIntensityChange(sliderValue) },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = AccentPrimary,
                activeTrackColor = AccentPrimary,
                inactiveTrackColor = Color(0x30FFFFFF)
            )
        )
    }
}

// ═══════════════════════════════════════════════════════
// Grain Controls
// Row: ic_grain icon 18dp + "Film Grain" + value% + CheckBox
// SeekBar (below), grain style row (always visible, alpha when off)
// ═══════════════════════════════════════════════════════

@Composable
private fun GrainControls(
    editState: EditState,
    viewModel: MainViewModel,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?
) {
    var grainEnabled by remember { mutableStateOf(editState.grainEnabled) }
    var grainIntensity by remember { mutableFloatStateOf(editState.grainIntensity) }
    var selectedStyle by remember { mutableStateOf(editState.grainStyle) }

    val accent = if (grainEnabled) AccentPrimary else TextTertiary

    // ── Grain row ───────────────────────────────────────
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
            color = TextSecondary,
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
                glSurfaceView?.queueEvent {
                    renderer?.setGrainEnabled(on)
                    if (on) renderer?.setGrainIntensity(grainIntensity)
                    glSurfaceView.requestRender()
                }
            },
            colors = CheckboxDefaults.colors(checkedColor = AccentPrimary),
            modifier = Modifier.size(24.dp)
        )
    }

    // ── Grain SeekBar ───────────────────────────────────
    Slider(
        value = grainIntensity,
        onValueChange = { grainIntensity = it },
        onValueChangeFinished = {
            viewModel.setGrainIntensity(grainIntensity)
            if (grainEnabled) {
                glSurfaceView?.queueEvent {
                    renderer?.setGrainIntensity(grainIntensity)
                    glSurfaceView.requestRender()
                }
            }
        },
        enabled = grainEnabled,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = SliderDefaults.colors(
            thumbColor = accent,
            activeTrackColor = accent,
            inactiveTrackColor = Color(0x25FFFFFF),
            disabledThumbColor = TextTertiary,
            disabledActiveTrackColor = TextTertiary,
            disabledInactiveTrackColor = Color(0x25FFFFFF)
        )
    )

    // ── Grain Style row — always visible, alpha=0.4 when off ──
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (grainEnabled) 1f else 0.4f)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_grain),
            contentDescription = null,
            tint = AccentSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.label_grain_style),
            color = TextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(end = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Xiaomi" to R.string.grain_style_xiaomi, "OnePlus" to R.string.grain_style_oneplus)
                .forEach { (key, labelRes) ->
                    GlowChip(
                        text = stringResource(labelRes),
                        selected = selectedStyle == key,
                        enabled = grainEnabled,
                        onClick = {
                            if (grainEnabled) {
                                selectedStyle = key
                                viewModel.setGrainStyle(key)
                                glSurfaceView?.queueEvent {
                                    renderer?.setGrainStyle(key)
                                    glSurfaceView.requestRender()
                                }
                            }
                        }
                    )
                }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Watermark Controls
// "WATERMARK" 10sp header → brand row (icon + label + chips) →
// style row (label + chips, visible when brand ≠ None) →
// input fields (device / lens / time / location, conditional)
// ═══════════════════════════════════════════════════════

@Composable
private fun WatermarkControls(
    watermarkState: WatermarkState,
    viewModel: MainViewModel,
    onRefreshWatermark: () -> Unit
) {
    val defaultTime = remember { WatermarkProcessor.getDefaultTimeString() }

    var selectedBrand by rememberSaveable { mutableStateOf("None") }
    var selectedStyleChip by rememberSaveable { mutableStateOf<WatermarkStyle?>(null) }

    var deviceName by remember(watermarkState.deviceName) { mutableStateOf(watermarkState.deviceName) }
    var timeText by remember(watermarkState.timeText) {
        mutableStateOf(watermarkState.timeText.ifEmpty { defaultTime })
    }
    var locationText by remember(watermarkState.locationText) { mutableStateOf(watermarkState.locationText) }
    var lensInfo by remember(watermarkState.lensInfo) { mutableStateOf(watermarkState.lensInfo) }

    // Brand → style list
    val honorStyles = listOf(
        R.string.watermark_frame to WatermarkStyle.FRAME,
        R.string.watermark_text to WatermarkStyle.TEXT,
        R.string.watermark_frame_yg to WatermarkStyle.FRAME_YG,
        R.string.watermark_text_yg to WatermarkStyle.TEXT_YG
    )
    val meizuStyles = listOf(
        R.string.meizu_norm to WatermarkStyle.MEIZU_NORM,
        R.string.meizu_pro to WatermarkStyle.MEIZU_PRO,
        R.string.meizu_z1 to WatermarkStyle.MEIZU_Z1,
        R.string.meizu_z2 to WatermarkStyle.MEIZU_Z2,
        R.string.meizu_z3 to WatermarkStyle.MEIZU_Z3,
        R.string.meizu_z4 to WatermarkStyle.MEIZU_Z4,
        R.string.meizu_z5 to WatermarkStyle.MEIZU_Z5,
        R.string.meizu_z6 to WatermarkStyle.MEIZU_Z6,
        R.string.meizu_z7 to WatermarkStyle.MEIZU_Z7
    )
    val vivoStyles = listOf(
        R.string.vivo_zeiss to WatermarkStyle.VIVO_ZEISS,
        R.string.vivo_classic to WatermarkStyle.VIVO_CLASSIC,
        R.string.vivo_pro to WatermarkStyle.VIVO_PRO,
        R.string.vivo_iqoo to WatermarkStyle.VIVO_IQOO,
        R.string.vivo_zeiss_v1 to WatermarkStyle.VIVO_ZEISS_V1,
        R.string.vivo_zeiss_sonnar to WatermarkStyle.VIVO_ZEISS_SONNAR,
        R.string.vivo_zeiss_humanity to WatermarkStyle.VIVO_ZEISS_HUMANITY,
        R.string.vivo_iqoo_v1 to WatermarkStyle.VIVO_IQOO_V1,
        R.string.vivo_iqoo_humanity to WatermarkStyle.VIVO_IQOO_HUMANITY,
        R.string.vivo_zeiss_frame to WatermarkStyle.VIVO_ZEISS_FRAME,
        R.string.vivo_zeiss_overlay to WatermarkStyle.VIVO_ZEISS_OVERLAY,
        R.string.vivo_zeiss_center to WatermarkStyle.VIVO_ZEISS_CENTER,
        R.string.vivo_frame to WatermarkStyle.VIVO_FRAME,
        R.string.vivo_frame_time to WatermarkStyle.VIVO_FRAME_TIME,
        R.string.vivo_iqoo_frame to WatermarkStyle.VIVO_IQOO_FRAME,
        R.string.vivo_iqoo_frame_time to WatermarkStyle.VIVO_IQOO_FRAME_TIME,
        R.string.vivo_os to WatermarkStyle.VIVO_OS,
        R.string.vivo_os_corner to WatermarkStyle.VIVO_OS_CORNER,
        R.string.vivo_os_simple to WatermarkStyle.VIVO_OS_SIMPLE,
        R.string.vivo_event to WatermarkStyle.VIVO_EVENT
    )

    val availableStyles = when (selectedBrand) {
        "Honor" -> honorStyles
        "Meizu" -> meizuStyles
        "Vivo"  -> vivoStyles
        else    -> emptyList()
    }

    val currentStyle = selectedStyleChip ?: WatermarkStyle.NONE

    // Field visibility (matches old companion-object constants)
    val noDeviceStyles = setOf(
        WatermarkStyle.MEIZU_Z6, WatermarkStyle.MEIZU_Z7,
        WatermarkStyle.VIVO_OS_CORNER, WatermarkStyle.VIVO_OS_SIMPLE
    )
    val noLensStyles = setOf(
        WatermarkStyle.FRAME_YG, WatermarkStyle.TEXT_YG,
        WatermarkStyle.VIVO_CLASSIC, WatermarkStyle.VIVO_ZEISS_HUMANITY,
        WatermarkStyle.VIVO_IQOO_HUMANITY, WatermarkStyle.VIVO_FRAME,
        WatermarkStyle.VIVO_IQOO_FRAME, WatermarkStyle.VIVO_OS_CORNER,
        WatermarkStyle.VIVO_OS_SIMPLE
    )
    val noTimeStyles = setOf(
        WatermarkStyle.FRAME_YG, WatermarkStyle.TEXT_YG,
        WatermarkStyle.VIVO_ZEISS_HUMANITY, WatermarkStyle.VIVO_IQOO_HUMANITY,
        WatermarkStyle.VIVO_FRAME, WatermarkStyle.VIVO_IQOO_FRAME,
        WatermarkStyle.VIVO_OS_CORNER, WatermarkStyle.VIVO_OS_SIMPLE
    )
    val showFields = currentStyle != WatermarkStyle.NONE
    val showDevice = showFields && currentStyle !in noDeviceStyles
    val showLens   = showFields && currentStyle !in noLensStyles
    val showTime   = showFields && currentStyle !in noTimeStyles

    Column {
        // "WATERMARK" sub-header (10sp)
        Text(
            stringResource(R.string.header_watermark).uppercase(),
            color = AccentPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.15.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // ── Brand row: icon + "Brand" + scrollable chips ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_watermark),
                contentDescription = null,
                tint = AccentSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.label_watermark_brand),
                color = TextSecondary,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(end = 12.dp)
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    R.string.brand_none to "None",
                    R.string.brand_honor to "Honor",
                    R.string.brand_meizu to "Meizu",
                    R.string.brand_vivo to "Vivo"
                ).forEach { (labelRes, brand) ->
                    GlowChip(
                        text = stringResource(labelRes),
                        selected = selectedBrand == brand,
                        onClick = {
                            selectedBrand = brand
                            if (brand == "None") {
                                selectedStyleChip = null
                                viewModel.updateWatermarkStyle(WatermarkStyle.NONE)
                            } else {
                                val first = when (brand) {
                                    "Honor" -> honorStyles
                                    "Meizu" -> meizuStyles
                                    "Vivo"  -> vivoStyles
                                    else -> emptyList()
                                }.firstOrNull()
                                selectedStyleChip = first?.second
                                viewModel.updateWatermarkStyle(first?.second ?: WatermarkStyle.NONE)
                            }
                            onRefreshWatermark()
                        }
                    )
                }
            }
        }

        // ── Style row (visible when brand ≠ None) ───────
        if (availableStyles.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.label_watermark_style),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableStyles.forEach { (labelRes, style) ->
                        GlowChip(
                            text = stringResource(labelRes),
                            selected = selectedStyleChip == style,
                            onClick = {
                                selectedStyleChip = style
                                viewModel.updateWatermarkStyle(style)
                                onRefreshWatermark()
                            }
                        )
                    }
                }
            }
        }

        // ── Input fields ────────────────────────────────
        if (showDevice) {
            WatermarkInputRow(
                label = stringResource(R.string.label_watermark_device),
                value = deviceName,
                onValueChange = {
                    deviceName = it
                    viewModel.updateWatermarkFields(deviceName = it)
                    onRefreshWatermark()
                }
            )
        }
        if (showLens) {
            WatermarkInputRow(
                label = stringResource(R.string.label_watermark_lens),
                value = lensInfo,
                onValueChange = {
                    lensInfo = it
                    viewModel.updateWatermarkFields(lensInfo = it)
                    onRefreshWatermark()
                }
            )
        }
        if (showTime) {
            WatermarkInputRow(
                label = stringResource(R.string.label_watermark_time),
                value = timeText,
                onValueChange = {
                    timeText = it
                    viewModel.updateWatermarkFields(timeText = it)
                    onRefreshWatermark()
                }
            )
            WatermarkInputRow(
                label = stringResource(R.string.label_watermark_location),
                value = locationText,
                onValueChange = {
                    locationText = it
                    viewModel.updateWatermarkFields(locationText = it)
                    onRefreshWatermark()
                }
            )
        }
    }
}

// ─── Watermark text‐input row ───────────────────────────
// paddingStart=30dp, label width 56dp, EditText height 34dp,
// bg_liquid_glass_input (rounded 12dp, stroke 1dp)

@Composable
private fun WatermarkInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 30.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = TextTertiary,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.width(56.dp).padding(end = 8.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(InputBg)
                .border(1.dp, InputBorder, RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentPrimary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
        )
    }
}
