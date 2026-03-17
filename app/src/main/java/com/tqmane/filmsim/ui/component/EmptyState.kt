package com.tqmane.filmsim.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.ui.theme.LiquidColors

/**
 * Placeholder content shown when no image is loaded.
 */
@Composable
fun EmptyState(
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(56.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(LiquidColors.AccentPrimary.copy(alpha = 0.12f))
            )
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0x22FFFFFF),
                                Color(0x10FFFFFF)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        LiquidColors.AccentPrimary.copy(alpha = 0.28f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_photo),
                    contentDescription = stringResource(R.string.cd_pick_image),
                    tint = LiquidColors.AccentPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Text(
            stringResource(R.string.label_pick_image),
            color = LiquidColors.TextHighEmphasis,
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.005.sp,
            modifier = Modifier.padding(top = 32.dp)
        )

        Text(
            stringResource(R.string.desc_pick_image),
            color = LiquidColors.TextLowEmphasis,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = stringResource(R.string.workflow_title).uppercase(),
            color = LiquidColors.AccentPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.12.sp,
            modifier = Modifier.padding(top = 28.dp, bottom = 10.dp)
        )

        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WorkflowStep(stepNumber = 1, text = stringResource(R.string.workflow_step_import))
            WorkflowStep(stepNumber = 2, text = stringResource(R.string.workflow_step_choose))
            WorkflowStep(stepNumber = 3, text = stringResource(R.string.workflow_step_refine_save))
        }

        LiquidButton(
            onClick = {
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                )
                onPickImage()
            },
            modifier = Modifier
                .padding(top = 32.dp)
                .height(58.dp)
                .widthIn(min = 220.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gallery),
                contentDescription = stringResource(R.string.btn_open_gallery),
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.btn_open_gallery),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.01.sp
            )
        }
    }
}

@Composable
private fun WorkflowStep(
    stepNumber: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(LiquidColors.AccentPrimary.copy(alpha = 0.16f))
                .border(1.dp, LiquidColors.AccentPrimary.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber.toString(),
                color = LiquidColors.AccentPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = LiquidColors.TextMediumEmphasis,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            lineHeight = 18.sp
        )
    }
}
