package com.example.feature.player

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.designsystem.AlbumArtPaletteExtractor
import com.example.core.designsystem.LocalMotionSettings
import com.example.core.designsystem.LocalMyatoColors
import com.example.domain.models.ExtractedArtPalette
import com.example.domain.models.MotionSettingsState
import com.example.domain.models.MusicTrack
import com.example.domain.models.PlayerVisualBackgroundStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic full-screen background renderer for FullPlayerScreen.
 * Supports instant, audio-safe switching between Audio Spectrum, Aurora + Liquid Waves, and Minimal Motion.
 */
@Composable
fun PlayerAnimatedBackground(
    track: MusicTrack?,
    isPlaying: Boolean,
    spectrumBars: List<Float>,
    hasRealFftSignal: Boolean,
    ambientBgEnabled: Boolean,
    scrollOffsetPx: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyatoColors.current
    val motion = LocalMotionSettings.current
    val context = LocalContext.current

    var artPalette by remember(colors.themeId) {
        mutableStateOf(
            ExtractedArtPalette(
                primary = colors.primary,
                secondary = colors.secondary,
                tertiary = colors.primary.copy(alpha = 0.72f),
                darkBase = colors.background,
                hasArtwork = false,
            )
        )
    }

    LaunchedEffect(track?.id, track?.albumArtUri, ambientBgEnabled, colors.themeId) {
        if (ambientBgEnabled && track != null) {
            artPalette = AlbumArtPaletteExtractor.extractAuroraPalette(
                context = context,
                trackId = track.id,
                artUri = track.albumArtUri,
                filePath = track.filePath,
                fallbackColors = colors,
            )
        } else {
            artPalette = ExtractedArtPalette(
                primary = colors.primary,
                secondary = colors.secondary,
                tertiary = colors.primary.copy(alpha = 0.72f),
                darkBase = colors.background,
                hasArtwork = false,
            )
        }
    }

    val colorDuration = if (motion.isAnimationsActive) 700 else 0
    val animatedPrimary by animateColorAsState(artPalette.primary, tween(colorDuration), label = "bg_primary")
    val animatedSecondary by animateColorAsState(artPalette.secondary, tween(colorDuration), label = "bg_secondary")
    val animatedTertiary by animateColorAsState(artPalette.tertiary, tween(colorDuration), label = "bg_tertiary")
    val animatedDarkBase by animateColorAsState(artPalette.darkBase, tween(colorDuration), label = "bg_dark")

    val style = motion.effectiveStyle
    val crossfadeDuration = if (motion.isAnimationsActive) 550 else 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Crossfade(
            targetState = style,
            animationSpec = tween(durationMillis = crossfadeDuration),
            label = "player_bg_style_crossfade",
        ) { activeStyle ->
            when (activeStyle) {
                PlayerVisualBackgroundStyle.AUDIO_SPECTRUM -> {
                    AudioSpectrumBackground(
                        hasTrack = track != null,
                        isPlaying = isPlaying,
                        spectrumBars = spectrumBars,
                        hasRealFftSignal = hasRealFftSignal,
                        accentPrimary = if (ambientBgEnabled && artPalette.hasArtwork) animatedPrimary else colors.primary,
                        accentSecondary = if (ambientBgEnabled && artPalette.hasArtwork) animatedSecondary else colors.secondary,
                        motion = motion,
                        scrollOffsetPx = scrollOffsetPx,
                    )
                }

                PlayerVisualBackgroundStyle.AURORA_LIQUID_WAVES -> {
                    AuroraLiquidWavesBackground(
                        track = track,
                        isPlaying = isPlaying,
                        hasArtwork = artPalette.hasArtwork && ambientBgEnabled,
                        primaryColor = animatedPrimary,
                        secondaryColor = animatedSecondary,
                        tertiaryColor = animatedTertiary,
                        darkBaseColor = animatedDarkBase,
                        motion = motion,
                        scrollOffsetPx = scrollOffsetPx,
                    )
                }

                PlayerVisualBackgroundStyle.MINIMAL_MOTION -> {
                    MinimalMotionBackground(
                        accentColor = if (ambientBgEnabled && artPalette.hasArtwork) animatedPrimary else colors.primary,
                        secondaryColor = if (ambientBgEnabled && artPalette.hasArtwork) animatedSecondary else colors.secondary,
                        isPlaying = isPlaying,
                        motion = motion,
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioSpectrumBackground(
    hasTrack: Boolean,
    isPlaying: Boolean,
    spectrumBars: List<Float>,
    hasRealFftSignal: Boolean,
    accentPrimary: Color,
    accentSecondary: Color,
    motion: MotionSettingsState,
    scrollOffsetPx: Float,
) {
    val colors = LocalMyatoColors.current
    val barCount = 24
    val smoothedBars = remember { FloatArray(barCount) { 0f } }
    val peakHoldBars = remember { FloatArray(barCount) { 0f } }

    val runMotion = hasTrack && isPlaying && motion.isAnimationsActive
    val phaseState = if (runMotion) {
        val transition = rememberInfiniteTransition(label = "bg_spectrum_transition")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (motion.isReducedMotionActive) 4200 else 2400,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "bg_spectrum_phase",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    // Smooth deceleration down to 0 on pause, fade out completely on stop
    val playbackGain by animateFloatAsState(
        targetValue = if (!hasTrack) 0f else if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = if (motion.isAnimationsActive) 420 else 0),
        label = "spectrum_playback_gain",
    )
    val activeAlpha by animateFloatAsState(
        targetValue = if (!hasTrack) 0f else if (isPlaying) 1f else 0.25f,
        animationSpec = tween(durationMillis = if (motion.isAnimationsActive) 450 else 0),
        label = "spectrum_active_alpha",
    )

    val intensity = motion.effectiveIntensity
    var cachedHeight by remember { mutableFloatStateOf(0f) }
    var cachedBarBrush by remember(accentPrimary, accentSecondary, colors.background) {
        mutableStateOf<Brush?>(null)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("player_bg_audio_spectrum"),
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val phase = phaseState.value
        val gain = playbackGain

        if (cachedBarBrush == null || kotlin.math.abs(cachedHeight - h) > 2f) {
            cachedHeight = h
            cachedBarBrush = Brush.verticalGradient(
                colors = listOf(
                    accentSecondary.copy(alpha = 0.72f),
                    accentPrimary.copy(alpha = 0.42f),
                    accentPrimary.copy(alpha = 0.06f),
                ),
                startY = h * 0.22f,
                endY = h,
            )
        }

        // Top ambient theme glow
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentPrimary.copy(alpha = 0.22f * activeAlpha.coerceAtLeast(0.45f)),
                    colors.altBackground.copy(alpha = 0.92f),
                    colors.background,
                ),
            ),
        )

        if (!motion.isAnimationsActive || activeAlpha <= 0.01f) return@Canvas

        val hasReal = isPlaying && hasRealFftSignal && spectrumBars.isNotEmpty()
        val parallaxShift = (scrollOffsetPx * 0.10f).coerceIn(-60f, 60f)
        val totalBarSpan = w * 0.94f
        val startX = (w - totalBarSpan) * 0.5f
        val gap = 4.dp.toPx()
        val barWidth = ((totalBarSpan - gap * (barCount - 1)) / barCount).coerceAtLeast(3.dp.toPx())
        val maxBarAreaH = h * 0.52f
        val baselineY = (h * 0.88f) - parallaxShift
        val corner = CornerRadius(barWidth * 0.45f, barWidth * 0.45f)
        val barBrush = cachedBarBrush!!

        for (i in 0 until barCount) {
            val rawTarget = when {
                gain <= 0.01f -> 0f
                hasReal -> {
                    val srcIdx = ((i * spectrumBars.size) / barCount).coerceIn(0, spectrumBars.lastIndex)
                    val raw = spectrumBars[srcIdx].coerceIn(0f, 1f)
                    val bandWeight = if (i < 6) 1.18f else if (i < 16) 1.0f else 0.9f
                    (raw * bandWeight * intensity).coerceIn(0.05f, 1f)
                }
                else -> {
                    // Subtle decorative idle motion when device Visualizer FFT isn't active
                    val pos = i.toFloat() / barCount.toFloat()
                    val waveA = sin(phase * 2f + pos * 5.2f) * 0.5f + 0.5f
                    val waveB = cos(phase - pos * 3.1f) * 0.5f + 0.5f
                    ((0.12f + 0.28f * waveA * waveB) * intensity).coerceIn(0.05f, 0.58f)
                }
            }
            val target = rawTarget * gain
            val prev = smoothedBars[i]
            val smooth = prev + (target - prev) * 0.34f
            smoothedBars[i] = if (smooth < 0.004f && !isPlaying) 0f else smooth
            if (smoothedBars[i] > peakHoldBars[i]) {
                peakHoldBars[i] = smoothedBars[i]
            } else {
                peakHoldBars[i] = (peakHoldBars[i] - 0.012f).coerceAtLeast(smoothedBars[i])
            }

            val level = smoothedBars[i]
            val barH = (level * maxBarAreaH).coerceAtLeast(if (isPlaying) 6.dp.toPx() else 2.dp.toPx())
            val x = startX + i * (barWidth + gap)
            val topY = (baselineY - barH).coerceAtLeast(h * 0.18f)

            if (level > 0.25f) {
                drawRoundRect(
                    color = accentPrimary.copy(alpha = 0.10f * level * activeAlpha),
                    topLeft = Offset(x - 2.dp.toPx(), topY - 4.dp.toPx()),
                    size = Size(barWidth + 4.dp.toPx(), barH + 6.dp.toPx()),
                    cornerRadius = corner,
                )
            }

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, topY),
                size = Size(barWidth, barH),
                cornerRadius = corner,
                alpha = activeAlpha * 0.52f,
            )

            val peakLevel = peakHoldBars[i]
            if (peakLevel > 0.04f) {
                val peakY = (baselineY - (peakLevel * maxBarAreaH) - 6.dp.toPx()).coerceAtLeast(h * 0.16f)
                drawCircle(
                    color = accentSecondary.copy(alpha = 0.48f * activeAlpha),
                    radius = (barWidth * 0.36f).coerceAtLeast(2f),
                    center = Offset(x + barWidth * 0.5f, peakY),
                )
            }
        }
    }
}

