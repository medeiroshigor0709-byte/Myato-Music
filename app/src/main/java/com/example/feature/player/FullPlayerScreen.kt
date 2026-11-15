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
import com.example.core.designsystem.FrequencyBarsVisualizer
import com.example.core.designsystem.LocalMyatoColors
import com.example.domain.models.AnimationMode
import com.example.domain.models.Bookmark
import com.example.domain.models.MusicNote
import com.example.domain.models.PlayerAppearance
import com.example.domain.models.RepeatMode as MyatoRepeatMode

/**
 * 13. Full Player Screen — Cinematic artwork, frequency visualizer, interactive progress,
 * shuffle, repeat, favorites, volume control, bookmarks, notes, queue, lyrics, EQ, and timer links.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    playbackState: PlaybackUiState,
    spectrumBars: List<Float>,
    visualizerEnabled: Boolean,
    bookmarks: List<Bookmark>,
    note: MusicNote?,
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
    onAddBookmark: (Long, String, Long) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onSaveNote: (Long, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
) {
    val colors = LocalMyatoColors.current
    val track = playbackState.currentTrack
    var isSeeking by remember { mutableStateOf(false) }
    var sliderDragPos by remember { mutableFloatStateOf(0f) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showNoteSheet by remember { mutableStateOf(false) }

    val duration = playbackState.durationMs.coerceAtLeast(1L)
    val effectiveProgress = if (isSeeking) sliderDragPos else {
        (playbackState.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    val coverScale by animateFloatAsState(
        targetValue = if (playbackState.isPlaying) 1f else 0.92f,
        animationSpec = spring(),
        label = "cover_scale",
    )

    val spinTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vinyl_angle",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.primary.copy(alpha = 0.24f),
                        colors.altBackground,
                        colors.background,
                    )
                )
            )
            .safeDrawingPadding()
            .testTag("full_player_screen"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar
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
                IconButton(onClick = onOpenQueue, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = stringResource(R.string.cd_queue), tint = colors.textPrimary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Artwork Showcase
            val rotateMod = if (
                colors.playerAppearance == PlayerAppearance.VINYL_DISC &&
                playbackState.isPlaying &&
                colors.animationMode != AnimationMode.BATTERY_SAVER
            ) {
                Modifier.rotate(spinAngle)
            } else Modifier

            val coverSize = if (colors.playerAppearance == PlayerAppearance.COMPACT) 200.dp else 272.dp
            AlbumArtwork(
                artUri = track?.albumArtUri,
                size = coverSize,
                small = false,
                modifier = Modifier
                    .scale(if (colors.animationMode == AnimationMode.BATTERY_SAVER) 1f else coverScale)
                    .then(rotateMod),
            )

            Spacer(Modifier.height(14.dp))

            // Frequency Visualizer
            if (visualizerEnabled) {
                FrequencyBarsVisualizer(
                    bars = spectrumBars,
                    isPlaying = playbackState.isPlaying,
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(34.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

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
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_favorite_button"),
                ) {
                    Icon(
                        imageVector = if (track?.isFavorite == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_favorite),
                        tint = if (track?.isFavorite == true) colors.primary else colors.textSecondary,
                    )
                }
            }

            playbackState.errorMessage?.let { err ->
                Spacer(Modifier.height(6.dp))
                Text(err, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(10.dp))

            // Interactive Progress Bar
            Slider(
                value = effectiveProgress,
                onValueChange = {
                    isSeeking = true
                    sliderDragPos = it
                },
                onValueChangeFinished = {
                    val targetMs = (sliderDragPos * duration).toLong()
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
                        text = TimeFormatter.formatDuration(if (isSeeking) (sliderDragPos * duration).toLong() else playbackState.positionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                    )
                    IconButton(onClick = onSeekBack10s, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Replay10, contentDescription = "Retroceder 10s", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                }
                Text(
                    text = track?.formatExtension ?: "--",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSeekForward10s, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Forward10, contentDescription = "Avançar 10s", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = TimeFormatter.formatDuration(playbackState.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Primary Playback Controls (5 controls fit cleanly on all screen widths)
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
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("player_play_pause_button")
                        .clip(colors.resolveButtonShape())
                        .background(colors.primary),
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause),
                        tint = if (colors.isLight) Color.White else Color.Black,
                        modifier = Modifier.size(38.dp),
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
        }
    }

    if (showBookmarksSheet && track != null) {
        var newLabel by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showBookmarksSheet = false }, containerColor = colors.card) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Marcadores de ${track.title}", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        label = { Text("Nome do marcador (${TimeFormatter.formatDuration(playbackState.positionMs)})") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onAddBookmark(track.id, newLabel.ifBlank { "Ponto ${bookmarks.size + 1}" }, playbackState.positionMs)
                            newLabel = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
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
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                    ) {
                        Text("Salvar Nota")
                    }
                }
            }
        }
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
