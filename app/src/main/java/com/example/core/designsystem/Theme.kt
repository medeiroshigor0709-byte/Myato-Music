package com.example.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.models.AnimationMode
import com.example.domain.models.AppFontOption
import com.example.domain.models.ButtonStyle
import com.example.domain.models.CoverShape
import com.example.domain.models.MyatoTheme
import com.example.domain.models.PlayerAppearance
import com.example.domain.models.ThemeAnimationType
import com.example.domain.models.ThemeBorderStyle
import com.example.domain.models.ThemeCategory
import com.example.domain.models.ThemeEffectPreset
import com.example.domain.models.ThemeGradientStyle
import com.example.domain.models.ThemeIconStyle
import com.example.domain.models.WallpaperStyle

@Immutable
data class MyatoExtendedColors(
    val themeId: String,
    val themeName: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val altBackground: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val buttonContainer: Color,
    val buttonContent: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
    val borderColor: Color,
    val gradientEnd: Color,
    val surfaceAlpha: Float,
    val backgroundAlpha: Float,
    val shadowElevationDp: Float,
    val borderWidthDp: Float,
    val isLight: Boolean,
    val coverShape: CoverShape,
    val buttonStyle: ButtonStyle,
    val animationMode: AnimationMode,
    val playerAppearance: PlayerAppearance,
    val gradientStyle: ThemeGradientStyle,
    val borderStyle: ThemeBorderStyle,
    val iconStyle: ThemeIconStyle,
    val effectPreset: ThemeEffectPreset,
    val wallpaperStyle: WallpaperStyle,
    val wallpaperPatternSeed: Int,
    val wallpaperImagePath: String?,
    val wallpaperDimAlpha: Float,
    val animationType: ThemeAnimationType,
    val category: ThemeCategory,
    val description: String,
    val isFeatured: Boolean,
    val seasonalTag: String?,
) {
    fun resolveCoverShape(): Shape = when {
        borderStyle == ThemeBorderStyle.PIXEL && coverShape == CoverShape.SHARP -> RoundedCornerShape(3.dp)
        coverShape == CoverShape.ROUNDED -> RoundedCornerShape(16.dp)
        coverShape == CoverShape.CIRCLE || coverShape == CoverShape.VINYL -> CircleShape
        else -> RoundedCornerShape(4.dp)
    }

    fun resolveSmallCoverShape(): Shape = when {
        borderStyle == ThemeBorderStyle.PIXEL && coverShape == CoverShape.SHARP -> RoundedCornerShape(2.dp)
        coverShape == CoverShape.ROUNDED -> RoundedCornerShape(10.dp)
        coverShape == CoverShape.CIRCLE || coverShape == CoverShape.VINYL -> CircleShape
        else -> RoundedCornerShape(3.dp)
    }

    fun resolveCardShape(): Shape = when {
        borderStyle == ThemeBorderStyle.PIXEL -> RoundedCornerShape(4.dp)
        coverShape == CoverShape.SHARP -> RoundedCornerShape(8.dp)
        else -> RoundedCornerShape(18.dp)
    }

    fun resolveButtonShape(): Shape = when {
        borderStyle == ThemeBorderStyle.PIXEL -> RoundedCornerShape(4.dp)
        buttonStyle == ButtonStyle.CIRCULAR -> CircleShape
        buttonStyle == ButtonStyle.PILL -> RoundedCornerShape(24.dp)
        else -> RoundedCornerShape(14.dp)
    }

    fun cardSurfaceColor(alphaMultiplier: Float = 1f): Color {
        val alpha = (surfaceAlpha * alphaMultiplier).coerceIn(0.35f, 1f)
        return card.copy(alpha = alpha)
    }

    fun altSurfaceColor(alphaMultiplier: Float = 1f): Color {
        val alpha = ((surfaceAlpha + 0.05f).coerceAtMost(1f) * alphaMultiplier).coerceIn(0.45f, 1f)
        return altBackground.copy(alpha = alpha)
    }

    fun borderStroke(highlighted: Boolean = false, widthBoostDp: Float = 0f): BorderStroke? {
        if (borderStyle == ThemeBorderStyle.NONE && !highlighted) return null
        val baseWidth = when (borderStyle) {
            ThemeBorderStyle.NONE -> if (highlighted) 1.4f else 0f
            ThemeBorderStyle.SUBTLE -> borderWidthDp.coerceIn(0.6f, 1.5f)
            ThemeBorderStyle.SOLID -> borderWidthDp.coerceIn(1f, 3f)
            ThemeBorderStyle.GLOW -> borderWidthDp.coerceIn(1.1f, 3.2f)
            ThemeBorderStyle.DOUBLE -> borderWidthDp.coerceIn(1.6f, 3.6f)
            ThemeBorderStyle.PIXEL -> borderWidthDp.coerceIn(1.5f, 3.2f)
        }
        val finalWidth = (baseWidth + widthBoostDp + if (highlighted) 0.6f else 0f).coerceAtLeast(0.75f)
        val alpha = when {
            highlighted -> 0.85f
            borderStyle == ThemeBorderStyle.SUBTLE -> if (isLight) 0.35f else 0.28f
            borderStyle == ThemeBorderStyle.GLOW -> 0.68f
            borderStyle == ThemeBorderStyle.PIXEL -> 0.82f
            borderStyle == ThemeBorderStyle.DOUBLE -> 0.65f
            else -> 0.50f
        }
        return BorderStroke(finalWidth.dp, borderColor.copy(alpha = alpha))
    }

    fun elevationDp(defaultDp: Dp = 0.dp): Dp {
        return shadowElevationDp.coerceAtLeast(defaultDp.value).dp
    }

    fun backgroundBrush(): Brush = when (gradientStyle) {
        ThemeGradientStyle.SOLID -> Brush.verticalGradient(listOf(background, background))
        ThemeGradientStyle.VERTICAL -> Brush.verticalGradient(listOf(background, altBackground, gradientEnd))
        ThemeGradientStyle.DIAGONAL -> Brush.linearGradient(
            colors = listOf(background, altBackground, gradientEnd),
            start = Offset(0f, 0f),
            end = Offset(900f, 1600f),
        )
        ThemeGradientStyle.RADIAL -> Brush.radialGradient(
            colors = listOf(gradientEnd, altBackground, background),
            radius = 1300f,
        )
    }
}

