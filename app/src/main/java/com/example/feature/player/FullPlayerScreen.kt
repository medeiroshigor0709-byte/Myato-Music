package com.example.feature.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.audio.PlaybackUiState
import com.example.core.common.TimeFormatter
import com.example.core.designsystem.AlbumArtwork
import com.example.core.designsystem.AnimatedFavoriteIconButton
import com.example.core.designsystem.AnimatedPlayPauseIcon
import com.example.core.designsystem.LocalMotionSettings
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.designsystem.pressScaleFeedback
import com.example.domain.models.AnimationMode
import com.example.domain.models.Bookmark
import com.example.domain.models.ListeningStatsSummary
import com.example.domain.models.MusicNote
import com.example.domain.models.MusicTrack
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.models.PlayerAppearance
import com.example.domain.models.PlayerVisualBackgroundStyle
import com.example.domain.models.RepeatMode as MyatoRepeatMode
import kotlinx.coroutines.flow.StateFlow

/**
 * 13. Full Player Screen — Cinematic artwork, dynamic ambient backdrop, interactive progress,
 * primary playback controls, secondary tools, and fully expanded lower panel featuring:
 * 1. Animated Audio Visualizer (bass/mid/treble, animated response)
 * 2. Upcoming Tracks Queue Preview (with direct play, reorder, remove, clear)
 * 3. Compact Synchronized Lyrics (current line highlighted, click to seek, full view)
 * 4. Detailed Track Metadata & Audio Specs (codec, bitrate, sample rate, size, path, etc.)
 * 5. Playback Statistics & Library Overview (plays, rank, listening time)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    playbackState: PlaybackUiState,
    spectrumBars: List<Float>,
    visualizerEnabled: Boolean,
    bookmarks: List<Bookmark>,
    note: MusicNote?,
    allTracks: List<MusicTrack> = emptyList(),
    statsSummary: ListeningStatsSummary = ListeningStatsSummary(),
    visualizerStyle: String = "BARS",
    animationIntensity: Float = 1.0f,
    particlesEnabled: Boolean = true,
    ambientBgEnabled: Boolean = true,
    showStatsEnabled: Boolean = true,
    queueVisibleCount: Int = 4,
    animatedTransitions: Boolean = true,
    hasRealFftSignal: Boolean = false,
    playerVisualStyle: PlayerVisualBackgroundStyle = PlayerVisualBackgroundStyle.AUDIO_SPECTRUM,
    animationsDisabled: Boolean = false,
    reduceMotion: Boolean = false,
    reduceEffectsOnLowBattery: Boolean = true,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward10s: () -> Unit,
    onSeekBack10s: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: (Long) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenTagEditor: (Long) -> Unit,
    onOpenStats: () -> Unit = {},
    onPlayQueueIndex: (Int) -> Unit = {},
    onRemoveFromQueue: (Int) -> Unit = {},
    onMoveQueueTrack: (Int, Int) -> Unit = { _, _ -> },
    onClearQueue: () -> Unit = {},
    onAddBookmark: (Long, String, Long) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onSaveNote: (Long, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onSetVisualizerEnabled: (Boolean) -> Unit = {},
    onSetVisualizerStyle: (String) -> Unit = {},
    onSetAnimationIntensity: (Float) -> Unit = {},
    onSetParticlesEnabled: (Boolean) -> Unit = {},
    onSetAmbientBgEnabled: (Boolean) -> Unit = {},
    onSetShowStatsEnabled: (Boolean) -> Unit = {},
    onSetQueueCount: (Int) -> Unit = {},
    onSetAnimatedTransitions: (Boolean) -> Unit = {},
    onSetPlayerVisualStyle: (PlayerVisualBackgroundStyle) -> Unit = {},
    onSetAnimationsDisabled: (Boolean) -> Unit = {},
    onSetReduceMotion: (Boolean) -> Unit = {},
    onSetReduceEffectsOnLowBattery: (Boolean) -> Unit = {},
    progressPositionFlow: StateFlow<Long>? = null,
) {
    val colors = LocalMyatoColors.current
    val motion = LocalMotionSettings.current
    val scrollState = rememberScrollState()
    val track = playbackState.currentTrack
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showNoteSheet by remember { mutableStateOf(false) }
    var showPlayerSettingsSheet by remember { mutableStateOf(false) }

    val duration = playbackState.durationMs.coerceAtLeast(1L)

    val coverScale by animateFloatAsState(
        targetValue = if (!motion.isAnimationsActive || motion.isReducedMotionActive) 1f else if (playbackState.isPlaying) 1f else 0.94f,
        animationSpec = spring(),
        label = "cover_scale",
    )

    val shouldSpinVinyl = colors.playerAppearance == PlayerAppearance.VINYL_DISC &&
        playbackState.isPlaying &&
        motion.isAnimationsActive &&
        !motion.isReducedMotionActive &&
        colors.animationMode != AnimationMode.BATTERY_SAVER

    val spinAngle = if (shouldSpinVinyl) {
        val spinTransition = rememberInfiniteTransition(label = "vinyl_spin")
        spinTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(14000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "vinyl_angle",
        ).value
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("full_player_screen"),
        contentAlignment = Alignment.TopCenter,
    ) {
        PlayerAnimatedBackground(
            track = track,
            isPlaying = playbackState.isPlaying,
            spectrumBars = spectrumBars,
            hasRealFftSignal = hasRealFftSignal,
            ambientBgEnabled = ambientBgEnabled,
            scrollOffsetPx = scrollState.value.toFloat(),
            modifier = Modifier.fillMaxSize(),
        )

        // Optional subtle floating sparkles (suspended when paused, reduced motion, or disabled)
        if (particlesEnabled && motion.isAnimationsActive && !motion.isReducedMotionActive && playbackState.isPlaying) {
            FloatingAmbientParticles(
                isPlaying = playbackState.isPlaying,
                particleColor = colors.primary,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.cd_back), tint = colors.textPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOCANDO DA FILA", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                    Text(
                        text = playbackState.activeQueue?.name ?: "Fila Principal",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showPlayerSettingsSheet = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_settings_button"),
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Ajustes do Player", tint = colors.textSecondary)
                    }
                    IconButton(onClick = onOpenQueue, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = stringResource(R.string.cd_queue), tint = colors.textPrimary)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Artwork Showcase
            val rotateMod = if (
                colors.playerAppearance == PlayerAppearance.VINYL_DISC &&
                playbackState.isPlaying &&
                colors.animationMode != AnimationMode.BATTERY_SAVER
            ) {
                Modifier.rotate(spinAngle)
            } else Modifier

            val coverSize = if (colors.playerAppearance == PlayerAppearance.COMPACT) 200.dp else 268.dp
            AlbumArtwork(
                artUri = track?.albumArtUri,
                size = coverSize,
                small = false,
                modifier = Modifier
                    .scale(if (colors.animationMode == AnimationMode.BATTERY_SAVER) 1f else coverScale)
                    .then(rotateMod),
            )

            Spacer(Modifier.height(16.dp))

            // Title, Artist & Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track?.title ?: "Nenhuma música selecionada",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = track?.let { "${it.artist} • ${it.album}" } ?: "Escolha uma música na biblioteca",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AnimatedFavoriteIconButton(
                    isFavorite = track?.isFavorite == true,
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("player_favorite_button"),
                )
            }

            playbackState.errorMessage?.let { err ->
                Spacer(Modifier.height(6.dp))
                Text(err, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(10.dp))

            // Interactive Progress Bar (isolated recomposition scope for 500ms ticks)
            FullPlayerProgressSection(
                fallbackPositionMs = playbackState.positionMs,
                durationMs = duration,
                rawDurationMs = playbackState.durationMs,
                formatExtension = track?.formatExtension ?: "--",
                progressPositionFlow = progressPositionFlow,
                onSeekTo = onSeekTo,
                onSeekBack10s = onSeekBack10s,
                onSeekForward10s = onSeekForward10s,
            )

            Spacer(Modifier.height(10.dp))

            // Primary Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = stringResource(R.string.cd_shuffle),
                        tint = if (playbackState.isShuffleEnabled) colors.primary else colors.textSecondary,
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(52.dp).testTag("player_prev_button")) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.cd_prev_track), tint = colors.textPrimary, modifier = Modifier.size(34.dp))
                }
                val playInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = onPlayPause,
                    interactionSource = playInteraction,
                    modifier = Modifier
                        .size(72.dp)
                        .pressScaleFeedback(playInteraction, pressedScale = 0.92f)
                        .testTag("player_play_pause_button")
                        .clip(colors.resolveButtonShape())
                        .background(colors.buttonContainer),
                ) {
                    AnimatedPlayPauseIcon(
                        isPlaying = playbackState.isPlaying,
                        tint = colors.buttonContent,
                        iconSize = 38.dp,
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(52.dp).testTag("player_next_button")) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.cd_next_track), tint = colors.textPrimary, modifier = Modifier.size(34.dp))
                }
                IconButton(onClick = onCycleRepeat, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == MyatoRepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = stringResource(R.string.cd_repeat),
                        tint = if (playbackState.repeatMode != MyatoRepeatMode.OFF) colors.primary else colors.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Volume Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Volume", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = playbackState.volumeFraction,
                    onValueChange = onVolumeChange,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.secondary,
                        activeTrackColor = colors.secondary,
                        inactiveTrackColor = colors.card,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(10.dp))

            // Secondary Tools Bar: Lyrics, Bookmarks, Notes, EQ, Timer, Tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                PlayerActionPill(Icons.Rounded.Lyrics, "Letras") { track?.let { onOpenLyrics(it.id) } }
                PlayerActionPill(Icons.Rounded.BookmarkAdd, "Marcadores (${bookmarks.size})") { showBookmarksSheet = true }
                PlayerActionPill(Icons.Rounded.Notes, "Notas") { showNoteSheet = true }
                PlayerActionPill(Icons.Rounded.Equalizer, "EQ", onOpenEqualizer)
                PlayerActionPill(Icons.Rounded.Bedtime, "Timer", onOpenSleepTimer)
                PlayerActionPill(Icons.Rounded.Edit, "Tags") { track?.let { onOpenTagEditor(it.id) } }
            }

            Spacer(Modifier.height(16.dp))

            // =========================================================================
            // NOVA ÁREA INFERIOR — PAINEL COMPLETO FIXO
            // =========================================================================

            // 1. VISUALIZADOR DE ÁUDIO ANIMADO (Graves, Médios, Agudos, adaptativo ao volume)
            if (visualizerEnabled) {
                AnimatedAudioVisualizerSection(
                    spectrumBars = spectrumBars,
                    isPlaying = playbackState.isPlaying,
                    style = visualizerStyle,
                    intensity = animationIntensity,
                    volumeFraction = playbackState.volumeFraction,
                    onOpenSettings = { showPlayerSettingsSheet = true },
                )
                Spacer(Modifier.height(10.dp))
            }

            // 2. PRÓXIMAS MÚSICAS — FILA DE REPRODUÇÃO (3 a 5 músicas com tocar, reordenar, remover)
            UpcomingQueueSection(
                playbackState = playbackState,
                visibleCount = queueVisibleCount,
                onOpenFullQueue = onOpenQueue,
                onPlayTrackAtIndex = onPlayQueueIndex,
                onRemoveTrack = onRemoveFromQueue,
                onMoveTrack = onMoveQueueTrack,
                onClearQueue = onClearQueue,
            )

            Spacer(Modifier.height(10.dp))

            // 3. LETRAS DA MÚSICA (Prévia sincronizada com verso atual em destaque)
            FullPlayerLyricsSection(
                track = track,
                fallbackPositionMs = playbackState.positionMs,
                progressPositionFlow = progressPositionFlow,
                onOpenFullLyrics = onOpenLyrics,
                onSeekTo = onSeekTo,
            )

            Spacer(Modifier.height(10.dp))

            // 4. INFORMAÇÕES DETALHADAS DA MÚSICA (Metadados técnicos reais do arquivo)
            TrackDetailsSection(
                track = track,
                onOpenTagEditor = onOpenTagEditor,
            )

            // 5. ESTATÍSTICAS DE REPRODUÇÃO (Desta música e da biblioteca)
            if (showStatsEnabled) {
                Spacer(Modifier.height(10.dp))
                PlaybackStatsSection(
                    track = track,
                    allTracks = allTracks,
                    statsSummary = statsSummary,
                    onOpenFullStats = onOpenStats,
                )
            }

            // Space to ensure contents do not get clipped behind system navigation
            Spacer(Modifier.height(36.dp))
        }
    }

    if (showBookmarksSheet && track != null) {
        var newLabel by remember { mutableStateOf("") }
        val livePositionMs = if (progressPositionFlow != null) {
            progressPositionFlow.collectAsStateWithLifecycle().value
        } else {
            playbackState.positionMs
        }
        ModalBottomSheet(onDismissRequest = { showBookmarksSheet = false }, containerColor = colors.card) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Marcadores de ${track.title}", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        label = { Text("Nome do marcador (${TimeFormatter.formatDuration(livePositionMs)})") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onAddBookmark(track.id, newLabel.ifBlank { "Ponto ${bookmarks.size + 1}" }, livePositionMs)
                            newLabel = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    ) {
                        Text("Salvar")
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(bookmarks, key = { it.id }) { bm ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSeekTo(bm.positionMs); showBookmarksSheet = false },
                            colors = CardDefaults.cardColors(containerColor = colors.altBackground),
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bm.label, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                                    Text("Retomar em ${TimeFormatter.formatDuration(bm.positionMs)}", color = colors.primary, style = MaterialTheme.typography.labelMedium)
                                }
                                IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Excluir marcador", tint = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNoteSheet && track != null) {
        var noteText by remember(note) { mutableStateOf(note?.content.orEmpty()) }
        ModalBottomSheet(onDismissRequest = { showNoteSheet = false }, containerColor = colors.card) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Notas da Música", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Anotações pessoais (afinação, letra, lembrete...)") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (note != null) {
                        TextButton(onClick = { onDeleteNote(track.id); showNoteSheet = false }) {
                            Text("Excluir", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(
                        onClick = { onSaveNote(track.id, noteText); showNoteSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    ) {
                        Text("Salvar Nota")
                    }
                }
            }
        }
    }

    if (showPlayerSettingsSheet) {
        PlayerSettingsSheet(
            visualizerEnabled = visualizerEnabled,
            visualizerStyle = visualizerStyle,
            animationIntensity = animationIntensity,
            particlesEnabled = particlesEnabled,
            ambientBgEnabled = ambientBgEnabled,
            showStatsEnabled = showStatsEnabled,
            queueCount = queueVisibleCount,
            animatedTransitions = animatedTransitions,
            playerVisualStyle = playerVisualStyle,
            animationsDisabled = animationsDisabled,
            reduceMotion = reduceMotion,
            reduceEffectsOnLowBattery = reduceEffectsOnLowBattery,
            onDismiss = { showPlayerSettingsSheet = false },
            onSetVisualizerEnabled = onSetVisualizerEnabled,
            onSetVisualizerStyle = onSetVisualizerStyle,
            onSetAnimationIntensity = onSetAnimationIntensity,
            onSetParticlesEnabled = onSetParticlesEnabled,
            onSetAmbientBgEnabled = onSetAmbientBgEnabled,
            onSetShowStatsEnabled = onSetShowStatsEnabled,
            onSetQueueCount = onSetQueueCount,
            onSetAnimatedTransitions = onSetAnimatedTransitions,
            onSetPlayerVisualStyle = onSetPlayerVisualStyle,
            onSetAnimationsDisabled = onSetAnimationsDisabled,
            onSetReduceMotion = onSetReduceMotion,
            onSetReduceEffectsOnLowBattery = onSetReduceEffectsOnLowBattery,
        )
    }
}

@Composable
private fun PlayerActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Surface(shape = CircleShape, color = colors.card) {
            Icon(icon, contentDescription = label, tint = colors.primary, modifier = Modifier.padding(10.dp).size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1)
    }
}

@Composable
private fun FullPlayerProgressSection(
    fallbackPositionMs: Long,
    durationMs: Long,
    rawDurationMs: Long,
    formatExtension: String,
    progressPositionFlow: StateFlow<Long>?,
    onSeekTo: (Long) -> Unit,
    onSeekBack10s: () -> Unit,
    onSeekForward10s: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    var isSeeking by remember { mutableStateOf(false) }
    var sliderDragPos by remember { mutableFloatStateOf(0f) }
    val livePositionMs = if (progressPositionFlow != null) {
        progressPositionFlow.collectAsStateWithLifecycle().value
    } else {
        fallbackPositionMs
    }
    val effectiveProgress = if (isSeeking) sliderDragPos else {
        (livePositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    Slider(
        value = effectiveProgress,
        onValueChange = {
            isSeeking = true
            sliderDragPos = it
        },
        onValueChangeFinished = {
            val targetMs = (sliderDragPos * durationMs).toLong()
            isSeeking = false
            onSeekTo(targetMs)
        },
        colors = SliderDefaults.colors(
            thumbColor = colors.primary,
            activeTrackColor = colors.primary,
            inactiveTrackColor = colors.textSecondary.copy(alpha = 0.25f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("player_progress_slider"),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = TimeFormatter.formatDuration(if (isSeeking) (sliderDragPos * durationMs).toLong() else livePositionMs),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
            )
            IconButton(onClick = onSeekBack10s, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Rounded.Replay10, contentDescription = "Retroceder 10s", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            text = formatExtension,
            style = MaterialTheme.typography.labelSmall,
            color = colors.primary,
            fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSeekForward10s, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Rounded.Forward10, contentDescription = "Avançar 10s", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
            }
            Text(
                text = TimeFormatter.formatDuration(rawDurationMs),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun FullPlayerLyricsSection(
    track: MusicTrack?,
    fallbackPositionMs: Long,
    progressPositionFlow: StateFlow<Long>?,
    onOpenFullLyrics: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
) {
    val livePositionMs = if (progressPositionFlow != null) {
        progressPositionFlow.collectAsStateWithLifecycle().value
    } else {
        fallbackPositionMs
    }
    LyricsPreviewSection(
        track = track,
        positionMs = livePositionMs,
        onOpenFullLyrics = onOpenFullLyrics,
        onSeekTo = onSeekTo,
    )
}
