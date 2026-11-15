package com.example.domain.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
enum class PlayerVisualBackgroundStyle(
    val id: String,
    val title: String,
    val subtitle: String,
) {
    AUDIO_SPECTRUM(
        id = "AUDIO_SPECTRUM",
        title = "Audio Spectrum",
        subtitle = "Barras reativas ao espectro do áudio",
    ),
    AURORA_LIQUID_WAVES(
        id = "AURORA_LIQUID_WAVES",
        title = "Aurora + Liquid Waves",
        subtitle = "Camadas luminosas e ondas com cores da capa",
    ),
    MINIMAL_MOTION(
        id = "MINIMAL_MOTION",
        title = "Minimal Motion",
        subtitle = "Movimento suave com economia de energia",
    );

    companion object {
        fun fromId(id: String?): PlayerVisualBackgroundStyle {
            return entries.firstOrNull { it.id == id || it.name == id } ?: AUDIO_SPECTRUM
        }
    }
}

@Immutable
data class MotionSettingsState(
    val playerVisualStyle: PlayerVisualBackgroundStyle = PlayerVisualBackgroundStyle.AUDIO_SPECTRUM,
    val effectIntensity: Float = 1.0f,
    val animationsDisabled: Boolean = false,
    val reduceMotion: Boolean = false,
    val reduceEffectsOnLowBattery: Boolean = true,
    val systemReducedMotion: Boolean = false,
    val isLowBatteryOrPowerSave: Boolean = false,
    val isAppInForeground: Boolean = true,
) {
    val isAnimationsActive: Boolean
        get() = !animationsDisabled && isAppInForeground

    val isReducedMotionActive: Boolean
        get() = reduceMotion || systemReducedMotion || (reduceEffectsOnLowBattery && isLowBatteryOrPowerSave)

    val effectiveIntensity: Float
        get() = when {
            animationsDisabled -> 0f
            isReducedMotionActive -> (effectIntensity * 0.42f).coerceIn(0.2f, 0.65f)
            else -> effectIntensity.coerceIn(0.3f, 1.5f)
        }

    val effectiveStyle: PlayerVisualBackgroundStyle
        get() = when {
            animationsDisabled -> PlayerVisualBackgroundStyle.MINIMAL_MOTION
            reduceEffectsOnLowBattery && isLowBatteryOrPowerSave -> PlayerVisualBackgroundStyle.MINIMAL_MOTION
            else -> playerVisualStyle
        }

    val shouldAnimateTabTransitions: Boolean
        get() = isAnimationsActive && !isReducedMotionActive
}

@Immutable
data class ExtractedArtPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val darkBase: Color,
    val hasArtwork: Boolean,
)
