package com.tqmane.filmsim.ui.editor.panel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.ui.EditorViewModel
import com.tqmane.filmsim.ui.Preset
import com.tqmane.filmsim.ui.component.LiquidNoticeCard
import com.tqmane.filmsim.ui.theme.LiquidColors

@Composable
internal fun PresetsTab(
    viewModel: EditorViewModel,
    showHints: Boolean,
    modifier: Modifier = Modifier
) {
    val presets by viewModel.presets.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (showHints) {
            LiquidNoticeCard(
                title = stringResource(R.string.presets_hint_title),
                message = stringResource(
                    if (presets.isEmpty()) R.string.presets_hint_body_empty else R.string.presets_hint_body_ready
                ),
                label = stringResource(R.string.tab_presets),
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showSaveDialog = true }) {
                Text(
                    stringResource(R.string.preset_save),
                    color = LiquidColors.AccentPrimary,
                    fontSize = 12.sp
                )
            }
        }

        if (presets.isEmpty()) {
            Text(
                stringResource(R.string.preset_empty),
                color = LiquidColors.TextMediumEmphasis,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(modifier = Modifier.height(160.dp)) {
                items(presets, key = { it.id }) { preset ->
                    PresetItem(
                        preset = preset,
                        lutDisplayName = viewModel.resolveLutDisplayName(preset.lutPath),
                        onLoad = { viewModel.loadPreset(preset) },
                        onDelete = { viewModel.deletePreset(preset.id) }
                    )
                }
            }
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.savePreset(name)
                showSaveDialog = false
            }
        )
    }
}

@Composable
private fun PresetItem(
    preset: Preset,
    lutDisplayName: String?,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onLoad)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                preset.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val lutName = preset.lutPath?.substringAfterLast("/")?.substringBeforeLast(".") ?: "—"
            Text(
                text = lutDisplayName ?: lutName,
                color = LiquidColors.TextMediumEmphasis,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_delete),
                contentDescription = stringResource(R.string.cd_delete_preset),
                tint = LiquidColors.TextMediumEmphasis,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.preset_save_title), color = Color.White) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = {
                    Text(
                        stringResource(R.string.preset_name_hint),
                        color = LiquidColors.TextMediumEmphasis
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = LiquidColors.AccentPrimary,
                    unfocusedBorderColor = LiquidColors.TextMediumEmphasis,
                    cursorColor = LiquidColors.AccentPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save), color = LiquidColors.AccentPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = LiquidColors.TextMediumEmphasis)
            }
        },
        containerColor = LiquidColors.SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    )
}
