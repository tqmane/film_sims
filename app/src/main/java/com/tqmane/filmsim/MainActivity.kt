package com.tqmane.filmsim

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.Window
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
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tqmane.filmsim.data.LutRepository
import com.tqmane.filmsim.gl.FilmSimRenderer
import com.tqmane.filmsim.gl.GpuExportRenderer
import com.tqmane.filmsim.ui.BrandAdapter
import com.tqmane.filmsim.ui.GenreAdapter
import com.tqmane.filmsim.ui.LutAdapter
import com.tqmane.filmsim.util.CubeLUT
import com.tqmane.filmsim.util.CubeLUTParser
import com.tqmane.filmsim.util.HighResLutProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


class MainActivity : ComponentActivity() {

    private lateinit var renderer: FilmSimRenderer
    private lateinit var glSurfaceView: android.opengl.GLSurfaceView
    private lateinit var lutAdapter: LutAdapter
    
    private lateinit var placeholderContainer: LinearLayout
    private lateinit var sliderPanel: LinearLayout
    private lateinit var adjustmentHeader: LinearLayout
    private lateinit var adjustmentToggle: ImageView
    
    private lateinit var prefs: SharedPreferences
    
    private var hasSelectedLut = false
    private var savePath = "Pictures/FilmSims"
    private var saveQuality = 100
    private var isAdjustmentPanelExpanded = true
    
    // Immersive mode and before/after
    private var isImmersiveMode = false
    private var isShowingOriginal = false
    private lateinit var topBar: View
    private lateinit var controlPanel: LinearLayout
    private lateinit var quickIntensityPanel: LinearLayout
    private lateinit var quickIntensitySlider: SeekBar
    private lateinit var quickIntensityValue: TextView
    
    // Store original image data for full-resolution export
    private var originalImageUri: Uri? = null
    private var originalBitmap: Bitmap? = null
    private var originalExifData: ByteArray? = null
    private var currentLutPath: String? = null
    private var currentIntensity: Float = 1f
    private var currentGrainEnabled: Boolean = false
    private var currentGrainIntensity: Float = 0.5f
    
    private var gpuExportRenderer: GpuExportRenderer? = null
    
