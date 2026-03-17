package com.tqmane.filmsim.ui.editor

import androidx.compose.runtime.saveable.Saver
import com.tqmane.filmsim.ui.editor.panel.AdjustTab

/**
 * Sealed interface representing the current panel state of the editor.
 * Replaces the scattered boolean flags and the String-based panelState.
 *
 * [Adjustments] carries the active [AdjustTab] so tab state is never lost
 * when switching between panels.
 */
internal sealed interface EditorPanelState {
    /** No panels visible, GL view fully visible */
    data object Hidden : EditorPanelState

    /** LUT selector bottom sheet visible */
    data object LutSelector : EditorPanelState

    /** Adjustment panel visible with the given active tab */
    data class Adjustments(val tab: AdjustTab = AdjustTab.INTENSITY) : EditorPanelState

    /** Immersive mode — top bar and all panels hidden */
    data object Immersive : EditorPanelState
}

/** Returns true when the top bar should be visible. */
internal val EditorPanelState.showTopBar: Boolean
    get() = this !is EditorPanelState.Immersive

/** Returns true when either bottom panel is visible. */
internal val EditorPanelState.hasBottomPanel: Boolean
    get() = this is EditorPanelState.LutSelector || this is EditorPanelState.Adjustments

/**
 * [Saver] for [rememberSaveable] — encodes state as a string so it survives
 * configuration changes and process death.
 */
internal val EditorPanelStateSaver = Saver<EditorPanelState, String>(
    save = { state ->
        when (state) {
            is EditorPanelState.Hidden -> "Hidden"
            is EditorPanelState.LutSelector -> "LutSelector"
            is EditorPanelState.Adjustments -> "Adjustments:${state.tab.name}"
            is EditorPanelState.Immersive -> "Immersive"
        }
    },
    restore = { str ->
        when {
            str == "Hidden" -> EditorPanelState.Hidden
            str == "LutSelector" -> EditorPanelState.LutSelector
            str.startsWith("Adjustments:") -> {
                val tabName = str.substringAfter(":")
                val tab = runCatching { AdjustTab.valueOf(tabName) }.getOrElse { AdjustTab.INTENSITY }
                EditorPanelState.Adjustments(tab)
            }
            str == "Immersive" -> EditorPanelState.Immersive
            else -> EditorPanelState.LutSelector
        }
    }
)
