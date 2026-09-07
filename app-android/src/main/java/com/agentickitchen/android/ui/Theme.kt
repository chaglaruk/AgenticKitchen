package com.agentickitchen.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class AppColors(
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceSub: Color,
    val divider: Color,
    val surfaceAlt: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val success: Color
)

data class ThemeSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val colors: AppColors,
    val isLight: Boolean,
    val cornerRadius: Int,
    val compact: Boolean
)

private fun palette(
    primary: Color,
    primaryDark: Color,
    primaryLight: Color,
    accent: Color,
    background: Color,
    surface: Color,
    onPrimary: Color,
    onBackground: Color,
    onSurface: Color,
    onSurfaceSub: Color,
    divider: Color,
    surfaceAlt: Color,
    success: Color = accent
) = AppColors(
    primary, primaryDark, primaryLight, accent, background, surface, onPrimary,
    onBackground, onSurface, onSurfaceSub, divider, surfaceAlt, background, background, success
)

private val themeSpecs = mapOf(
    ThemePreference.MODERN_MINIMAL_A to ThemeSpec(
        ThemePreference.MODERN_MINIMAL_A.storageValue, "A — Modern Minimal", "Calm, clear kitchen control",
        palette(Color(0xFF347A54), Color(0xFF22583B), Color(0xFFB7D8C2), Color(0xFF6C9B79), Color(0xFFF7F9F6), Color.White, Color.White, Color(0xFF17201A), Color(0xFF17201A), Color(0xFF5B665E), Color(0xFFDDE6DF), Color(0xFFF0F5F1)), true, 18, false
    ),
    ThemePreference.PREMIUM_DARK_B to ThemeSpec(
        ThemePreference.PREMIUM_DARK_B.storageValue, "B — Premium Dark", "Layered, cinematic cooking",
        palette(Color(0xFFD88956), Color(0xFFA85D39), Color(0xFFF1B18A), Color(0xFFD88956), Color(0xFF11100F), Color(0xFF1D1A18), Color(0xFF17120F), Color(0xFFF8F1EA), Color(0xFFF8F1EA), Color(0xFFB9AAA0), Color(0xFF3C332D), Color(0xFF28221E)), false, 20, false
    ),
    ThemePreference.LUXE_APPLIANCE_DARK_K to ThemeSpec(
        ThemePreference.LUXE_APPLIANCE_DARK_K.storageValue, "K — Luxe Appliance Dark", "Precise appliance intelligence",
        palette(Color(0xFFA8D66D), Color(0xFF6B963D), Color(0xFFD8F0AE), Color(0xFFE8A34A), Color(0xFF0C1512), Color(0xFF14211C), Color(0xFF122014), Color(0xFFF2F8EF), Color(0xFFF2F8EF), Color(0xFFA8B8AB), Color(0xFF2C4035), Color(0xFF1A2C24)), false, 12, true
    ),
    ThemePreference.WARM_EDITORIAL_L to ThemeSpec(
        ThemePreference.WARM_EDITORIAL_L.storageValue, "L — Warm Editorial Utility", "Cookbook warmth, useful detail",
        palette(Color(0xFF9A5D43), Color(0xFF744331), Color(0xFFDDB09C), Color(0xFF657B58), Color(0xFFF5EFE5), Color(0xFFFFFBF5), Color.White, Color(0xFF211B17), Color(0xFF211B17), Color(0xFF6D6258), Color(0xFFE0D4C6), Color(0xFFFBF6EE)), true, 16, false
    ),
    ThemePreference.MINIMAL_PRO_M to ThemeSpec(
        ThemePreference.MINIMAL_PRO_M.storageValue, "M — Minimal Pro Control", "Fast, organized kitchen operations",
        palette(Color(0xFF356A9A), Color(0xFF234B70), Color(0xFFB7D1E8), Color(0xFF5D88B0), Color(0xFFF4F7FA), Color.White, Color.White, Color(0xFF17212B), Color(0xFF17212B), Color(0xFF53616D), Color(0xFFD8E1E8), Color(0xFFEDF2F6)), true, 10, true
    )
)

val ThemeCatalog = themeSpecs.values.toList()

fun themeSpec(themeName: String, systemDark: Boolean = false): ThemeSpec =
    themeSpecs.getValue(ThemePreference.resolve(themeName, systemDark))

val LocalAppColors = compositionLocalOf { themeSpec(ThemePreference.FOLLOW_SYSTEM.storageValue).colors }
val LocalThemeSpec = compositionLocalOf { themeSpec(ThemePreference.FOLLOW_SYSTEM.storageValue) }

@Composable
fun AgenticTheme(themeName: String, content: @Composable () -> Unit) {
    val spec = themeSpec(themeName, isSystemInDarkTheme())
    val colors = spec.colors
    val materialColors = androidx.compose.material.Colors(
        primary = colors.primary,
        primaryVariant = colors.primaryDark,
        secondary = colors.accent,
        secondaryVariant = colors.accent,
        background = colors.background,
        surface = colors.surface,
        error = Color(0xFF9B3F32),
        onPrimary = colors.onPrimary,
        onSecondary = colors.onPrimary,
        onBackground = colors.onBackground,
        onSurface = colors.onSurface,
        onError = Color.White,
        isLight = spec.isLight
    )

    CompositionLocalProvider(LocalAppColors provides colors, LocalThemeSpec provides spec) {
        MaterialTheme(colors = materialColors, typography = themeTypography(spec)) {
            Surface(modifier = Modifier.fillMaxSize(), color = colors.background) { content() }
        }
    }
}

private fun themeTypography(spec: ThemeSpec) = Typography(
    h1 = TextStyle(fontFamily = if (spec.compact) FontFamily.SansSerif else FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = if (spec.compact) 32.sp else 36.sp, letterSpacing = (-0.6).sp),
    h2 = TextStyle(fontFamily = if (spec.compact) FontFamily.SansSerif else FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 28.sp, letterSpacing = (-0.3).sp),
    h3 = TextStyle(fontFamily = if (spec.compact) FontFamily.SansSerif else FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 26.sp, letterSpacing = (-0.2).sp),
    h4 = TextStyle(fontFamily = if (spec.compact) FontFamily.SansSerif else FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 24.sp),
    h5 = TextStyle(fontFamily = if (spec.compact) FontFamily.SansSerif else FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    h6 = TextStyle(fontFamily = if (spec.compact) FontFamily.SansSerif else FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    body1 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    body2 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    subtitle1 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.5.sp),
    subtitle2 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = .2.sp),
    button = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 1.sp),
    caption = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = .5.sp),
    overline = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.1.sp)
)

@Composable
fun getBgGradient(): Brush {
    val colors = LocalAppColors.current
    return Brush.verticalGradient(listOf(colors.heroStart, colors.heroEnd))
}
