package com.example.feature.player

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import com.example.core.audio.AudioSpectrumAnalyzer
import com.example.core.audio.AudioVisualizerFrame
import com.example.core.audio.PlaybackUiState
import com.example.core.audio.VisualizerCaptureStatus
import com.example.core.common.TimeFormatter
import com.example.core.designsystem.AlbumArtPaletteExtractor
import com.example.core.designsystem.AlbumArtwork
import com.example.core.designsystem.BuiltInThemes
import com.example.core.designsystem.LocalMotionSettings
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.metadata.LrcLyricsEngine
import com.example.domain.models.AnimationMode
import com.example.domain.models.ListeningStatsSummary
import com.example.domain.models.LyricLine
import com.example.domain.models.MusicTrack
import com.example.domain.models.PlayerVisualBackgroundStyle
import kotlin.math.sin

/**
 * 1. Real Audio-Synchronized Visualizer Section
 * Sincronizado com o FFT real da sessão do ExoPlayer (Graves/Médios/Agudos),
 * sem ondas falsas quando em pausa, silêncio ou sem permissão.
 */
@Composable
fun AnimatedAudioVisualizerSection(
    spectrumBars: List<Float>,
    isPlaying: Boolean,
    style: String = "BARS",
    intensity: Float = 1.0f,
    volumeFraction: Float = 1.0f,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    visualizerFrame: AudioVisualizerFrame? = null,
    onRequestAudioPermission: (() -> Unit)? = null,
) {
    val colors = LocalMyatoColors.current
    val motion = LocalMotionSettings.current
    val barCount = AudioSpectrumAnalyzer.BAR_COUNT

    val effectiveStatus = visualizerFrame?.status ?: when {
        !isPlaying -> VisualizerCaptureStatus.PAUSED
        spectrumBars.any { it > 0.02f } -> VisualizerCaptureStatus.ACTIVE_SYNCED
        else -> VisualizerCaptureStatus.UNAVAILABLE
    }
    val effectiveBars = visualizerFrame?.bars ?: spectrumBars
    val bassLevel = visualizerFrame?.bassLevel ?: (effectiveBars.take(6).average().toFloat().coerceIn(0f, 1f))
    val midLevel = visualizerFrame?.midLevel ?: (effectiveBars.drop(6).take(7).average().toFloat().coerceIn(0f, 1f))
    val trebleLevel = visualizerFrame?.trebleLevel ?: (effectiveBars.drop(13).take(5).average().toFloat().coerceIn(0f, 1f))
    val beatPulse = visualizerFrame?.beatPulse ?: 0f
    val hasActiveRealSignal = isPlaying &&
        effectiveStatus == VisualizerCaptureStatus.ACTIVE_SYNCED &&
        ((visualizerFrame?.hasRealSignal ?: effectiveBars.any { it > 0.02f }))

    val effectiveIntensity = if (motion.animationsDisabled) {
        0f
    } else {
        intensity * (if (motion.isReducedMotionActive) 0.65f else 1f)
    }

    val heights = (0 until barCount).map { i ->
        val rawSpectrum = effectiveBars.getOrNull(i) ?: 0f
        val target = if (hasActiveRealSignal && rawSpectrum > 0.005f) {
            val beatBoost = if (i < 6) 1f + beatPulse * 0.16f else 1f
            (rawSpectrum * beatBoost * effectiveIntensity.coerceAtLeast(0.2f)).coerceIn(0.03f, 1f)
        } else {
            0.03f
        }
        val animatedHeight by animateFloatAsState(
            targetValue = target,
            animationSpec = if (motion.isAnimationsActive) {
                tween(durationMillis = if (hasActiveRealSignal) 55 else 180, easing = FastOutSlowInEasing)
            } else {
                tween(durationMillis = 0)
            },
            label = "bar_h_$i",
        )
        animatedHeight
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("audio_visualizer_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = if (hasActiveRealSignal) colors.primary else colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "VISUALIZADOR DE ÁUDIO",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    if (hasActiveRealSignal && beatPulse > 0.28f) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(colors.secondary),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (effectiveStatus == VisualizerCaptureStatus.PERMISSION_REQUIRED && onRequestAudioPermission != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.primary.copy(alpha = 0.18f),
                            modifier = Modifier
                                .clickable { onRequestAudioPermission() }
                                .testTag("visualizer_permission_button"),
                        ) {
                            Text(
                                text = "Ativar áudio",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    } else {
                        val statusText = when {
                            effectiveStatus == VisualizerCaptureStatus.PERMISSION_REQUIRED -> "Permissão necessária"
                            effectiveStatus == VisualizerCaptureStatus.DISABLED -> "Desativado"
                            effectiveStatus == VisualizerCaptureStatus.STOPPED -> "Parado"
                            effectiveStatus == VisualizerCaptureStatus.PAUSED || !isPlaying -> "Em pausa"
                            effectiveStatus == VisualizerCaptureStatus.NO_AUDIO_SESSION -> "Aguardando áudio"
                            effectiveStatus == VisualizerCaptureStatus.UNAVAILABLE -> "FFT indisponível"
                            hasActiveRealSignal -> "Sincronizado • Real"
                            else -> "Silêncio detectado"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasActiveRealSignal) colors.primary else colors.textSecondary,
                            modifier = Modifier.testTag("visualizer_status_badge"),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Configurar visualizador",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Canvas renderer according to chosen visualizer style (BARS, WAVE, PEAKS)
            val wavePath = remember { Path() }
            val waveFillPath = remember { Path() }
            val barBrush = remember(colors.primary, colors.secondary) {
                Brush.verticalGradient(colors = listOf(colors.secondary, colors.primary))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val barWidth = (w / (barCount * 1.5f)).coerceAtLeast(6f)
                    val spacing = (w - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)

                    when (style) {
                        "WAVE" -> {
                            wavePath.reset()
                            waveFillPath.reset()
                            waveFillPath.moveTo(0f, h)

                            heights.forEachIndexed { index, fraction ->
                                val x = index * (w / (barCount - 1).coerceAtLeast(1))
                                val y = h - (fraction * h * 0.9f)
                                if (index == 0) {
                                    wavePath.moveTo(x, y)
                                    waveFillPath.lineTo(x, y)
                                } else {
                                    wavePath.lineTo(x, y)
                                    waveFillPath.lineTo(x, y)
                                }
                            }
                            waveFillPath.lineTo(w, h)
                            waveFillPath.close()

                            drawPath(
                                path = waveFillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        colors.primary.copy(alpha = 0.35f),
                                        colors.primary.copy(alpha = 0.05f),
                                    ),
                                ),
                            )
                            drawPath(
                                path = wavePath,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(colors.secondary, colors.primary, colors.secondary),
                                ),
                                style = Stroke(width = 3.5f, cap = StrokeCap.Round),
                            )
                        }

                        "PEAKS" -> {
                            heights.forEachIndexed { index, fraction ->
                                val x = index * (barWidth + spacing)
                                val barH = (fraction * h * 0.88f).coerceAtLeast(4f)
                                val y = h - barH
                                drawRoundRect(
                                    brush = barBrush,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barH),
                                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                                )
                                // Floating peak indicator dot
                                val peakY = (y - 5.dp.toPx()).coerceAtLeast(2f)
                                drawCircle(
                                    color = colors.secondary,
                                    radius = (barWidth / 2f) * 0.85f,
                                    center = Offset(x + (barWidth / 2f), peakY),
                                )
                            }
                        }

                        else -> { // "BARS" - Modern vertical glowing bars
                            heights.forEachIndexed { index, fraction ->
                                val x = index * (barWidth + spacing)
                                val barH = (fraction * h * 0.92f).coerceAtLeast(4f)
                                val y = h - barH
                                drawRoundRect(
                                    brush = barBrush,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barH),
                                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Frequency spectrum bands label indicator driven by real band energies
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "GRAVES (20-250Hz)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasActiveRealSignal && bassLevel > 0.12f) {
                        colors.primary.copy(alpha = (0.55f + bassLevel * 0.45f).coerceIn(0f, 1f))
                    } else {
                        colors.textSecondary.copy(alpha = 0.65f)
                    },
                    fontWeight = if (hasActiveRealSignal && bassLevel > 0.35f) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp,
                )
                Text(
                    text = "MÉDIOS (250-4kHz)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasActiveRealSignal && midLevel > 0.12f) {
                        colors.secondary.copy(alpha = (0.55f + midLevel * 0.45f).coerceIn(0f, 1f))
                    } else {
                        colors.textSecondary.copy(alpha = 0.65f)
                    },
                    fontWeight = if (hasActiveRealSignal && midLevel > 0.35f) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp,
                )
                Text(
                    text = "AGUDOS (4k-20kHz)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasActiveRealSignal && trebleLevel > 0.10f) {
                        colors.primary.copy(alpha = (0.55f + trebleLevel * 0.45f).coerceIn(0f, 1f))
                    } else {
                        colors.textSecondary.copy(alpha = 0.65f)
                    },
                    fontWeight = if (hasActiveRealSignal && trebleLevel > 0.30f) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

/**
 * 2. Upcoming Tracks — Playback Queue Preview Section
 * Exibe de 3 a 5 músicas da fila, opção de tocar direto, remover e reorganizar.
 */
@Composable
fun UpcomingQueueSection(
    playbackState: PlaybackUiState,
    visibleCount: Int = 4,
    onOpenFullQueue: () -> Unit,
    onPlayTrackAtIndex: (Int) -> Unit,
    onRemoveTrack: (Int) -> Unit,
    onMoveTrack: (Int, Int) -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyatoColors.current
    val currentIdx = playbackState.currentIndex
    val queueTracks = playbackState.queueTracks
    var showClearConfirm by remember { mutableStateOf(false) }

    // Upcoming tracks: tracks strictly after current playing index
    val upcomingItems = remember(queueTracks, currentIdx, visibleCount) {
        if (queueTracks.isEmpty()) emptyList()
        else {
            val start = (currentIdx + 1).coerceAtMost(queueTracks.size)
            queueTracks.drop(start).take(visibleCount).mapIndexed { offset, track ->
                (start + offset) to track
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "PRÓXIMAS MÚSICAS",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = "Fila: ${playbackState.activeQueue?.name ?: "Principal"} (${queueTracks.size} faixas)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (queueTracks.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Limpar fila",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    TextButton(onClick = onOpenFullQueue) {
                        Text("Ver Tudo", color = colors.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (upcomingItems.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.altBackground,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (queueTracks.isEmpty()) "Fila vazia." else "Fim da fila atual. Toque para adicionar mais faixas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onOpenFullQueue,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Gerenciar Fila")
                        }
                    }
                }
            } else {
                upcomingItems.forEachIndexed { listIndex, (queueIndex, track) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPlayTrackAtIndex(queueIndex) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "#${listIndex + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(26.dp),
                        )
                        AlbumArtwork(artUri = track.albumArtUri, size = 40.dp, small = true)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${track.artist} • ${TimeFormatter.formatDuration(track.durationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                maxLines = 1,
                            )
                        }

                        // Reorder up/down
                        IconButton(
                            onClick = { if (queueIndex > 0) onMoveTrack(queueIndex, queueIndex - 1) },
                            enabled = queueIndex > 0,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Subir", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { if (queueIndex < queueTracks.lastIndex) onMoveTrack(queueIndex, queueIndex + 1) },
                            enabled = queueIndex < queueTracks.lastIndex,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Descer", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                        // Remove single track
                        IconButton(
                            onClick = { onRemoveTrack(queueIndex) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Remover da fila", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (listIndex < upcomingItems.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            color = colors.textSecondary.copy(alpha = 0.12f),
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = colors.card,
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Limpar Fila?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Deseja esvaziar todas as faixas da fila de reprodução ativa?", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onClearQueue()
                    showClearConfirm = false
                }) {
                    Text("Limpar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}

/**
 * 3. Compact Lyrics Preview Section
 * Exibe versos sincronizados, destaque do verso atual, clique para seek e expansão em tela cheia.
 */
@Composable
fun LyricsPreviewSection(
    track: MusicTrack?,
    positionMs: Long,
    onOpenFullLyrics: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyatoColors.current
    val context = LocalContext.current
    var rawLyrics by remember { mutableStateOf("") }
    var parsedLines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }

    LaunchedEffect(track?.id) {
        if (track != null) {
            val content = LrcLyricsEngine.loadLyricsForTrack(
                context = context,
                trackId = track.id,
                audioFilePath = track.filePath,
                embeddedLyrics = track.embeddedLyrics,
            )
            rawLyrics = content
            parsedLines = LrcLyricsEngine.parseLyrics(content)
        } else {
            rawLyrics = ""
            parsedLines = emptyList()
        }
    }

    val activeIndex = remember(parsedLines, positionMs) {
        LrcLyricsEngine.findActiveLineIndex(parsedLines, positionMs)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "LETRAS DA MÚSICA",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(onClick = { track?.let { onOpenFullLyrics(it.id) } }) {
                    Text("Tela Cheia", color = colors.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(8.dp))

            if (parsedLines.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.altBackground,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Nenhuma letra disponível para esta música.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { track?.let { onOpenFullLyrics(it.id) } },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Adicionar / Editar Letra")
                        }
                    }
                }
            } else {
                // Show a centered window around active lyric line
                val center = if (activeIndex >= 0) activeIndex else 0
                val windowStart = (center - 1).coerceAtLeast(0)
                val windowEnd = (center + 2).coerceAtMost(parsedLines.size)
                val visibleLines = parsedLines.subList(windowStart, windowEnd)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.altBackground,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    ) {
                        visibleLines.forEach { line ->
                            val isCurrent = parsedLines.indexOf(line) == activeIndex
                            val targetTs = line.timestampMs

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) colors.primary.copy(alpha = 0.16f) else Color.Transparent)
                                    .clickable(enabled = targetTs != null) {
                                        targetTs?.let { onSeekTo(it) }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (line.timestampMs != null) {
                                    Text(
                                        text = TimeFormatter.formatDuration(line.timestampMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCurrent) colors.primary else colors.textSecondary.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        modifier = Modifier.width(36.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = line.text,
                                    style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                    color = if (isCurrent) colors.primary else colors.textSecondary,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. Detailed Track Metadata Section
 * Exibe dados técnicos reais do áudio (codec, bitrate, sample rate, tamanho, caminho, etc.).
 */
@Composable
fun TrackDetailsSection(
    track: MusicTrack?,
    onOpenTagEditor: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyatoColors.current
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "INFORMAÇÕES DA FAIXA",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { track?.let { onOpenTagEditor(it.id) } }) {
                        Text("Editar Tags", color = colors.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (isExpanded) "Recolher" else "Expandir",
                            tint = colors.textSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (track == null) {
                Text("Nenhuma música carregada.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            } else {
                // Key metadata grid
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MetaCell("Formato", "${track.formatExtension} (${track.mimeType})", Modifier.weight(1f))
                        MetaCell("Bitrate", if (track.bitrateKbps > 0) "${track.bitrateKbps} kbps" else "Variável / VBR", Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MetaCell("Amostragem", if (track.sampleRateHz > 0) "${track.sampleRateHz} Hz" else "44.1 kHz", Modifier.weight(1f))
                        MetaCell("Tamanho", TimeFormatter.formatFileSize(track.sizeBytes), Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MetaCell("Gênero", track.genre.ifBlank { "Desconhecido" }, Modifier.weight(1f))
                        MetaCell("Ano", if (track.year > 0) track.year.toString() else "--", Modifier.weight(1f))
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp),
                        ) {
                            if (track.albumArtist.isNotBlank()) {
                                MetaCell("Artista do Álbum", track.albumArtist, Modifier.fillMaxWidth())
                            }
                            if (track.composer.isNotBlank()) {
                                MetaCell("Compositor", track.composer, Modifier.fillMaxWidth())
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                MetaCell("Faixa / Disco", "${track.trackNumber.coerceAtLeast(1)} / ${track.discNumber.coerceAtLeast(1)}", Modifier.weight(1f))
                                MetaCell("Favorita", if (track.isFavorite) "Sim (Adicionada)" else "Não", Modifier.weight(1f))
                            }
                            MetaCell("Caminho no Arquivo", track.filePath, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaCell(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalMyatoColors.current
    Surface(
        modifier = modifier.padding(horizontal = 2.dp),
        shape = RoundedCornerShape(10.dp),
        color = colors.altBackground,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.primary, fontSize = 11.sp)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * 5. Playback Statistics Section
 * Métricas reais da música atual e visão geral da biblioteca.
 */
@Composable
fun PlaybackStatsSection(
    track: MusicTrack?,
    allTracks: List<MusicTrack>,
    statsSummary: ListeningStatsSummary,
    onOpenFullStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyatoColors.current

    // Rank of current track among all library tracks sorted by playCount
    val rankText = remember(track, allTracks) {
        if (track == null || track.playCount <= 0 || allTracks.isEmpty()) "--"
        else {
            val sorted = allTracks.sortedByDescending { it.playCount }
            val rankIndex = sorted.indexOfFirst { it.id == track.id }
            if (rankIndex >= 0) "#${rankIndex + 1} mais ouvida" else "--"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.QueryStats,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ESTATÍSTICAS DE REPRODUÇÃO",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(onClick = onOpenFullStats) {
                    Text("Ver Todas", color = colors.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Current Track Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    title = "Reproduções",
                    value = "${track?.playCount ?: 0}x",
                    subtitle = "Nesta faixa",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "Tempo Total",
                    value = TimeFormatter.formatHoursMinutes((track?.playCount ?: 0) * (track?.durationMs ?: 0L)),
                    subtitle = "Ouvido nesta música",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "Ranking",
                    value = rankText,
                    subtitle = "Na biblioteca",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Overall Library Snapshot
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = colors.altBackground,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Biblioteca Geral", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                        Text(
                            text = "${TimeFormatter.formatHoursMinutes(statsSummary.totalListenMs)} ouvidas • ${statsSummary.totalPlays} reproduções totais",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    statsSummary.topArtists.firstOrNull()?.let { topArt ->
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Top Artista", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                            Text(topArt.artist, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    val colors = LocalMyatoColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colors.altBackground,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = colors.primary, fontSize = 10.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, fontSize = 9.sp, maxLines = 1)
        }
    }
}

/**
 * 6. Subtle Floating Ambient Particles & Lighting
 */
@Composable
fun FloatingAmbientParticles(
    isPlaying: Boolean,
    particleColor: Color,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotionSettings.current
    if (!isPlaying || !motion.isAnimationsActive || motion.isReducedMotionActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particle_phase",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 8 soft floating sparkles
        for (i in 0 until 8) {
            val progress = (phase + (i * 0.125f)) % 1f
            val x = (sin((phase * 6.28f) + i) * 0.35f + 0.5f) * w
            val y = h - (progress * h)
            val alpha = (sin(progress * 3.14159f) * 0.25f * motion.effectiveIntensity).coerceIn(0f, 0.35f)
            val radius = 3.dp.toPx() + (i % 3) * 1.5.dp.toPx()

            drawCircle(
                color = particleColor.copy(alpha = alpha),
                radius = radius,
                center = Offset(x, y),
            )
        }
    }
}

/**
 * 7. Player Settings BottomSheet
 * Permite personalizar estilo visual da reprodução, visualizador, intensidade, acessibilidade/bateria, partículas e fila.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsSheet(
    visualizerEnabled: Boolean,
    visualizerStyle: String,
    animationIntensity: Float,
    particlesEnabled: Boolean,
    ambientBgEnabled: Boolean,
    showStatsEnabled: Boolean,
    queueCount: Int,
    animatedTransitions: Boolean,
    playerVisualStyle: PlayerVisualBackgroundStyle = PlayerVisualBackgroundStyle.AUDIO_SPECTRUM,
    animationsDisabled: Boolean = false,
    reduceMotion: Boolean = false,
    reduceEffectsOnLowBattery: Boolean = true,
    onDismiss: () -> Unit,
    onSetVisualizerEnabled: (Boolean) -> Unit,
    onSetVisualizerStyle: (String) -> Unit,
    onSetAnimationIntensity: (Float) -> Unit,
    onSetParticlesEnabled: (Boolean) -> Unit,
    onSetAmbientBgEnabled: (Boolean) -> Unit,
    onSetShowStatsEnabled: (Boolean) -> Unit,
    onSetQueueCount: (Int) -> Unit,
    onSetAnimatedTransitions: (Boolean) -> Unit,
    onSetPlayerVisualStyle: (PlayerVisualBackgroundStyle) -> Unit = {},
    onSetAnimationsDisabled: (Boolean) -> Unit = {},
    onSetReduceMotion: (Boolean) -> Unit = {},
    onSetReduceEffectsOnLowBattery: (Boolean) -> Unit = {},
) {
    val colors = LocalMyatoColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.altBackground,
        contentColor = colors.textPrimary,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Settings, contentDescription = null, tint = colors.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Ajustes da Tela de Reprodução",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))

            PlayerVisualStyleSelectorSection(
                selectedStyle = playerVisualStyle,
                effectIntensity = animationIntensity,
                animationsDisabled = animationsDisabled,
                reduceMotion = reduceMotion,
                reduceEffectsOnLowBattery = reduceEffectsOnLowBattery,
                onSelectStyle = onSetPlayerVisualStyle,
                onIntensityChange = onSetAnimationIntensity,
                onSetAnimationsDisabled = onSetAnimationsDisabled,
                onSetReduceMotion = onSetReduceMotion,
                onSetReduceEffectsOnLowBattery = onSetReduceEffectsOnLowBattery,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.textSecondary.copy(alpha = 0.15f))

            // Visualizer Toggle
            SettingSwitchRow("Ativar Visualizador de Áudio", visualizerEnabled, onSetVisualizerEnabled)

            if (visualizerEnabled) {
                Spacer(Modifier.height(8.dp))
                Text("Estilo do Painel Visualizador", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("BARS" to "Barras", "WAVE" to "Ondas", "PEAKS" to "Picos").forEach { (key, label) ->
                        FilterChip(
                            selected = visualizerStyle == key,
                            onClick = { onSetVisualizerStyle(key) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = colors.onPrimary,
                            ),
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = colors.textSecondary.copy(alpha = 0.15f))

            // Ambient background lighting & particles
            SettingSwitchRow("Cores Dinâmicas da Capa", ambientBgEnabled, onSetAmbientBgEnabled)
            SettingSwitchRow("Partículas Luminosas Sutis", particlesEnabled, onSetParticlesEnabled)
            SettingSwitchRow("Painel de Estatísticas", showStatsEnabled, onSetShowStatsEnabled)
            SettingSwitchRow("Transições Animadas de Faixa", animatedTransitions, onSetAnimatedTransitions)

            Spacer(Modifier.height(8.dp))
            Text("Músicas na Prévia da Fila", style = MaterialTheme.typography.labelMedium, color = colors.primary)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 4, 5).forEach { count ->
                    FilterChip(
                        selected = queueCount == count,
                        onClick = { onSetQueueCount(count) },
                        label = { Text("$count faixas") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.onPrimary,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalMyatoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
