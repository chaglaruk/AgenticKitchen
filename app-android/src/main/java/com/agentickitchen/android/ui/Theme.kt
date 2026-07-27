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
    val colors: AppColors
)

val PaletteGreen = AppColors(
    primary = Color(0xFF28A76A),
    primaryDark = Color(0xFF134D34),
    primaryLight = Color(0xFF4DD68C),
    accent = Color(0xFFFFC107),
    background = Color(0xFF0F1F15),
    surface = Color(0xFF172B1F),
    onPrimary = Color.White,
    onBackground = Color(0xFFE8F5E9),
    onSurface = Color(0xFFE8F5E9),
    onSurfaceSub = Color(0xFF9BBFAC),
    divider = Color(0xFF1F3829),
    surfaceAlt = Color(0xFF122319),
    heroStart = Color(0xFF183423),
    heroEnd = Color(0xFF0F1F15),
    success = Color(0xFF4DD68C)
)

val PaletteBlue = AppColors(
    primary = Color(0xFF3B82F6),
    primaryDark = Color(0xFF1E3A8A),
    primaryLight = Color(0xFF93C5FD),
    accent = Color(0xFFF59E0B),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onPrimary = Color.White,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceSub = Color(0xFF94A3B8),
    divider = Color(0xFF334155),
    surfaceAlt = Color(0xFF172032),
    heroStart = Color(0xFF1B376E),
    heroEnd = Color(0xFF0F172A),
    success = Color(0xFF38BDF8)
)

val PaletteOrange = AppColors(
    primary = Color(0xFFF97316),
    primaryDark = Color(0xFF7C2D12),
    primaryLight = Color(0xFFFDBA74),
    accent = Color(0xFF10B981),
    background = Color(0xFF2C1910),
    surface = Color(0xFF3F2314),
    onPrimary = Color.White,
    onBackground = Color(0xFFFFF7ED),
    onSurface = Color(0xFFFFF7ED),
    onSurfaceSub = Color(0xFFD6A282),
    divider = Color(0xFF5A3622),
    surfaceAlt = Color(0xFF342014),
    heroStart = Color(0xFF6C3414),
    heroEnd = Color(0xFF2C1910),
    success = Color(0xFF34D399)
)

val PaletteDark = AppColors(
    primary = Color(0xFFFACC15),
    primaryDark = Color(0xFFB45309),
    primaryLight = Color(0xFFFEF08A),
    accent = Color(0xFF14B8A6),
    background = Color(0xFF09090B),
    surface = Color(0xFF18181B),
    onPrimary = Color(0xFF09090B),
    onBackground = Color(0xFFFAFAFA),
    onSurface = Color(0xFFF4F4F5),
    onSurfaceSub = Color(0xFFA1A1AA),
    divider = Color(0xFF27272A),
    surfaceAlt = Color(0xFF111114),
    heroStart = Color(0xFF18181B),
    heroEnd = Color(0xFF09090B),
    success = Color(0xFF2DD4BF)
)

val PaletteHeritage = AppColors(
    primary = Color(0xFF040C21),
    primaryDark = Color(0xFF1A2238),
    primaryLight = Color(0xFFBEC6E3),
    accent = Color(0xFFA13C3F),
    background = Color(0xFFFCF9F8),
    surface = Color(0xFFFCF9F8),
    onPrimary = Color.White,
    onBackground = Color(0xFF1B1C1C),
    onSurface = Color(0xFF1B1C1C),
    onSurfaceSub = Color(0xFF45464D),
    divider = Color(0xFF76777E),
    surfaceAlt = Color(0xFFF5F2EA),
    heroStart = Color(0xFFF5F2EA),
    heroEnd = Color(0xFFFDFCFA),
    success = Color(0xFF687864)
)

val PaletteZen = AppColors(
    primary = Color(0xFF00450D),
    primaryDark = Color(0xFF1B5E20),
    primaryLight = Color(0xFF91D78A),
    accent = Color(0xFF91D78A),
    background = Color(0xFFFBF9F8),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF1B1C1C),
    onSurface = Color(0xFF1B1C1C),
    onSurfaceSub = Color(0xFF41493E),
    divider = Color(0xFFE0E0E0),
    surfaceAlt = Color(0xFFF5F3F3),
    heroStart = Color(0xFFFFFFFF),
    heroEnd = Color(0xFFF7F6F1),
    success = Color(0xFF2A6B2C)
)

