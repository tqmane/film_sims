package com.tqmane.filmsim.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tqmane.filmsim.R
import com.tqmane.filmsim.data.LutBrand
import com.tqmane.filmsim.data.LutItem
import com.tqmane.filmsim.data.LutRepository
import com.tqmane.filmsim.domain.ImageLoadUseCase
import com.tqmane.filmsim.domain.LutApplyUseCase
import com.tqmane.filmsim.domain.WatermarkUseCase
import com.tqmane.filmsim.gl.GpuExportRenderer
import com.tqmane.filmsim.util.AppCoroutineExceptionHandler
import com.tqmane.filmsim.util.SettingsManager
import com.tqmane.filmsim.util.UpdateChecker
import com.tqmane.filmsim.util.WatermarkProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val imageLoadUseCase: ImageLoadUseCase,
    private val lutApplyUseCase: LutApplyUseCase,
    private val watermarkUseCase: WatermarkUseCase,
    val settings: SettingsManager
) : AndroidViewModel(application) {

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

    private val _watermarkState = MutableStateFlow(WatermarkState())
    val watermarkState: StateFlow<WatermarkState> = _watermarkState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    // ─── Brand data (loaded once) ───────────────────────

    val brands: List<LutBrand> by lazy { LutRepository.getLutBrands(getApplication()) }

    // ─── Watermark preview job ──────────────────────────

    private var watermarkPreviewJob: Job? = null

    // ─── Cached export renderer reference ───────────────
    var gpuExportRenderer: GpuExportRenderer? = null

    // ─── Image loading ──────────────────────────────────

    fun loadImage(uri: Uri) {
        _viewState.value = ViewState.Loading
        viewModelScope.launch(Dispatchers.IO + AppCoroutineExceptionHandler.create()) {
            try {
                val result = imageLoadUseCase.load(getApplication<Application>().contentResolver, uri)
                _viewState.value = ViewState.Ready(
                    originalUri = uri,
                    originalBitmap = result.exportBitmap,
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
                        getApplication<Application>().getString(
                            R.string.image_loaded,
                            result.imageDimensions,
                            result.previewDimensions
                        )
                    )
                )
            } catch (e: OutOfMemoryError) {
                _viewState.value = ViewState.Error("Out of memory")
                _uiEvent.emit(
                    UiEvent.ShowToast(
                        getApplication<Application>().getString(R.string.image_load_failed, "Out of memory")
                    )
                )
            } catch (e: Exception) {
                _viewState.value = ViewState.Error(e.message ?: "Unknown error")
                _uiEvent.emit(
                    UiEvent.ShowToast(
                        getApplication<Application>().getString(R.string.image_load_failed, e.message ?: "")
                    )
                )
            }
        }
    }

    // ─── LUT application ────────────────────────────────

    fun applyLut(lutItem: LutItem) {
        val path = lutItem.assetPath
        _editState.value = _editState.value.copy(currentLutPath = path, hasSelectedLut = true)

        viewModelScope.launch(Dispatchers.IO + AppCoroutineExceptionHandler.create()) {
            val lut = lutApplyUseCase.parseLut(getApplication(), path)
            if (lut != null) {
                _editState.value = _editState.value.copy(currentLut = lut)
            } else {
                _uiEvent.emit(
                    UiEvent.ShowToast(getApplication<Application>().getString(R.string.lut_load_failed))
                )
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
        val state = _viewState.value as? ViewState.Ready ?: return
        val wm = _watermarkState.value
        if (wm.style == WatermarkProcessor.WatermarkStyle.NONE) return
        val preview = state.previewBitmapCopy ?: return
        val edit = _editState.value

        watermarkPreviewJob?.cancel()
        watermarkPreviewJob = viewModelScope.launch(Dispatchers.Default) {
            val config = WatermarkProcessor.WatermarkConfig(
                style = wm.style,
                deviceName = wm.deviceName.ifEmpty { null },
                timeText = wm.timeText.ifEmpty { null },
                locationText = wm.locationText.ifEmpty { null },
                lensInfo = wm.lensInfo.ifEmpty { null }
            )
            val result = watermarkUseCase.renderPreview(
                getApplication(), preview, edit.currentLut, edit.intensity, config
            )
            withContext(Dispatchers.Main) { callback(result) }
        }
    }

    // ─── Save / Export ──────────────────────────────────

    fun saveHighResImage(
        glSurfaceView: android.opengl.GLSurfaceView,
        onStartCpu: () -> Unit
    ) {
        val state = _viewState.value as? ViewState.Ready ?: return
        val edit = _editState.value
        val wm = _watermarkState.value
        val sourceBitmap = state.originalBitmap

        viewModelScope.launch(Dispatchers.IO + AppCoroutineExceptionHandler.create()) {
            try {
                val lut = if (edit.currentLutPath != null) {
                    lutApplyUseCase.parseLut(getApplication(), edit.currentLutPath)
                } else null

                val effectiveIntensity = if (lut != null) edit.intensity else 0f

                // GPU export
                val outputHolder = arrayOfNulls<Bitmap>(1)
                val gpuError = arrayOf<Exception?>(null)
                val latch = java.util.concurrent.CountDownLatch(1)

                glSurfaceView.queueEvent {
                    try {
                        if (gpuExportRenderer == null) {
                            gpuExportRenderer = GpuExportRenderer(getApplication())
                        }
                        gpuExportRenderer!!.setGrainStyle(edit.grainStyle)
                        outputHolder[0] = gpuExportRenderer!!.renderHighRes(
                            sourceBitmap, lut, effectiveIntensity,
                            edit.grainEnabled, edit.grainIntensity, 4.0f
                        )
                    } catch (e: Exception) {
                        gpuError[0] = e
                    }
                    latch.countDown()
                }
                latch.await()

                var outputBitmap = outputHolder[0]
                var shouldRecycleOutput = true

                if (outputBitmap == null) {
                    withContext(Dispatchers.Main) { onStartCpu() }
                    outputBitmap = if (lut != null) {
                        watermarkUseCase.applyCpuLut(sourceBitmap, lut, effectiveIntensity)
                    } else {
                        shouldRecycleOutput = false
                        sourceBitmap
                    }
                }

                // Apply watermark
                if (wm.style != WatermarkProcessor.WatermarkStyle.NONE) {
                    val wmConfig = WatermarkProcessor.WatermarkConfig(
                        style = wm.style,
                        deviceName = wm.deviceName.ifEmpty { null },
                        timeText = wm.timeText.ifEmpty { null },
                        locationText = wm.locationText.ifEmpty { null },
                        lensInfo = wm.lensInfo.ifEmpty { null }
                    )
                    val watermarked = watermarkUseCase.applyWatermark(getApplication(), outputBitmap, wmConfig)
                    if (watermarked !== outputBitmap) {
                        if (shouldRecycleOutput && outputBitmap != sourceBitmap) outputBitmap.recycle()
                        outputBitmap = watermarked
                        shouldRecycleOutput = true
                    }
                }

                val saveResult = watermarkUseCase.saveBitmapWithExif(
                    getApplication(), outputBitmap, state.originalUri,
                    settings.savePath, settings.saveQuality
                )

                if (shouldRecycleOutput && outputBitmap != sourceBitmap) outputBitmap.recycle()

                _uiEvent.emit(
                    UiEvent.ImageSaved(saveResult.width, saveResult.height, saveResult.path, saveResult.filename)
                )
            } catch (e: Exception) {
                _uiEvent.emit(
                    UiEvent.ShowToast(
                        getApplication<Application>().getString(R.string.save_error, e.message ?: "")
                    )
                )
            }
        }
    }

    // ─── Update check ───────────────────────────────────

    fun checkForUpdates() {
        viewModelScope.launch(AppCoroutineExceptionHandler.create()) {
            try {
                val release = UpdateChecker.checkForUpdate(getApplication())
                if (release != null) {
                    _uiEvent.emit(UiEvent.ShowUpdateDialog(release))
                }
            } catch (_: Exception) { /* silently ignore */ }
        }
    }

    // ─── Cleanup ────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        gpuExportRenderer?.release()
        gpuExportRenderer = null
    }
}
