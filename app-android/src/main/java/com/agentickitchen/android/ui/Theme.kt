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

enum class TypographyProfile {
    MODERN_SANS,
    PREMIUM_CINEMATIC,
    APPLIANCE_CONTROL,
    WARM_EDITORIAL,
    MINIMAL_PRO
}

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
    val success: Color,
    val accent2: Color = accent,
    val surface2: Color = surfaceAlt,
    val border: Color = divider,
    val warn: Color = accent,
    val danger: Color = Color(0xFFD34B3B),
    val ai: Color = primary,
    val aiBg: Color = surfaceAlt,
    val nav: Color = surface
)

data class ThemeSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val colors: AppColors,
    val isLight: Boolean,
    val cornerRadius: Int,
    val compact: Boolean,
    val typographyProfile: TypographyProfile,
    val dense: Boolean = compact
)

private fun palette(
    primary: Color,
    primaryDark: Color,
    primaryLight: Color,
    accent: Color,
    accent2: Color,
    background: Color,
    surface: Color,
    surface2: Color,
    text: Color,
    muted: Color,
    border: Color,
    success: Color,
    warn: Color,
    danger: Color,
    ai: Color,
    aiBg: Color,
    nav: Color,
    onPrimary: Color = if (surface.luminance() > 0.5f && background.luminance() > 0.5f) Color.White else Color(0xFF101419)
) = AppColors(
    primary = primary,
    primaryDark = primaryDark,
    primaryLight = primaryLight,
    accent = accent,
    background = background,
    surface = surface,
    onPrimary = onPrimary,
    onBackground = text,
    onSurface = text,
    onSurfaceSub = muted,
    divider = border,
    surfaceAlt = surface2,
    heroStart = background,
    heroEnd = background,
    success = success,
    accent2 = accent2,
    surface2 = surface2,
    border = border,
    warn = warn,
    danger = danger,
    ai = ai,
    aiBg = aiBg,
    nav = nav
)

