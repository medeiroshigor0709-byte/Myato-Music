package com.example.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.LocalMotionSettings
import com.example.core.designsystem.LocalMyatoColors
import com.example.domain.models.PlayerVisualBackgroundStyle
import kotlin.math.roundToInt

/**
 * Reusable selector for "Estilo visual da reprodução" and motion/battery controls.
 * Used inside PlayerSettingsSheet (FullPlayer) and SettingsScreen (Configurações).
 */
@Composable
fun PlayerVisualStyleSelectorSection(
    selectedStyle: PlayerVisualBackgroundStyle,
    effectIntensity: Float,
    animationsDisabled: Boolean,
    reduceMotion: Boolean,
    reduceEffectsOnLowBattery: Boolean,
    onSelectStyle: (PlayerVisualBackgroundStyle) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onSetAnimationsDisabled: (Boolean) -> Unit,
    onSetReduceMotion: (Boolean) -> Unit,
    onSetReduceEffectsOnLowBattery: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyatoColors.current
    val motion = LocalMotionSettings.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Estilo visual da reprodução",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Escolha o fundo animado da tela Reproduzindo Agora sem interromper o áudio.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlayerVisualBackgroundStyle.entries.forEach { style ->
                val isSelected = selectedStyle == style
                val tag = when (style) {
                    PlayerVisualBackgroundStyle.AUDIO_SPECTRUM -> "visual_style_audio_spectrum"
                    PlayerVisualBackgroundStyle.AURORA_LIQUID_WAVES -> "visual_style_aurora_liquid_waves"
                    PlayerVisualBackgroundStyle.MINIMAL_MOTION -> "visual_style_minimal_motion"
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectStyle(style) }
                        .testTag(tag),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) colors.primary.copy(alpha = 0.14f) else colors.card,
                    ),
                    border = if (isSelected) BorderStroke(1.5.dp, colors.primary) else BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.12f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VisualStyleThumbnailPreview(
                            style = style,
                            modifier = Modifier.size(width = 76.dp, height = 52.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = style.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) colors.primary else colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = style.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selecionado",
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Effect intensity controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Intensidade dos efeitos",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "${(effectIntensity * 100f).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
                fontWeight = FontWeight.Bold,
            )
        }

        Slider(
            value = effectIntensity.coerceIn(0.35f, 1.5f),
            onValueChange = onIntensityChange,
            valueRange = 0.35f..1.5f,
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.textSecondary.copy(alpha = 0.25f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider_effect_intensity"),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(0.5f to "Suave", 1.0f to "Normal", 1.5f to "Intensa").forEach { (value, label) ->
                FilterChip(
                    selected = kotlin.math.abs(effectIntensity - value) < 0.12f,
                    onClick = { onIntensityChange(value) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primary,
                        selectedLabelColor = if (colors.isLight) Color.White else Color.Black,
                    ),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        MotionToggleRow(
            icon = Icons.Rounded.MotionPhotosOff,
            title = "Desativar animações",
            subtitle = "Desliga efeitos de fundo, transições e micro-animações.",
            checked = animationsDisabled,
            onCheckedChange = onSetAnimationsDisabled,
            testTag = "toggle_disable_animations",
        )

        MotionToggleRow(
            icon = Icons.Rounded.Waves,
            title = "Reduzir movimentos",
            subtitle = if (motion.systemReducedMotion) {
                "Ativo também pela acessibilidade do Android."
            } else {
                "Suaviza transições e limita movimentos intensos."
            },
            checked = reduceMotion,
            onCheckedChange = onSetReduceMotion,
            testTag = "toggle_reduce_motion",
        )

        MotionToggleRow(
            icon = Icons.Rounded.BatterySaver,
            title = "Reduzir efeitos em bateria baixa",
            subtitle = if (motion.isLowBatteryOrPowerSave && reduceEffectsOnLowBattery) {
                "Modo econômico ativo no momento — usando Minimal Motion."
            } else {
                "Alterna para efeitos leves quando a bateria estiver <= 15% ou em economia."
            },
            checked = reduceEffectsOnLowBattery,
            onCheckedChange = onSetReduceEffectsOnLowBattery,
            testTag = "toggle_low_battery_effects",
        )
    }
}

@Composable
private fun VisualStyleThumbnailPreview(
    style: PlayerVisualBackgroundStyle,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyatoColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.background),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (style) {
                PlayerVisualBackgroundStyle.AUDIO_SPECTRUM -> {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(colors.primary.copy(alpha = 0.22f), colors.background),
                        ),
                    )
                    val heights = floatArrayOf(0.32f, 0.65f, 0.88f, 0.52f, 0.78f, 0.44f, 0.92f, 0.58f, 0.36f)
                    val gap = 2.dp.toPx()
                    val bw = (w - 10.dp.toPx() - gap * (heights.size - 1)) / heights.size
                    heights.forEachIndexed { i, frac ->
                        val bh = h * 0.72f * frac
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(colors.secondary, colors.primary)),
                            topLeft = Offset(5.dp.toPx() + i * (bw + gap), h - 4.dp.toPx() - bh),
                            size = Size(bw, bh),
                            cornerRadius = CornerRadius(bw / 2f, bw / 2f),
                        )
                    }
                }

                PlayerVisualBackgroundStyle.AURORA_LIQUID_WAVES -> {
                    drawRect(color = colors.altBackground)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(colors.primary.copy(alpha = 0.55f), Color.Transparent),
                            center = Offset(w * 0.28f, h * 0.30f),
                            radius = w * 0.55f,
                        ),
                        center = Offset(w * 0.28f, h * 0.30f),
                        radius = w * 0.55f,
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(colors.secondary.copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(w * 0.76f, h * 0.45f),
                            radius = w * 0.48f,
                        ),
                        center = Offset(w * 0.76f, h * 0.45f),
                        radius = w * 0.48f,
                    )
                    val wave = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, h * 0.65f)
                        cubicTo(w * 0.35f, h * 0.42f, w * 0.65f, h * 0.86f, w, h * 0.58f)
                        lineTo(w, h)
                        close()
                    }
                    drawPath(wave, color = colors.primary.copy(alpha = 0.35f))
                }

                PlayerVisualBackgroundStyle.MINIMAL_MOTION -> {
                    drawRect(color = colors.background)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(colors.primary.copy(alpha = 0.28f), Color.Transparent),
                            center = Offset(w * 0.5f, h * 0.48f),
                            radius = w * 0.52f,
                        ),
                        center = Offset(w * 0.5f, h * 0.48f),
                        radius = w * 0.52f,
                    )
                }
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
            shape = CircleShape,
            color = colors.background.copy(alpha = 0.65f),
        ) {
            Icon(
                imageVector = when (style) {
                    PlayerVisualBackgroundStyle.AUDIO_SPECTRUM -> Icons.Rounded.GraphicEq
                    PlayerVisualBackgroundStyle.AURORA_LIQUID_WAVES -> Icons.Rounded.Waves
                    PlayerVisualBackgroundStyle.MINIMAL_MOTION -> Icons.Rounded.AutoAwesome
                },
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier
                    .padding(3.dp)
                    .size(12.dp),
            )
        }
    }
}

@Composable
private fun MotionToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    val colors = LocalMyatoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}
