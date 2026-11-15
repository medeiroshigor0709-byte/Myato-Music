package com.example.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.audio.PlaybackUiState
import com.example.core.common.TimeFormatter
import com.example.core.designsystem.AlbumArtwork
import com.example.core.designsystem.CompactTrackRow
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.designsystem.MyatoCompactBrandBadge
import com.example.core.designsystem.SectionHeader
import com.example.core.designsystem.StaggeredEntryContainer
import com.example.core.designsystem.pressScaleFeedback
import com.example.domain.models.ListeningStatsSummary
import com.example.domain.models.MusicTrack
import com.example.domain.models.PlaybackQueue
import com.example.domain.models.QueueOriginType

/**
 * 4. Home Screen (Início)
 */
@Composable
fun HomeScreen(
    tracks: List<MusicTrack>,
    recentTracks: List<MusicTrack>,
    mostPlayed: List<MusicTrack>,
    queues: List<PlaybackQueue>,
    playbackState: PlaybackUiState,
    statsSummary: ListeningStatsSummary,
    isScanning: Boolean,
    onRefreshLibrary: () -> Unit,
    onOpenPermissions: () -> Unit,
    onPlayTrackList: (List<MusicTrack>, Int, String, QueueOriginType) -> Unit,
    onSwitchQueue: (Long) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenAllSongs: () -> Unit,
    onOpenQueues: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenFileManager: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenWidgets: () -> Unit,
    onTrackMoreClick: (MusicTrack) -> Unit,
) {
    val colors = LocalMyatoColors.current
    val topRecent = remember(recentTracks) { recentTracks.take(6) }
    val topPlayed = remember(mostPlayed) { mostPlayed.take(6) }
    val heroInteraction = remember { MutableInteractionSource() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 140.dp),
    ) {
        item(key = "home_header", contentType = "home_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    MyatoCompactBrandBadge(symbolSize = 34.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${tracks.size} músicas locais • ${TimeFormatter.formatHoursMinutes(statsSummary.totalListenMs)} ouvidas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
                IconButton(
                    onClick = onRefreshLibrary,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("home_refresh_button"),
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = colors.primary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar biblioteca", tint = colors.primary)
                    }
                }
            }
        }

        // Quick Search Bar
        item(key = "home_search", contentType = "home_search") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onOpenAllSongs() }
                    .testTag("home_quick_search_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Buscar músicas, artistas, álbuns...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Hero Resume / Quick Shuffle Banner
        item(key = "home_hero", contentType = "home_hero") {
            val current = playbackState.currentTrack
            StaggeredEntryContainer(index = 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .pressScaleFeedback(interactionSource = heroInteraction, pressedScale = 0.98f)
                        .clickable(interactionSource = heroInteraction, indication = androidx.compose.foundation.LocalIndication.current) {
                            if (current != null) onOpenPlayer()
                            else if (tracks.isNotEmpty()) onPlayTrackList(tracks.shuffled(), 0, "Mix Aleatório", QueueOriginType.ALL_SONGS)
                            else onOpenPermissions()
                        },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(colors.primary.copy(alpha = 0.22f), colors.card)
                            )
                        )
                        .padding(16.dp),
                ) {
                    if (current != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AlbumArtwork(artUri = current.albumArtUri, size = 64.dp, small = false)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Continuar reprodução", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                                Text(current.title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(current.artist, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1)
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Abrir player", tint = Color.Black)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (tracks.isEmpty()) Icons.Rounded.LibraryMusic else Icons.Rounded.Shuffle,
                                    contentDescription = null,
                                    tint = Color.Black,
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (tracks.isEmpty()) "Sua biblioteca está vazia" else "Modo Aleatório Instantâneo",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.textPrimary,
                                )
                                Text(
                                    text = if (tracks.isEmpty()) "Toque para conceder permissão ou importar pastas SAF" else "Reproduzir ${tracks.size} músicas offline agora",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
            }
        }

        // Quick Tools Grid
        item(key = "home_tools", contentType = "home_tools") {
            Spacer(Modifier.height(10.dp))
            SectionHeader("Ferramentas MYATO")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { QuickToolChip(Icons.AutoMirrored.Rounded.QueueMusic, "Filas (${queues.size})", onOpenQueues) }
                item { QuickToolChip(Icons.Rounded.Equalizer, "Equalizador", onOpenEqualizer) }
                item { QuickToolChip(Icons.Rounded.Bedtime, "Temporizador", onOpenSleepTimer) }
                item { QuickToolChip(Icons.Rounded.Folder, "Arquivos", onOpenFileManager) }
                item { QuickToolChip(Icons.Rounded.AutoGraph, "Estatísticas", onOpenStats) }
                item { QuickToolChip(Icons.Rounded.Backup, "Backup", onOpenBackup) }
                item { QuickToolChip(Icons.Rounded.Widgets, "Widgets", onOpenWidgets) }
            }
        }

        // Active & Recent Queues
        if (queues.isNotEmpty()) {
            item(key = "home_queues", contentType = "home_queues") {
                Spacer(Modifier.height(10.dp))
                SectionHeader("Múltiplas Filas", actionLabel = "Gerenciar", onAction = onOpenQueues)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(queues, key = { it.id }, contentType = { "queue_card" }) { queue ->
                        Card(
                            modifier = Modifier
                                .width(180.dp)
                                .clickable { onSwitchQueue(queue.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (queue.isActive) colors.primary.copy(alpha = 0.2f) else colors.card
                            ),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (queue.isActive) "ATIVA" else "SALVA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.primary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = queue.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${queue.trackCount} faixas • ${TimeFormatter.formatDuration(queue.currentPositionMs)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recently Added
        if (topRecent.isNotEmpty()) {
            item(key = "recent_header", contentType = "section_header") {
                Spacer(Modifier.height(12.dp))
                SectionHeader("Adicionadas recentemente", actionLabel = "Ver todas", onAction = onOpenAllSongs)
            }
            itemsIndexed(topRecent, key = { _, t -> "recent_${t.id}" }, contentType = { _, _ -> "track_row" }) { idx, track ->
                CompactTrackRow(
                    track = track,
                    isCurrent = playbackState.currentTrack?.id == track.id,
                    onClick = { onPlayTrackList(recentTracks, idx, "Adicionadas recentemente", QueueOriginType.ALL_SONGS) },
                    onMoreClick = { onTrackMoreClick(track) },
                )
            }
        }

        // Most Played
        if (topPlayed.isNotEmpty()) {
            item(key = "top_header", contentType = "section_header") {
                Spacer(Modifier.height(12.dp))
                SectionHeader("Mais reproduzidas", actionLabel = "Estatísticas", onAction = onOpenStats)
            }
            itemsIndexed(topPlayed, key = { _, t -> "top_${t.id}" }, contentType = { _, _ -> "track_row" }) { idx, track ->
                CompactTrackRow(
                    track = track,
                    isCurrent = playbackState.currentTrack?.id == track.id,
                    onClick = { onPlayTrackList(mostPlayed, idx, "Mais reproduzidas", QueueOriginType.CUSTOM) },
                    onMoreClick = { onTrackMoreClick(track) },
                )
            }
        }

        if (tracks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = colors.primary, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Nenhuma música local detectada", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Adicione arquivos MP3, FLAC, WAV, OGG ou M4A ao aparelho e toque em Atualizar ou selecione uma pasta SAF.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onOpenPermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                        ) {
                            Text("Configurar Pastas & Permissões")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToolChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .pressScaleFeedback(interactionSource = interactionSource, pressedScale = 0.94f)
            .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = colors.card,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
        }
    }
}
