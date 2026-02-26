package com.tqmane.filmsim.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Liquid Color Palette ───────────────────────────────────────────

object LiquidColors {
    // Base Colors — warmer undertone for premium feel
    val Background = Color(0xFF060609)
    val SurfaceDark = Color(0xFF0C0C11)
    val SurfaceMedium = Color(0xFF141419)
    val SurfaceElevated = Color(0xFF1C1C23)
    
    // Glass Surface Colors — subtler for modern depth
    val GlassSurface = Color(0x1AFFFFFF)
    val GlassSurfaceDark = Color(0x10FFFFFF)
    val GlassBorder = Color(0x14FFFFFF)
    val GlassBorderHighlight = Color(0x20FFFFFF)
    
    // Accent Colors — kept identical per requirement
    val AccentPrimary = Color(0xFFFFAB60)
    val AccentPrimaryDark = Color(0xFFE07830)
    val AccentSecondary = Color(0xFF40C4B0)
    val AccentTertiary = Color(0xFF6200EE)
    
    // Ambient Light Colors (for background aurora)
    val AmbientAmber = Color(0xFFFFAB60)
    val AmbientCyan = Color(0xFF40C4B0)
    val AmbientPurple = Color(0xFF6200EE)
    
    // Text Colors — improved hierarchy
    val TextHighEmphasis = Color(0xFFF2F2F5)
    val TextMediumEmphasis = Color(0xBBFFFFFF)
    val TextLowEmphasis = Color(0x70FFFFFF)
    val TextDisabled = Color(0x38FFFFFF)
    
    // State Colors
    val ChipSelected = Color(0xFFFFAB60)
    val ChipSelectedText = Color(0xFF0C0C10)
    val ChipUnselected = Color(0x1CFFFFFF)
    
    // Gradient Colors
    val GradientAccentStart = Color(0xFFFF8A50)
    val GradientAccentEnd = Color(0xFFFFC06A)
    
    // Overlay Colors
    val Scrim = Color(0x80000000)
    val ScrimDark = Color(0xCC000000)
}

// ─── Liquid Typography ──────────────────────────────────────────────

object LiquidTypography {
    val DisplayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 57.sp,
        letterSpacing = (-0.25).sp
    )
    
    val DisplayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 45.sp,
        letterSpacing = 0.sp
    )
    
    val HeadlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        letterSpacing = 0.005.sp
    )
    
    val HeadlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = 0.005.sp
    )
    
    val TitleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.01.sp
    )
    
    val TitleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.01.sp
    )
    
    val BodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.03.sp
    )
    
    val BodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.02.sp
    )
    
    val LabelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.1.sp
    )
    
    val LabelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.12.sp
    )
    
    val LabelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.15.sp
    )
}

// ─── Liquid Animation Specs ──────────────────────────────────────────

object LiquidAnimation {
    /**
     * Physics-based spring animation with "viscous liquid" feel
     * Damping Ratio: 0.75f - Slightly bouncy but settles quickly
     * Stiffness: 200f - Slow, weighted movement
     */
    val SpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    /**
     * Faster spring for micro-interactions
     */
    val SpringSpecFast = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    /**
     * Gentle spring for large movements
     */
    val SpringSpecGentle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessVeryLow
    )
    
    // Animation durations
    const val DurationQuick = 150
    const val DurationNormal = 300
    const val DurationSlow = 500
    const val DurationVerySlow = 800
}

// ─── Liquid Dimensions ───────────────────────────────────────────────

object LiquidDimensions {
    // Corner radii — rounder for modern feel
    val CornerSmall = 10.dp
    val CornerMedium = 14.dp
    val CornerLarge = 18.dp
    val CornerXLarge = 22.dp
    val CornerRound = 28.dp
    val CornerCircle = 50.dp
    
    // Spacing — more breathing room
    val SpaceXS = 4.dp
    val SpaceS = 8.dp
    val SpaceM = 14.dp
    val SpaceL = 20.dp
    val SpaceXL = 28.dp
    val SpaceXXL = 40.dp
    
    // Component sizes — larger touch targets
    val ButtonHeight = 48.dp
    val ButtonHeightLarge = 56.dp
    val ChipHeight = 36.dp
    val IconSizeSmall = 16.dp
    val IconSizeMedium = 22.dp
    val IconSizeLarge = 28.dp
    
    // Glass panel
    val GlassBlurRadius = 30f
    val GlassElevation = 8.dp
}

// ─── Composition Locals ───────────────────────────────────────────────

val LocalLiquidColors = staticCompositionLocalOf { LiquidColors }
val LocalLiquidTypography = staticCompositionLocalOf { LiquidTypography }
val LocalLiquidAnimation = staticCompositionLocalOf { LiquidAnimation }
val LocalLiquidDimensions = staticCompositionLocalOf { LiquidDimensions }

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

