package com.tqmane.filmsim.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.ui.theme.LiquidColors

/**
 * Application top bar with title and action buttons.
 */
@Composable
fun TopBar(
    onPickImage: () -> Unit,
    onSettings: () -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean = false,
    canCompare: Boolean = false,
    compareEnabled: Boolean = false,
    onCompareToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LiquidColors.SurfaceDark.copy(alpha = 0.65f),
                        Color(0xFF0C0C11).copy(alpha = 0.35f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.app_name),
                    color = LiquidColors.TextHighEmphasis,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.005.sp
                )
                Text(
                    stringResource(R.string.subtitle_film_simulator),
                    color = LiquidColors.AccentPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.1.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            LiquidRoundButton(
                iconRes = R.drawable.ic_add,
                contentDesc = stringResource(R.string.cd_add_photo),
                onClick = onPickImage,
                modifier = Modifier.padding(end = 8.dp)
            )

            LiquidRoundButton(
                iconRes = R.drawable.ic_settings,
                contentDesc = stringResource(R.string.cd_open_settings),
                onClick = onSettings,
                modifier = Modifier.padding(end = 8.dp)
            )

            if (canCompare) {
                LiquidRoundButton(
                    iconRes = R.drawable.ic_compare,
                    contentDesc = stringResource(R.string.compare_preview),
                    onClick = onCompareToggle,
                    tint = if (compareEnabled) LiquidColors.AccentPrimary else LiquidColors.TextMediumEmphasis,
                    modifier = Modifier.padding(end = 12.dp)
                )
            } else {
                Spacer(Modifier.width(4.dp))
            }

            LiquidButton(
                onClick = {
                    haptic.performHapticFeedback(
                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                    )
                    onSave()
                },
                enabled = canSave,
                modifier = Modifier.width(94.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.exporting),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_save),
                        contentDescription = stringResource(R.string.cd_save_image),
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        stringResource(R.string.save),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}
