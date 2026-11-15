package com.example.core.designsystem

import com.example.domain.models.AnimationMode
import com.example.domain.models.ButtonStyle
import com.example.domain.models.CoverShape
import com.example.domain.models.MyatoTheme
import com.example.domain.models.PlayerAppearance

/**
 * Provides the 12 mandatory curated themes for MYATO MUSIC.
 */
object BuiltInThemes {

    val MidnightOrange = MyatoTheme(
        id = "midnight_orange",
        name = "Midnight Orange",
        isBuiltIn = true,
        isLight = false,
        primaryColorHex = 0xFFFF8500L,
        secondaryColorHex = 0xFFFF9D24L,
        backgroundColorHex = 0xFF000000L,
        altBackgroundColorHex = 0xFF111111L,
        cardColorHex = 0xFF1C1C1CL,
        textColorHex = 0xFFFFFFFFL,
        secondaryTextColorHex = 0xFFA5A5A5L,
        coverShape = CoverShape.ROUNDED,
        buttonStyle = ButtonStyle.CIRCULAR,
        animationMode = AnimationMode.FULL,
        playerAppearance = PlayerAppearance.CINEMATIC,
    )

    val AmoledBlack = MyatoTheme(
        id = "amoled_black",
        name = "AMOLED Black",
        isBuiltIn = true,
        isLight = false,
        primaryColorHex = 0xFFFF8500L,
        secondaryColorHex = 0xFFFFAB40L,
        backgroundColorHex = 0xFF000000L,
        altBackgroundColorHex = 0xFF050505L,
        cardColorHex = 0xFF0F0F0FL,
        textColorHex = 0xFFFFFFFFL,
        secondaryTextColorHex = 0xFF909090L,
    )

    val NeonGalaxy = MyatoTheme(
        id = "neon_galaxy",
        name = "Neon Galaxy",
        isBuiltIn = true,
        isLight = false,
        primaryColorHex = 0xFF00E5FFL,
        secondaryColorHex = 0xFFFF4081L,
        backgroundColorHex = 0xFF060814L,
        altBackgroundColorHex = 0xFF0D1126L,
        cardColorHex = 0xFF151B38L,
        textColorHex = 0xFFF4F8FFL,
        secondaryTextColorHex = 0xFF98A7D4L,
    )

    val Cyberpunk = MyatoTheme(
        id = "cyberpunk",
        name = "Cyberpunk",
        isBuiltIn = true,
        isLight = false,
        primaryColorHex = 0xFFFFE600L,
        secondaryColorHex = 0xFFFF007FL,
        backgroundColorHex = 0xFF0B0714L,
        altBackgroundColorHex = 0xFF140D24L,
        cardColorHex = 0xFF1E1436L,
        textColorHex = 0xFFFFFFFFL,
        secondaryTextColorHex = 0xFFC5B3E6L,
    )

    val RoyalGold = MyatoTheme(
        id = "royal_gold",
        name = "Royal Gold",
        isBuiltIn = true,
        isLight = false,
        primaryColorHex = 0xFFD4AF37L,
        secondaryColorHex = 0xFFF6D365L,
        backgroundColorHex = 0xFF0A0805L,
        altBackgroundColorHex = 0xFF141009L,
        cardColorHex = 0xFF201A0FL,
        textColorHex = 0xFFFFFBF0L,
        secondaryTextColorHex = 0xFFB8AA8EL,
    )

    val PureLight = MyatoTheme(
        id = "pure_light",
        name = "Pure Light",
        isBuiltIn = true,
        isLight = true,
        primaryColorHex = 0xFFFF7A00L,
        secondaryColorHex = 0xFFFF9E3DL,
        backgroundColorHex = 0xFFF6F7FAL,
        altBackgroundColorHex = 0xFFECEEF4L,
        cardColorHex = 0xFFFFFFFFL,
        textColorHex = 0xFF111318L,
        secondaryTextColorHex = 0xFF606675L,
    )

    val OceanBlue = MyatoTheme(
        id = "ocean_blue",
        name = "Ocean Blue",
        isBuiltIn = true,
        isLight = false,
        primaryColorHex = 0xFF29B6F6L,
        secondaryColorHex = 0xFF00E5FFL,
        backgroundColorHex = 0xFF05111CL,
        altBackgroundColorHex = 0xFF0A1D2EL,
        cardColorHex = 0xFF112940L,
        textColorHex = 0xFFF0F8FFL,
        secondaryTextColorHex = 0xFF8FB4D4L,
    )

    val ForestGreen = MyatoTheme(
        id = "forest_green",
        name = "Forest Green",
        isBuiltIn = true,
        isLight = false,
        primaryColorHex = 0xFF2ECC71L,
        secondaryColorHex = 0xFF69F0AEL,
        backgroundColorHex = 0xFF06120BL,
        altBackgroundColorHex = 0xFF0D2115L,
        cardColorHex = 0xFF153020L,
        textColorHex = 0xFFF2FFF6L,
        secondaryTextColorHex = 0xFF93BEA4L,
    )

    val CrimsonRed = MyatoTheme(
        id = "crimson_red",
        name = "Crimson Red",
        isBuiltIn = true,
        isLight = false,
        primaryColorHex = 0xFFFF3B5CL,
        secondaryColorHex = 0xFFFF758FL,
        backgroundColorHex = 0xFF110508L,
        altBackgroundColorHex = 0xFF1D0A0FL,
        cardColorHex = 0xFF2B1018L,
        textColorHex = 0xFFFFF5F7L,
        secondaryTextColorHex = 0xFFC899A4L,
    )

    val Glass = MyatoTheme(
        id = "glass",
        name = "Glass",
        isBuiltIn = true,
        isLight = false,
        primaryColorHex = 0xFF64B5F6L,
        secondaryColorHex = 0xFFFF8500L,
        backgroundColorHex = 0xFF0C1017L,
        altBackgroundColorHex = 0xFF151C28L,
        cardColorHex = 0xFF1E2838L,
        textColorHex = 0xFFF5F8FFL,
        secondaryTextColorHex = 0xFFA6B4C8L,
    )

    val PurpleNight = MyatoTheme(
        id = "purple_night",
        name = "Purple Night",
        isBuiltIn = true,
        isLight = false,
        primaryColorHex = 0xFFB388FFL,
        secondaryColorHex = 0xFFEA80FCL,
        backgroundColorHex = 0xFF0B0614L,
        altBackgroundColorHex = 0xFF150C26L,
        cardColorHex = 0xFF21143BL,
        textColorHex = 0xFFF8F4FFL,
        secondaryTextColorHex = 0xFFB29ED8L,
    )

    val ArcticSilver = MyatoTheme(
        id = "arctic_silver",
        name = "Arctic Silver",
        isBuiltIn = true,
        isLight = false,
        primaryColorHex = 0xFFB0C4DEL,
        secondaryColorHex = 0xFFFF8500L,
        backgroundColorHex = 0xFF0E1216L,
        altBackgroundColorHex = 0xFF171D24L,
        cardColorHex = 0xFF222A33L,
        textColorHex = 0xFFF2F6FAL,
        secondaryTextColorHex = 0xFF9BA8B8L,
    )

    val all: List<MyatoTheme> = listOf(
        MidnightOrange,
        AmoledBlack,
        NeonGalaxy,
        Cyberpunk,
        RoyalGold,
        PureLight,
        OceanBlue,
        ForestGreen,
        CrimsonRed,
        Glass,
        PurpleNight,
        ArcticSilver,
    )
}
