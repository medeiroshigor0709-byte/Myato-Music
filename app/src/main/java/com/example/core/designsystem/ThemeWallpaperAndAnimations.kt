package com.example.core.designsystem

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.models.AnimationMode
import com.example.domain.models.MyatoTheme
import com.example.domain.models.ThemeAnimationType
import com.example.domain.models.ThemeEffectPreset
import com.example.domain.models.WallpaperStyle
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ThemeParticleSeed(
    val xFrac: Float,
    val yFrac: Float,
    val radiusScale: Float,
    val speedScale: Float,
    val phase: Float,
    val drift: Float,
    val tintIndex: Int,
)

@Composable
fun ThemeWallpaperLayer(
    colors: MyatoExtendedColors,
    modifier: Modifier = Modifier,
    overlayStrength: Float = 1f,
    allowCustomImage: Boolean = true,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundBrush())
        )

        if (allowCustomImage && !colors.wallpaperImagePath.isNullOrBlank()) {
            val imageModel = remember(colors.wallpaperImagePath) {
                val path = colors.wallpaperImagePath
                if (path.startsWith("content://") || path.startsWith("file://")) {
                    Uri.parse(path)
                } else {
                    File(path)
                }
            }
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background.copy(alpha = colors.wallpaperDimAlpha.coerceIn(0.15f, 0.85f)))
            )
        } else {
            val patternAlpha = (if (colors.isLight) 0.16f else 0.18f) * overlayStrength.coerceIn(0.1f, 1.4f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawProceduralWallpaper(
                    style = colors.wallpaperStyle,
                    seed = colors.wallpaperPatternSeed,
                    primary = colors.primary,
                    secondary = colors.secondary,
                    accent = colors.accent,
                    alpha = patternAlpha,
                    effectPreset = colors.effectPreset,
                )
            }
        }
    }
}

