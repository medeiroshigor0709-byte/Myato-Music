package com.example.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Lightweight tactile spring scale feedback on press. Respects animationsDisabled / reduceMotion.
 */
fun Modifier.pressScaleFeedback(
    interactionSource: MutableInteractionSource? = null,
    pressedScale: Float = 0.96f,
): Modifier = composed {
    val motion = LocalMotionSettings.current
    if (!motion.isAnimationsActive || motion.isReducedMotionActive || interactionSource == null) {
        this
    } else {
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) pressedScale else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "press_scale",
        )
        this.scale(scale)
    }
}

/**
 * Subtle staggered fade + vertical entry for playlist cards & top library items.
 * Capped at index <= 8 so deep list scrolling remains instantaneous and 60fps+.
 */
@Composable
fun StaggeredEntryContainer(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val motion = LocalMotionSettings.current
    val shouldAnimate = motion.isAnimationsActive && !motion.isReducedMotionActive && index in 0..8

    if (!shouldAnimate) {
        Box(modifier = modifier) { content() }
        return
    }

    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(18f) }

    LaunchedEffect(Unit) {
        val delayMs = (index * 28L).coerceAtMost(200L)
        if (delayMs > 0L) kotlinx.coroutines.delay(delayMs)
        alpha.animateTo(1f, tween(durationMillis = 220))
    }
    LaunchedEffect(Unit) {
        val delayMs = (index * 28L).coerceAtMost(200L)
        if (delayMs > 0L) kotlinx.coroutines.delay(delayMs)
        offsetY.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
    }

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            this.translationY = offsetY.value
        },
    ) {
        content()
    }
}

/**
 * Smooth Play <-> Pause icon transition with subtle scale crossfade.
 */
@Composable
fun AnimatedPlayPauseIcon(
    isPlaying: Boolean,
    tint: Color,
    iconSize: Dp = 24.dp,
) {
    val motion = LocalMotionSettings.current
    if (!motion.isAnimationsActive) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = stringResource(R.string.cd_play_pause),
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
        return
    }

    val dur = if (motion.isReducedMotionActive) 120 else 200
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (fadeIn(tween(dur)) + scaleIn(initialScale = 0.78f, animationSpec = tween(dur)))
                .togetherWith(fadeOut(tween(dur)) + scaleOut(targetScale = 0.78f, animationSpec = tween(dur)))
        },
        label = "play_pause_morph",
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = stringResource(R.string.cd_play_pause),
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Favorite heart button with spring bounce and subtle expanding ring pulse when favorited.
 */
@Composable
fun AnimatedFavoriteIconButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyatoColors.current
    val motion = LocalMotionSettings.current
    val pulseProgress = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }
    var previousFav by remember { mutableStateOf(isFavorite) }

    LaunchedEffect(isFavorite) {
        if (isFavorite && !previousFav && motion.isAnimationsActive && !motion.isReducedMotionActive) {
            scaleAnim.snapTo(1.32f)
            pulseProgress.snapTo(0f)
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
            )
        }
        previousFav = isFavorite
    }

    LaunchedEffect(isFavorite) {
        if (isFavorite && motion.isAnimationsActive && !motion.isReducedMotionActive) {
            pulseProgress.snapTo(0.05f)
            pulseProgress.animateTo(1f, tween(380))
            pulseProgress.snapTo(0f)
        }
    }

    Box(contentAlignment = Alignment.Center) {
        if (pulseProgress.value > 0f && pulseProgress.value < 1f) {
            Canvas(modifier = Modifier.size(44.dp)) {
                val p = pulseProgress.value
                drawCircle(
                    color = colors.primary.copy(alpha = (1f - p) * 0.6f),
                    radius = (size.minDimension * 0.25f) + (size.minDimension * 0.35f * p),
                    style = Stroke(width = 2.5.dp.toPx() * (1f - p * 0.6f)),
                )
            }
        }
        IconButton(
            onClick = onClick,
            modifier = modifier.minimumInteractiveComponentSize(),
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(R.string.cd_favorite),
                tint = if (isFavorite) colors.primary else colors.textSecondary,
                modifier = Modifier.scale(scaleAnim.value),
            )
        }
    }
}
