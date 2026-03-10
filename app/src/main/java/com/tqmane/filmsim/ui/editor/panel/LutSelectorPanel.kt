package com.tqmane.filmsim.ui.editor.panel

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tqmane.filmsim.ui.theme.LiquidColors
import com.tqmane.filmsim.R
import com.tqmane.filmsim.data.LutBrand
import com.tqmane.filmsim.data.LutItem
import com.tqmane.filmsim.gl.FilmSimRenderer
import com.tqmane.filmsim.ui.EditState
import com.tqmane.filmsim.ui.EditorViewModel
import com.tqmane.filmsim.ui.ViewState
import com.tqmane.filmsim.ui.WatermarkState
import com.tqmane.filmsim.ui.component.GlassBottomSheet
import com.tqmane.filmsim.ui.component.LiquidChip
import com.tqmane.filmsim.ui.component.LiquidNoticeCard
import com.tqmane.filmsim.ui.component.LiquidSectionHeader
import com.tqmane.filmsim.ui.component.LutPreviewCard

@Composable
fun LutSelectorPanel(
    viewModel: EditorViewModel,
    editState: EditState,
    watermarkState: WatermarkState,
    viewState: ViewState,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?,
    isWatermarkActive: Boolean,
    onRefreshWatermark: () -> Unit,
    onLutReselected: () -> Unit,
    showPanelHints: Boolean = true,
    isSelectingOverlay: Boolean = false,
    onCancelOverlaySelection: () -> Unit = {},
    onOverlaySelectionComplete: () -> Unit = {},
    isProUser: Boolean = false,
    selectedBrandIndex: Int = 0,
    onBrandIndexChanged: (Int) -> Unit = {},
    selectedCategoryIndex: Int = 0,
    onCategoryIndexChanged: (Int) -> Unit = {},
    squareTop: Boolean = false,
    modifier: Modifier = Modifier
) {
    GlassBottomSheet(
        modifier = modifier,
        squareTop = squareTop
    ) {
        BrandGenreLutSection(
            viewModel.brands, viewModel, viewState, editState, watermarkState,
            glSurfaceView, renderer, isWatermarkActive, onRefreshWatermark,
            onLutReselected = onLutReselected,
            showPanelHints = showPanelHints,
            isSelectingOverlay = isSelectingOverlay,
            onCancelOverlaySelection = onCancelOverlaySelection,
            onOverlaySelectionComplete = onOverlaySelectionComplete,
            isProUser = isProUser,
            selectedBrandIndex = selectedBrandIndex,
            onBrandIndexChanged = onBrandIndexChanged,
            selectedCategoryIndex = selectedCategoryIndex,
            onCategoryIndexChanged = onCategoryIndexChanged
        )
    }
}

