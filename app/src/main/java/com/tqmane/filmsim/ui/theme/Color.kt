package com.tqmane.filmsim.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Liquid Design System — Color Tokens
 *
 * 全ての色定義を集約。コンポーネントからは LiquidColors.xxx で参照する。
 */
object LiquidColors {
    // ─── Base Surface ─────────────────────────────────────────────
    val Background = Color(0xFF060609)
    val SurfaceDark = Color(0xFF0C0C11)
    val SurfaceMedium = Color(0xFF141419)
    val SurfaceElevated = Color(0xFF1C1C23)

    // ─── Glass ────────────────────────────────────────────────────
    val GlassSurface = Color(0x1AFFFFFF)
    val GlassSurfaceDark = Color(0x10FFFFFF)
    val GlassBorder = Color(0x14FFFFFF)
    val GlassBorderHighlight = Color(0x20FFFFFF)

    // ─── Accent ───────────────────────────────────────────────────
    val AccentPrimary = Color(0xFFFFAB60)
    val AccentPrimaryDark = Color(0xFFE07830)
    val AccentSecondary = Color(0xFF40C4B0)
    val AccentTertiary = Color(0xFF6200EE)

    // ─── Ambient (aurora background) ──────────────────────────────
    val AmbientAmber = Color(0xFFFFAB60)
    val AmbientCyan = Color(0xFF40C4B0)
    val AmbientPurple = Color(0xFF6200EE)

    // ─── Text ─────────────────────────────────────────────────────
    val TextHighEmphasis = Color(0xFFF2F2F5)
    val TextMediumEmphasis = Color(0xBBFFFFFF)
    val TextLowEmphasis = Color(0x70FFFFFF)
    val TextDisabled = Color(0x38FFFFFF)

    // ─── Chip ─────────────────────────────────────────────────────
    val ChipSelected = Color(0xFFFFAB60)
    val ChipSelectedText = Color(0xFF0C0C10)
    val ChipUnselected = Color(0x1CFFFFFF)

    // ─── Gradient ─────────────────────────────────────────────────
    val GradientAccentStart = Color(0xFFFF8A50)
    val GradientAccentEnd = Color(0xFFFFC06A)

    // ─── Overlay ──────────────────────────────────────────────────
    val Scrim = Color(0x80000000)
    val ScrimDark = Color(0xCC000000)
}