@Composable
fun ThemeParticleOverlay(
    animationType: ThemeAnimationType,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    themeAnimationMode: AnimationMode = AnimationMode.FULL,
    intensityOverride: Float? = null,
) {
    val motion = LocalMotionSettings.current
    val resolvedIntensity = intensityOverride ?: when {
        motion.animationsDisabled || themeAnimationMode == AnimationMode.BATTERY_SAVER -> 0f
        !motion.isAnimationsActive -> 0f
        motion.isReducedMotionActive -> (motion.effectiveIntensity * 0.55f).coerceAtLeast(0.25f)
        else -> motion.effectiveIntensity
    }

    if (animationType == ThemeAnimationType.NONE || resolvedIntensity <= 0.02f) {
        return
    }

    val particleCount = (18 * resolvedIntensity.coerceIn(0.35f, 1.6f)).toInt().coerceIn(8, 34)
    val seeds = remember(animationType, particleCount) {
        val rng = Random(animationType.ordinal * 997 + 13)
        List(particleCount) { idx ->
            ThemeParticleSeed(
                xFrac = rng.nextFloat(),
                yFrac = rng.nextFloat(),
                radiusScale = 0.55f + rng.nextFloat() * 0.9f,
                speedScale = 0.65f + rng.nextFloat() * 0.7f,
                phase = rng.nextFloat() * 6.283f,
                drift = (rng.nextFloat() - 0.5f) * 2f,
                tintIndex = idx % 3,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "theme_particles_${animationType.name}")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particle_progress",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val alphaFactor = (0.24f * resolvedIntensity.coerceIn(0.4f, 1.5f)).coerceAtMost(0.42f)
        drawAnimatedParticles(
            type = animationType,
            seeds = seeds,
            progress = progress,
            primary = primaryColor,
            secondary = secondaryColor,
            accent = accentColor,
            alphaFactor = alphaFactor,
        )
    }
}

@Composable
fun ThemeThumbnailArtwork(
    theme: MyatoTheme,
    modifier: Modifier = Modifier,
    enableParticles: Boolean = false,
) {
    val ext = remember(theme) { theme.toExtendedColors() }
    Box(
        modifier = modifier
            .clip(ext.resolveCardShape())
    ) {
        ThemeWallpaperLayer(
            colors = ext,
            overlayStrength = 1.35f,
            allowCustomImage = true,
            modifier = Modifier.fillMaxSize(),
        )
        if (enableParticles) {
            ThemeParticleOverlay(
                animationType = ext.animationType,
                primaryColor = ext.primary,
                secondaryColor = ext.secondary,
                accentColor = ext.accent,
                themeAnimationMode = ext.animationMode,
                intensityOverride = 0.75f,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Mini mock cover & mini player bar inside the thumbnail so each theme feels visually unique
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopStart)
                    .clip(ext.resolveCoverShape())
                    .background(
                        Brush.linearGradient(
                            listOf(ext.primary, ext.secondary)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .clip(ext.resolveButtonShape())
                    .background(ext.buttonContainer),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(ext.buttonContent)
                )
            }
        }
    }
}

private fun DrawScope.drawProceduralWallpaper(
    style: WallpaperStyle,
    seed: Int,
    primary: Color,
    secondary: Color,
    accent: Color,
    alpha: Float,
    effectPreset: ThemeEffectPreset,
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    when (style) {
        WallpaperStyle.SOLID -> Unit
        WallpaperStyle.GRADIENT -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = alpha * 0.9f), Color.Transparent),
                    center = Offset(w * 0.8f, h * 0.18f),
                    radius = w * 0.75f,
                ),
                center = Offset(w * 0.8f, h * 0.18f),
                radius = w * 0.75f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondary.copy(alpha = alpha * 0.75f), Color.Transparent),
                    center = Offset(w * 0.18f, h * 0.82f),
                    radius = w * 0.68f,
                ),
                center = Offset(w * 0.18f, h * 0.82f),
                radius = w * 0.68f,
            )
        }
        WallpaperStyle.CAT_PAWS -> {
            val rng = Random(seed)
            repeat(12) { i ->
                val cx = (0.08f + rng.nextFloat() * 0.84f) * w
                val cy = (0.08f + rng.nextFloat() * 0.84f) * h
                val r = (10f + rng.nextFloat() * 14f)
                val c = if (i % 2 == 0) primary.copy(alpha = alpha) else secondary.copy(alpha = alpha)
                drawCircle(c, radius = r, center = Offset(cx, cy))
                drawCircle(c, radius = r * 0.42f, center = Offset(cx - r * 1.15f, cy - r * 0.95f))
                drawCircle(c, radius = r * 0.42f, center = Offset(cx - r * 0.38f, cy - r * 1.35f))
                drawCircle(c, radius = r * 0.42f, center = Offset(cx + r * 0.42f, cy - r * 1.35f))
                drawCircle(c, radius = r * 0.42f, center = Offset(cx + r * 1.15f, cy - r * 0.95f))
            }
        }
        WallpaperStyle.HEARTS, WallpaperStyle.RIBBONS -> {
            val rng = Random(seed)
            repeat(14) { i ->
                val cx = rng.nextFloat() * w
                val cy = rng.nextFloat() * h
                val sz = 14f + rng.nextFloat() * 20f
                val c = if (i % 2 == 0) primary.copy(alpha = alpha) else accent.copy(alpha = alpha)
                drawHeartShape(Offset(cx, cy), sz, c)
                if (style == WallpaperStyle.RIBBONS && i % 3 == 0) {
                    drawOval(
                        color = secondary.copy(alpha = alpha * 0.8f),
                        topLeft = Offset(cx - sz * 1.3f, cy - sz * 0.4f),
                        size = Size(sz * 1.1f, sz * 0.65f),
                        style = Stroke(width = 2.2f),
                    )
                    drawOval(
                        color = secondary.copy(alpha = alpha * 0.8f),
                        topLeft = Offset(cx + sz * 0.2f, cy - sz * 0.4f),
                        size = Size(sz * 1.1f, sz * 0.65f),
                        style = Stroke(width = 2.2f),
                    )
                }
            }
        }
        WallpaperStyle.CHERRY_BLOSSOMS -> {
            val rng = Random(seed)
            repeat(14) { i ->
                val cx = rng.nextFloat() * w
                val cy = rng.nextFloat() * h
                val r = 10f + rng.nextFloat() * 14f
                val petalColor = if (i % 2 == 0) primary.copy(alpha = alpha) else secondary.copy(alpha = alpha)
                repeat(5) { p ->
                    val angle = (p * 72f) * (PI / 180f)
                    val px = cx + cos(angle).toFloat() * r * 0.85f
                    val py = cy + sin(angle).toFloat() * r * 0.85f
                    drawCircle(petalColor, radius = r * 0.56f, center = Offset(px, py))
                }
                drawCircle(accent.copy(alpha = alpha * 1.1f), radius = r * 0.25f, center = Offset(cx, cy))
            }
        }
        WallpaperStyle.STARFIELD, WallpaperStyle.NEBULA -> {
            if (style == WallpaperStyle.NEBULA) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(primary.copy(alpha = alpha * 1.15f), Color.Transparent),
                        center = Offset(w * 0.3f, h * 0.32f),
                        radius = w * 0.65f,
                    ),
                    center = Offset(w * 0.3f, h * 0.32f),
                    radius = w * 0.65f,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(secondary.copy(alpha = alpha), Color.Transparent),
                        center = Offset(w * 0.72f, h * 0.65f),
                        radius = w * 0.65f,
                    ),
                    center = Offset(w * 0.72f, h * 0.65f),
                    radius = w * 0.65f,
                )
            }
            val rng = Random(seed)
            repeat(38) { i ->
                val x = rng.nextFloat() * w
                val y = rng.nextFloat() * h
                val r = 1.5f + rng.nextFloat() * 3.5f
                val col = when (i % 3) {
                    0 -> primary.copy(alpha = alpha * 1.4f)
                    1 -> secondary.copy(alpha = alpha * 1.3f)
                    else -> accent.copy(alpha = alpha * 1.3f)
                }
                drawCircle(col, radius = r, center = Offset(x, y))
            }
        }
        WallpaperStyle.SATURN -> {
            val center = Offset(w * 0.68f, h * 0.34f)
            val planetR = (minOf(w, h) * 0.18f).coerceAtLeast(24f)
            drawCircle(
                brush = Brush.radialGradient(listOf(primary.copy(alpha = alpha * 1.35f), secondary.copy(alpha = alpha * 0.55f))),
                radius = planetR,
                center = center,
            )
            rotate(-22f, pivot = center) {
                drawOval(
                    color = accent.copy(alpha = alpha * 1.25f),
                    topLeft = Offset(center.x - planetR * 1.85f, center.y - planetR * 0.42f),
                    size = Size(planetR * 3.7f, planetR * 0.84f),
                    style = Stroke(width = 3.5f),
                )
            }
        }
        WallpaperStyle.AURORA -> {
            val path1 = Path().apply {
                moveTo(0f, h * 0.25f)
                cubicTo(w * 0.3f, h * 0.1f, w * 0.65f, h * 0.42f, w, h * 0.22f)
                lineTo(w, h * 0.48f)
                cubicTo(w * 0.65f, h * 0.65f, w * 0.3f, h * 0.35f, 0f, h * 0.52f)
                close()
            }
            drawPath(
                path = path1,
                brush = Brush.horizontalGradient(listOf(primary.copy(alpha = alpha), secondary.copy(alpha = alpha), accent.copy(alpha = alpha))),
            )
        }
        WallpaperStyle.PIXEL_GRID -> {
            val step = (minOf(w, h) / 10f).coerceAtLeast(18f)
            var x = 0f
            while (x < w) {
                drawLine(primary.copy(alpha = alpha * 0.55f), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < h) {
                drawLine(secondary.copy(alpha = alpha * 0.55f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                y += step
            }
        }
        WallpaperStyle.SCANLINES, WallpaperStyle.SYNTHWAVE -> {
            if (style == WallpaperStyle.SYNTHWAVE) {
                val sunR = (minOf(w, h) * 0.22f).coerceAtLeast(28f)
                drawCircle(
                    brush = Brush.verticalGradient(listOf(accent.copy(alpha = alpha * 1.3f), primary.copy(alpha = alpha * 1.2f))),
                    radius = sunR,
                    center = Offset(w * 0.5f, h * 0.36f),
                )
            }
            var y = 0f
            while (y < h) {
                drawLine(primary.copy(alpha = alpha * 0.45f), Offset(0f, y), Offset(w, y), strokeWidth = 1.3f)
                y += 14f
            }
        }
        WallpaperStyle.CLOUDS -> {
            val rng = Random(seed)
            repeat(8) { i ->
                val cx = rng.nextFloat() * w
                val cy = (0.12f + rng.nextFloat() * 0.72f) * h
                val r = 28f + rng.nextFloat() * 34f
                val c = if (i % 2 == 0) primary.copy(alpha = alpha * 0.75f) else secondary.copy(alpha = alpha * 0.75f)
                drawCircle(c, radius = r, center = Offset(cx, cy))
                drawCircle(c, radius = r * 0.78f, center = Offset(cx - r * 0.65f, cy + r * 0.18f))
                drawCircle(c, radius = r * 0.72f, center = Offset(cx + r * 0.65f, cy + r * 0.22f))
            }
        }
        WallpaperStyle.RAINDROPS -> {
            val rng = Random(seed)
            repeat(28) { i ->
                val x = rng.nextFloat() * w
                val y = rng.nextFloat() * h
                val len = 18f + rng.nextFloat() * 28f
                val c = if (i % 2 == 0) primary.copy(alpha = alpha) else accent.copy(alpha = alpha)
                drawLine(c, Offset(x, y), Offset(x - 6f, y + len), strokeWidth = 2f, cap = StrokeCap.Round)
            }
        }
        WallpaperStyle.GOLD_GEOMETRY, WallpaperStyle.CRYSTALS -> {
            val rng = Random(seed)
            repeat(9) { i ->
                val cx = rng.nextFloat() * w
                val cy = rng.nextFloat() * h
                val r = 24f + rng.nextFloat() * 44f
                val col = if (i % 2 == 0) primary.copy(alpha = alpha * 0.85f) else secondary.copy(alpha = alpha * 0.85f)
                val diamond = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.75f, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r * 0.75f, cy)
                    close()
                }
                drawPath(diamond, col, style = Stroke(width = 2f))
            }
        }
        WallpaperStyle.ZEN_WAVES, WallpaperStyle.OCEAN_WAVES -> {
            repeat(6) { idx ->
                val baseY = h * (0.18f + idx * 0.13f)
                val path = Path().apply {
                    moveTo(0f, baseY)
                    cubicTo(w * 0.28f, baseY - 26f, w * 0.68f, baseY + 28f, w, baseY - 8f)
                }
                val c = if (idx % 2 == 0) primary.copy(alpha = alpha) else secondary.copy(alpha = alpha)
                drawPath(path, c, style = Stroke(width = 2.6f, cap = StrokeCap.Round))
            }
        }
        WallpaperStyle.BOTANICAL -> {
            val rng = Random(seed)
            repeat(12) { i ->
                val cx = rng.nextFloat() * w
                val cy = rng.nextFloat() * h
                val rx = 18f + rng.nextFloat() * 22f
                val col = if (i % 2 == 0) primary.copy(alpha = alpha) else accent.copy(alpha = alpha)
                rotate(rng.nextFloat() * 180f, pivot = Offset(cx, cy)) {
                    drawOval(col, topLeft = Offset(cx - rx, cy - rx * 0.42f), size = Size(rx * 2f, rx * 0.84f))
                }
            }
        }
        WallpaperStyle.CITY_SKYLINE -> {
            val rng = Random(seed)
            val cols = 9
            val barW = w / cols
            for (i in 0 until cols) {
                val barH = h * (0.18f + rng.nextFloat() * 0.36f)
                drawRect(
                    color = primary.copy(alpha = alpha * 0.55f),
                    topLeft = Offset(i * barW + 3f, h - barH),
                    size = Size(barW - 6f, barH),
                )
                drawRect(
                    color = accent.copy(alpha = alpha * 0.75f),
                    topLeft = Offset(i * barW + 8f, h - barH + 10f),
                    size = Size((barW - 16f).coerceAtLeast(4f), 4f),
                )
            }
        }
        WallpaperStyle.HOLOGRAPHIC -> {
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        primary.copy(alpha = alpha),
                        secondary.copy(alpha = alpha),
                        accent.copy(alpha = alpha),
                        primary.copy(alpha = alpha),
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = maxOf(w, h) * 0.65f,
            )
        }
        WallpaperStyle.CUSTOM_IMAGE -> Unit
    }

    if (effectPreset == ThemeEffectPreset.VIGNETTE) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = maxOf(w, h) * 0.72f,
            )
        )
    }
}

