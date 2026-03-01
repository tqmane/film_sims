package com.tqmane.filmsim.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Liquid Design System — Motion Tokens
 *
 * アニメーション仕様と時間定数。
 */
object LiquidMotion {
    /** 粘性液体的なスプリング — ゆっくり沈み込む */
    val SpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    /** 高速マイクロインタラクション用 */
    val SpringSpecFast = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** 大きな移動向けのやわらかいスプリング */
    val SpringSpecGentle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessVeryLow
    )

    // Duration constants (ms)
    const val DurationQuick = 150
    const val DurationNormal = 300
    const val DurationSlow = 500
    const val DurationVerySlow = 800
}
