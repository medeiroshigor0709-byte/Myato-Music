package com.example.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Official MYATO MUSIC square rounded emblem:
 * - Deep black rounded square (#050505 -> #111111) with subtle metallic top sheen
 * - Vertical orange/amber equalizer frequency bars in the background (optionally animated)
 * - Angular white soundwave shaped as the letter M in the center foreground
 * - Stylized musical note on the right with white tilted stem and glowing orange circular notehead
 *
 * Brand colors are strictly invariant across all app themes.
 */
@Composable
fun MyatoOfficialSymbol(
    modifier: Modifier = Modifier,
    animateBars: Boolean = false,
    showOuterGlow: Boolean = true,
    cornerRadiusFraction: Float = 0.24f,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "myato_logo_eq")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "myato_logo_eq_phase",
    )
    val effectivePhase = if (animateBars) phase else 0.75f

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Logomarca oficial MYATO MUSIC"
        },
    ) {
        val w = size.width
        val h = size.height
        val corner = w * cornerRadiusFraction

        // Subtle ambient orange aura behind the rounded square
        if (showOuterGlow) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MyatoBrandOrange.copy(alpha = 0.24f),
                        MyatoBrandOrangeVibrant.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.72f,
                ),
                radius = w * 0.72f,
                center = Offset(w * 0.5f, h * 0.5f),
            )
        }

        // Rounded square dark surface (#050505 -> #111111)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(MyatoBrandBlackSecondary, MyatoBrandBlack),
                start = Offset(0f, 0f),
                end = Offset(w, h),
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner),
        )

        // Subtle dark glossy diagonal sheen at top edge
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    MyatoBrandWhite.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = h * 0.45f,
            ),
            topLeft = Offset(1f, 1f),
            size = Size(w - 2f, h - 2f),
            cornerRadius = CornerRadius(corner, corner),
        )

        // Subtle orange/white precision rim
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    MyatoBrandOrange.copy(alpha = 0.35f),
                    MyatoBrandWhite.copy(alpha = 0.12f),
                    MyatoBrandAmber.copy(alpha = 0.30f),
                ),
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = (w * 0.018f).coerceAtLeast(1f)),
        )

        // Equalizer bars in background (varying rhythmic heights, orange & amber palette)
        val baseHeights = floatArrayOf(0.28f, 0.44f, 0.62f, 0.38f, 0.72f, 0.50f, 0.66f, 0.40f, 0.32f)
        val barCount = baseHeights.size
        val eqLeft = w * 0.14f
        val eqRight = w * 0.86f
        val eqSpan = eqRight - eqLeft
        val barWidth = (eqSpan / (barCount * 1.65f)).coerceAtLeast(1.5f)
        val gap = (eqSpan - barCount * barWidth) / (barCount - 1).coerceAtLeast(1)
        val eqBottom = h * 0.82f
        val maxEqHeight = h * 0.56f

        for (i in 0 until barCount) {
            val waveMod = if (animateBars) {
                0.80f + 0.24f * abs(sin(effectivePhase + i * 0.68f))
            } else {
                1f
            }
            val barH = (maxEqHeight * baseHeights[i] * waveMod).coerceIn(h * 0.10f, maxEqHeight)
            val x = eqLeft + i * (barWidth + gap)
            val y = eqBottom - barH
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MyatoBrandOrangeLight.copy(alpha = 0.90f),
                        MyatoBrandOrange.copy(alpha = 0.75f),
                        MyatoBrandAmber.copy(alpha = 0.55f),
                    ),
                    startY = y,
                    endY = eqBottom,
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth * 0.5f, barWidth * 0.5f),
            )
        }

        // Sharp angular white M-shaped soundwave in center foreground
        val mStrokeWidth = (w * 0.082f).coerceAtLeast(2f)
        val mPath = Path().apply {
            moveTo(w * 0.17f, h * 0.68f)
            lineTo(w * 0.24f, h * 0.33f)
            lineTo(w * 0.35f, h * 0.56f)
            lineTo(w * 0.46f, h * 0.26f)
            lineTo(w * 0.57f, h * 0.60f)
            lineTo(w * 0.66f, h * 0.42f)
        }

        // Dark separation halo so white soundwave stands crisply above equalizer bars
        drawPath(
            path = mPath,
            color = MyatoBrandBlack.copy(alpha = 0.78f),
            style = Stroke(
                width = mStrokeWidth * 1.55f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Miter,
            ),
        )
        drawPath(
            path = mPath,
            color = MyatoBrandWhite,
            style = Stroke(
                width = mStrokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Miter,
            ),
        )

        // Stylized musical note on the right: tilted white stem + glowing vibrant orange head
        val noteHeadCenter = Offset(w * 0.69f, h * 0.69f)
        val noteHeadRadius = w * 0.088f
        val stemStart = Offset(w * 0.745f, h * 0.65f)
        val stemTop = Offset(w * 0.79f, h * 0.25f)
        val flagEnd = Offset(w * 0.87f, h * 0.33f)

        // Subtle orange glow around notehead
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    MyatoBrandOrangeLight.copy(alpha = 0.52f),
                    MyatoBrandOrangeVibrant.copy(alpha = 0.20f),
                    Color.Transparent,
                ),
                center = noteHeadCenter,
                radius = noteHeadRadius * 2.5f,
            ),
            radius = noteHeadRadius * 2.5f,
            center = noteHeadCenter,
        )

        val stemPath = Path().apply {
            moveTo(stemStart.x, stemStart.y)
            lineTo(stemTop.x, stemTop.y)
            lineTo(flagEnd.x, flagEnd.y)
        }
        drawPath(
            path = stemPath,
            color = MyatoBrandBlack.copy(alpha = 0.75f),
            style = Stroke(
                width = mStrokeWidth * 1.25f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        drawPath(
            path = stemPath,
            color = MyatoBrandWhite,
            style = Stroke(
                width = mStrokeWidth * 0.82f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Note circular head in vibrant orange
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    MyatoBrandOrangeLight,
                    MyatoBrandOrangeVibrant,
                    MyatoBrandAmber,
                ),
                center = Offset(
                    noteHeadCenter.x - noteHeadRadius * 0.25f,
                    noteHeadCenter.y - noteHeadRadius * 0.25f,
                ),
                radius = noteHeadRadius * 1.4f,
            ),
            radius = noteHeadRadius,
            center = noteHeadCenter,
        )
    }
}

