package com.tqmane.filmsim.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tqmane.filmsim.R
import com.tqmane.filmsim.data.LutBrand
import com.tqmane.filmsim.data.LutItem
import com.tqmane.filmsim.di.DefaultDispatcher
import com.tqmane.filmsim.di.IoDispatcher
import com.tqmane.filmsim.domain.ImageLoadUseCase
import com.tqmane.filmsim.domain.LutApplyUseCase
import com.tqmane.filmsim.domain.WatermarkUseCase
import com.tqmane.filmsim.core.security.SecurityCheckUseCase
import com.tqmane.filmsim.gl.GlCommandExecutor
import com.tqmane.filmsim.gl.GpuExportRenderer
import com.tqmane.filmsim.util.SettingsManager
import com.tqmane.filmsim.di.UpdateCheckerWrapper
import com.tqmane.filmsim.util.WatermarkProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver,
    private val imageLoadUseCase: ImageLoadUseCase,
    private val lutApplyUseCase: LutApplyUseCase,
    private val watermarkUseCase: WatermarkUseCase,
    val settings: SettingsManager,
    val brands: List<LutBrand>,
    private val updateChecker: UpdateCheckerWrapper,
    private val securityCheck: SecurityCheckUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    // ─── State flows ────────────────────────────────────

    private val _viewState = MutableStateFlow<ViewState>(ViewState.Empty)
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    private val _editState = MutableStateFlow(
        EditState(
            intensity = settings.lastIntensity,
            overlayIntensity = settings.lastOverlayIntensity,
            grainEnabled = settings.lastGrainEnabled,
            grainIntensity = settings.lastGrainIntensity,
            grainStyle = settings.lastGrainStyle,
            exposure = settings.lastExposure,
            contrast = settings.lastContrast,
            highlights = settings.lastHighlights,
            shadows = settings.lastShadows,
            colorTemp = settings.lastColorTemp,
            hue = settings.lastHue,
            saturation = settings.lastSaturation,
            luminance = settings.lastLuminance
        )
    )
    val editState: StateFlow<EditState> = _editState.asStateFlow()

    private val _showPanelHints = MutableStateFlow(settings.panelHintsEnabled)
    val showPanelHints: StateFlow<Boolean> = _showPanelHints.asStateFlow()

    private val _watermarkState = MutableStateFlow(WatermarkState())
    val watermarkState: StateFlow<WatermarkState> = _watermarkState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _pendingUpdate = MutableStateFlow<com.tqmane.filmsim.util.ReleaseInfo?>(null)
    val pendingUpdate: StateFlow<com.tqmane.filmsim.util.ReleaseInfo?> = _pendingUpdate.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    // ─── Watermark preview job ──────────────────────────

    private var watermarkPreviewJob: Job? = null

    // ─── Image loading ──────────────────────────────────

    fun loadImage(uri: Uri) {
        _viewState.value = ViewState.Loading
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                imageLoadUseCase.load(contentResolver, uri)
            }.onSuccess { result ->
                _viewState.value = ViewState.Content(
                    originalUri = uri,
                    previewBitmap = result.previewBitmap,
                    thumbnailBitmap = result.thumbnailBitmap,
                    previewBitmapCopy = result.previewCopy,
                    imageDimensions = result.imageDimensions,
                    previewDimensions = result.previewDimensions
                )
                result.exifData?.let { exif ->
                    _watermarkState.value = _watermarkState.value.copy(
                        deviceName = exif.deviceName,
                        timeText = exif.timeText,
                        lensInfo = exif.lensInfo,
                        locationText = exif.locationText
                    )
                }
                _uiEvent.emit(
                    UiEvent.ShowToast(
                        R.string.image_loaded,
                        arrayOf(result.imageDimensions, result.previewDimensions)
                    )
                )
            }.onFailure { e ->
                val msg = if (e is OutOfMemoryError) "Out of memory" else (e.message ?: "Unknown error")
                _viewState.value = ViewState.Error(msg)
                _uiEvent.emit(UiEvent.ShowToast(R.string.image_load_failed, arrayOf(msg)))
            }
        }
    }

    private fun ensureTrustedEnvironment(showErrorState: Boolean = false): Boolean {
        if (securityCheck.isTrusted()) return true

        if (showErrorState) {
            _viewState.value = ViewState.Error(context.getString(R.string.security_environment_untrusted))
        }
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowToast(R.string.security_environment_untrusted))
        }
        return false
    }

    // ─── LUT application ────────────────────────────────

    fun applyLut(lutItem: LutItem) {
        applyParsedLut(lutItem.assetPath, asOverlay = false)
    }

    fun applyOverlayLut(lutItem: LutItem) {
        applyParsedLut(lutItem.assetPath, asOverlay = true)
    }

    fun resolveLutDisplayName(path: String?): String? {
        if (path == null) return null

        return brands
            .asSequence()
            .flatMap { it.categories.asSequence() }
            .flatMap { it.items.asSequence() }
            .firstOrNull { it.assetPath == path }
            ?.name
            ?: path.substringAfterLast('/').substringBeforeLast('.')
    }

    fun clearOverlayLut() {
        val current = _editState.value
        _editState.value = current.copy(
            overlayLutPath = null,
            overlayLut = null,
            lutVersion = current.lutVersion + 1
        )
    }

    private fun applyParsedLut(path: String, asOverlay: Boolean) {
        if (!ensureTrustedEnvironment()) return

        val current = _editState.value
        _editState.value = if (asOverlay) {
            current.copy(overlayLutPath = path)
        } else {
            current.copy(currentLutPath = path, hasSelectedLut = true)
        }

        viewModelScope.launch(ioDispatcher) {
            val lut = lutApplyUseCase.parseLut(path)
            if (lut != null) {
                val updated = _editState.value
                _editState.value = if (asOverlay) {
                    updated.copy(
                        overlayLutPath = path,
                        overlayLut = lut,
                        lutVersion = updated.lutVersion + 1
                    )
                } else {
                    updated.copy(
                        currentLutPath = path,
                        currentLut = lut,
                        hasSelectedLut = true,
                        lutVersion = updated.lutVersion + 1
                    )
                }
            } else {
                _uiEvent.emit(UiEvent.ShowToast(R.string.lut_load_failed))
            }
        }
    }

    // ─── Adjustments ────────────────────────────────────

    fun setIntensity(value: Float) {
        _editState.value = _editState.value.copy(intensity = value)
        settings.lastIntensity = value
    }

    fun setOverlayIntensity(value: Float) {
        _editState.value = _editState.value.copy(overlayIntensity = value)
        settings.lastOverlayIntensity = value
    }

    fun setGrainEnabled(enabled: Boolean) {
        _editState.value = _editState.value.copy(grainEnabled = enabled)
        settings.lastGrainEnabled = enabled
    }

    fun setGrainIntensity(value: Float) {
        _editState.value = _editState.value.copy(grainIntensity = value)
        settings.lastGrainIntensity = value
    }

    fun setGrainStyle(style: String) {
        _editState.value = _editState.value.copy(grainStyle = style)
        settings.lastGrainStyle = style
    }

    // ─── Basic adjustments ──────────────────────────────

    fun setExposure(value: Float) {
        _editState.value = _editState.value.copy(exposure = value)
        settings.lastExposure = value
    }

    fun setContrast(value: Float) {
        _editState.value = _editState.value.copy(contrast = value)
        settings.lastContrast = value
    }

    fun setHighlights(value: Float) {
        _editState.value = _editState.value.copy(highlights = value)
        settings.lastHighlights = value
    }

    fun setShadows(value: Float) {
        _editState.value = _editState.value.copy(shadows = value)
        settings.lastShadows = value
    }

    fun setColorTemp(value: Float) {
        _editState.value = _editState.value.copy(colorTemp = value)
        settings.lastColorTemp = value
    }

    fun setHue(value: Float) {
        _editState.value = _editState.value.copy(hue = value)
        settings.lastHue = value
    }

    fun setSaturation(value: Float) {
        _editState.value = _editState.value.copy(saturation = value)
        settings.lastSaturation = value
    }

    fun setLuminance(value: Float) {
        _editState.value = _editState.value.copy(luminance = value)
        settings.lastLuminance = value
    }

    fun resetAdjustments() {
        _editState.value = _editState.value.copy(
            exposure = 0f,
            contrast = 0f,
            highlights = 0f,
            shadows = 0f,
            colorTemp = 0f,
            hue = 0f,
            saturation = 0f,
            luminance = 0f
        )
        settings.lastExposure = 0f
        settings.lastContrast = 0f
        settings.lastHighlights = 0f
        settings.lastShadows = 0f
        settings.lastColorTemp = 0f
        settings.lastHue = 0f
        settings.lastSaturation = 0f
        settings.lastLuminance = 0f
    }

    fun setPanelHintsEnabled(enabled: Boolean) {
        _showPanelHints.value = enabled
        settings.panelHintsEnabled = enabled
    }

    // ─── Presets ────────────────────────────────────────

    private val _presets = MutableStateFlow(settings.loadPresets())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()

    fun savePreset(name: String) {
        val edit = _editState.value
        val wm = _watermarkState.value
        val preset = Preset(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            lutPath = edit.currentLutPath,
            intensity = edit.intensity,
            overlayLutPath = edit.overlayLutPath,
            overlayIntensity = edit.overlayIntensity,
            grainEnabled = edit.grainEnabled,
            grainIntensity = edit.grainIntensity,
            grainStyle = edit.grainStyle,
            exposure = edit.exposure,
            contrast = edit.contrast,
            highlights = edit.highlights,
            shadows = edit.shadows,
            colorTemp = edit.colorTemp,
            hue = edit.hue,
            saturation = edit.saturation,
            luminance = edit.luminance,
            watermarkStyleName = wm.style.name,
            watermarkDeviceName = wm.deviceName,
            watermarkTimeText = wm.timeText,
            watermarkLocationText = wm.locationText,
            watermarkLensInfo = wm.lensInfo
        )
        val ok = settings.savePreset(preset)
        _presets.value = settings.loadPresets()
        viewModelScope.launch {
            _uiEvent.emit(
                if (ok) UiEvent.ShowToast(R.string.preset_saved)
                else UiEvent.ShowToast(R.string.preset_limit_reached)
            )
        }
    }

    fun loadPreset(preset: Preset) {
        if ((preset.lutPath != null || preset.overlayLutPath != null) && !ensureTrustedEnvironment()) {
            return
        }

        // Restore edit state (LUT will be applied separately)
        _editState.value = _editState.value.copy(
            currentLutPath = preset.lutPath,
            currentLut = null,
            overlayLutPath = preset.overlayLutPath,
            overlayLut = null,
            intensity = preset.intensity,
            overlayIntensity = preset.overlayIntensity,
            grainEnabled = preset.grainEnabled,
            grainIntensity = preset.grainIntensity,
            grainStyle = preset.grainStyle,
            exposure = preset.exposure,
            contrast = preset.contrast,
            highlights = preset.highlights,
            shadows = preset.shadows,
            colorTemp = preset.colorTemp,
            hue = preset.hue,
            saturation = preset.saturation,
            luminance = preset.luminance,
            hasSelectedLut = preset.lutPath != null
        )
        // Restore watermark state
        val wmStyle = try {
            WatermarkProcessor.WatermarkStyle.valueOf(preset.watermarkStyleName)
        } catch (_: Exception) {
            WatermarkProcessor.WatermarkStyle.NONE
        }
        _watermarkState.value = _watermarkState.value.copy(
            style = wmStyle,
            deviceName = preset.watermarkDeviceName,
            timeText = preset.watermarkTimeText,
            locationText = preset.watermarkLocationText,
            lensInfo = preset.watermarkLensInfo
        )
        // Persist the restored values
        settings.lastIntensity = preset.intensity
        settings.lastOverlayIntensity = preset.overlayIntensity
        settings.lastGrainEnabled = preset.grainEnabled
        settings.lastGrainIntensity = preset.grainIntensity
        settings.lastGrainStyle = preset.grainStyle
        settings.lastExposure = preset.exposure
        settings.lastContrast = preset.contrast
        settings.lastHighlights = preset.highlights
        settings.lastShadows = preset.shadows
        settings.lastColorTemp = preset.colorTemp
        settings.lastHue = preset.hue
        settings.lastSaturation = preset.saturation
        settings.lastLuminance = preset.luminance
        // Apply the LUT if present
        viewModelScope.launch(ioDispatcher) {
            val baseLut = preset.lutPath?.let { lutApplyUseCase.parseLut(it) }
            val overlayLut = preset.overlayLutPath?.let { lutApplyUseCase.parseLut(it) }
            val current = _editState.value
            _editState.value = current.copy(
                currentLut = baseLut,
                overlayLut = overlayLut,
                lutVersion = current.lutVersion + 1
            )
            _uiEvent.emit(UiEvent.ShowToast(R.string.preset_loaded, arrayOf(preset.name)))
        }
    }

    fun deletePreset(id: String) {
        settings.deletePreset(id)
        _presets.value = settings.loadPresets()
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowToast(R.string.preset_deleted))
        }
    }

    // ─── Watermark ──────────────────────────────────────

    fun updateWatermarkStyle(style: WatermarkProcessor.WatermarkStyle) {
        _watermarkState.value = _watermarkState.value.copy(style = style)
    }

    fun updateWatermarkBrand(name: String) {
        _watermarkState.value = _watermarkState.value.copy(brandName = name)
    }

    fun updateWatermarkFields(
        deviceName: String? = null,
        timeText: String? = null,
        locationText: String? = null,
        lensInfo: String? = null
    ) {
        val w = _watermarkState.value
        _watermarkState.value = w.copy(
            deviceName = deviceName ?: w.deviceName,
            timeText = timeText ?: w.timeText,
            locationText = locationText ?: w.locationText,
            lensInfo = lensInfo ?: w.lensInfo
        )
    }

    fun renderWatermarkPreview(callback: (Bitmap) -> Unit) {
        val state = _viewState.value as? ViewState.Content ?: return
        val wm = _watermarkState.value
        if (wm.style == WatermarkProcessor.WatermarkStyle.NONE) return
        val preview = state.previewBitmapCopy ?: return
        val edit = _editState.value

        watermarkPreviewJob?.cancel()
        watermarkPreviewJob = viewModelScope.launch(defaultDispatcher) {
            val config = WatermarkProcessor.WatermarkConfig(
                style = wm.style,
                deviceName = wm.deviceName.ifEmpty { null },
                timeText = wm.timeText.ifEmpty { null },
                locationText = wm.locationText.ifEmpty { null },
                lensInfo = wm.lensInfo.ifEmpty { null }
            )
            val result = watermarkUseCase.renderPreview(
                preview,
                edit.currentLut,
                edit.intensity,
                edit.overlayLut,
                edit.overlayIntensity,
                config
            )
            withContext(kotlinx.coroutines.Dispatchers.Main) { callback(result) }
        }
    }

    /**
     * Check watermark state directly from StateFlow (not stale Compose state)
     * and refresh the watermark preview if a watermark is active.
     */
    fun refreshWatermarkIfActive(callback: (Bitmap) -> Unit) {
        val wm = _watermarkState.value
        if (wm.style == WatermarkProcessor.WatermarkStyle.NONE) return
        renderWatermarkPreview(callback)
    }

    // ─── Save / Export ──────────────────────────────────

    /**
     * High-resolution export pipeline.
     *
     * 1. Loads full-res bitmap on-demand from [ViewState.Content.originalUri]
     * 2. Renders LUT + grain on the GL thread via [GlCommandExecutor] (no CountDownLatch)
     * 3. Applies watermark
     * 4. Saves to MediaStore
     * 5. Immediately recycles the full-res bitmap
     */
    fun saveHighResImage(
        glExecutor: GlCommandExecutor,
        gpuExportRenderer: GpuExportRenderer?,
        onCpuFallback: () -> Unit
    ) {
        val state = _viewState.value as? ViewState.Content ?: return
        val edit = _editState.value
        val wm = _watermarkState.value

        if (!ensureTrustedEnvironment()) return

        _isSaving.value = true
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                // 1. Load full-res on-demand (not kept in memory)
                val sourceBitmap = imageLoadUseCase.loadFullResolution(contentResolver, state.originalUri)

                try {
                    val lut = edit.currentLutPath?.let { lutApplyUseCase.parseLut(it) }
                    val overlayLut = edit.overlayLutPath?.let { lutApplyUseCase.parseLut(it) }
                    val effectiveIntensity = if (lut != null) edit.intensity else 0f
                    val effectiveOverlayIntensity = if (overlayLut != null) edit.overlayIntensity else 0f

                    // 2. GPU rendering via suspendCancellableCoroutine
                    var outputBitmap: Bitmap? = runCatching {
                        glExecutor.execute {
                            if (gpuExportRenderer == null) {
                                throw IllegalStateException("GpuExportRenderer not initialized")
                            }
                            gpuExportRenderer.setGrainStyle(edit.grainStyle)
                            gpuExportRenderer.renderHighRes(
                                sourceBitmap,
                                lut,
                                effectiveIntensity,
                                overlayLut,
                                effectiveOverlayIntensity,
                                edit.grainEnabled, edit.grainIntensity, 4.0f,
                                edit.exposure, edit.contrast, edit.highlights,
                                edit.shadows, edit.colorTemp
                            )
                        }
                    }.getOrNull()

                    var shouldRecycleOutput = true

                    // 3. CPU fallback
                    if (outputBitmap == null) {
                        withContext(kotlinx.coroutines.Dispatchers.Main) { onCpuFallback() }
                        outputBitmap = if (lut != null || overlayLut != null) {
                            watermarkUseCase.applyCpuLut(
                                sourceBitmap,
                                lut,
                                effectiveIntensity,
                                overlayLut,
                                effectiveOverlayIntensity
                            )
                        } else {
                            shouldRecycleOutput = false
                            sourceBitmap
                        }
                    }

                    // 4. Watermark
                    if (wm.style != WatermarkProcessor.WatermarkStyle.NONE) {
                        val wmConfig = WatermarkProcessor.WatermarkConfig(
                            style = wm.style,
                            deviceName = wm.deviceName.ifEmpty { null },
                            timeText = wm.timeText.ifEmpty { null },
                            locationText = wm.locationText.ifEmpty { null },
                            lensInfo = wm.lensInfo.ifEmpty { null }
                        )
                        val watermarked = watermarkUseCase.applyWatermark(outputBitmap, wmConfig)
                        if (watermarked !== outputBitmap) {
                            if (shouldRecycleOutput && outputBitmap != sourceBitmap) outputBitmap.recycle()
                            outputBitmap = watermarked
                            shouldRecycleOutput = true
                        }
                    }

                    // 5. Save
                    val saveResult = watermarkUseCase.saveBitmapWithExif(
                        outputBitmap, state.originalUri,
                        settings.savePath, settings.saveQuality
                    )

                    // 6. Cleanup full-res immediately
                    if (shouldRecycleOutput && outputBitmap != sourceBitmap) outputBitmap.recycle()
                    if (sourceBitmap != outputBitmap) sourceBitmap.recycle()

                    _uiEvent.emit(
                        UiEvent.ImageSaved(saveResult.width, saveResult.height, saveResult.path, saveResult.filename)
                    )
                } catch (e: Exception) {
                    sourceBitmap.recycle()
                    throw e
                }
            }.onFailure { e ->
                _uiEvent.emit(UiEvent.ShowToast(R.string.save_error, arrayOf(e.message ?: "")))
            }
            _isSaving.value = false
        }
    }

    // ─── Update check ───────────────────────────────────

    fun checkForUpdates() {
        viewModelScope.launch {
            val release = try {
                updateChecker.checkForUpdate()
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowToast(R.string.update_check_failed))
                null
            }
            _pendingUpdate.value = release
        }
    }

    fun dismissUpdate() {
        _pendingUpdate.value = null
    }

    // ─── Cleanup ────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        watermarkPreviewJob?.cancel()
    }
}
