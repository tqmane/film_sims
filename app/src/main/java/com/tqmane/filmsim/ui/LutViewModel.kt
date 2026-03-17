package com.tqmane.filmsim.ui

import androidx.lifecycle.ViewModel
import com.tqmane.filmsim.data.LutBrand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Manages LUT browser UI state (brand/category selection).
 * Deliberately lightweight — LUT application itself stays in [EditorViewModel]
 * since it mutates [EditState].
 */
@HiltViewModel
class LutViewModel @Inject constructor(
    val brands: List<LutBrand>
) : ViewModel() {

    private val _selectedBrandIndex = MutableStateFlow(0)
    val selectedBrandIndex: StateFlow<Int> = _selectedBrandIndex.asStateFlow()

    private val _selectedCategoryIndex = MutableStateFlow(0)
    val selectedCategoryIndex: StateFlow<Int> = _selectedCategoryIndex.asStateFlow()

    fun setSelectedBrandIndex(index: Int) {
        _selectedBrandIndex.value = index
    }

    fun setSelectedCategoryIndex(index: Int) {
        _selectedCategoryIndex.value = index
    }
}
