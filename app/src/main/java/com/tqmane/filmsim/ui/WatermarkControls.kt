package com.tqmane.filmsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tqmane.filmsim.R
import com.tqmane.filmsim.ui.components.LiquidChip
import com.tqmane.filmsim.ui.theme.LiquidColors
import com.tqmane.filmsim.util.WatermarkProcessor
import com.tqmane.filmsim.util.WatermarkProcessor.WatermarkStyle

// ═══════════════════════════════════════════════════════════════════════════════
// WATERMARK CONTROLS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
internal fun LiquidWatermarkControls(
    watermarkState: WatermarkState,
    viewModel: EditorViewModel,
    onRefreshWatermark: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val defaultTime = remember { WatermarkProcessor.getDefaultTimeString() }
    val selectedBrand = watermarkState.brandName
    val selectedStyle = watermarkState.style

    var deviceName by remember(watermarkState.deviceName) { mutableStateOf(watermarkState.deviceName) }
    var timeText by remember(watermarkState.timeText) { mutableStateOf(watermarkState.timeText.ifEmpty { defaultTime }) }
    var locationText by remember(watermarkState.locationText) { mutableStateOf(watermarkState.locationText) }
    var lensInfo by remember(watermarkState.lensInfo) { mutableStateOf(watermarkState.lensInfo) }
    
    val honorStyles = listOf(R.string.watermark_frame to WatermarkStyle.FRAME, R.string.watermark_text to WatermarkStyle.TEXT, R.string.watermark_frame_yg to WatermarkStyle.FRAME_YG, R.string.watermark_text_yg to WatermarkStyle.TEXT_YG)
    val meizuStyles = listOf(R.string.meizu_norm to WatermarkStyle.MEIZU_NORM, R.string.meizu_pro to WatermarkStyle.MEIZU_PRO, R.string.meizu_z1 to WatermarkStyle.MEIZU_Z1, R.string.meizu_z2 to WatermarkStyle.MEIZU_Z2, R.string.meizu_z3 to WatermarkStyle.MEIZU_Z3, R.string.meizu_z4 to WatermarkStyle.MEIZU_Z4, R.string.meizu_z5 to WatermarkStyle.MEIZU_Z5, R.string.meizu_z6 to WatermarkStyle.MEIZU_Z6, R.string.meizu_z7 to WatermarkStyle.MEIZU_Z7)
    val vivoStyles = listOf(R.string.vivo_zeiss to WatermarkStyle.VIVO_ZEISS, R.string.vivo_classic to WatermarkStyle.VIVO_CLASSIC, R.string.vivo_pro to WatermarkStyle.VIVO_PRO, R.string.vivo_iqoo to WatermarkStyle.VIVO_IQOO, R.string.vivo_zeiss_v1 to WatermarkStyle.VIVO_ZEISS_V1, R.string.vivo_zeiss_sonnar to WatermarkStyle.VIVO_ZEISS_SONNAR, R.string.vivo_zeiss_humanity to WatermarkStyle.VIVO_ZEISS_HUMANITY, R.string.vivo_iqoo_v1 to WatermarkStyle.VIVO_IQOO_V1, R.string.vivo_iqoo_humanity to WatermarkStyle.VIVO_IQOO_HUMANITY, R.string.vivo_zeiss_frame to WatermarkStyle.VIVO_ZEISS_FRAME, R.string.vivo_zeiss_overlay to WatermarkStyle.VIVO_ZEISS_OVERLAY, R.string.vivo_zeiss_center to WatermarkStyle.VIVO_ZEISS_CENTER, R.string.vivo_frame to WatermarkStyle.VIVO_FRAME, R.string.vivo_frame_time to WatermarkStyle.VIVO_FRAME_TIME, R.string.vivo_iqoo_frame to WatermarkStyle.VIVO_IQOO_FRAME, R.string.vivo_iqoo_frame_time to WatermarkStyle.VIVO_IQOO_FRAME_TIME, R.string.vivo_os to WatermarkStyle.VIVO_OS, R.string.vivo_os_corner to WatermarkStyle.VIVO_OS_CORNER, R.string.vivo_os_simple to WatermarkStyle.VIVO_OS_SIMPLE, R.string.vivo_event to WatermarkStyle.VIVO_EVENT)
    val tecnoStyles = listOf(R.string.tecno_1 to WatermarkStyle.TECNO_1, R.string.tecno_2 to WatermarkStyle.TECNO_2, R.string.tecno_3 to WatermarkStyle.TECNO_3, R.string.tecno_4 to WatermarkStyle.TECNO_4)

    val availableStyles = when (selectedBrand) { "Honor" -> honorStyles; "Meizu" -> meizuStyles; "Vivo" -> vivoStyles; "TECNO" -> tecnoStyles; else -> emptyList() }

    val noDeviceStyles = setOf(WatermarkStyle.MEIZU_Z6, WatermarkStyle.MEIZU_Z7, WatermarkStyle.VIVO_OS_CORNER, WatermarkStyle.VIVO_OS_SIMPLE)
    val noLensStyles = setOf(WatermarkStyle.FRAME_YG, WatermarkStyle.TEXT_YG, WatermarkStyle.VIVO_CLASSIC, WatermarkStyle.VIVO_ZEISS_HUMANITY, WatermarkStyle.VIVO_IQOO_HUMANITY, WatermarkStyle.VIVO_FRAME, WatermarkStyle.VIVO_IQOO_FRAME, WatermarkStyle.VIVO_OS_CORNER, WatermarkStyle.VIVO_OS_SIMPLE, WatermarkStyle.TECNO_1)
    val noTimeStyles = setOf(WatermarkStyle.FRAME_YG, WatermarkStyle.TEXT_YG, WatermarkStyle.VIVO_ZEISS_HUMANITY, WatermarkStyle.VIVO_IQOO_HUMANITY, WatermarkStyle.VIVO_FRAME, WatermarkStyle.VIVO_IQOO_FRAME, WatermarkStyle.VIVO_OS_CORNER, WatermarkStyle.VIVO_OS_SIMPLE)
    
    val showFields = selectedStyle != WatermarkStyle.NONE
    val showDevice = showFields && selectedStyle !in noDeviceStyles
    val showLens   = showFields && selectedStyle !in noLensStyles
    val showTime   = showFields && selectedStyle !in noTimeStyles

    // Debounce text inputs to avoid GPU rendering lag on every keystroke
    LaunchedEffect(deviceName, timeText, locationText, lensInfo) {
        kotlinx.coroutines.delay(300)
        viewModel.updateWatermarkFields(
            deviceName = deviceName,
            timeText = timeText,
            locationText = locationText,
            lensInfo = lensInfo
        )
        onRefreshWatermark()
    }

    Column {
        Text(
            stringResource(R.string.header_watermark).uppercase(),
            color = LiquidColors.AccentPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.18.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_watermark),
                contentDescription = null,
                tint = LiquidColors.AccentSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.label_watermark_brand),
                color = LiquidColors.TextMediumEmphasis,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(end = 12.dp)
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(R.string.brand_none to "None", R.string.brand_honor to "Honor", R.string.brand_meizu to "Meizu", R.string.brand_vivo to "Vivo", R.string.brand_tecno to "TECNO").forEach { (labelRes, brand) ->
                    LiquidChip(
                        text = stringResource(labelRes),
                        selected = selectedBrand == brand,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.updateWatermarkBrand(brand)
                            if (brand == "None") viewModel.updateWatermarkStyle(WatermarkStyle.NONE)
                            else viewModel.updateWatermarkStyle(when(brand){"Honor"->honorStyles;"Meizu"->meizuStyles;"Vivo"->vivoStyles;"TECNO"->tecnoStyles;else->emptyList()}.firstOrNull()?.second ?: WatermarkStyle.NONE)
                            onRefreshWatermark()
                        }
                    )
                }
            }
        }
        
        if (availableStyles.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.label_watermark_style),
                    color = LiquidColors.TextMediumEmphasis,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableStyles.forEach { (labelRes, style) ->
                        LiquidChip(
                            text = stringResource(labelRes),
                            selected = selectedStyle == style,
                            onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.updateWatermarkStyle(style)
                                onRefreshWatermark()
                            }
                        )
                    }
                }
            }
        }
        
        if (showDevice) LiquidWatermarkInputRow(stringResource(R.string.label_watermark_device), deviceName) { deviceName = it }
        if (showLens) LiquidWatermarkInputRow(stringResource(R.string.label_watermark_lens), lensInfo) { lensInfo = it }
        if (showTime) {
            LiquidWatermarkInputRow(stringResource(R.string.label_watermark_time), timeText) { timeText = it }
            LiquidWatermarkInputRow(stringResource(R.string.label_watermark_location), locationText) { locationText = it }
        }
    }
}

@Composable
internal fun LiquidWatermarkInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = LiquidColors.TextLowEmphasis,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .weight(0.25f)
                .padding(end = 8.dp)
        )
        Box(
            modifier = Modifier
                .weight(0.75f)
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x10FFFFFF))
                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = LiquidColors.TextHighEmphasis
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(LiquidColors.AccentPrimary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            "—",
                            color = LiquidColors.TextDisabled,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    inner()
                }
            )
        }
    }
}
