package com.tqmane.filmsim.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.data.LutItem
import com.tqmane.filmsim.ui.theme.LiquidColors
import com.tqmane.filmsim.util.CubeLUTParser
import com.tqmane.filmsim.util.LutBitmapProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Liquid-style card for LUT preview items with async thumbnail generation.
 */
@Composable
fun LutPreviewCard(
    item: LutItem,
    thumbnailBitmap: Bitmap?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var previewBitmap by remember(item.assetPath, thumbnailBitmap) {
        mutableStateOf<Bitmap?>(null)
    }
    var isLoadingPreview by remember(item.assetPath, thumbnailBitmap) {
        mutableStateOf(thumbnailBitmap != null)
    }
    LaunchedEffect(item.assetPath, thumbnailBitmap) {
        if (thumbnailBitmap != null) {
            isLoadingPreview = true
            previewBitmap = withContext(Dispatchers.IO) {
                try {
                    val lut = CubeLUTParser.parse(context, item.assetPath)
                    if (lut != null) LutBitmapProcessor.applyLutToBitmap(thumbnailBitmap, lut) else null
                } catch (e: Exception) { null }
            }
            isLoadingPreview = false
        } else {
            previewBitmap = null
            isLoadingPreview = false
        }
    }

    val borderColor by animateColorAsState(
        targetValue = if (selected) LiquidColors.AccentPrimary else Color.Transparent,
        animationSpec = tween(300),
        label = "card_border"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(start = 2.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)
            .clickable {
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                )
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .size(94.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LiquidColors.SurfaceMedium)
                .then(
                    if (selected) {
                        Modifier.border(2.5.dp, borderColor, RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    }
                )
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.5f)
                )
            }

            if (isLoadingPreview) {
                val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
                val shimmerAlpha by shimmerTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = 0.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "shimmer_alpha"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LiquidColors.GlassSurface.copy(alpha = shimmerAlpha))
                )
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    LiquidColors.AccentPrimary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.60f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(13.dp)) {
                            val w = size.width
                            val h = size.height
                            val stroke = 1.6.dp.toPx()
                            val knobR = 2.4.dp.toPx()
                            drawLine(
                                color = Color.White,
                                start = Offset(0f, h * 0.28f),
                                end = Offset(w, h * 0.28f),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = Color.White, radius = knobR, center = Offset(w * 0.68f, h * 0.28f))
                            drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = knobR * 0.45f, center = Offset(w * 0.68f, h * 0.28f))
                            drawLine(
                                color = Color.White,
                                start = Offset(0f, h * 0.72f),
                                end = Offset(w, h * 0.72f),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = Color.White, radius = knobR, center = Offset(w * 0.32f, h * 0.72f))
                            drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = knobR * 0.45f, center = Offset(w * 0.32f, h * 0.72f))
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = stringResource(R.string.adjustments),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.sp
                        )
                    }
                }
            }
        }

        Text(
            item.name,
            color = if (selected) LiquidColors.TextHighEmphasis else LiquidColors.TextMediumEmphasis,
            fontSize = 10.sp,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.01.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Clip,
            lineHeight = 12.sp,
            modifier = Modifier
                .width(94.dp)
                .padding(top = 6.dp, bottom = 2.dp)
        )
    }
}