private fun DrawScope.drawAnimatedParticles(
    type: ThemeAnimationType,
    seeds: List<ThemeParticleSeed>,
    progress: Float,
    primary: Color,
    secondary: Color,
    accent: Color,
    alphaFactor: Float,
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    seeds.forEach { p ->
        val t = (progress * p.speedScale + p.phase / 6.283f) % 1f
        val color = when (p.tintIndex) {
            0 -> primary
            1 -> secondary
            else -> accent
        }.copy(alpha = alphaFactor * (0.65f + 0.35f * sin(t * PI).toFloat()))

        val baseSize = (5f + 8f * p.radiusScale)

        when (type) {
            ThemeAnimationType.HEARTS, ThemeAnimationType.RIBBONS -> {
                val y = ((1f - t + p.yFrac) % 1f) * h
                val x = (p.xFrac * w + sin(t * 2f * PI + p.phase).toFloat() * 24f).coerceIn(0f, w)
                drawHeartShape(Offset(x, y), baseSize * 1.1f, color)
            }
            ThemeAnimationType.SAKURA_PETALS, ThemeAnimationType.FOREST_LEAVES,
            ThemeAnimationType.BOTANICAL_VINES, ThemeAnimationType.PIXEL_LEAVES -> {
                val y = ((t + p.yFrac) % 1f) * h
                val x = ((p.xFrac + t * 0.14f * p.drift) % 1f + 1f) % 1f * w
                rotate(t * 360f, pivot = Offset(x, y)) {
                    drawOval(
                        color = color,
                        topLeft = Offset(x - baseSize, y - baseSize * 0.5f),
                        size = Size(baseSize * 2f, baseSize),
                    )
                }
            }
            ThemeAnimationType.RAIN_DROPS, ThemeAnimationType.SILVER_STREAKS -> {
                val y = ((t + p.yFrac) % 1f) * h
                val x = ((p.xFrac * w) - t * 36f + w) % w
                drawLine(color, Offset(x, y), Offset(x - 6f, y + baseSize * 2.4f), strokeWidth = 2f, cap = StrokeCap.Round)
            }
            ThemeAnimationType.PIXEL_BLOCKS, ThemeAnimationType.ARCADE_SCANLINES,
            ThemeAnimationType.CYBER_GRID, ThemeAnimationType.CYBER_CIRCUIT -> {
                val y = ((1f - t + p.yFrac) % 1f) * h
                val x = p.xFrac * w
                drawRect(color, topLeft = Offset(x, y), size = Size(baseSize * 1.25f, baseSize * 1.25f))
            }
            ThemeAnimationType.SOUND_WAVES, ThemeAnimationType.NEON_WAVES,
            ThemeAnimationType.OCEAN_WAVES, ThemeAnimationType.ZEN_RIPPLES,
            ThemeAnimationType.RETRO_WAVE -> {
                val ringProgress = (progress + p.phase / 6.283f) % 1f
                val maxR = minOf(w, h) * 0.44f
                if (p.tintIndex == 0) {
                    drawCircle(
                        color = color.copy(alpha = alphaFactor * (1f - ringProgress)),
                        radius = ringProgress * maxR,
                        center = Offset(w * 0.5f, h * 0.5f),
                        style = Stroke(width = 2.2f),
                    )
                } else {
                    val y = p.yFrac * h
                    val x = (p.xFrac * w + sin(t * 2f * PI).toFloat() * 30f).coerceIn(0f, w)
                    drawCircle(color, radius = baseSize * 0.55f, center = Offset(x, y))
                }
            }
            ThemeAnimationType.CRYSTAL_SHARDS, ThemeAnimationType.HOLOGRAPHIC_PRISM,
            ThemeAnimationType.PASTEL_PRISM -> {
                val y = ((1f - t + p.yFrac) % 1f) * h
                val x = p.xFrac * w
                val path = Path().apply {
                    moveTo(x, y - baseSize)
                    lineTo(x + baseSize * 0.75f, y)
                    lineTo(x, y + baseSize)
                    lineTo(x - baseSize * 0.75f, y)
                    close()
                }
                drawPath(path, color)
            }
            ThemeAnimationType.SATURN_RINGS, ThemeAnimationType.GALAXY_SWIRL,
            ThemeAnimationType.MYATO_GALAXY -> {
                val angle = t * 2f * PI + p.phase
                val orbit = minOf(w, h) * (0.14f + 0.30f * p.xFrac)
                val cx = w * 0.5f + cos(angle).toFloat() * orbit
                val cy = h * 0.5f + sin(angle).toFloat() * orbit * 0.65f
                drawCircle(color, radius = baseSize * 0.6f, center = Offset(cx, cy))
            }
            ThemeAnimationType.FLOATING_CLOUDS, ThemeAnimationType.DREAM_CLOUDS,
            ThemeAnimationType.AURORA_LIGHTS, ThemeAnimationType.MOUNTAIN_MIST,
            ThemeAnimationType.LAVENDER_MIST, ThemeAnimationType.CREAM_BOKEH -> {
                val x = ((p.xFrac + t * 0.25f) % 1f) * w
                val y = p.yFrac * h
                drawCircle(
                    brush = Brush.radialGradient(listOf(color, Color.Transparent), center = Offset(x, y), radius = baseSize * 3.2f),
                    radius = baseSize * 3.2f,
                    center = Offset(x, y),
                )
            }
            else -> {
                val y = ((1f - t + p.yFrac) % 1f) * h
                val x = (p.xFrac * w + sin(t * 2f * PI + p.phase).toFloat() * 18f).coerceIn(0f, w)
                drawCircle(color, radius = baseSize * 0.62f, center = Offset(x, y))
            }
        }
    }
}

private fun DrawScope.drawHeartShape(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        val x = center.x
        val y = center.y
        moveTo(x, y + size * 0.7f)
        cubicTo(x - size * 1.1f, y - size * 0.1f, x - size * 0.55f, y - size * 0.9f, x, y - size * 0.28f)
        cubicTo(x + size * 0.55f, y - size * 0.9f, x + size * 1.1f, y - size * 0.1f, x, y + size * 0.7f)
        close()
    }
    drawPath(path, color)
}
