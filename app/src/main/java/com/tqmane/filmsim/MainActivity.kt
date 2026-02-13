package com.tqmane.filmsim

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tqmane.filmsim.data.LutRepository
import com.tqmane.filmsim.gl.FilmSimRenderer
import com.tqmane.filmsim.ui.BrandAdapter
import com.tqmane.filmsim.ui.GenreAdapter
import com.tqmane.filmsim.ui.GlTouchHandler
import com.tqmane.filmsim.ui.LutAdapter
import com.tqmane.filmsim.ui.MainViewModel
import com.tqmane.filmsim.ui.UiEvent
import com.tqmane.filmsim.ui.ViewState
import com.tqmane.filmsim.util.ReleaseInfo
import com.tqmane.filmsim.util.WatermarkProcessor.WatermarkStyle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private lateinit var renderer: FilmSimRenderer
    private lateinit var glSurfaceView: android.opengl.GLSurfaceView
    private lateinit var lutAdapter: LutAdapter
    private lateinit var touchHandler: GlTouchHandler

    private lateinit var placeholderContainer: LinearLayout
    private lateinit var sliderPanel: LinearLayout
    private lateinit var adjustmentHeader: LinearLayout
    private lateinit var adjustmentToggle: ImageView
    private lateinit var topBar: View
    private lateinit var controlPanel: LinearLayout
    private lateinit var quickIntensityPanel: LinearLayout
    private lateinit var quickIntensitySlider: SeekBar
    private lateinit var quickIntensityValue: TextView
    private lateinit var watermarkPreview: ImageView
    private lateinit var watermarkTimeInput: EditText
    private lateinit var watermarkLocationInput: EditText
    private lateinit var watermarkDeviceInput: EditText
    private lateinit var watermarkLensInput: EditText
    private lateinit var watermarkTimeContainer: LinearLayout
    private lateinit var watermarkLocationContainer: LinearLayout
    private lateinit var watermarkDeviceContainer: LinearLayout
    private lateinit var watermarkLensContainer: LinearLayout

    private var isImmersive = false
    private var isShowingOriginal = false
    private var panelExpanded = true

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.loadImage(it) }
    }

    // ─── Lifecycle ──────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        setupGL()
        setupControls()
        setupLutList()
        setupWindowInsets()
        createDefaultThumbnail()
        observeState()
        vm.checkForUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::lutAdapter.isInitialized) lutAdapter.clearCache()
    }

    // ─── View binding ───────────────────────────────────

    private fun bindViews() {
        glSurfaceView = findViewById(R.id.glSurfaceView)
        placeholderContainer = findViewById(R.id.placeholderContainer)
        sliderPanel = findViewById(R.id.sliderPanel)
        adjustmentHeader = findViewById(R.id.adjustmentHeader)
        adjustmentToggle = findViewById(R.id.adjustmentToggle)
        topBar = findViewById(R.id.topBar)
        controlPanel = findViewById(R.id.controlPanel)
        quickIntensityPanel = findViewById(R.id.quickIntensityPanel)
        quickIntensitySlider = findViewById(R.id.quickIntensitySlider)
        quickIntensityValue = findViewById(R.id.quickIntensityValue)
        watermarkPreview = findViewById(R.id.watermarkPreview)
        watermarkPreview.pivotX = 0f; watermarkPreview.pivotY = 0f
        watermarkTimeInput = findViewById(R.id.watermarkTimeInput)
        watermarkLocationInput = findViewById(R.id.watermarkLocationInput)
        watermarkDeviceInput = findViewById(R.id.watermarkDeviceInput)
        watermarkLensInput = findViewById(R.id.watermarkLensInput)
        watermarkTimeContainer = findViewById(R.id.watermarkTimeContainer)
        watermarkLocationContainer = findViewById(R.id.watermarkLocationContainer)
        watermarkDeviceContainer = findViewById(R.id.watermarkDeviceContainer)
        watermarkLensContainer = findViewById(R.id.watermarkLensContainer)
    }

    // ─── GL ─────────────────────────────────────────────

    private fun setupGL() {
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.preserveEGLContextOnPause = true
        renderer = FilmSimRenderer(this)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
        val state = vm.viewState.value
        touchHandler = GlTouchHandler(glSurfaceView, renderer,
            onSingleTap = { if (state is ViewState.Ready) toggleImmersiveMode() },
            onLongPressStart = { if (vm.editState.value.hasSelectedLut) showOriginal() },
            onLongPressEnd = { restoreEdited() }
        )
        touchHandler.install()
    }

    // ─── Controls ───────────────────────────────────────

    private fun setupControls() {
        placeholderContainer.setOnClickListener { launchPicker() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPickImage).apply {
            setOnClickListener { launchPicker() }
            rippleColor = ColorStateList.valueOf(getColor(R.color.accent_primary))
        }
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener { showSettingsDialog() }
        findViewById<ImageButton>(R.id.btnChangePhoto)?.setOnClickListener { launchPicker() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave).apply {
            rippleColor = ColorStateList.valueOf(getColor(R.color.accent_primary))
            setOnClickListener {
                if (vm.viewState.value !is ViewState.Ready) {
                    Toast.makeText(this@MainActivity, getString(R.string.select_image_first), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.exporting), Toast.LENGTH_SHORT).show()
                    vm.saveHighResImage(glSurfaceView) {
                        Toast.makeText(this@MainActivity, getString(R.string.cpu_processing), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        adjustmentHeader.setOnClickListener { toggleAdjustmentPanel() }

        // Intensity slider
        quickIntensitySlider.progress = (vm.settings.lastIntensity * 100).toInt().coerceIn(0, 100)
        quickIntensityValue.text = "${quickIntensitySlider.progress}%"
        quickIntensitySlider.setOnSeekBarChangeListener(seekBarChanged { p ->
            vm.setIntensity(p / 100f)
            quickIntensityValue.text = "${p}%"
            glSurfaceView.queueEvent { renderer.setIntensity(p / 100f); glSurfaceView.requestRender() }
            refreshWatermarkPreview()
        })

        setupGrainControls()
        setupWatermarkControls()
    }

    private fun setupGrainControls() {
        val toggle = findViewById<android.widget.CheckBox>(R.id.grainToggle)
        val slider = findViewById<SeekBar>(R.id.grainSlider)
        val value = findViewById<TextView>(R.id.grainValue)
        val icon = findViewById<ImageView>(R.id.grainIcon)
        val styleContainer = findViewById<LinearLayout>(R.id.grainStyleContainer)
        val chipGroup = findViewById<com.google.android.material.chip.ChipGroup>(R.id.grainStyleChipGroup)
        val accent = getColor(R.color.accent_primary)
        val disabled = getColor(R.color.text_tertiary)

        slider.progress = (vm.settings.lastGrainIntensity * 100).toInt().coerceIn(0, 100)
        value.text = "${slider.progress}%"
        toggle.isChecked = vm.settings.lastGrainEnabled
        slider.isEnabled = vm.settings.lastGrainEnabled
        styleContainer.visibility = if (vm.settings.lastGrainEnabled) View.VISIBLE else View.GONE

        when (vm.settings.lastGrainStyle) {
            "OnePlus" -> findViewById<com.google.android.material.chip.Chip>(R.id.chipOnePlus).isChecked = true
            else -> findViewById<com.google.android.material.chip.Chip>(R.id.chipXiaomi).isChecked = true
        }
        glSurfaceView.queueEvent { renderer.setGrainStyle(vm.settings.lastGrainStyle) }

        slider.setOnSeekBarChangeListener(seekBarChanged { p ->
            value.text = "${p}%"; vm.setGrainIntensity(p / 100f)
            if (toggle.isChecked) glSurfaceView.queueEvent { renderer.setGrainIntensity(p / 100f); glSurfaceView.requestRender() }
        })
        toggle.setOnCheckedChangeListener { _, on ->
            vm.setGrainEnabled(on); slider.isEnabled = on
            styleContainer.visibility = if (on) View.VISIBLE else View.GONE
            val c = if (on) accent else disabled
            value.setTextColor(c); icon.imageTintList = ColorStateList.valueOf(c)
            slider.progressTintList = ColorStateList.valueOf(c); slider.thumbTintList = ColorStateList.valueOf(c)
            glSurfaceView.queueEvent { renderer.setGrainEnabled(on); if (on) renderer.setGrainIntensity(slider.progress / 100f); glSurfaceView.requestRender() }
        }
        chipGroup.setOnCheckedStateChangeListener { _, ids ->
            if (ids.isNotEmpty()) {
                val s = if (ids[0] == R.id.chipOnePlus) "OnePlus" else "Xiaomi"
                vm.setGrainStyle(s); glSurfaceView.queueEvent { renderer.setGrainStyle(s); glSurfaceView.requestRender() }
            }
        }
    }

    private fun setupWatermarkControls() {
        com.tqmane.filmsim.util.WatermarkProcessor.getDefaultTimeString().let { watermarkTimeInput.setText(it) }
        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                vm.updateWatermarkFields(
                    deviceName = watermarkDeviceInput.text.toString(),
                    timeText = watermarkTimeInput.text.toString(),
                    locationText = watermarkLocationInput.text.toString(),
                    lensInfo = watermarkLensInput.text.toString()
                )
                refreshWatermarkPreview()
            }
        }
        listOf(watermarkTimeInput, watermarkLocationInput, watermarkDeviceInput, watermarkLensInput).forEach { it.addTextChangedListener(watcher) }

        val styleRow = findViewById<LinearLayout>(R.id.watermarkStyleRow)
        val styleChipGroup = findViewById<com.google.android.material.chip.ChipGroup>(R.id.watermarkStyleChipGroup)
        val brandChipGroup = findViewById<com.google.android.material.chip.ChipGroup>(R.id.watermarkBrandChipGroup)
        val honorIds = listOf(R.id.chipWatermarkFrame, R.id.chipWatermarkText, R.id.chipWatermarkFrameYG, R.id.chipWatermarkTextYG)
        val meizuIds = listOf(R.id.chipMeizuNorm, R.id.chipMeizuPro, R.id.chipMeizuZ1, R.id.chipMeizuZ2,
            R.id.chipMeizuZ3, R.id.chipMeizuZ4, R.id.chipMeizuZ5, R.id.chipMeizuZ6, R.id.chipMeizuZ7)
        val vivoIds = listOf(R.id.chipVivoZeiss, R.id.chipVivoClassic, R.id.chipVivoPro, R.id.chipVivoIqoo,
            R.id.chipVivoZeissV1, R.id.chipVivoZeissSonnar, R.id.chipVivoZeissHumanity,
            R.id.chipVivoIqooV1, R.id.chipVivoIqooHumanity,
            R.id.chipVivoZeissFrame, R.id.chipVivoZeissOverlay, R.id.chipVivoZeissCenter,
            R.id.chipVivoFrame, R.id.chipVivoFrameTime, R.id.chipVivoIqooFrame, R.id.chipVivoIqooFrameTime,
            R.id.chipVivoOS, R.id.chipVivoOSCorner, R.id.chipVivoOSSimple, R.id.chipVivoEvent)
        val allIds = honorIds + meizuIds + vivoIds
        findViewById<com.google.android.material.chip.Chip>(R.id.chipBrandNone).isChecked = true

        fun showBrandChips(ids: List<Int>) {
            allIds.forEach { findViewById<com.google.android.material.chip.Chip>(it).visibility = if (it in ids) View.VISIBLE else View.GONE }
            styleChipGroup.clearCheck()
            if (ids.isNotEmpty()) findViewById<com.google.android.material.chip.Chip>(ids[0]).isChecked = true
        }

        brandChipGroup.setOnCheckedStateChangeListener { _, ids ->
            if (ids.isEmpty()) return@setOnCheckedStateChangeListener
            when (ids[0]) {
                R.id.chipBrandNone -> {
                    styleRow.visibility = View.GONE; vm.updateWatermarkStyle(WatermarkStyle.NONE)
                    listOf(watermarkDeviceContainer, watermarkTimeContainer, watermarkLocationContainer, watermarkLensContainer).forEach { it.visibility = View.GONE }
                    refreshWatermarkPreview()
                }
                R.id.chipBrandHonor -> { styleRow.visibility = View.VISIBLE; showBrandChips(honorIds) }
                R.id.chipBrandMeizu -> { styleRow.visibility = View.VISIBLE; showBrandChips(meizuIds) }
                R.id.chipBrandVivo -> { styleRow.visibility = View.VISIBLE; showBrandChips(vivoIds) }
            }
        }

        styleChipGroup.setOnCheckedStateChangeListener { _, ids ->
            if (ids.isEmpty()) return@setOnCheckedStateChangeListener
            val style = CHIP_TO_STYLE[ids[0]] ?: WatermarkStyle.NONE
            vm.updateWatermarkStyle(style)
            val isYG = style == WatermarkStyle.FRAME_YG || style == WatermarkStyle.TEXT_YG
            val noDevice = style in NO_DEVICE_STYLES
            val noLens = isYG || style in NO_LENS_STYLES
            val noTime = isYG || style in NO_TIME_STYLES
            val show = style != WatermarkStyle.NONE
            watermarkDeviceContainer.visibility = if (show && !noDevice) View.VISIBLE else View.GONE
            watermarkLensContainer.visibility = if (show && !noLens) View.VISIBLE else View.GONE
            watermarkTimeContainer.visibility = if (show && !noTime) View.VISIBLE else View.GONE
            watermarkLocationContainer.visibility = if (show && !noTime) View.VISIBLE else View.GONE
            refreshWatermarkPreview()
        }
    }

    // ─── LUT list ───────────────────────────────────────

    private fun setupLutList() {
        val brandList = findViewById<RecyclerView>(R.id.brandList)
        val genreList = findViewById<RecyclerView>(R.id.genreList)
        val lutListView = findViewById<RecyclerView>(R.id.lutList)
        brandList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        genreList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        lutListView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val brands = vm.brands
        lutAdapter = LutAdapter(emptyList(), this) { lutItem ->
            vm.applyLut(lutItem)
            showFirstLutUI()
        }
        lutListView.adapter = lutAdapter

        val genreAdapter = GenreAdapter(emptyList()) { cat -> lutAdapter.updateItems(cat.items) }
        genreList.adapter = genreAdapter

        val brandAdapter = BrandAdapter(brands) { brand ->
            vm.updateWatermarkBrand(brand.name)
            genreAdapter.updateCategories(brand.categories)
        }
        brandList.adapter = brandAdapter
        if (brands.isNotEmpty()) brandAdapter.selectFirst()
    }

    // ─── State observation ──────────────────────────────

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.viewState.collect { onViewState(it) } }
                launch { vm.editState.collect { onEditState(it) } }
                launch { vm.uiEvent.collect { onUiEvent(it) } }
            }
        }
    }

    private fun onViewState(state: ViewState) {
        when (state) {
            is ViewState.Empty -> placeholderContainer.visibility = View.VISIBLE
            is ViewState.Loading -> { /* could show spinner */ }
            is ViewState.Ready -> {
                placeholderContainer.visibility = View.GONE
                touchHandler.resetZoom()
                controlPanel.post { touchHandler.applyVerticalOffset(topBar.height.toFloat(), controlPanel.height.toFloat()) }
                glSurfaceView.queueEvent { renderer.setImage(state.previewBitmap); glSurfaceView.requestRender() }
                lutAdapter.setSourceBitmap(state.thumbnailBitmap)
            }
            is ViewState.Error -> Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onEditState(edit: com.tqmane.filmsim.ui.EditState) {
        edit.currentLut?.let { lut ->
            glSurfaceView.queueEvent {
                renderer.setIntensity(edit.intensity)
                renderer.setLut(lut)
                glSurfaceView.requestRender()
            }
            if (quickIntensityPanel.visibility != View.VISIBLE) animateIn(quickIntensityPanel)
            refreshWatermarkPreview()
        }
    }

    private fun onUiEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ShowToast -> Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            is UiEvent.ShowUpdateDialog -> showUpdateDialog(event.release)
            is UiEvent.ImageSaved -> Toast.makeText(this, getString(R.string.image_saved, "${event.width}x${event.height}", event.path, event.filename), Toast.LENGTH_LONG).show()
        }
    }

    // ─── Helpers ────────────────────────────────────────

    private fun launchPicker() = pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    private fun refreshWatermarkPreview() {
        val wmState = vm.watermarkState.value
        if (wmState.style == WatermarkStyle.NONE) { watermarkPreview.visibility = View.GONE; return }
        vm.renderWatermarkPreview { bmp ->
            watermarkPreview.setImageBitmap(bmp); watermarkPreview.visibility = View.VISIBLE
            touchHandler.syncWatermarkPreview(watermarkPreview)
        }
    }

    private fun showFirstLutUI() {
        if (vm.editState.value.hasSelectedLut) return
        val dur = resources.getInteger(R.integer.animation_duration_default).toLong()
        val interp = OvershootInterpolator(0.8f)
        quickIntensitySlider.progress = 100; quickIntensityValue.text = "100%"; vm.setIntensity(1f)
        listOf(adjustmentHeader, sliderPanel).forEach { v ->
            v.visibility = View.VISIBLE; v.alpha = 0f; v.scaleX = 0.95f; v.scaleY = 0.95f
            v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(dur).setInterpolator(interp).start()
        }
        adjustmentToggle.rotation = 0f; panelExpanded = true
    }

    private fun toggleAdjustmentPanel() {
        panelExpanded = !panelExpanded
        val dur = resources.getInteger(R.integer.animation_duration_default).toLong()
        if (panelExpanded) {
            sliderPanel.visibility = View.VISIBLE; sliderPanel.alpha = 0f
            sliderPanel.animate().alpha(1f).setDuration(dur).start()
            adjustmentToggle.animate().rotation(0f).setDuration(dur).start()
        } else {
            sliderPanel.animate().alpha(0f).setDuration(dur).withEndAction { sliderPanel.visibility = View.GONE }.start()
            adjustmentToggle.animate().rotation(180f).setDuration(dur).start()
        }
    }

    private fun toggleImmersiveMode() {
        isImmersive = !isImmersive
        val dur = resources.getInteger(R.integer.animation_duration_immersive).toLong()
        if (isImmersive) {
            topBar.animate().alpha(0f).translationY(-topBar.height.toFloat()).setDuration(dur).withEndAction { topBar.visibility = View.INVISIBLE }.start()
            controlPanel.animate().alpha(0f).translationY(controlPanel.height.toFloat()).setDuration(dur).withEndAction { controlPanel.visibility = View.INVISIBLE }.start()
        } else {
            topBar.visibility = View.VISIBLE; controlPanel.visibility = View.VISIBLE
            topBar.animate().alpha(1f).translationY(0f).setDuration(dur).start()
            controlPanel.animate().alpha(1f).translationY(0f).setDuration(dur).start()
        }
    }

    private fun showOriginal() {
        if (isShowingOriginal) return; isShowingOriginal = true
        glSurfaceView.queueEvent { renderer.setIntensity(0f); renderer.setGrainEnabled(false); glSurfaceView.requestRender() }
    }

    private fun restoreEdited() {
        if (!isShowingOriginal) return; isShowingOriginal = false
        val e = vm.editState.value
        glSurfaceView.queueEvent { renderer.setIntensity(e.intensity); renderer.setGrainEnabled(e.grainEnabled); if (e.grainEnabled) renderer.setGrainIntensity(e.grainIntensity); glSurfaceView.requestRender() }
    }

    private fun animateIn(v: View) {
        val dur = resources.getInteger(R.integer.animation_duration_default).toLong()
        v.visibility = View.VISIBLE; v.alpha = 0f; v.scaleX = 0.95f; v.scaleY = 0.95f
        v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(dur).setInterpolator(OvershootInterpolator(0.8f)).start()
    }

    private fun createDefaultThumbnail() {
        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply {
            val p = Paint()
            p.color = Color.RED; drawRect(0f, 0f, 50f, 50f, p)
            p.color = Color.GREEN; drawRect(50f, 0f, 100f, 50f, p)
            p.color = Color.BLUE; drawRect(0f, 50f, 50f, 100f, p)
            p.color = Color.YELLOW; drawRect(50f, 50f, 100f, 100f, p)
        }
        lutAdapter.setSourceBitmap(bmp)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout)) { _, wi ->
            controlPanel.updatePadding(bottom = wi.getInsets(WindowInsetsCompat.Type.systemBars()).bottom); wi
        }
    }

    // ─── Dialogs ────────────────────────────────────────

    private fun showSettingsDialog() {
        val dialog = Dialog(this, R.style.Theme_FilmSims).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE); setContentView(R.layout.dialog_settings)
            window?.setLayout((resources.displayMetrics.widthPixels * 0.90).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        val tvPath = dialog.findViewById<TextView>(R.id.tvSavePath).apply { text = vm.settings.savePath }
        val tvQuality = dialog.findViewById<TextView>(R.id.tvQualityValue).apply { text = "${vm.settings.saveQuality}%" }
        dialog.findViewById<TextView>(R.id.btnChangePath).setOnClickListener { showPathDialog(tvPath) }
        dialog.findViewById<SeekBar>(R.id.qualitySlider).apply {
            progress = vm.settings.saveQuality
            setOnSeekBarChangeListener(seekBarChanged { p -> val q = p.coerceAtLeast(10); tvQuality.text = "${q}%"; vm.settings.saveQuality = q })
        }
        dialog.findViewById<Button>(R.id.btnCloseSettings).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showPathDialog(tvPath: TextView) {
        val et = EditText(this).apply { setText(vm.settings.savePath); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); hint = getString(R.string.save_path_hint); setPadding(48, 32, 48, 32) }
        AlertDialog.Builder(this, R.style.Theme_FilmSims).setTitle(getString(R.string.enter_save_folder)).setView(et)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val p = et.text.toString().trim()
                if (p.isNotEmpty()) { vm.settings.savePath = p; tvPath.text = p; Toast.makeText(this, getString(R.string.save_path_changed, p), Toast.LENGTH_SHORT).show() }
            }.setNegativeButton(getString(R.string.cancel), null).show()
    }

    private fun showUpdateDialog(release: ReleaseInfo) {
        val dialog = Dialog(this, R.style.Theme_FilmSims).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE); setContentView(R.layout.dialog_update)
            window?.setLayout((resources.displayMetrics.widthPixels * 0.90).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.findViewById<TextView>(R.id.tvVersionInfo).text = getString(R.string.new_version_available, release.version)
        dialog.findViewById<TextView>(R.id.tvReleaseNotes).apply { if (release.releaseNotes.isNotBlank()) { visibility = View.VISIBLE; text = release.releaseNotes } }
        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLater).setOnClickListener {
            com.tqmane.filmsim.util.UpdateChecker.skipVersion(this@MainActivity, release.version); dialog.dismiss()
        }
        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUpdate).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))); dialog.dismiss()
        }
        dialog.show()
    }

    // ─── Utility ────────────────────────────────────────

    private fun seekBarChanged(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) = onChange(progress)
        override fun onStartTrackingTouch(sb: SeekBar?) {}
        override fun onStopTrackingTouch(sb: SeekBar?) {}
    }

    companion object {
        private val CHIP_TO_STYLE = mapOf(
            R.id.chipWatermarkFrame to WatermarkStyle.FRAME, R.id.chipWatermarkText to WatermarkStyle.TEXT,
            R.id.chipWatermarkFrameYG to WatermarkStyle.FRAME_YG, R.id.chipWatermarkTextYG to WatermarkStyle.TEXT_YG,
            R.id.chipMeizuNorm to WatermarkStyle.MEIZU_NORM, R.id.chipMeizuPro to WatermarkStyle.MEIZU_PRO,
            R.id.chipMeizuZ1 to WatermarkStyle.MEIZU_Z1, R.id.chipMeizuZ2 to WatermarkStyle.MEIZU_Z2,
            R.id.chipMeizuZ3 to WatermarkStyle.MEIZU_Z3, R.id.chipMeizuZ4 to WatermarkStyle.MEIZU_Z4,
            R.id.chipMeizuZ5 to WatermarkStyle.MEIZU_Z5, R.id.chipMeizuZ6 to WatermarkStyle.MEIZU_Z6,
            R.id.chipMeizuZ7 to WatermarkStyle.MEIZU_Z7,
            R.id.chipVivoZeiss to WatermarkStyle.VIVO_ZEISS, R.id.chipVivoClassic to WatermarkStyle.VIVO_CLASSIC,
            R.id.chipVivoPro to WatermarkStyle.VIVO_PRO, R.id.chipVivoIqoo to WatermarkStyle.VIVO_IQOO,
            R.id.chipVivoZeissV1 to WatermarkStyle.VIVO_ZEISS_V1, R.id.chipVivoZeissSonnar to WatermarkStyle.VIVO_ZEISS_SONNAR,
            R.id.chipVivoZeissHumanity to WatermarkStyle.VIVO_ZEISS_HUMANITY,
            R.id.chipVivoIqooV1 to WatermarkStyle.VIVO_IQOO_V1, R.id.chipVivoIqooHumanity to WatermarkStyle.VIVO_IQOO_HUMANITY,
            R.id.chipVivoZeissFrame to WatermarkStyle.VIVO_ZEISS_FRAME, R.id.chipVivoZeissOverlay to WatermarkStyle.VIVO_ZEISS_OVERLAY,
            R.id.chipVivoZeissCenter to WatermarkStyle.VIVO_ZEISS_CENTER,
            R.id.chipVivoFrame to WatermarkStyle.VIVO_FRAME, R.id.chipVivoFrameTime to WatermarkStyle.VIVO_FRAME_TIME,
            R.id.chipVivoIqooFrame to WatermarkStyle.VIVO_IQOO_FRAME, R.id.chipVivoIqooFrameTime to WatermarkStyle.VIVO_IQOO_FRAME_TIME,
            R.id.chipVivoOS to WatermarkStyle.VIVO_OS, R.id.chipVivoOSCorner to WatermarkStyle.VIVO_OS_CORNER,
            R.id.chipVivoOSSimple to WatermarkStyle.VIVO_OS_SIMPLE, R.id.chipVivoEvent to WatermarkStyle.VIVO_EVENT
        )
        private val NO_DEVICE_STYLES = setOf(WatermarkStyle.MEIZU_Z6, WatermarkStyle.MEIZU_Z7, WatermarkStyle.VIVO_OS_CORNER, WatermarkStyle.VIVO_OS_SIMPLE)
        private val NO_LENS_STYLES = setOf(WatermarkStyle.VIVO_CLASSIC, WatermarkStyle.VIVO_ZEISS_HUMANITY, WatermarkStyle.VIVO_IQOO_HUMANITY, WatermarkStyle.VIVO_FRAME, WatermarkStyle.VIVO_IQOO_FRAME, WatermarkStyle.VIVO_OS_CORNER, WatermarkStyle.VIVO_OS_SIMPLE)
        private val NO_TIME_STYLES = setOf(WatermarkStyle.VIVO_ZEISS_HUMANITY, WatermarkStyle.VIVO_IQOO_HUMANITY, WatermarkStyle.VIVO_FRAME, WatermarkStyle.VIVO_IQOO_FRAME, WatermarkStyle.VIVO_OS_CORNER, WatermarkStyle.VIVO_OS_SIMPLE)
    }
}