@Composable
private fun AuroraLiquidWavesBackground(
    track: MusicTrack?,
    isPlaying: Boolean,
    hasArtwork: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    darkBaseColor: Color,
    motion: MotionSettingsState,
    scrollOffsetPx: Float,
) {
    val colors = LocalMyatoColors.current
    val context = LocalContext.current
    val intensity = motion.effectiveIntensity
    val runMotion = isPlaying && motion.isAnimationsActive

    val phaseState = if (runMotion) {
        val transition = rememberInfiniteTransition(label = "aurora_wave_transition")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (motion.isReducedMotionActive) 14000 else 8200,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "aurora_wave_phase",
        )
    } else {
        remember { mutableFloatStateOf(0.4f) }
    }

    val wavePath1 = remember { Path() }
    val wavePath2 = remember { Path() }
    val wavePath3 = remember { Path() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("player_bg_aurora_waves"),
    ) {
        // Optional subtle blurred album cover backdrop without touching main crisp Artwork
        if (hasArtwork && !track?.albumArtUri.isNullOrBlank()) {
            val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(54.dp)
            } else {
                Modifier
            }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(track?.albumArtUri)
                    .size(160)
                    .crossfade(motion.isAnimationsActive)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurModifier)
                    .alpha(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.22f else 0.08f),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val parallax = (scrollOffsetPx * 0.14f).coerceIn(-80f, 80f)
            val p = phaseState.value

            // Deep base gradient for clean readability across all 12 themes
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        darkBaseColor.copy(alpha = 0.86f),
                        colors.altBackground.copy(alpha = 0.90f),
                        colors.background.copy(alpha = 0.96f),
                    ),
                ),
            )

            // 1. Aurora Luminous Gradient Blobs
            val orb1Center = Offset(
                x = w * (0.28f + 0.18f * cos(p * 0.65f)),
                y = h * (0.24f + 0.09f * sin(p * 0.50f)) - parallax * 0.6f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = (0.34f * intensity).coerceIn(0.10f, 0.45f)),
                        primaryColor.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = orb1Center,
                    radius = w * 0.68f,
                ),
                center = orb1Center,
                radius = w * 0.68f,
            )

            val orb2Center = Offset(
                x = w * (0.74f + 0.15f * sin(p * 0.52f)),
                y = h * (0.38f + 0.10f * cos(p * 0.42f)) - parallax * 0.45f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = (0.28f * intensity).coerceIn(0.08f, 0.38f)),
                        secondaryColor.copy(alpha = 0.06f),
                        Color.Transparent,
                    ),
                    center = orb2Center,
                    radius = w * 0.62f,
                ),
                center = orb2Center,
                radius = w * 0.62f,
            )

            val orb3Center = Offset(
                x = w * (0.50f + 0.22f * sin(p * 0.36f + 1.4f)),
                y = h * (0.62f + 0.08f * cos(p * 0.48f)) - parallax * 0.3f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiaryColor.copy(alpha = (0.22f * intensity).coerceIn(0.06f, 0.32f)),
                        Color.Transparent,
                    ),
                    center = orb3Center,
                    radius = w * 0.58f,
                ),
                center = orb3Center,
                radius = w * 0.58f,
            )

            // 2. Multi-speed Liquid Waves
            buildLiquidWavePath(
                path = wavePath1,
                width = w,
                height = h,
                baseY = h * 0.66f - parallax * 0.4f,
                amplitude = 26.dp.toPx() * intensity,
                frequency = 1.6f,
                phase = p * 1.15f,
            )
            drawPath(
                path = wavePath1,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.16f),
                        primaryColor.copy(alpha = 0.03f),
                    ),
                    startY = h * 0.58f,
                    endY = h,
                ),
            )

            buildLiquidWavePath(
                path = wavePath2,
                width = w,
                height = h,
                baseY = h * 0.74f - parallax * 0.25f,
                amplitude = 20.dp.toPx() * intensity,
                frequency = 2.2f,
                phase = -p * 0.88f + 1.1f,
            )
            drawPath(
                path = wavePath2,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.13f),
                        secondaryColor.copy(alpha = 0.02f),
                    ),
                    startY = h * 0.68f,
                    endY = h,
                ),
            )

            buildLiquidWavePath(
                path = wavePath3,
                width = w,
                height = h,
                baseY = h * 0.83f - parallax * 0.15f,
                amplitude = 14.dp.toPx() * intensity,
                frequency = 2.8f,
                phase = p * 1.5f + 2.5f,
            )
            drawPath(
                path = wavePath3,
                color = tertiaryColor.copy(alpha = 0.10f),
            )

            // Protective subtle scrim so white/themed typography always has high contrast
            drawRect(
                color = colors.background.copy(alpha = if (colors.isLight) 0.58f else 0.32f),
            )
        }
    }
}

