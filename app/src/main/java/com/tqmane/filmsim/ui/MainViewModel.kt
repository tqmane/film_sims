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
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver,
    private val imageLoadUseCase: ImageLoadUseCase,
    private val lutApplyUseCase: LutApplyUseCase,
    private val watermarkUseCase: WatermarkUseCase,
    val settings: SettingsManager,
    val brands: List<LutBrand>,
    private val updateChecker: UpdateCheckerWrapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    // ─── State flows ────────────────────────────────────

    private val _viewState = MutableStateFlow<ViewState>(ViewState.Empty)
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    private val _editState = MutableStateFlow(
        EditState(
            intensity = settings.lastIntensity,
            grainEnabled = settings.lastGrainEnabled,
            grainIntensity = settings.lastGrainIntensity,
            grainStyle = settings.lastGrainStyle
        )
    )
    val editState: StateFlow<EditState> = _editState.asStateFlow()

    // Lifted UI State for Brand/Category Selection
    private val _selectedBrandIndex = MutableStateFlow(0)
    val selectedBrandIndex: StateFlow<Int> = _selectedBrandIndex.asStateFlow()

    private val _selectedCategoryIndex = MutableStateFlow(0)
    val selectedCategoryIndex: StateFlow<Int> = _selectedCategoryIndex.asStateFlow()

    // Scroll positions for LazyRows (persisted across immersive toggle)
    private val _brandScrollIndex = MutableStateFlow(0)
    val brandScrollIndex: StateFlow<Int> = _brandScrollIndex.asStateFlow()

    private val _categoryScrollIndex = MutableStateFlow(0)
    val categoryScrollIndex: StateFlow<Int> = _categoryScrollIndex.asStateFlow()

    private val _lutScrollIndex = MutableStateFlow(0)
    val lutScrollIndex: StateFlow<Int> = _lutScrollIndex.asStateFlow()

    fun setSelectedBrandIndex(index: Int) {
        _selectedBrandIndex.value = index
    }

    fun setSelectedCategoryIndex(index: Int) {
        _selectedCategoryIndex.value = index
    }

    fun setBrandScrollIndex(index: Int) { _brandScrollIndex.value = index }
    fun setCategoryScrollIndex(index: Int) { _categoryScrollIndex.value = index }
    fun setLutScrollIndex(index: Int) { _lutScrollIndex.value = index }

    private val _watermarkState = MutableStateFlow(WatermarkState())
    val watermarkState: StateFlow<WatermarkState> = _watermarkState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    // ─── Watermark preview job ──────────────────────────

    private var watermarkPreviewJob: Job? = null

    // ─── Cached GPU export renderer (set from UI layer) ─

    var gpuExportRenderer: GpuExportRenderer? = null

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

    // ─── LUT application ────────────────────────────────

    fun applyLut(lutItem: LutItem) {
        if (!com.tqmane.filmsim.util.SecurityManager.verifySignature(context)) return

        val path = lutItem.assetPath
        _editState.value = _editState.value.copy(currentLutPath = path, hasSelectedLut = true)

        viewModelScope.launch(ioDispatcher) {
            val lut = lutApplyUseCase.parseLut(path)
            if (lut != null) {
                val cur = _editState.value
                _editState.value = cur.copy(
                    currentLut = lut,
                    lutVersion = cur.lutVersion + 1
                )
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
                preview, edit.currentLut, edit.intensity, config
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
        onCpuFallback: () -> Unit
    ) {
        val state = _viewState.value as? ViewState.Content ?: return
        val edit = _editState.value
        val wm = _watermarkState.value

        if (!com.tqmane.filmsim.util.SecurityManager.verifySignature(context)) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowToast(R.string.lut_load_failed))
            }
            return
        }

        viewModelScope.launch(ioDispatcher) {
            runCatching {
                // 1. Load full-res on-demand (not kept in memory)
                val sourceBitmap = imageLoadUseCase.loadFullResolution(contentResolver, state.originalUri)

                try {
                    val lut = if (edit.currentLutPath != null) {
                        lutApplyUseCase.parseLut(edit.currentLutPath)
                    } else null
                    val effectiveIntensity = if (lut != null) edit.intensity else 0f

                    // 2. GPU rendering via suspendCancellableCoroutine
                    var outputBitmap: Bitmap? = runCatching {
                        glExecutor.execute {
                            if (gpuExportRenderer == null) {
                                // GpuExportRenderer needs a Context; created on GL thread
                                // The UI layer must set gpuExportRenderer before calling save
                                throw IllegalStateException("GpuExportRenderer not initialized")
                            }
                            gpuExportRenderer!!.setGrainStyle(edit.grainStyle)
                            gpuExportRenderer!!.renderHighRes(
                                sourceBitmap, lut, effectiveIntensity,
                                edit.grainEnabled, edit.grainIntensity, 4.0f
                            )
                        }
                    }.getOrNull()

                    var shouldRecycleOutput = true

                    // 3. CPU fallback
                    if (outputBitmap == null) {
                        withContext(kotlinx.coroutines.Dispatchers.Main) { onCpuFallback() }
                        outputBitmap = if (lut != null) {
                            watermarkUseCase.applyCpuLut(sourceBitmap, lut, effectiveIntensity)
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
        }
    }

    // ─── Update check ───────────────────────────────────

    fun checkForUpdates() {
        viewModelScope.launch {
            runCatching { updateChecker.checkForUpdate() }
                .onSuccess { release -> release?.let { _uiEvent.emit(UiEvent.ShowUpdateDialog(it)) } }
        }
    }

    // ─── Cleanup ────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        gpuExportRenderer?.release()
        gpuExportRenderer = null
    }
}
