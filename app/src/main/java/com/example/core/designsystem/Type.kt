package com.example.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold),
)

val PlusJakartaFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
)

private fun singleResourceFontFamily(resId: Int): FontFamily = FontFamily(
    Font(resId, FontWeight.Light),
    Font(resId, FontWeight.Normal),
    Font(resId, FontWeight.Medium),
    Font(resId, FontWeight.SemiBold),
    Font(resId, FontWeight.Bold),
)

// Minimalistas & Modernas
val RobotoFontFamily = singleResourceFontFamily(R.font.roboto)
val InterFontFamily = singleResourceFontFamily(R.font.inter)
val PoppinsFontFamily = singleResourceFontFamily(R.font.poppins)
val MontserratFontFamily = singleResourceFontFamily(R.font.montserrat)
val NunitoSansFontFamily = singleResourceFontFamily(R.font.nunito_sans)
val LatoFontFamily = singleResourceFontFamily(R.font.lato)
val OpenSansFontFamily = singleResourceFontFamily(R.font.open_sans)
val RubikFontFamily = singleResourceFontFamily(R.font.rubik)
val ManropeFontFamily = singleResourceFontFamily(R.font.manrope)
val DmSansFontFamily = singleResourceFontFamily(R.font.dm_sans)
val UrbanistFontFamily = singleResourceFontFamily(R.font.urbanist)

// Góticas
val UnifrakturMaguntiaFontFamily = singleResourceFontFamily(R.font.unifrakturmaguntia)
val PirataOneFontFamily = singleResourceFontFamily(R.font.pirata_one)
val NewRockerFontFamily = singleResourceFontFamily(R.font.new_rocker)

// Horror
val CreepsterFontFamily = singleResourceFontFamily(R.font.creepster)
val NosiferFontFamily = singleResourceFontFamily(R.font.nosifer)
val ButchermanFontFamily = singleResourceFontFamily(R.font.butcherman)

// Grunge
val RubikWetPaintFontFamily = singleResourceFontFamily(R.font.rubik_wet_paint)
val RubikGlitchFontFamily = singleResourceFontFamily(R.font.rubik_glitch)
val SyneTactileFontFamily = singleResourceFontFamily(R.font.syne_tactile)

// Vintage
val BlackOpsOneFontFamily = singleResourceFontFamily(R.font.black_ops_one)
val SpecialEliteFontFamily = singleResourceFontFamily(R.font.special_elite)
val RyeFontFamily = singleResourceFontFamily(R.font.rye)

// Rock e Metal
val MetalManiaFontFamily = singleResourceFontFamily(R.font.metal_mania)
val AntonFontFamily = singleResourceFontFamily(R.font.anton)
val OswaldFontFamily = singleResourceFontFamily(R.font.oswald)

// Urbanas
val PermanentMarkerFontFamily = singleResourceFontFamily(R.font.permanent_marker)
val BungeeFontFamily = singleResourceFontFamily(R.font.bungee)
val RubikSprayPaintFontFamily = singleResourceFontFamily(R.font.rubik_spray_paint)

// Medievais
val CinzelDecorativeFontFamily = singleResourceFontFamily(R.font.cinzel_decorative)
val AlmendraDisplayFontFamily = singleResourceFontFamily(R.font.almendra_display)
val MedievalSharpFontFamily = singleResourceFontFamily(R.font.medievalsharp)

// Manuscritas
val CaveatFontFamily = singleResourceFontFamily(R.font.caveat)
val PacificoFontFamily = singleResourceFontFamily(R.font.pacifico)
val DancingScriptFontFamily = singleResourceFontFamily(R.font.dancing_script)

// Decorativas
val RighteousFontFamily = singleResourceFontFamily(R.font.righteous)
val MonotonFontFamily = singleResourceFontFamily(R.font.monoton)
val SyneFontFamily = singleResourceFontFamily(R.font.syne)

// Futuristas
val OrbitronFontFamily = singleResourceFontFamily(R.font.orbitron)
val AudiowideFontFamily = singleResourceFontFamily(R.font.audiowide)
val RajdhaniFontFamily = singleResourceFontFamily(R.font.rajdhani)

// Clássicas
val CinzelFontFamily = singleResourceFontFamily(R.font.cinzel)
val PlayfairDisplayFontFamily = singleResourceFontFamily(R.font.playfair_display)
val CormorantGaramondFontFamily = singleResourceFontFamily(R.font.cormorant_garamond)
val EbGaramondFontFamily = singleResourceFontFamily(R.font.eb_garamond)

/**
 * Builds the complete Material 3 Typography (all 15 scale roles) supporting separate
 * Decorative (display/headline/titleLarge) and General (body/menus/labels/lyrics) fonts
 * while preserving sizes, line heights, weights, and letter spacings.
 */
fun createAppTypography(
    displayFamily: FontFamily,
    bodyFamily: FontFamily = displayFamily,
    monoFamily: FontFamily = bodyFamily,
): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.3).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = monoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = monoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 13.sp,
    ),
)

val MyatoTypography: Typography = createAppTypography(
    displayFamily = OutfitFontFamily,
    bodyFamily = PlusJakartaFontFamily,
    monoFamily = JetBrainsMonoFontFamily,
)
