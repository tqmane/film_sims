package com.tqmane.filmsim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Palette ────────────────────────────────────────────

val SurfaceDark    = Color(0xFF000000)
val SurfaceMedium  = Color(0xFF0A0A0E)
val SurfaceLight   = Color(0xFF121216)
val SurfaceElevated = Color(0xFF1A1A20)
val AccentPrimary  = Color(0xFFFFAB60)
val AccentDark     = Color(0xFFE07830)
val AccentSecondary = Color(0xFF50D0B8)
val TextPrimary    = Color(0xFFFFFFFF)
val TextSecondary  = Color(0xD8FFFFFF)
val TextTertiary   = Color(0x80FFFFFF)
val TextDisabled   = Color(0x40FFFFFF)
val ChipSelected   = Color(0xFFFFAB60)
val ChipSelectedText = Color(0xFF0C0C10)
val ChipUnselected = Color(0x28FFFFFF)
val GlassBg        = Color(0xF0101014)
val GlassBorder    = Color(0x20FFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.Black,
    secondary = AccentSecondary,
    onSecondary = Color.Black,
    background = SurfaceDark,
    surface = SurfaceMedium,
    surfaceVariant = SurfaceLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun FilmSimsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
