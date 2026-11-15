package com.example.core.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.domain.models.AnimationMode
import com.example.domain.models.AppFontOption
import com.example.domain.models.ButtonStyle
import com.example.domain.models.CoverShape
import com.example.domain.models.MyatoTheme
import com.example.domain.models.PlayerAppearance

@Immutable
data class MyatoExtendedColors(
    val themeId: String,
    val themeName: String,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val altBackground: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isLight: Boolean,
    val coverShape: CoverShape,
    val buttonStyle: ButtonStyle,
    val animationMode: AnimationMode,
    val playerAppearance: PlayerAppearance,
) {
    fun resolveCoverShape(): Shape = when (coverShape) {
        CoverShape.ROUNDED -> RoundedCornerShape(16.dp)
        CoverShape.CIRCLE, CoverShape.VINYL -> CircleShape
        CoverShape.SHARP -> RoundedCornerShape(4.dp)
    }

    fun resolveSmallCoverShape(): Shape = when (coverShape) {
        CoverShape.ROUNDED -> RoundedCornerShape(10.dp)
        CoverShape.CIRCLE, CoverShape.VINYL -> CircleShape
        CoverShape.SHARP -> RoundedCornerShape(3.dp)
    }

    fun resolveButtonShape(): Shape = when (buttonStyle) {
        ButtonStyle.CIRCULAR -> CircleShape
        ButtonStyle.PILL -> RoundedCornerShape(24.dp)
        ButtonStyle.OUTLINED -> RoundedCornerShape(14.dp)
    }
}

val LocalMyatoColors = staticCompositionLocalOf {
    BuiltInThemes.MidnightOrange.toExtendedColors()
}

fun MyatoTheme.toExtendedColors(): MyatoExtendedColors = MyatoExtendedColors(
    themeId = id,
    themeName = name,
    primary = Color(primaryColorHex),
    secondary = Color(secondaryColorHex),
    background = Color(backgroundColorHex),
    altBackground = Color(altBackgroundColorHex),
    card = Color(cardColorHex),
    textPrimary = Color(textColorHex),
    textSecondary = Color(secondaryTextColorHex),
    isLight = isLight,
    coverShape = coverShape,
    buttonStyle = buttonStyle,
    animationMode = animationMode,
    playerAppearance = playerAppearance,
)

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
    content: @Composable () -> Unit,
) {
    val extended = activeTheme.toExtendedColors()
    val onPrimaryColor = if (extended.isLight) Color.White else Color.Black
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
            onPrimary = Color.White,
            primaryContainer = extended.primary.copy(alpha = 0.14f),
            onPrimaryContainer = extended.primary,
            secondary = extended.secondary,
            onSecondary = Color.White,
            background = extended.background,
            onBackground = extended.textPrimary,
            surface = extended.altBackground,
            onSurface = extended.textPrimary,
            surfaceVariant = extended.card,
            onSurfaceVariant = extended.textSecondary,
            outline = extended.textSecondary.copy(alpha = 0.25f),
            error = MyatoErrorRed,
        )
    } else {
        darkColorScheme(
            primary = extended.primary,
            onPrimary = onPrimaryColor,
            primaryContainer = extended.primary.copy(alpha = 0.18f),
            onPrimaryContainer = extended.primary,
            secondary = extended.secondary,
            onSecondary = onPrimaryColor,
            background = extended.background,
            onBackground = extended.textPrimary,
            surface = extended.altBackground,
            onSurface = extended.textPrimary,
            surfaceVariant = extended.card,
            onSurfaceVariant = extended.textSecondary,
            outline = extended.textSecondary.copy(alpha = 0.22f),
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
