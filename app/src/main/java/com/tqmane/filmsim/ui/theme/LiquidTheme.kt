package com.tqmane.filmsim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─── Composition Locals ───────────────────────────────────────────────

val LocalLiquidColors = staticCompositionLocalOf { LiquidColors }
val LocalLiquidTypography = staticCompositionLocalOf { LiquidTypography }
val LocalLiquidMotion = staticCompositionLocalOf { LiquidMotion }
val LocalLiquidDimensions = staticCompositionLocalOf { LiquidDimensions }

// ─── Backward compatibility ───────────────────────────────────────────
@Deprecated("Use LocalLiquidMotion", ReplaceWith("LocalLiquidMotion"))
val LocalLiquidAnimation = LocalLiquidMotion

// ─── Liquid Material Theme ────────────────────────────────────────────

private val LiquidDarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = LiquidColors.AccentPrimary,
    onPrimary = Color.Black,
    secondary = LiquidColors.AccentSecondary,
    onSecondary = Color.Black,
    tertiary = LiquidColors.AccentTertiary,
    onTertiary = Color.White,
    background = LiquidColors.Background,
    surface = LiquidColors.SurfaceDark,
    surfaceVariant = LiquidColors.SurfaceMedium,
    onBackground = LiquidColors.TextHighEmphasis,
    onSurface = LiquidColors.TextHighEmphasis,
    onSurfaceVariant = LiquidColors.TextMediumEmphasis,
    outline = LiquidColors.GlassBorder,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun LiquidTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LiquidDarkColorScheme,
        typography = androidx.compose.material3.Typography(
            displayLarge = LiquidTypography.DisplayLarge,
            displayMedium = LiquidTypography.DisplayMedium,
            headlineLarge = LiquidTypography.HeadlineLarge,
            headlineMedium = LiquidTypography.HeadlineMedium,
            titleLarge = LiquidTypography.TitleLarge,
            titleMedium = LiquidTypography.TitleMedium,
            bodyLarge = LiquidTypography.BodyLarge,
            bodyMedium = LiquidTypography.BodyMedium,
            labelLarge = LiquidTypography.LabelLarge,
            labelMedium = LiquidTypography.LabelMedium,
            labelSmall = LiquidTypography.LabelSmall
        ),
        content = content
    )
}