/**
 * Official MYATO MUSIC stacked wordmark:
 * - "MYATO" bold geometric uppercase white (#FFFFFF)
 * - "— M U S I C —" fine wide-spaced uppercase orange (#FF8A00) with lateral orange dashes
 */
@Composable
fun MyatoOfficialWordmark(
    modifier: Modifier = Modifier,
    titleSizeSp: Float = 30f,
    subtitleSizeSp: Float = 12f,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "MYATO",
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = titleSizeSp.sp,
            letterSpacing = (titleSizeSp * 0.16f).sp,
            color = MyatoBrandWhite,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .width((subtitleSizeSp * 1.5f).dp)
                    .height(1.5.dp)
                    .background(MyatoBrandOrange),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "MUSIC",
                fontFamily = PlusJakartaFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = subtitleSizeSp.sp,
                letterSpacing = (subtitleSizeSp * 0.55f).sp,
                color = MyatoBrandOrange,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width((subtitleSizeSp * 1.5f).dp)
                    .height(1.5.dp)
                    .background(MyatoBrandOrange),
            )
        }
    }
}

/**
 * Full vertical official brand lockup combining symbol + wordmark.
 * Automatically wraps in an official #050505 dark container on light themes so white/orange contrast never drops.
 */
@Composable
fun MyatoOfficialBrandLockup(
    modifier: Modifier = Modifier,
    symbolSize: Dp = 112.dp,
    animateBars: Boolean = false,
    showGlow: Boolean = true,
    useContrastCardOnLightTheme: Boolean = true,
) {
    val colors = LocalMyatoColors.current
    val needsDarkCard = useContrastCardOnLightTheme && colors.isLight

    val innerContent: @Composable () -> Unit = {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = if (needsDarkCard) 28.dp else 0.dp, vertical = if (needsDarkCard) 22.dp else 0.dp)
                .testTag("myato_official_brand_lockup"),
        ) {
            MyatoOfficialSymbol(
                modifier = Modifier.size(symbolSize),
                animateBars = animateBars,
                showOuterGlow = showGlow,
            )
            Spacer(Modifier.height(18.dp))
            MyatoOfficialWordmark(
                titleSizeSp = (symbolSize.value * 0.27f).coerceIn(22f, 36f),
                subtitleSizeSp = (symbolSize.value * 0.11f).coerceIn(10f, 14f),
            )
        }
    }

    if (needsDarkCard) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(26.dp),
            color = MyatoBrandBlack,
            border = BorderStroke(1.dp, MyatoBrandOrange.copy(alpha = 0.28f)),
            shadowElevation = 6.dp,
        ) {
            innerContent()
        }
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            innerContent()
        }
    }
}

/**
 * Compact horizontal brand badge for Home, Onboarding, and Settings headers.
 * Always preserves official white + orange + #050505 identity across all light and dark themes.
 */
@Composable
fun MyatoCompactBrandBadge(
    modifier: Modifier = Modifier,
    symbolSize: Dp = 38.dp,
    animateBars: Boolean = false,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = "MYATO MUSIC"
            }
            .testTag("myato_compact_brand_badge"),
        shape = RoundedCornerShape(14.dp),
        color = MyatoBrandBlack,
        border = BorderStroke(1.dp, MyatoBrandOrange.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MyatoOfficialSymbol(
                modifier = Modifier.size(symbolSize),
                animateBars = animateBars,
                showOuterGlow = false,
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "MYATO",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 2.4.sp,
                    color = MyatoBrandWhite,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(1.dp)
                            .background(MyatoBrandOrange),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "MUSIC",
                        fontFamily = PlusJakartaFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp,
                        letterSpacing = 3.5.sp,
                        color = MyatoBrandOrange,
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(1.dp)
                            .background(MyatoBrandOrange),
                    )
                }
            }
        }
    }
}