fun contrastContentColor(background: Color): Color {
    return if (background.luminance() > 0.44f) Color(0xFF0E0E12) else Color.White
}

val LocalMyatoColors = staticCompositionLocalOf {
    BuiltInThemes.MidnightOrange.toExtendedColors()
}

fun MyatoTheme.toExtendedColors(): MyatoExtendedColors {
    val primaryColor = Color(primaryColorHex)
    val secondaryColor = Color(secondaryColorHex)
    val accentColor = Color(accentColorHex)
    val buttonColor = Color(buttonColorHex)
    val iconColor = Color(iconColorHex)
    val borderCol = Color(borderColorHex)
    val gradEnd = Color(gradientEndColorHex)

    return MyatoExtendedColors(
        themeId = id,
        themeName = name,
        primary = primaryColor,
        secondary = secondaryColor,
        accent = accentColor,
        background = Color(backgroundColorHex),
        altBackground = Color(altBackgroundColorHex),
        card = Color(cardColorHex),
        textPrimary = Color(textColorHex),
        textSecondary = Color(secondaryTextColorHex),
        onPrimary = contrastContentColor(primaryColor),
        onSecondary = contrastContentColor(secondaryColor),
        buttonContainer = buttonColor,
        buttonContent = contrastContentColor(buttonColor),
        iconPrimary = iconColor,
        iconSecondary = Color(secondaryTextColorHex),
        borderColor = borderCol,
        gradientEnd = gradEnd,
        surfaceAlpha = surfaceAlpha.coerceIn(0.40f, 1f),
        backgroundAlpha = backgroundAlpha.coerceIn(0.50f, 1f),
        shadowElevationDp = shadowElevationDp.coerceIn(0f, 24f),
        borderWidthDp = borderWidthDp.coerceIn(0f, 4f),
        isLight = isLight,
        coverShape = coverShape,
        buttonStyle = buttonStyle,
        animationMode = animationMode,
        playerAppearance = playerAppearance,
        gradientStyle = gradientStyle,
        borderStyle = borderStyle,
        iconStyle = iconStyle,
        effectPreset = effectPreset,
        wallpaperStyle = wallpaperStyle,
        wallpaperPatternSeed = wallpaperPatternSeed,
        wallpaperImagePath = wallpaperImagePath,
        wallpaperDimAlpha = wallpaperDimAlpha.coerceIn(0f, 0.85f),
        animationType = animationType,
        category = category,
        description = description,
        isFeatured = isFeatured,
        seasonalTag = seasonalTag,
    )
}