private fun buildLiquidWavePath(
    path: Path,
    width: Float,
    height: Float,
    baseY: Float,
    amplitude: Float,
    frequency: Float,
    phase: Float,
) {
    path.reset()
    val steps = 20
    val stepX = width / steps.toFloat()
    path.moveTo(0f, height)
    for (i in 0..steps) {
        val x = i * stepX
        val normX = i.toFloat() / steps.toFloat()
        val y = baseY +
            sin(normX * PI.toFloat() * 2f * frequency + phase) * amplitude +
            cos(normX * PI.toFloat() * 1.3f * frequency - phase * 0.6f) * (amplitude * 0.45f)
        path.lineTo(x, y)
    }
    path.lineTo(width, height)
    path.close()
}

@Composable
private fun MinimalMotionBackground(
    accentColor: Color,
    secondaryColor: Color,
    isPlaying: Boolean,
    motion: MotionSettingsState,
) {
    val colors = LocalMyatoColors.current
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.18f else 0.10f,
        animationSpec = tween(durationMillis = if (motion.isAnimationsActive) 500 else 0),
        label = "minimal_glow",
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("player_bg_minimal_motion"),
    ) {
        drawRect(color = colors.background)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = glowAlpha),
                    secondaryColor.copy(alpha = glowAlpha * 0.4f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.5f, size.height * 0.28f),
                radius = size.width * 0.85f,
            ),
            center = Offset(size.width * 0.5f, size.height * 0.28f),
            radius = size.width * 0.85f,
        )
    }
}
