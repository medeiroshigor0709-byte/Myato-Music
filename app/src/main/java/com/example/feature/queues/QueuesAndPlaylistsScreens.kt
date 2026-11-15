package com.example.feature.queues

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.audio.PlaybackUiState
import com.example.core.common.TimeFormatter
import com.example.core.designsystem.AlbumArtwork
import com.example.core.designsystem.LocalMyatoColors
import com.example.domain.models.MusicTrack
import com.example.domain.models.PlaybackQueue
import com.example.domain.models.Playlist
import com.example.domain.models.QueueOriginType
import com.example.feature.library.SimpleHeader
import kotlinx.coroutines.flow.Flow

/**
 * 12. Multi-Queue Manager Screen — Full management of independent playback queues.
 */
@Composable
fun QueuesScreen(
    queues: List<PlaybackQueue>,
    allTracks: List<MusicTrack>,
    playbackState: PlaybackUiState,
    onBack: () -> Unit,
    onSwitchQueue: (Long) -> Unit,
    onCreateQueue: (String, List<Long>) -> Unit,
    onRenameQueue: (Long, String) -> Unit,
    onDuplicateQueue: (Long) -> Unit,
    onDeleteQueue: (Long) -> Unit,
    onMergeQueues: (Long, Long) -> Unit,
    onReplaceQueue: (Long, List<Long>) -> Unit,
    onMoveActiveTrack: (Int, Int) -> Unit,
    onRemoveActiveTrack: (Int) -> Unit,
) {
    val colors = LocalMyatoColors.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PlaybackQueue?>(null) }
    var mergeSource by remember { mutableStateOf<PlaybackQueue?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("queues_screen"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SimpleHeader(title = "Múltiplas Filas (${queues.size})", onBack = onBack)
            }
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                modifier = Modifier.padding(end = 8.dp).testTag("create_queue_button"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Nova Fila")
            }
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
            items(queues, key = { it.id }) { queue ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onSwitchQueue(queue.id) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (queue.isActive) colors.primary.copy(alpha = 0.16f) else colors.card
                    ),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (queue.isActive) "${queue.name} • ATIVA" else queue.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (queue.isActive) colors.primary else colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${queue.trackCount} músicas • Posição salva: ${TimeFormatter.formatDuration(queue.currentPositionMs)} • ${queue.originType.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                )
                            }
                            IconButton(onClick = { onSwitchQueue(queue.id) }) {
                                Icon(Icons.Rounded.SwapHoriz, contentDescription = "Alternar para esta fila", tint = colors.primary)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { renameTarget = queue }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Renomear fila", tint = colors.textSecondary)
                            }
                            IconButton(onClick = { onDuplicateQueue(queue.id) }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplicar fila", tint = colors.textSecondary)
                            }
                            IconButton(onClick = { mergeSource = queue }) {
                                Icon(Icons.AutoMirrored.Rounded.CallMerge, contentDescription = "Mesclar fila", tint = colors.textSecondary)
                            }
                            IconButton(onClick = {
                                onReplaceQueue(queue.id, allTracks.shuffled().take(25).map { it.id })
                            }) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = "Substituir por mix", tint = colors.textSecondary)
                            }
                            if (queues.size > 1) {
                                IconButton(onClick = { onDeleteQueue(queue.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Excluir fila", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            if (playbackState.queueTracks.isNotEmpty()) {
                item {
                    Text(
                        text = "Faixas na fila ativa (${playbackState.queueTracks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                itemsIndexed(playbackState.queueTracks, key = { idx, t -> "${t.id}_$idx" }) { idx, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (idx == playbackState.currentIndex) colors.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumArtwork(artUri = track.albumArtUri, size = 42.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1)
                        }
                        IconButton(onClick = { if (idx > 0) onMoveActiveTrack(idx, idx - 1) }, enabled = idx > 0) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Subir na fila", tint = colors.textSecondary)
                        }
                        IconButton(onClick = { if (idx < playbackState.queueTracks.lastIndex) onMoveActiveTrack(idx, idx + 1) }, enabled = idx < playbackState.queueTracks.lastIndex) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Descer na fila", tint = colors.textSecondary)
                        }
                        IconButton(onClick = { onRemoveActiveTrack(idx) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Remover da fila", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = colors.card,
            title = { Text("Criar Nova Fila", color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da fila (permite nomes iguais)") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onCreateQueue(name.ifBlank { "Nova Fila" }, allTracks.take(20).map { it.id })
                    showCreateDialog = false
                }) { Text("Criar", color = colors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar", color = colors.textSecondary) }
            },
        )
    }

    renameTarget?.let { target ->
        var newName by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = colors.card,
            title = { Text("Renomear Fila", color = colors.textPrimary) },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { onRenameQueue(target.id, newName); renameTarget = null }) {
                    Text("Salvar", color = colors.primary)
                }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancelar") } },
        )
    }

    mergeSource?.let { source ->
        val targets = queues.filter { it.id != source.id }
        AlertDialog(
            onDismissRequest = { mergeSource = null },
            containerColor = colors.card,
            title = { Text("Mesclar \"${source.name}\" em:", color = colors.textPrimary) },
            text = {
                Column {
                    targets.forEach { target ->
                        TextButton(onClick = { onMergeQueues(source.id, target.id); mergeSource = null }) {
                            Text("${target.name} (${target.trackCount} faixas)", color = colors.primary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mergeSource = null }) { Text("Fechar") }
            },
        )
    }
}

/**
 * 10. Playlists Screen — Create, rename, delete, reorder, add/remove tracks, export/import M3U.
 */
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    allTracks: List<MusicTrack>,
    observePlaylistTrackIds: (Long) -> Flow<List<Long>>,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlayPlaylist: (Playlist, List<MusicTrack>, Boolean) -> Unit,
    onRemoveTrackFromPlaylist: (Long, Long) -> Unit,
    onReorderPlaylist: (Long, List<Long>) -> Unit,
    onAddTracksToPlaylist: (Long, List<Long>) -> Unit,
    onExportM3u: (Long, Uri) -> Unit,
    onImportM3u: (Uri, String) -> Unit,
) {
    val colors = LocalMyatoColors.current
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var exportTargetId by remember { mutableStateOf<Long?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        val pid = exportTargetId
        if (uri != null && pid != null) onExportM3u(pid, uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportM3u(uri, "Playlist Importada")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("playlists_screen"),
    ) {
        if (selectedPlaylist == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Playlists (${playlists.size})", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { importLauncher.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "*/*")) }) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = "Importar M3U", tint = colors.primary)
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                        modifier = Modifier.testTag("create_playlist_button"),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Criar")
                    }
                }
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
                items(playlists, key = { it.id }) { playlist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { selectedPlaylist = playlist },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(playlist.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    "${playlist.trackCount} músicas • ${TimeFormatter.formatDuration(playlist.totalDurationMs)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                )
                            }
                            IconButton(onClick = {
                                exportTargetId = playlist.id
                                exportLauncher.launch("${playlist.name}.m3u")
                            }) {
                                Icon(Icons.Rounded.FileDownload, contentDescription = "Exportar M3U", tint = colors.primary)
                            }
                            IconButton(onClick = { renameTarget = playlist }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Renomear", tint = colors.textSecondary)
                            }
                            IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        } else {
            val playlist = selectedPlaylist!!
            val trackIds by observePlaylistTrackIds(playlist.id).collectAsState(initial = emptyList())
            val tracksById = remember(allTracks) { allTracks.associateBy { it.id } }
            val playlistTracks = remember(trackIds, tracksById) { trackIds.mapNotNull { tracksById[it] } }

            SimpleHeader(title = playlist.name, onBack = { selectedPlaylist = null })
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f).clickable(enabled = playlistTracks.isNotEmpty()) {
                        onPlayPlaylist(playlist, playlistTracks, false)
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.primary,
                ) {
                    Row(modifier = Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("Reproduzir", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f).clickable(enabled = playlistTracks.isNotEmpty()) {
                        onPlayPlaylist(playlist, playlistTracks, true)
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.card,
                ) {
                    Row(modifier = Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Aleatório", color = colors.textPrimary)
                    }
                }
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
                itemsIndexed(playlistTracks, key = { _, t -> t.id }) { idx, track ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumArtwork(artUri = track.albumArtUri, size = 44.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1)
                        }
                        IconButton(
                            onClick = {
                                if (idx > 0) {
                                    val mutable = trackIds.toMutableList()
                                    val moved = mutable.removeAt(idx)
                                    mutable.add(idx - 1, moved)
                                    onReorderPlaylist(playlist.id, mutable)
                                }
                            },
                            enabled = idx > 0,
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Subir", tint = colors.textSecondary)
                        }
                        IconButton(onClick = { onRemoveTrackFromPlaylist(playlist.id, track.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = colors.card,
            title = { Text("Nova Playlist", color = colors.textPrimary) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { onCreatePlaylist(name); showCreateDialog = false }) { Text("Criar", color = colors.primary) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") } },
        )
    }

    renameTarget?.let { pl ->
        var name by remember(pl.id) { mutableStateOf(pl.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = colors.card,
            title = { Text("Renomear Playlist", color = colors.textPrimary) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { onRenamePlaylist(pl.id, name); renameTarget = null }) { Text("Salvar", color = colors.primary) }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancelar") } },
        )
    }
}
