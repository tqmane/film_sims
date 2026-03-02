package com.tqmane.filmsim.ui.editor.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.ui.EditorViewModel
import com.tqmane.filmsim.ui.theme.LiquidColors

@Composable
internal fun SaveSettingsSection(
    viewModel: EditorViewModel,
    isProUser: Boolean
) {
    val initialQuality = if (!isProUser && viewModel.settings.saveQuality > 60) 60
                         else viewModel.settings.saveQuality
    var qualityProgress by remember { mutableFloatStateOf(initialQuality.toFloat()) }
    var savePath by remember { mutableStateOf(viewModel.settings.savePath) }
    var showPathEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isProUser) {
        if (!isProUser && qualityProgress > 60f) {
            qualityProgress = 60f
            viewModel.settings.saveQuality = 60
        }
    }

    // ─── Save Folder ───
    SettingsSectionLabel(stringResource(R.string.label_save_folder))
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x12FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_save),
            contentDescription = null,
            tint = LiquidColors.TextLowEmphasis,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            savePath,
            color = LiquidColors.TextMediumEmphasis,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(LiquidColors.AccentPrimary.copy(alpha = 0.15f))
                .border(1.dp, LiquidColors.AccentPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .clickable { showPathEditDialog = true }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                stringResource(R.string.btn_change),
                color = LiquidColors.AccentPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    Spacer(Modifier.height(20.dp))

    // ─── Quality ───
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsSectionLabel(
            stringResource(R.string.label_quality),
            modifier = Modifier.weight(1f)
        )
        Text(
            "${qualityProgress.toInt()}%",
            color = LiquidColors.AccentPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
    Spacer(Modifier.height(2.dp))
    Slider(
        value = qualityProgress,
        onValueChange = { v ->
            val q = if (isProUser) v.coerceAtLeast(10f) else v.coerceIn(10f, 60f)
            qualityProgress = q
            viewModel.settings.saveQuality = q.toInt()
        },
        valueRange = 10f..100f,
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = LiquidColors.AccentPrimary,
            activeTrackColor = LiquidColors.AccentPrimary,
            inactiveTrackColor = Color(0x22FFFFFF)
        )
    )
    if (!isProUser) {
        Text(
            stringResource(R.string.pro_quality_limit),
            color = LiquidColors.TextLowEmphasis,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 4.dp)
        )
    }

    // ─── Path edit sub-dialog ───
    if (showPathEditDialog) {
        PathEditDialog(
            currentPath = savePath,
            onConfirm = { newPath ->
                if (newPath.isNotEmpty()) {
                    viewModel.settings.savePath = newPath
                    savePath = newPath
                }
                showPathEditDialog = false
            },
            onDismiss = { showPathEditDialog = false }
        )
    }
}

@Composable
internal fun SettingsSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text.uppercase(),
        color = LiquidColors.TextLowEmphasis,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.18.sp,
        modifier = modifier.padding(start = 2.dp)
    )
}

@Composable
private fun PathEditDialog(
    currentPath: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentPath) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.enter_save_folder),
                color = LiquidColors.TextHighEmphasis,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(stringResource(R.string.save_path_hint), color = LiquidColors.TextLowEmphasis)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LiquidColors.TextHighEmphasis,
                    unfocusedTextColor = LiquidColors.TextHighEmphasis,
                    focusedBorderColor = LiquidColors.AccentPrimary,
                    unfocusedBorderColor = Color(0x40FFFFFF),
                    cursorColor = LiquidColors.AccentPrimary,
                    focusedContainerColor = Color(0x0CFFFFFF),
                    unfocusedContainerColor = Color(0x08FFFFFF)
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) {
                Text(stringResource(R.string.save), color = LiquidColors.AccentPrimary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = LiquidColors.TextMediumEmphasis)
            }
        },
        containerColor = Color(0xFF181820),
        titleContentColor = LiquidColors.TextHighEmphasis,
        shape = RoundedCornerShape(24.dp)
    )
}
