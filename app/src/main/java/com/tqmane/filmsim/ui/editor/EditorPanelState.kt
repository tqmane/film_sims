package com.tqmane.filmsim.ui.editor

/**
 * Sealed interface representing the current panel state of the editor.
 * Replaces the scattered boolean flags (showAdjustPanel, isImmersive, etc.)
 */
sealed interface EditorPanelState {
    /** No panels visible, GL view fully visible */
    data object Hidden : EditorPanelState

    /** LUT selector bottom sheet visible */
    data object LutSelector : EditorPanelState

    /** Adjustment panel (Intensity / Grain / Watermark) visible */
    data object Adjustments : EditorPanelState

    /** Immersive mode — top bar and all panels hidden */
    data object Immersive : EditorPanelState
}

/**
 * Returns true when the top bar should be visible.
 */
val EditorPanelState.showTopBar: Boolean
    get() = this !is EditorPanelState.Immersive

/**
 * Returns true when either bottom panel is visible.
 */
val EditorPanelState.hasBottomPanel: Boolean
    get() = this is EditorPanelState.LutSelector || this is EditorPanelState.Adjustments