    // Pinch-to-zoom
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: android.view.GestureDetector
    private val transformMatrix = Matrix()
    private val matrixValues = FloatArray(9)
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID


    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            loadImage(uri)
        }
    }

    private fun loadImage(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Store original URI
                originalImageUri = uri
                
                // Read EXIF data from original image
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    // Store raw EXIF bytes for later
                    originalExifData = extractExifBytes(uri)
                }
                
                // Load full-resolution bitmap (kept for export)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                if (bitmap != null) {
                    originalBitmap = bitmap
                    
                    // Scale down for preview (max 10MP = ~3650x2740 for 4:3)
                    val previewBitmap = scaleToMaxPixels(bitmap, 10_000_000)
                    
                    withContext(Dispatchers.Main) {
                        // Hide placeholder
                        placeholderContainer.visibility = View.GONE
                        
                        // Update GL View for preview (using scaled bitmap)
                        glSurfaceView.queueEvent {
                            renderer.setImage(previewBitmap)
                            glSurfaceView.requestRender()
                        }
                        
                        // Thumbnails for LUT preview (500px)
                        val maxDim = 500
                        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val thumbWidth: Int
                        val thumbHeight: Int
                        if (ratio > 1) {
                            thumbWidth = maxDim
                            thumbHeight = (maxDim / ratio).toInt()
                        } else {
                            thumbHeight = maxDim
                            thumbWidth = (maxDim * ratio).toInt()
                        }
                        val thumb = Bitmap.createScaledBitmap(bitmap, thumbWidth, thumbHeight, true)
                        lutAdapter.setSourceBitmap(thumb)
                        
                        Toast.makeText(
                            this@MainActivity, 
                            "画像を読み込みました: ${bitmap.width}x${bitmap.height} (プレビュー: ${previewBitmap.width}x${previewBitmap.height})", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "画像の読み込みに失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun extractExifBytes(uri: Uri): ByteArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun scaleToMaxPixels(bitmap: Bitmap, maxPixels: Int): Bitmap {
        val currentPixels = bitmap.width * bitmap.height
        if (currentPixels <= maxPixels) {
            return bitmap
        }
        
        val scale = kotlin.math.sqrt(maxPixels.toFloat() / currentPixels.toFloat())
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load preferences
        prefs = getSharedPreferences("filmsim_settings", Context.MODE_PRIVATE)
        savePath = prefs.getString("save_path", "Pictures/FilmSims") ?: "Pictures/FilmSims"
        saveQuality = prefs.getInt("save_quality", 100)

        setupWindowInsets()
        setupViews()
        setupLutList()
        createDefaultThumbnail()
    }
    
    private fun setupViews() {
        glSurfaceView = findViewById(R.id.glSurfaceView)
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.preserveEGLContextOnPause = true
        renderer = FilmSimRenderer(this)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
        
        // Setup pinch-to-zoom
        glSurfaceView.pivotX = 0f
        glSurfaceView.pivotY = 0f

        gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                resetZoom()
                return true
            }
            
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (originalBitmap != null) {
                    toggleImmersiveMode()
                }
                return true
            }
            
            override fun onLongPress(e: MotionEvent) {
                if (originalBitmap != null && currentLutPath != null) {
                    showOriginalImage()
                }
            }
        })

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private var lastFocusX = 0f
            private var lastFocusY = 0f

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                lastFocusX = detector.focusX
                lastFocusY = detector.focusY
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                val focusX = detector.focusX
                val focusY = detector.focusY

                // Get current scale BEFORE applying new transform
                transformMatrix.getValues(matrixValues)
                val currentScale = matrixValues[Matrix.MSCALE_X]

                // Calculate what the new scale would be
                val newScale = currentScale * scaleFactor

                // PAN: Calculate movement of the focal point
                val dx = focusX - lastFocusX
                val dy = focusY - lastFocusY

                // Check if scale would be within bounds and significantly different from 1.0
                val canScale = newScale >= 0.5f && newScale <= 10.0f && kotlin.math.abs(scaleFactor - 1.0f) > 0.001f

                if (canScale) {
                    // Apply scale FIRST around the focal point
                    transformMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
                }

                // Then apply pan (always allow panning during pinch)
                transformMatrix.postTranslate(dx, dy)

                updateGLViewTransform()

                // Update state for next frame / seamless transition
                lastFocusX = focusX
                lastFocusY = focusY
                lastTouchX = focusX
                lastTouchY = focusY

                return true
            }
        })
        
        glSurfaceView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.getX(0)
                    lastTouchY = event.getY(0)
                    activePointerId = event.getPointerId(0)
                }
                MotionEvent.ACTION_MOVE -> {
                    // Only pan if NOT scaling
                    if (!scaleGestureDetector.isInProgress && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                        val pointerIndex = event.findPointerIndex(activePointerId)
                        if (pointerIndex >= 0) {
                            val x = event.getX(pointerIndex)
                            val y = event.getY(pointerIndex)
                            val dx = x - lastTouchX
                            val dy = y - lastTouchY

                            transformMatrix.postTranslate(dx, dy)
                            updateGLViewTransform()

                            lastTouchX = x
                            lastTouchY = y
                        }
                    } else if (scaleGestureDetector.isInProgress) {
                        // While scaling, keep updating touch coordinates to the focus point
                        // This ensures that if one finger is lifted, panning resumes smoothly
                        lastTouchX = scaleGestureDetector.focusX
                        lastTouchY = scaleGestureDetector.focusY
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                    if (isShowingOriginal) {
                        restoreEditedImage()
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerIndex = event.actionIndex
                    val pointerId = event.getPointerId(pointerIndex)
                    if (pointerId == activePointerId) {
                        val newPointerIndex = if (pointerIndex == 0) 1 else 0
                        if (newPointerIndex < event.pointerCount) {
                            lastTouchX = event.getX(newPointerIndex)
                            lastTouchY = event.getY(newPointerIndex)
                            activePointerId = event.getPointerId(newPointerIndex)
                        }
                    }
                }
            }
            true
        }
        
        placeholderContainer = findViewById(R.id.placeholderContainer)
        sliderPanel = findViewById(R.id.sliderPanel)
        adjustmentHeader = findViewById(R.id.adjustmentHeader)
        adjustmentToggle = findViewById(R.id.adjustmentToggle)
        
        // Immersive mode views
        topBar = findViewById(R.id.topBar)
        controlPanel = findViewById(R.id.controlPanel)
        
        // Quick intensity slider (always visible when LUT selected)
        quickIntensityPanel = findViewById(R.id.quickIntensityPanel)
        quickIntensitySlider = findViewById(R.id.quickIntensitySlider)
        quickIntensityValue = findViewById(R.id.quickIntensityValue)
        
        // Setup collapsible adjustment panel
        adjustmentHeader.setOnClickListener {
            toggleAdjustmentPanel()
        }
        
        // Placeholder tap to pick image
        placeholderContainer.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Settings button (ImageButton in new layout)
        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
        btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        // Pick image button (MaterialButton in placeholder)
        val btnPick = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPickImage)
        btnPick.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Change photo button (ImageButton in top bar)
        findViewById<ImageButton>(R.id.btnChangePhoto)?.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Save button (MaterialButton in top bar)
        val btnSave = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)
        btnSave.setOnClickListener {
            if (originalBitmap == null) {
                Toast.makeText(this, "先に画像を選択してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveHighResImage()
        }
        
        // Setup intensity slider (single quick slider above presets)
        quickIntensitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val intensity = progress / 100f
                currentIntensity = intensity
                quickIntensityValue.text = "${progress}%"
                
                glSurfaceView.queueEvent {
                    renderer.setIntensity(intensity)
                    glSurfaceView.requestRender()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Setup film grain controls
        val grainToggle = findViewById<android.widget.CheckBox>(R.id.grainToggle)
        val grainSlider = findViewById<SeekBar>(R.id.grainSlider)
        val grainValue = findViewById<TextView>(R.id.grainValue)
        val grainIcon = findViewById<ImageView>(R.id.grainIcon)
        
        val accentColor = getColor(R.color.accent_primary)
        val disabledColor = getColor(R.color.text_tertiary)
        
        grainToggle.setOnCheckedChangeListener { _, isChecked ->
            currentGrainEnabled = isChecked
            grainSlider.isEnabled = isChecked
            
            // Update colors based on state
            val color = if (isChecked) accentColor else disabledColor
            grainValue.setTextColor(color)
            grainIcon.imageTintList = ColorStateList.valueOf(color)
            grainSlider.progressTintList = ColorStateList.valueOf(color)
            grainSlider.thumbTintList = ColorStateList.valueOf(color)
            
            glSurfaceView.queueEvent {
                renderer.setGrainEnabled(isChecked)
                if (isChecked) {
                    renderer.setGrainIntensity(grainSlider.progress / 100f)
                }
                glSurfaceView.requestRender()
            }
        }
        
        grainSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                grainValue.text = "${progress}%"
                currentGrainIntensity = progress / 100f
                
                if (grainToggle.isChecked) {
                    glSurfaceView.queueEvent {
                        renderer.setGrainIntensity(progress / 100f)
                        glSurfaceView.requestRender()
                    }
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun updateGLViewTransform() {
        transformMatrix.getValues(matrixValues)
        
        val scale = matrixValues[Matrix.MSCALE_X]
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]
        
        val viewWidth = glSurfaceView.width.toFloat()
        val viewHeight = glSurfaceView.height.toFloat()
        
        if (viewWidth > 0 && viewHeight > 0) {
            // Calculate the displacement of the center point
            // Android Matrix tracks the Top-Left corner (transX, transY)
            // We need to find how much the CENTER of the image moved from the CENTER of the screen
            
            // Image Center X = transX + (ViewWidth * scale) / 2
            // Screen Center X = ViewWidth / 2
            // Delta X = Image Center X - Screen Center X
            val deltaX = transX + (viewWidth * scale) / 2f - (viewWidth / 2f)
            
            // Same for Y
            val deltaY = transY + (viewHeight * scale) / 2f - (viewHeight / 2f)
            
            // Convert to OpenGL coordinates (-1 to 1)
            val glOffsetX = (deltaX / viewWidth) * 2.0f
            val glOffsetY = -(deltaY / viewHeight) * 2.0f // Flip Y for OpenGL
            
            glSurfaceView.queueEvent {
                renderer.updateTransform(scale, glOffsetX, glOffsetY)
                glSurfaceView.requestRender()
            }
        }
    }
    
    private fun toggleAdjustmentPanel() {
        isAdjustmentPanelExpanded = !isAdjustmentPanelExpanded
        
        if (isAdjustmentPanelExpanded) {
            sliderPanel.visibility = View.VISIBLE
            sliderPanel.alpha = 0f
            sliderPanel.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
            adjustmentToggle.animate()
                .rotation(0f)
                .setDuration(200)
                .start()
        } else {
            sliderPanel.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    sliderPanel.visibility = View.GONE
                }
                .start()
            adjustmentToggle.animate()
                .rotation(180f)
                .setDuration(200)
                .start()
        }
    }
    
    private fun resetZoom() {
        transformMatrix.reset()
        updateGLViewTransform()
    }
    
    private fun toggleImmersiveMode() {
        isImmersiveMode = !isImmersiveMode
        val duration = 200L
        
        if (isImmersiveMode) {
            // Store heights before hiding
            val topBarHeight = topBar.height.toFloat()
            val controlPanelHeight = controlPanel.height.toFloat()
            
            // Hide both top bar and control panel
            topBar.animate()
                .alpha(0f)
                .translationY(-topBarHeight)
                .setDuration(duration)
                .withEndAction { topBar.visibility = View.INVISIBLE }
                .start()
            controlPanel.animate()
                .alpha(0f)
                .translationY(controlPanelHeight)
                .setDuration(duration)
                .withEndAction { controlPanel.visibility = View.INVISIBLE }
                .start()
        } else {
            // Show both - use INVISIBLE -> VISIBLE so heights are preserved
            topBar.visibility = View.VISIBLE
            controlPanel.visibility = View.VISIBLE
            
            topBar.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(duration)
                .start()
            controlPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(duration)
                .start()
        }
    }
    
    private fun showOriginalImage() {
        if (isShowingOriginal) return
        isShowingOriginal = true
        
        // Temporarily set intensity to 0 and disable grain to show original
        glSurfaceView.queueEvent {
            renderer.setIntensity(0f)
            renderer.setGrainEnabled(false)
            glSurfaceView.requestRender()
        }
    }
    
    private fun restoreEditedImage() {
        if (!isShowingOriginal) return
        isShowingOriginal = false
        
        // Restore to current intensity and grain settings
        glSurfaceView.queueEvent {
            renderer.setIntensity(currentIntensity)
            renderer.setGrainEnabled(currentGrainEnabled)
            if (currentGrainEnabled) {
                renderer.setGrainIntensity(currentGrainIntensity)
            }
            glSurfaceView.requestRender()
        }
    }
    
    private fun showSettingsDialog() {
        val dialog = Dialog(this, R.style.Theme_FilmSims)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_settings)
        
        // Set dialog width to 90% of screen width for side margins
        val displayMetrics = resources.displayMetrics
        val dialogWidth = (displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(
            dialogWidth,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val tvSavePath = dialog.findViewById<TextView>(R.id.tvSavePath)
        val btnChangePath = dialog.findViewById<TextView>(R.id.btnChangePath)
        val qualitySlider = dialog.findViewById<SeekBar>(R.id.qualitySlider)
        val tvQualityValue = dialog.findViewById<TextView>(R.id.tvQualityValue)
        val btnClose = dialog.findViewById<Button>(R.id.btnCloseSettings)
        
        tvSavePath.text = savePath
        qualitySlider.progress = saveQuality
        tvQualityValue.text = "${saveQuality}%"
        
        btnChangePath.setOnClickListener {
            showPathInputDialog(tvSavePath)
        }
        
        qualitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val quality = progress.coerceAtLeast(10) // Minimum 10%
                tvQualityValue.text = "${quality}%"
                saveQuality = quality
                prefs.edit().putInt("save_quality", quality).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showPathInputDialog(tvSavePath: TextView) {
        val editText = EditText(this).apply {
            setText(savePath)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "例: Pictures/MyPhotos"
            setPadding(48, 32, 48, 32)
        }
        
        AlertDialog.Builder(this, R.style.Theme_FilmSims)
            .setTitle("保存先フォルダを入力")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val newPath = editText.text.toString().trim()
                if (newPath.isNotEmpty()) {
                    savePath = newPath
                    prefs.edit().putString("save_path", newPath).apply()
                    tvSavePath.text = newPath
                    Toast.makeText(this, "保存先を変更しました: $newPath", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun setupWindowInsets() {
        val rootLayout = findViewById<View>(R.id.rootLayout)
        val controlPanel = findViewById<View>(R.id.controlPanel)
        // buttonContainer removed in new design

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply top padding to root or toolbar container if needed, but for now just safely ignore buttonContainer
            // rootLayout.updatePadding(top = insets.top) // Don't apply to root or BG moves
            
            controlPanel.updatePadding(bottom = insets.bottom)
            
            windowInsets
        }
    }

    private fun createDefaultThumbnail() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        
        paint.color = Color.RED
        canvas.drawRect(0f, 0f, 50f, 50f, paint)
        paint.color = Color.GREEN
        canvas.drawRect(50f, 0f, 100f, 50f, paint)
        paint.color = Color.BLUE
        canvas.drawRect(0f, 50f, 50f, 100f, paint)
        paint.color = Color.YELLOW
        canvas.drawRect(50f, 50f, 100f, 100f, paint)
        
        lutAdapter.setSourceBitmap(bitmap)
    }

    private fun setupLutList() {
        val brandList = findViewById<RecyclerView>(R.id.brandList)
        brandList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        
        val genreList = findViewById<RecyclerView>(R.id.genreList)
        genreList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val lutListView = findViewById<RecyclerView>(R.id.lutList)
        lutListView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        
        val brands = LutRepository.getLutBrands(this)
        
        // LUT Adapter (filters)
        lutAdapter = LutAdapter(emptyList(), this) { lutItem ->
            currentLutPath = lutItem.assetPath
            applyLut(lutItem.assetPath)
            
            // Show adjustment panel when LUT is selected
            if (!hasSelectedLut) {
                hasSelectedLut = true
                isAdjustmentPanelExpanded = true
                
                adjustmentHeader.visibility = View.VISIBLE
                adjustmentHeader.alpha = 0f
                adjustmentHeader.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
                
                sliderPanel.visibility = View.VISIBLE
                sliderPanel.alpha = 0f
                sliderPanel.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
                    
                adjustmentToggle.rotation = 0f
            }
            
            // Reset slider to 100%
            quickIntensitySlider.progress = 100
            quickIntensityValue.text = "100%"
            currentIntensity = 1f
            
        }
        lutListView.adapter = lutAdapter
        
        // Category Adapter (genres within brand)
        val genreAdapter = GenreAdapter(emptyList()) { selectedCategory ->
            lutAdapter.updateItems(selectedCategory.items)
        }
        genreList.adapter = genreAdapter

        // Brand Adapter (top-level tabs)
        val brandAdapter = BrandAdapter(brands) { selectedBrand ->
            genreAdapter.updateCategories(selectedBrand.categories)
        }
        brandList.adapter = brandAdapter
        
        // Auto-select first brand
        if (brands.isNotEmpty()) {
            brandAdapter.selectFirst()
        }
    }

    private fun applyLut(assetPath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val lut = CubeLUTParser.parse(this@MainActivity, assetPath)
            if (lut != null) {
                glSurfaceView.queueEvent {
                    renderer.setIntensity(currentIntensity)
                    renderer.setLut(lut)
                    glSurfaceView.requestRender()
                }
                // Show quick intensity panel
                withContext(Dispatchers.Main) {
                    if (quickIntensityPanel.visibility != View.VISIBLE) {
                        quickIntensityPanel.visibility = View.VISIBLE
                        quickIntensityPanel.alpha = 0f
                        quickIntensityPanel.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to load LUT", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveHighResImage() {
        val sourceBitmap = originalBitmap ?: return
        val lutPath = currentLutPath
        
        Toast.makeText(this, "書き出し中...", Toast.LENGTH_SHORT).show()
        
        // Parse LUT first (can be done off main thread)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val lut = if (lutPath != null) {
                    CubeLUTParser.parse(this@MainActivity, lutPath)
                } else null
                
                // Try GPU rendering first
                val outputBitmapHolder = arrayOfNulls<Bitmap>(1)
                val gpuErrorHolder = arrayOf<Exception?>(null)
                val latch = java.util.concurrent.CountDownLatch(1)
                
                glSurfaceView.queueEvent {
                    try {
                        if (gpuExportRenderer == null) {
                            gpuExportRenderer = GpuExportRenderer(this@MainActivity)
                        }
                        outputBitmapHolder[0] = gpuExportRenderer!!.renderHighRes(
                            sourceBitmap,
                            lut,
                            currentIntensity,
                            currentGrainEnabled,
                            currentGrainIntensity,
                            4.0f
                        )
                    } catch (e: Exception) {
                        gpuErrorHolder[0] = e
                        e.printStackTrace()
                    }
                    latch.countDown()
                }
                
                // Wait for GPU rendering to complete
                latch.await()
                
                var outputBitmap = outputBitmapHolder[0]
                
                // Fallback to CPU if GPU failed or returned null
                if (outputBitmap == null) {
                    android.util.Log.d("MainActivity", "GPU export failed or unavailable, falling back to CPU processing...")
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "CPUで処理中... (大きな画像は時間がかかります)", Toast.LENGTH_SHORT).show()
                    }
                    
                    // CPU fallback
                    outputBitmap = if (lut != null) {
                        HighResLutProcessor.applyLut(sourceBitmap, lut, currentIntensity)
                    } else {
                        sourceBitmap
                    }
                    // Note: Film grain is not applied in CPU fallback (GPU-only feature)
                }
                
                // Save with EXIF preservation
                saveBitmapWithExif(outputBitmap)
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "保存エラー: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun saveBitmapWithExif(bitmap: Bitmap) {
        val filename = "FilmSim_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, savePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "保存先の作成に失敗しました", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            // Write bitmap to a byte array first
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val imageBytes = outputStream.toByteArray()
            
            // Write to file
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(imageBytes)
            }
            
            // Copy EXIF data from original image to new image
            originalImageUri?.let { sourceUri ->
                try {
                    // Read EXIF from original
                    val sourceExif = contentResolver.openInputStream(sourceUri)?.use { input ->
                        ExifInterface(input)
                    }
                    
                    // Apply EXIF to saved file
                    resolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                        val destExif = ExifInterface(pfd.fileDescriptor)
                        
                        // Copy all EXIF attributes
                        val exifTags = arrayOf(
                            ExifInterface.TAG_DATETIME,
                            ExifInterface.TAG_DATETIME_ORIGINAL,
                            ExifInterface.TAG_DATETIME_DIGITIZED,
                            ExifInterface.TAG_MAKE,
                            ExifInterface.TAG_MODEL,
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.TAG_X_RESOLUTION,
                            ExifInterface.TAG_Y_RESOLUTION,
                            ExifInterface.TAG_EXPOSURE_TIME,
                            ExifInterface.TAG_F_NUMBER,
                            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                            ExifInterface.TAG_FOCAL_LENGTH,
                            ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
                            ExifInterface.TAG_GPS_LATITUDE,
                            ExifInterface.TAG_GPS_LATITUDE_REF,
                            ExifInterface.TAG_GPS_LONGITUDE,
                            ExifInterface.TAG_GPS_LONGITUDE_REF,
                            ExifInterface.TAG_GPS_ALTITUDE,
                            ExifInterface.TAG_GPS_ALTITUDE_REF,
                            ExifInterface.TAG_GPS_TIMESTAMP,
                            ExifInterface.TAG_GPS_DATESTAMP,
                            ExifInterface.TAG_WHITE_BALANCE,
                            ExifInterface.TAG_FLASH,
                            ExifInterface.TAG_METERING_MODE,
                            ExifInterface.TAG_EXPOSURE_PROGRAM,
                            ExifInterface.TAG_EXPOSURE_MODE,
                            ExifInterface.TAG_SCENE_TYPE,
                            ExifInterface.TAG_LENS_MAKE,
                            ExifInterface.TAG_LENS_MODEL,
                            ExifInterface.TAG_ARTIST,
                            ExifInterface.TAG_COPYRIGHT,
                            ExifInterface.TAG_SOFTWARE,
                            ExifInterface.TAG_IMAGE_DESCRIPTION
                        )
                        
                        sourceExif?.let { src ->
                            for (tag in exifTags) {
                                src.getAttribute(tag)?.let { value ->
                                    destExif.setAttribute(tag, value)
                                }
                            }
                            
                            // Add software tag to indicate processing
                            destExif.setAttribute(ExifInterface.TAG_SOFTWARE, "FilmSims LUT Editor")
                            
                            destExif.saveAttributes()
                        }
                    }
                } catch (e: Exception) {
                    // EXIF copy failed but image is still saved
                    e.printStackTrace()
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity, 
                    "保存しました (${bitmap.width}x${bitmap.height}): $savePath/$filename", 
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "保存エラー: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
