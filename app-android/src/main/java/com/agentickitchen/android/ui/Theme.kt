package com.agentickitchen.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
    val isLight: Boolean
)

private val lightEditorialColors = AppColors(
    primary = Color(0xFFB7644C),
    primaryDark = Color(0xFF8F4E3D),
    primaryLight = Color(0xFFD7A18E),
    accent = Color(0xFF74806B),
    background = Color(0xFFF4F0E8),
    surface = Color(0xFFFAF7F1),
    onPrimary = Color.White,
    onBackground = Color(0xFF191714),
    onSurface = Color(0xFF191714),
    onSurfaceSub = Color(0xFF5F5951),
    divider = Color(0xFFD8D0C5),
    surfaceAlt = Color(0xFFFAF7F1),
    heroStart = Color(0xFFF4F0E8),
    heroEnd = Color(0xFFF4F0E8),
    success = Color(0xFF74806B)
)

private val darkEditorialColors = AppColors(
    primary = Color(0xFFD9866B),
    primaryDark = Color(0xFFB96952),
    primaryLight = Color(0xFFE4A18B),
    accent = Color(0xFF9AA58E),
    background = Color(0xFF151310),
    surface = Color(0xFF1D1A16),
    onPrimary = Color(0xFF191714),
    onBackground = Color(0xFFF4F0E8),
    onSurface = Color(0xFFF4F0E8),
    onSurfaceSub = Color(0xFFBDB4A8),
    divider = Color(0xFF3B352E),
    surfaceAlt = Color(0xFF24201B),
    heroStart = Color(0xFF151310),
    heroEnd = Color(0xFF151310),
    success = Color(0xFF9AA58E)
)

private val lightEditorial = ThemeSpec(
    id = "editorial-light",
    title = "Light Editorial",
    subtitle = "Warm paper cookbook",
    colors = lightEditorialColors,
    isLight = true
)

private val darkEditorial = ThemeSpec(
    id = "editorial-dark",
    title = "Dark Editorial",
    subtitle = "Low-luminance kitchen journal",
    colors = darkEditorialColors,
    isLight = false
)

val ThemeCatalog = listOf(lightEditorial, darkEditorial)

fun themeSpec(themeName: String): ThemeSpec =
    if (themeName == darkEditorial.id) darkEditorial else lightEditorial

val LocalAppColors = compositionLocalOf { themeSpec("editorial-light").colors }
val LocalThemeSpec = compositionLocalOf { themeSpec("editorial-light") }

@Composable
fun AgenticTheme(themeName: String, content: @Composable () -> Unit) {
    val spec = themeSpec(themeName)
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
        MaterialTheme(colors = materialColors, typography = editorialTypography()) {
            Surface(modifier = Modifier.fillMaxSize(), color = colors.background) { content() }
        }
    }
}

private fun editorialTypography() = Typography(
    h1 = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, letterSpacing = (-0.6).sp),
    h2 = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 28.sp, letterSpacing = (-0.3).sp),
    h3 = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 26.sp, letterSpacing = (-0.2).sp),
    h4 = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 24.sp),
    h5 = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    h6 = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
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
