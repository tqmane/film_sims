package com.tqmane.filmsim.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tqmane.filmsim.R
import com.tqmane.filmsim.ui.theme.LiquidColors

// ═══════════════════════════════════════════════════════════════════════════════
// SETTINGS DIALOG — Modern glassmorphic Compose redesign
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val isProUser by authViewModel.isProUser.collectAsState()
    val isPermanentLicense by authViewModel.isPermanentLicense.collectAsState()
    val licenseMismatchVersion by authViewModel.licenseMismatchVersion.collectAsState()

    // Clamp quality for non-pro on open
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A1A22), Color(0xFF0A0A10))
                        )
                    )
                    .border(1.dp, Color(0x1CFFFFFF), RoundedCornerShape(32.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp)
            ) {

                // ─── Title row ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(LiquidColors.AccentPrimary.copy(alpha = 0.15f))
                            .border(1.dp, LiquidColors.AccentPrimary.copy(alpha = 0.25f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = null,
                            tint = LiquidColors.AccentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        stringResource(R.string.title_settings),
                        color = LiquidColors.TextHighEmphasis,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x16FFFFFF))
                            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.btn_close),
                            tint = LiquidColors.TextMediumEmphasis,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ─── Save Folder ─────────────────────────────────────────────
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
                            .border(
                                1.dp,
                                LiquidColors.AccentPrimary.copy(alpha = 0.35f),
                                RoundedCornerShape(10.dp)
                            )
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

                // ─── Quality ─────────────────────────────────────────────────
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

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Color(0x18FFFFFF), thickness = 1.dp)
                Spacer(Modifier.height(20.dp))

                // ─── Account ─────────────────────────────────────────────────
                SettingsSectionLabel(stringResource(R.string.label_account))
                Spacer(Modifier.height(12.dp))

                AnimatedContent(
                    targetState = authState.isSignedIn,
                    transitionSpec = { fadeIn(initialAlpha = 0f) togetherWith fadeOut(targetAlpha = 0f) },
                    label = "auth_content"
                ) { isSignedIn ->
                    if (isSignedIn) {
                        Column {
                            // User info card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x12FFFFFF))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar initial circle
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(LiquidColors.AccentPrimary.copy(alpha = 0.18f))
                                        .border(
                                            2.dp,
                                            LiquidColors.AccentPrimary.copy(alpha = 0.45f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        (authState.userName?.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                                        color = LiquidColors.AccentPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        authState.userName ?: "",
                                        color = LiquidColors.TextHighEmphasis,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        authState.userEmail ?: "",
                                        color = LiquidColors.TextLowEmphasis,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isProUser) {
                                    Spacer(Modifier.width(8.dp))
                                    val badgeText = if (isPermanentLicense)
                                        stringResource(R.string.label_license_permanent)
                                    else stringResource(R.string.label_pro)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        LiquidColors.GradientAccentStart,
                                                        LiquidColors.GradientAccentEnd
                                                    )
                                                )
                                            )
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            badgeText,
                                            color = Color(0xFF0C0C10),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // License mismatch warning banner
                            licenseMismatchVersion?.let { mismatchVer ->
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(LiquidColors.AccentPrimary.copy(alpha = 0.10f))
                                        .border(
                                            1.dp,
                                            LiquidColors.AccentPrimary.copy(alpha = 0.25f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⚠", color = LiquidColors.AccentPrimary, fontSize = 13.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.label_license_version_mismatch, mismatchVer),
                                        color = LiquidColors.AccentPrimary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // License purchase link
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LiquidColors.AccentPrimary.copy(alpha = 0.08f))
                                    .border(1.dp, LiquidColors.AccentPrimary.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { uriHandler.openUri("https://tqmane.booth.pm/") }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    stringResource(R.string.label_purchase_license),
                                    color = LiquidColors.AccentPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Sign Out button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x14FFFFFF))
                                    .border(1.dp, Color(0x1EFFFFFF), RoundedCornerShape(14.dp))
                                    .clickable { onSignOut() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.btn_sign_out),
                                    color = LiquidColors.TextMediumEmphasis,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Column {
                            Text(
                                stringResource(R.string.sign_in_description),
                                color = LiquidColors.TextLowEmphasis,
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                            // Google Sign-In button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                LiquidColors.GradientAccentStart,
                                                LiquidColors.GradientAccentEnd
                                            )
                                        )
                                    )
                                    .clickable { onSignIn() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.btn_sign_in_google),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ─── Close button ─────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x1EFFFFFF), RoundedCornerShape(16.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.btn_close),
                        color = LiquidColors.TextHighEmphasis,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // ─── Path edit sub-dialog ─────────────────────────────────────────────────
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

// ─── Section label helper ─────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(
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

// ─── Path edit dialog ─────────────────────────────────────────────────────────

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
                    Text(
                        stringResource(R.string.save_path_hint),
                        color = LiquidColors.TextLowEmphasis
                    )
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
                Text(
                    stringResource(R.string.save),
                    color = LiquidColors.AccentPrimary,
                    fontWeight = FontWeight.SemiBold
                )
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