@Composable
private fun BrandGenreLutSection(
    brands: List<LutBrand>,
    viewModel: EditorViewModel,
    viewState: ViewState,
    editState: EditState,
    watermarkState: WatermarkState,
    glSurfaceView: GLSurfaceView?,
    renderer: FilmSimRenderer?,
    isWatermarkActive: Boolean,
    onRefreshWatermark: () -> Unit,
    onLutReselected: () -> Unit,
    showPanelHints: Boolean = true,
    isSelectingOverlay: Boolean = false,
    onCancelOverlaySelection: () -> Unit = {},
    onOverlaySelectionComplete: () -> Unit = {},
    isProUser: Boolean = false,
    selectedBrandIndex: Int = 0,
    onBrandIndexChanged: (Int) -> Unit = {},
    selectedCategoryIndex: Int = 0,
    onCategoryIndexChanged: (Int) -> Unit = {}
) {
    val freeBrands = setOf("TECNO", "Nothing", "Nubia")
    val haptic = LocalHapticFeedback.current
    val licenseMessageResState = rememberSaveable {
        mutableIntStateOf(R.string.premium_brands_hint)
    }

    LaunchedEffect(isProUser, brands.size) {
        if (!isProUser && brands.isNotEmpty()) {
            val currentBrand = brands.getOrNull(selectedBrandIndex)
            if (currentBrand != null && currentBrand.name !in freeBrands) {
                val firstFreeIndex = brands.indexOfFirst { it.name in freeBrands }
                if (firstFreeIndex >= 0) {
                    onBrandIndexChanged(firstFreeIndex)
                    onCategoryIndexChanged(0)
                }
            }
        }
    }

    val categories = remember(selectedBrandIndex) { brands.getOrNull(selectedBrandIndex)?.categories.orEmpty() }
    val lutItems = remember(selectedBrandIndex, selectedCategoryIndex) { categories.getOrNull(selectedCategoryIndex)?.items.orEmpty() }
    val currentBrand = brands.getOrNull(selectedBrandIndex)
    val currentCategory = categories.getOrNull(selectedCategoryIndex)
    val selectedLutName = (if (isSelectingOverlay) editState.overlayLutPath else editState.currentLutPath)
        ?.substringAfterLast("/")
        ?.substringBeforeLast(".")
    val collectionLabel = if (categories.size > 1) {
        currentCategory?.displayName ?: stringResource(R.string.section_collections)
    } else {
        currentCategory?.displayName ?: stringResource(R.string.single_collection_label)
    }

    var brandScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    var categoryScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    var lutScrollIndex by rememberSaveable { mutableIntStateOf(0) }

    val brandListState = rememberLazyListState()
    val categoryListState = rememberLazyListState()

    LaunchedEffect(brandScrollIndex) {
        if (brandListState.firstVisibleItemIndex != brandScrollIndex) {
            brandListState.scrollToItem(brandScrollIndex.coerceAtMost((brands.size - 1).coerceAtLeast(0)))
        }
    }
    LaunchedEffect(selectedBrandIndex, categoryScrollIndex) {
        if (categoryListState.firstVisibleItemIndex != categoryScrollIndex) {
            categoryListState.scrollToItem(categoryScrollIndex.coerceAtMost((categories.size - 1).coerceAtLeast(0)))
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { brandListState.firstVisibleItemIndex }.collect { brandScrollIndex = it }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { categoryListState.firstVisibleItemIndex }.collect { categoryScrollIndex = it }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showPanelHints && !isSelectingOverlay) {
            LiquidNoticeCard(
                title = selectedLutName ?: (currentBrand?.displayName ?: stringResource(R.string.section_brands)),
                message = if (selectedLutName != null) {
                    stringResource(R.string.look_ready_hint)
                } else {
                    stringResource(R.string.look_preview_hint, lutItems.size)
                },
                label = collectionLabel,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (showPanelHints && !isProUser && !isSelectingOverlay) {
            LiquidNoticeCard(
                title = stringResource(R.string.more_brands_title),
                message = stringResource(licenseMessageResState.intValue),
                label = stringResource(R.string.label_pro),
                accentColor = LiquidColors.AccentSecondary,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }

        if (showPanelHints && isSelectingOverlay) {
            LiquidNoticeCard(
                title = stringResource(R.string.overlay_selection_title),
                message = stringResource(R.string.overlay_selection_hint),
                label = selectedLutName ?: stringResource(R.string.overlay_filter_none),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (isSelectingOverlay) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                item {
                    if (editState.overlayLutPath != null) {
                        LiquidChip(
                            text = stringResource(R.string.overlay_remove),
                            selected = false,
                            onClick = {
                                viewModel.clearOverlayLut()
                                if (!isWatermarkActive) {
                                    glSurfaceView?.let { glView ->
                                        glView.queueEvent {
                                            renderer?.setOverlayIntensity(0f)
                                            glView.requestRender()
                                        }
                                    }
                                }
                                onRefreshWatermark()
                            }
                        )
                    }
                }
                item {
                    LiquidChip(
                        text = stringResource(R.string.overlay_done),
                        selected = false,
                        onClick = onOverlaySelectionComplete
                    )
                }
                item {
                    LiquidChip(
                        text = stringResource(R.string.cancel),
                        selected = false,
                        onClick = onCancelOverlaySelection
                    )
                }
            }
        }

        LiquidSectionHeader(text = stringResource(R.string.section_brands))
        LazyRow(
            state = brandListState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            itemsIndexed(brands) { index, brand ->
                val isFree = brand.name in freeBrands
                LiquidChip(
                    text = if (!isFree && !isProUser) "${brand.displayName} 🔒" else brand.displayName,
                    selected = index == selectedBrandIndex,
                    onClick = {
                        if (!isFree && !isProUser) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            licenseMessageResState.intValue = R.string.pro_brand_locked
                            return@LiquidChip
                        }
                        licenseMessageResState.intValue = R.string.premium_brands_hint
                        onBrandIndexChanged(index)
                        onCategoryIndexChanged(0)
                        viewModel.updateWatermarkBrand(brand.name)
                    }
                )
            }
        }

        if (categories.size > 1) {
            LiquidSectionHeader(text = stringResource(R.string.section_collections))
            LazyRow(
                state = categoryListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                itemsIndexed(categories) { index, cat ->
                    LiquidChip(
                        text = cat.displayName,
                        selected = index == selectedCategoryIndex,
                        onClick = { onCategoryIndexChanged(index) }
                    )
                }
            }
        }

        LiquidSectionHeader(text = stringResource(R.string.section_looks))
        Text(
            text = stringResource(R.string.look_count_label, lutItems.size),
            color = LiquidColors.TextLowEmphasis,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }

    val isCurrentBrandLocked = currentBrand != null && currentBrand.name !in freeBrands && !isProUser
    LutRow(
        items = lutItems,
        thumbnailBitmap = (viewState as? ViewState.Content)?.thumbnailBitmap,
        onLutSelected = { item ->
            if (isCurrentBrandLocked) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                licenseMessageResState.intValue = R.string.pro_brand_locked
                return@LutRow
            }
            if (isSelectingOverlay) {
                viewModel.applyOverlayLut(item)
                if (!isWatermarkActive) {
                    glSurfaceView?.let { glView ->
                        glView.queueEvent {
                            renderer?.setOverlayIntensity(editState.overlayIntensity)
                            glView.requestRender()
                        }
                    }
                }
                onRefreshWatermark()
            } else if (item.assetPath == editState.currentLutPath) {
                onLutReselected()
            } else {
                viewModel.applyLut(item)
            }
        },
        currentLutPath = if (isSelectingOverlay) editState.overlayLutPath else editState.currentLutPath,
        savedScrollIndex = lutScrollIndex,
        onScrollIndexChanged = { lutScrollIndex = it }
    )
}

@Composable
private fun LutRow(
    items: List<LutItem>,
    thumbnailBitmap: Bitmap?,
    onLutSelected: (LutItem) -> Unit,
    currentLutPath: String?,
    savedScrollIndex: Int = 0,
    onScrollIndexChanged: (Int) -> Unit = {}
) {
    val lutListState = rememberLazyListState()
    LaunchedEffect(savedScrollIndex) {
        if (lutListState.firstVisibleItemIndex != savedScrollIndex) {
            lutListState.scrollToItem(savedScrollIndex.coerceAtMost((items.size - 1).coerceAtLeast(0)))
        }
    }
    LaunchedEffect(lutListState) {
        snapshotFlow { lutListState.firstVisibleItemIndex }.collect { onScrollIndexChanged(it) }
    }
    LazyRow(
        state = lutListState,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.height(130.dp)
    ) {
        itemsIndexed(items) { index, item ->
            LutPreviewCard(
                item = item,
                thumbnailBitmap = thumbnailBitmap,
                selected = item.assetPath == currentLutPath,
                onClick = { onLutSelected(item) }
            )
        }
    }
}