val PaletteSignal = AppColors(
    primary = Color(0xFF0A5CFF),
    primaryDark = Color(0xFF072A75),
    primaryLight = Color(0xFF8CCBFF),
    accent = Color(0xFFFF6B35),
    background = Color(0xFFF5F7FB),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceSub = Color(0xFF526071),
    divider = Color(0xFFD6DEEA),
    surfaceAlt = Color(0xFFEFF4FF),
    heroStart = Color(0xFFE7EEFF),
    heroEnd = Color(0xFFFFF2EC),
    success = Color(0xFF00A66F)
)

val PaletteEditorial = AppColors(
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

val ThemeCatalog = listOf(
    ThemeSpec("editorial", "Editorial Kitchen", "Warm paper cookbook", PaletteEditorial),
    ThemeSpec("heritage", "Analog Heritage", "Editorial archive shell from Stitch", PaletteHeritage),
    ThemeSpec("zen", "Zen Precision", "Minimal culinary sanctuary from Stitch", PaletteZen),
    ThemeSpec("signal", "Signal Deck", "Original live-operations cockpit theme", PaletteSignal),
    ThemeSpec("green", "Midnight Green", "Legacy high-contrast premium theme", PaletteGreen),
    ThemeSpec("blue", "Ocean Blue", "Legacy cool tactical theme", PaletteBlue),
    ThemeSpec("orange", "Sunset Orange", "Legacy warm command theme", PaletteOrange),
    ThemeSpec("dark", "Premium Dark", "Legacy dark tactical theme", PaletteDark)
)

fun themeSpec(themeName: String): ThemeSpec {
    val key = themeName.lowercase()
    if (key in setOf("heritage", "zen", "signal", "green", "blue", "orange", "dark")) return ThemeCatalog.first { it.id == "editorial" }
    return ThemeCatalog.firstOrNull {
        when (it.id) {
            "green" -> key == "green" || key == "yeşil" || key == "yesil"
            "blue" -> key == "blue" || key == "mavi"
            "orange" -> key == "orange" || key == "turuncu"
            "dark" -> key == "dark" || key == "koyu"
            "zen" -> key == "zen" || key == "zen precision"
            else -> it.id == key
        }
    } ?: ThemeCatalog.first { it.id == "editorial" }
}

val LocalAppColors = compositionLocalOf { themeSpec("editorial").colors }
val LocalThemeSpec = compositionLocalOf { themeSpec("editorial") }

@Composable
fun AgenticTheme(themeName: String, content: @Composable () -> Unit) {
    val spec = themeSpec(themeName)
    val colors = spec.colors
    val typography = heritageTypography()

    val materialColors = androidx.compose.material.Colors(
        primary = colors.primary,
        primaryVariant = colors.primaryDark,
        secondary = colors.accent,
        secondaryVariant = colors.accent,
        background = colors.background,
        surface = colors.surface,
        error = Color(0xFFBA1A1A),
        onPrimary = colors.onPrimary,
        onSecondary = Color.White,
        onBackground = colors.onBackground,
        onSurface = colors.onSurface,
        onError = Color.White,
        isLight = true
    )

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalThemeSpec provides spec
    ) {
        MaterialTheme(
            colors = materialColors,
            typography = typography
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colors.background
            ) {
                content()
            }
        }
    }
}

private fun heritageTypography() = Typography(
    h1 = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, letterSpacing = (-0.6).sp),
    h2 = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 28.sp, letterSpacing = (-0.3).sp),
    h6 = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    body1 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    subtitle1 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.5.sp),
    button = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 1.sp),
    caption = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.2.sp)
)

private fun zenTypography() = Typography(
    h1 = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, letterSpacing = (-0.8).sp),
    h2 = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    h6 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
    body1 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    subtitle1 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    button = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.7.sp),
    caption = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.8.sp)
)

private fun signalTypography() = Typography(
    h1 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, letterSpacing = (-0.9).sp),
    h2 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.4).sp),
    h6 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    body1 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    subtitle1 = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.6.sp),
    button = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.1.sp),
    caption = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.sp)
)

@Composable
fun getBgGradient(): Brush {
    val c = LocalAppColors.current
    return Brush.verticalGradient(listOf(c.heroStart, c.heroEnd))
}