private fun Color.luminance(): Float {
    val red = red
    val green = green
    val blue = blue
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

private val themeSpecs = mapOf(
    ThemePreference.MODERN_MINIMAL_A to ThemeSpec(
        id = ThemePreference.MODERN_MINIMAL_A.storageValue,
        title = "A — Modern Minimal",
        subtitle = "Calm, sharp, long-lived",
        colors = palette(
            primary = Color(0xFF0D7A54),
            primaryDark = Color(0xFF095238),
            primaryLight = Color(0xFFB7D8C2),
            accent = Color(0xFF0D7A54),
            accent2 = Color(0xFFC75D3B),
            background = Color(0xFFF8F9F7),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFEEF6F1),
            text = Color(0xFF121514),
            muted = Color(0xFF68706C),
            border = Color(0xFFDFE5E1),
            success = Color(0xFF137B58),
            warn = Color(0xFFD86432),
            danger = Color(0xFFD34B3B),
            ai = Color(0xFF526BD8),
            aiBg = Color(0xFFEEF1FF),
            nav = Color(0xFFFBFCFA),
            onPrimary = Color.White
        ),
        isLight = true,
        cornerRadius = 22,
        compact = false,
        typographyProfile = TypographyProfile.MODERN_SANS,
        dense = false
    ),
    ThemePreference.PREMIUM_DARK_B to ThemeSpec(
        id = ThemePreference.PREMIUM_DARK_B.storageValue,
        title = "B — Premium Dark",
        subtitle = "Layered, cinematic cooking",
        colors = palette(
            primary = Color(0xFFEE934C),
            primaryDark = Color(0xFFC26E2E),
            primaryLight = Color(0xFFF4B382),
            accent = Color(0xFFEE934C),
            accent2 = Color(0xFF53C98C),
            background = Color(0xFF07090B),
            surface = Color(0xFF101419),
            surface2 = Color(0xFF13231C),
            text = Color(0xFFF7F7F5),
            muted = Color(0xFF9CA3AC),
            border = Color(0xFF293039),
            success = Color(0xFF56D29A),
            warn = Color(0xFFF2A14F),
            danger = Color(0xFFEF6D57),
            ai = Color(0xFFB77CF0),
            aiBg = Color(0xFF211827),
            nav = Color(0xFF0C1014),
            onPrimary = Color(0xFF101419)
        ),
        isLight = false,
        cornerRadius = 24,
        compact = false,
        typographyProfile = TypographyProfile.PREMIUM_CINEMATIC,
        dense = true
    ),
    ThemePreference.LUXE_APPLIANCE_DARK_K to ThemeSpec(
        id = ThemePreference.LUXE_APPLIANCE_DARK_K.storageValue,
        title = "K — Luxe Appliance Dark",
        subtitle = "Precise appliance intelligence",
        colors = palette(
            primary = Color(0xFFF1A54F),
            primaryDark = Color(0xFFC87E2C),
            primaryLight = Color(0xFFF7C78B),
            accent = Color(0xFF57C989),
            accent2 = Color(0xFF57C989),
            background = Color(0xFF05100E),
            surface = Color(0xFF0B1815),
            surface2 = Color(0xFF0D2A1E),
            text = Color(0xFFF5F1E7),
            muted = Color(0xFFAAB2AA),
            border = Color(0xFF27433A),
            success = Color(0xFF5AD08B),
            warn = Color(0xFFF1AA4C),
            danger = Color(0xFFE66E55),
            ai = Color(0xFFF0A64A),
            aiBg = Color(0xFF16251F),
            nav = Color(0xFF07120F),
            onPrimary = Color(0xFF0B1815)
        ),
        isLight = false,
        cornerRadius = 22,
        compact = true,
        typographyProfile = TypographyProfile.APPLIANCE_CONTROL,
        dense = true
    ),
    ThemePreference.WARM_EDITORIAL_L to ThemeSpec(
        id = ThemePreference.WARM_EDITORIAL_L.storageValue,
        title = "L — Warm Editorial Utility",
        subtitle = "Cookbook warmth, useful detail",
        colors = palette(
            primary = Color(0xFF13764E),
            primaryDark = Color(0xFF0C5236),
            primaryLight = Color(0xFFA8DEC7),
            accent = Color(0xFFCE6248),
            accent2 = Color(0xFFCE6248),
            background = Color(0xFFFFF9EF),
            surface = Color(0xFFFFFDFC),
            surface2 = Color(0xFFEEF6E9),
            text = Color(0xFF2B211C),
            muted = Color(0xFF756961),
            border = Color(0xFFE8DDD1),
            success = Color(0xFF247C58),
            warn = Color(0xFFC9673A),
            danger = Color(0xFFD65245),
            ai = Color(0xFF6D62D4),
            aiBg = Color(0xFFF0ECFF),
            nav = Color(0xFFFFFDF8),
            onPrimary = Color.White
        ),
        isLight = true,
        cornerRadius = 24,
        compact = false,
        typographyProfile = TypographyProfile.WARM_EDITORIAL,
        dense = false
    ),
    ThemePreference.MINIMAL_PRO_M to ThemeSpec(
        id = ThemePreference.MINIMAL_PRO_M.storageValue,
        title = "M — Minimal Pro Control",
        subtitle = "Fast, organized kitchen operations",
        colors = palette(
            primary = Color(0xFF0B7A56),
            primaryDark = Color(0xFF07553C),
            primaryLight = Color(0xFF8FE0C4),
            accent = Color(0xFFD46648),
            accent2 = Color(0xFFD46648),
            background = Color(0xFFF7FAFC),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFEDF8F3),
            text = Color(0xFF101820),
            muted = Color(0xFF5E6B78),
            border = Color(0xFFDDE4EC),
            success = Color(0xFF187A59),
            warn = Color(0xFFC9822F),
            danger = Color(0xFFD94E47),
            ai = Color(0xFF4A70E8),
            aiBg = Color(0xFFEDF2FF),
            nav = Color(0xFFF9FBFD),
            onPrimary = Color.White
        ),
        isLight = true,
        cornerRadius = 20,
        compact = true,
        typographyProfile = TypographyProfile.MINIMAL_PRO,
        dense = true
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
        error = colors.danger,
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

private fun themeTypography(spec: ThemeSpec): Typography {
    val displayFont = when (spec.typographyProfile) {
        TypographyProfile.MODERN_SANS, TypographyProfile.MINIMAL_PRO -> FontFamily.SansSerif
        TypographyProfile.PREMIUM_CINEMATIC, TypographyProfile.APPLIANCE_CONTROL, TypographyProfile.WARM_EDITORIAL -> FontFamily.Serif
    }
    val h1Size = when (spec.typographyProfile) {
        TypographyProfile.MINIMAL_PRO -> 30.sp
        TypographyProfile.APPLIANCE_CONTROL -> 32.sp
        TypographyProfile.MODERN_SANS -> 34.sp
        TypographyProfile.PREMIUM_CINEMATIC -> 36.sp
        TypographyProfile.WARM_EDITORIAL -> 38.sp
    }
    val h1Spacing = when (spec.typographyProfile) {
        TypographyProfile.MINIMAL_PRO -> (-0.8).sp
        TypographyProfile.APPLIANCE_CONTROL -> (-0.4).sp
        TypographyProfile.MODERN_SANS -> (-0.6).sp
        else -> (-0.3).sp
    }
    return Typography(
        h1 = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Bold, fontSize = h1Size, letterSpacing = h1Spacing),
        h2 = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.3).sp),
        h3 = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Medium, fontSize = 24.sp, letterSpacing = (-0.2).sp),
        h4 = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Medium, fontSize = 22.sp),
        h5 = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        h6 = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
        body1 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
        body2 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        subtitle1 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.4.sp),
        subtitle2 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.2.sp),
        button = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.8.sp),
        caption = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.4.sp),
        overline = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = if (spec.typographyProfile == TypographyProfile.APPLIANCE_CONTROL) 1.6.sp else 1.1.sp
        )
    )
}

@Composable
fun getBgGradient(): Brush {
    val colors = LocalAppColors.current
    return Brush.verticalGradient(listOf(colors.heroStart, colors.heroEnd))
}
