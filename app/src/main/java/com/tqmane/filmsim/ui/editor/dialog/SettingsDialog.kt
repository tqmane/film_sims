package com.tqmane.filmsim.ui.editor.dialog

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tqmane.filmsim.R
import com.tqmane.filmsim.ui.AuthViewModel
import com.tqmane.filmsim.ui.EditorViewModel
import com.tqmane.filmsim.ui.theme.LiquidColors

@Composable
fun SettingsDialog(
    viewModel: EditorViewModel,
    authViewModel: AuthViewModel,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val isProUser by authViewModel.isProUser.collectAsState()
    val isPermanentLicense by authViewModel.isPermanentLicense.collectAsState()
    val licenseMismatchVersion by authViewModel.licenseMismatchVersion.collectAsState()

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
                    .padding(28.dp)
            ) {
                // ─── Title row ───
                SettingsDialogHeader(onDismiss)

                Spacer(Modifier.height(24.dp))

                // ─── Save settings ───
                SaveSettingsSection(
                    viewModel = viewModel,
                    isProUser = isProUser
                )

                Spacer(Modifier.height(20.dp))
                androidx.compose.material3.HorizontalDivider(color = Color(0x18FFFFFF), thickness = 1.dp)
                Spacer(Modifier.height(20.dp))

                // ─── Account ───
                AccountSection(
                    authState = authState,
                    isProUser = isProUser,
                    isPermanentLicense = isPermanentLicense,
                    licenseMismatchVersion = licenseMismatchVersion,
                    onSignIn = onSignIn,
                    onSignOut = onSignOut
                )

                Spacer(Modifier.height(24.dp))

                // ─── Close button ───
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
}

@Composable
private fun SettingsDialogHeader(onDismiss: () -> Unit) {
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
                contentDescription = stringResource(R.string.title_settings),
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
}
