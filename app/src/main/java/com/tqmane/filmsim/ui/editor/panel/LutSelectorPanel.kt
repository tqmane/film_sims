package com.tqmane.filmsim.ui.editor.panel

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
        LiquidSectionHeader(stringResource(R.string.header_camera))
        BrandGenreLutSection(
            viewModel.brands, viewModel, viewState, editState, watermarkState,
            glSurfaceView, renderer, isWatermarkActive, onRefreshWatermark,
            onLutReselected = onLutReselected,
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
    isProUser: Boolean = false,
    selectedBrandIndex: Int = 0,
    onBrandIndexChanged: (Int) -> Unit = {},
    selectedCategoryIndex: Int = 0,
    onCategoryIndexChanged: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val freeBrands = setOf("TECNO", "Nothing", "Nubia")

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

    val brandScrollIndex by viewModel.brandScrollIndex.collectAsState()
    val categoryScrollIndex by viewModel.categoryScrollIndex.collectAsState()
    val lutScrollIndex by viewModel.lutScrollIndex.collectAsState()

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

    LaunchedEffect(brandListState) {
        snapshotFlow { brandListState.firstVisibleItemIndex }.collect { viewModel.setBrandScrollIndex(it) }
    }
    LaunchedEffect(categoryListState) {
        snapshotFlow { categoryListState.firstVisibleItemIndex }.collect { viewModel.setCategoryScrollIndex(it) }
    }

    // Brand chips
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
                        Toast.makeText(context, context.getString(R.string.pro_brand_locked), Toast.LENGTH_SHORT).show()
                        return@LiquidChip
                    }
                    onBrandIndexChanged(index)
                    onCategoryIndexChanged(0)
                    viewModel.updateWatermarkBrand(brand.name)
                }
            )
        }
    }

    LiquidSectionHeader(stringResource(R.string.header_style))

    // Category chips
    if (categories.isNotEmpty()) {
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

    LiquidSectionHeader(stringResource(R.string.header_presets))

    val currentBrand = brands.getOrNull(selectedBrandIndex)
    val isCurrentBrandLocked = currentBrand != null && currentBrand.name !in freeBrands && !isProUser
    LutRow(
        items = lutItems,
        thumbnailBitmap = (viewState as? ViewState.Content)?.thumbnailBitmap,
        onLutSelected = { item ->
            if (isCurrentBrandLocked) {
                Toast.makeText(context, context.getString(R.string.pro_brand_locked), Toast.LENGTH_SHORT).show()
                return@LutRow
            }
            if (item.assetPath == editState.currentLutPath) {
                onLutReselected()
            } else {
                viewModel.applyLut(item)
            }
        },
        currentLutPath = editState.currentLutPath,
        savedScrollIndex = lutScrollIndex,
        onScrollIndexChanged = { viewModel.setLutScrollIndex(it) }
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
