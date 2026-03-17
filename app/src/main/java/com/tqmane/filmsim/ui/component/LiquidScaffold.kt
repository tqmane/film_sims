package com.tqmane.filmsim.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Standard Liquid screen scaffold:
 * AuroraBackground → [content] (GL surface / state overlays) → [topBar] / [bottomPanel] column.
 *
 * Usage: replace manual Box + AuroraBackground + Column pattern in EditorScreen.
 */
@Composable
fun LiquidScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomPanel: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        AuroraBackground(modifier = Modifier.fillMaxSize())

        // GL surface and overlays sit behind the control column
        Box(modifier = Modifier.fillMaxSize(), content = content)

        // Top bar + spacer + bottom panel
        Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            Spacer(modifier = Modifier.weight(1f))
            bottomPanel()
        }
    }
}