private val MyatoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MyatoMusicTheme(
    activeTheme: MyatoTheme = BuiltInThemes.MidnightOrange,
    activeFontId: String = AppFontCatalog.DEFAULT_FONT_ID,
    activeGeneralFontId: String = activeFontId,
    activeDecorativeFontId: String = activeFontId,
    customFonts: List<AppFontOption> = emptyList(),
    animateTransitions: Boolean = false,
    content: @Composable () -> Unit,
) {
    val rawExtended = remember(activeTheme) { activeTheme.toExtendedColors() }
    val durationMs = if (animateTransitions && activeTheme.animationMode != AnimationMode.BATTERY_SAVER) 240 else 0

    val animPrimary by animateColorAsState(rawExtended.primary, tween(durationMs), label = "theme_primary")
    val animSecondary by animateColorAsState(rawExtended.secondary, tween(durationMs), label = "theme_secondary")
    val animAccent by animateColorAsState(rawExtended.accent, tween(durationMs), label = "theme_accent")
    val animBackground by animateColorAsState(rawExtended.background, tween(durationMs), label = "theme_bg")
    val animAltBackground by animateColorAsState(rawExtended.altBackground, tween(durationMs), label = "theme_alt_bg")
    val animCard by animateColorAsState(rawExtended.card, tween(durationMs), label = "theme_card")
    val animTextPrimary by animateColorAsState(rawExtended.textPrimary, tween(durationMs), label = "theme_text")
    val animTextSecondary by animateColorAsState(rawExtended.textSecondary, tween(durationMs), label = "theme_subtext")
    val animButtonContainer by animateColorAsState(rawExtended.buttonContainer, tween(durationMs), label = "theme_btn")
    val animIconPrimary by animateColorAsState(rawExtended.iconPrimary, tween(durationMs), label = "theme_icon")
    val animBorder by animateColorAsState(rawExtended.borderColor, tween(durationMs), label = "theme_border")
    val animGradEnd by animateColorAsState(rawExtended.gradientEnd, tween(durationMs), label = "theme_grad_end")

    val extended = if (durationMs == 0) {
        rawExtended
    } else {
        rawExtended.copy(
            primary = animPrimary,
            secondary = animSecondary,
            accent = animAccent,
            background = animBackground,
            altBackground = animAltBackground,
            card = animCard,
            textPrimary = animTextPrimary,
            textSecondary = animTextSecondary,
            onPrimary = contrastContentColor(animPrimary),
            onSecondary = contrastContentColor(animSecondary),
            buttonContainer = animButtonContainer,
            buttonContent = contrastContentColor(animButtonContainer),
            iconPrimary = animIconPrimary,
            iconSecondary = animTextSecondary,
            borderColor = animBorder,
            gradientEnd = animGradEnd,
        )
    }

    val resolvedTypography = remember(activeGeneralFontId, activeDecorativeFontId, customFonts) {
        AppFontCatalog.resolveDualTypography(
            generalFontId = activeGeneralFontId,
            decorativeFontId = activeDecorativeFontId,
            customFonts = customFonts,
        )
    }

    val colorScheme = if (extended.isLight) {
        lightColorScheme(
            primary = extended.primary,
            onPrimary = extended.onPrimary,
            primaryContainer = extended.primary.copy(alpha = 0.14f),
            onPrimaryContainer = extended.primary,
            secondary = extended.secondary,
            onSecondary = extended.onSecondary,
            tertiary = extended.accent,
            onTertiary = contrastContentColor(extended.accent),
            background = extended.background,
            onBackground = extended.textPrimary,
            surface = extended.altBackground,
            onSurface = extended.textPrimary,
            surfaceVariant = extended.card,
            onSurfaceVariant = extended.textSecondary,
            outline = extended.borderColor.copy(alpha = 0.35f),
            error = MyatoErrorRed,
        )
    } else {
        darkColorScheme(
            primary = extended.primary,
            onPrimary = extended.onPrimary,
            primaryContainer = extended.primary.copy(alpha = 0.18f),
            onPrimaryContainer = extended.primary,
            secondary = extended.secondary,
            onSecondary = extended.onSecondary,
            tertiary = extended.accent,
            onTertiary = contrastContentColor(extended.accent),
            background = extended.background,
            onBackground = extended.textPrimary,
            surface = extended.altBackground,
            onSurface = extended.textPrimary,
            surfaceVariant = extended.card,
            onSurfaceVariant = extended.textSecondary,
            outline = extended.borderColor.copy(alpha = 0.32f),
            error = MyatoErrorRed,
        )
    }

    CompositionLocalProvider(LocalMyatoColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = resolvedTypography,
            shapes = MyatoShapes,
            content = content,
        )
    }
}
