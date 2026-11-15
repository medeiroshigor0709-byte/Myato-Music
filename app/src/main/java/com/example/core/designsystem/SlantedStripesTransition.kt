package com.example.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import kotlin.math.tan

@Immutable
private data class SlantedStripeSpec(
    val widthFraction: Float,
    val offsetDelay: Float,
    val alphaWeight: Float,
    val useSecondaryColor: Boolean,
)

private val DEFAULT_SLANTED_STRIPES = listOf(
    SlantedStripeSpec(widthFraction = 0.055f, offsetDelay = 0.00f, alphaWeight = 0.24f, useSecondaryColor = false),
    SlantedStripeSpec(widthFraction = 0.120f, offsetDelay = 0.04f, alphaWeight = 0.56f, useSecondaryColor = false),
    SlantedStripeSpec(widthFraction = 0.042f, offsetDelay = 0.09f, alphaWeight = 0.36f, useSecondaryColor = true),
    SlantedStripeSpec(widthFraction = 0.145f, offsetDelay = 0.13f, alphaWeight = 0.62f, useSecondaryColor = false),
    SlantedStripeSpec(widthFraction = 0.072f, offsetDelay = 0.19f, alphaWeight = 0.44f, useSecondaryColor = true),
    SlantedStripeSpec(widthFraction = 0.095f, offsetDelay = 0.24f, alphaWeight = 0.40f, useSecondaryColor = false),
    SlantedStripeSpec(widthFraction = 0.036f, offsetDelay = 0.29f, alphaWeight = 0.26f, useSecondaryColor = true),
)

/**
 * Custom 400ms Slanted Stripes transition overlay for tab and library section switches.
 * - Cancels/restarts cleanly on rapid consecutive taps so overlays never pile up.
 * - Adapts automatically to the active MYATO theme's primary & secondary colors.
 * - Zero allocations inside Canvas onDraw; strictly unmounts once complete so it never blocks UI.
 */
@Composable
fun SlantedStripesTransitionOverlay(
    routeKey: String,
    enabled: Boolean,
    intensity: Float = 1.0f,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyatoColors.current
    val progress = remember { Animatable(0f) }
    var lastCommittedRoute by remember { mutableStateOf<String?>(null) }
    var isTransitionVisible by remember { mutableStateOf(false) }

    LaunchedEffect(routeKey, enabled) {
        if (lastCommittedRoute == null) {
            lastCommittedRoute = routeKey
            isTransitionVisible = false
            return@LaunchedEffect
        }
        if (lastCommittedRoute == routeKey) return@LaunchedEffect
        lastCommittedRoute = routeKey

        if (!enabled) {
            isTransitionVisible = false
            progress.snapTo(0f)
            return@LaunchedEffect
        }

        isTransitionVisible = true
        try {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            )
        } finally {
            isTransitionVisible = false
            progress.snapTo(0f)
        }
    }

    if (!isTransitionVisible || !enabled) return

    val reusablePath = remember { Path() }
    val slantTan = remember { tan(Math.toRadians(22.0)).toFloat() }
    val clampedIntensity = intensity.coerceIn(0.4f, 1.4f)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("slanted_stripes_overlay"),
    ) {
        val t = progress.value.coerceIn(0f, 1f)
        if (t <= 0f || t >= 1f) return@Canvas

        val w = size.width
        val h = size.height
        val slantOffset = h * slantTan

        // Smooth fade-in then complete fade-out so stripes dissolve cleanly before 1.0f
        val envelope = when {
            t < 0.18f -> t / 0.18f
            t > 0.62f -> ((1f - t) / 0.38f).coerceIn(0f, 1f)
            else -> 1f
        }
        if (envelope <= 0.01f) return@Canvas

        // Subtle dark veil trailing behind stripes for depth
        drawRect(
            color = Color.Black.copy(alpha = 0.18f * envelope),
        )

        val totalTravel = w + slantOffset + w * 0.45f

        for (i in DEFAULT_SLANTED_STRIPES.indices) {
            val spec = DEFAULT_SLANTED_STRIPES[i]
            val localT = ((t - spec.offsetDelay) / (1f - spec.offsetDelay * 0.65f)).coerceIn(0f, 1f)
            if (localT <= 0f || localT >= 1f) continue

            val stripeW = w * spec.widthFraction
            // Sweeps from right edge toward the left in sync with screen slide-left transition
            val rightEdgeX = (w + slantOffset + stripeW) - (localT * totalTravel)
            val leftEdgeX = rightEdgeX - stripeW

            reusablePath.reset()
            reusablePath.moveTo(leftEdgeX + slantOffset, 0f)
            reusablePath.lineTo(rightEdgeX + slantOffset, 0f)
            reusablePath.lineTo(rightEdgeX, h)
            reusablePath.lineTo(leftEdgeX, h)
            reusablePath.close()

            val baseColor = if (spec.useSecondaryColor) colors.secondary else colors.primary
            val stripeAlpha = (spec.alphaWeight * envelope * clampedIntensity).coerceIn(0f, 0.78f)
            drawPath(
                path = reusablePath,
                color = baseColor.copy(alpha = stripeAlpha),
            )
        }
    }
}
