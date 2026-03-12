package com.tqmane.filmsim.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Liquid Design System — Motion Tokens
 *
 * アニメーション仕様と時間定数。Web/CSSライクなリッチなイージングを含む。
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

    /** Webライクな弾性（バウンス）スプリング */
    val SpringSpecElastic = spring<Float>(
        dampingRatio = 0.6f, // Slightly bouncy
        stiffness = 400f     // Moderate stiffness
    )

    // リッチなWeb/CSSライクなイージング (Cubic Bezier)
    val EasingEmphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) // Material 3 Emphasized
    val EasingEmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val EasingEmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EasingLiquidBounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f) // カスタムバウンス
    val EasingSmoothFluid = CubicBezierEasing(0.25f, 1.0f, 0.25f, 1.0f) // Webのease-out互換

    // Duration constants (ms)
    const val DurationQuick = 150
    const val DurationNormal = 300
    const val DurationSlow = 500
    const val DurationVerySlow = 800
}
