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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.ui.AuthState
import com.tqmane.filmsim.ui.theme.LiquidColors

@Composable
internal fun AccountSection(
    authState: AuthState,
    isProUser: Boolean,
    isPermanentLicense: Boolean,
    licenseMismatchVersion: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    SettingsSectionLabel(stringResource(R.string.label_account))
    Spacer(Modifier.height(12.dp))

    AnimatedContent(
        targetState = authState.isSignedIn,
        transitionSpec = { fadeIn(initialAlpha = 0f) togetherWith fadeOut(targetAlpha = 0f) },
        label = "auth_content"
    ) { isSignedIn ->
        if (isSignedIn) {
            SignedInContent(
                authState = authState,
                isProUser = isProUser,
                isPermanentLicense = isPermanentLicense,
                licenseMismatchVersion = licenseMismatchVersion,
                onSignOut = onSignOut
            )
        } else {
            SignedOutContent(onSignIn = onSignIn)
        }
    }
}

@Composable
private fun SignedInContent(
    authState: AuthState,
    isProUser: Boolean,
    isPermanentLicense: Boolean,
    licenseMismatchVersion: String?,
    onSignOut: () -> Unit
) {
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
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(LiquidColors.AccentPrimary.copy(alpha = 0.18f))
                    .border(2.dp, LiquidColors.AccentPrimary.copy(alpha = 0.45f), CircleShape),
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

        // License mismatch warning
        licenseMismatchVersion?.let { mismatchVer ->
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LiquidColors.AccentPrimary.copy(alpha = 0.10f))
                    .border(1.dp, LiquidColors.AccentPrimary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
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

        // Sign Out
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
}

@Composable
private fun SignedOutContent(onSignIn: () -> Unit) {
    Column {
        Text(
            stringResource(R.string.sign_in_description),
            color = LiquidColors.TextLowEmphasis,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(bottom = 14.dp)
        )
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
