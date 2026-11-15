package com.example.feature.queues

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.audio.PlaybackUiState
import com.example.core.common.TimeFormatter
import com.example.core.designsystem.AlbumArtwork
import com.example.core.designsystem.CreatePlaylistDialog
import com.example.core.designsystem.LocalMyatoColors
import com.example.domain.models.MusicTrack
import com.example.domain.models.PlaybackQueue
import com.example.domain.models.Playlist
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
    onPlayTrackAtIndex: (Int) -> Unit = {},
    onClearActiveQueue: () -> Unit = {},
    onSaveQueueAsPlaylist: (String, List<Long>) -> Unit = { _, _ -> },
) {
    val colors = LocalMyatoColors.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PlaybackQueue?>(null) }
    var mergeSource by remember { mutableStateOf<PlaybackQueue?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showSaveAsPlaylistDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("queues_screen"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SimpleHeader(title = "Múltiplas Filas (${queues.size})", onBack = onBack)
            }
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag("create_queue_button"),
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
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Faixas na fila ativa (${playbackState.queueTracks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { showSaveAsPlaylistDialog = true }) {
                                    Icon(Icons.Rounded.Save, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Salvar playlist", color = colors.primary, style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(onClick = { showClearConfirmDialog = true }) {
                                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Limpar", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
                itemsIndexed(playbackState.queueTracks, key = { idx, t -> "${t.id}_$idx" }) { idx, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (idx == playbackState.currentIndex) colors.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onPlayTrackAtIndex(idx) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumArtwork(artUri = track.albumArtUri, size = 42.dp, small = true)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = if (idx == playbackState.currentIndex) colors.primary else colors.textPrimary,
                                fontWeight = if (idx == playbackState.currentIndex) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
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

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = colors.card,
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Limpar Fila Ativa?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Todas as faixas serão removidas da fila de reprodução atual. A reprodução será pausada.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onClearActiveQueue()
                    showClearConfirmDialog = false
                }) {
                    Text("Limpar Fila", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancelar", color = colors.textPrimary)
                }
            },
        )
    }

    if (showSaveAsPlaylistDialog) {
        CreatePlaylistDialog(
            initialName = "Fila - ${TimeFormatter.formatDuration(System.currentTimeMillis())}",
            trackCount = playbackState.queueTracks.size,
            onDismiss = { showSaveAsPlaylistDialog = false },
            onConfirm = { name ->
                onSaveQueueAsPlaylist(name, playbackState.queueTracks.map { it.id })
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
 * 10. Playlists Screen — Search playlists, Create, rename, duplicate, clear, delete, reorder,
 * add/remove tracks, multi-selection inside playlist, export/import M3U.
 */
@OptIn(ExperimentalFoundationApi::class)
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
    onDuplicatePlaylist: (Long) -> Unit = {},
    onClearPlaylist: (Long) -> Unit = {},
    onToggleFavorite: (Long) -> Unit = {},
    onUpdateCover: (Long, String?) -> Unit = { _, _ -> },
    onRemoveTracksFromPlaylist: (Long, List<Long>) -> Unit = { _, _ -> },
    onAddTracksToPlaylistsWithResult: (List<Long>, List<Long>, Boolean) -> Unit = { _, _, _ -> },
    onTrackMoreClick: ((MusicTrack) -> Unit)? = null,
) {
    val colors = LocalMyatoColors.current
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var exportTargetId by remember { mutableStateOf<Long?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var playlistToClear by remember { mutableStateOf<Playlist?>(null) }
    var playlistToEditCover by remember { mutableStateOf<Playlist?>(null) }
    var playlistSearchQuery by remember { mutableStateOf("") }
    var showAddSongsSheet by remember { mutableStateOf(false) }

    // Multi-selection state inside open playlist
    val selectedTrackIdsInPlaylist = remember { mutableStateListOf<Long>() }
    val isSelectionModeInPlaylist = selectedTrackIdsInPlaylist.isNotEmpty()

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
            // Main Playlists List View
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Playlists (${playlists.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
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

            // Search Bar for playlists
            OutlinedTextField(
                value = playlistSearchQuery,
                onValueChange = { playlistSearchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Filtrar playlists por nome...", color = colors.textSecondary) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary) },
                trailingIcon = {
                    if (playlistSearchQuery.isNotBlank()) {
                        IconButton(onClick = { playlistSearchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Limpar busca", tint = colors.textSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            val displayedPlaylists = remember(playlists, playlistSearchQuery) {
                val q = playlistSearchQuery.trim().lowercase()
                if (q.isBlank()) playlists
                else playlists.filter { it.name.lowercase().contains(q) }
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
                items(displayedPlaylists, key = { it.id }) { playlist ->
                    var showCardMenu by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable {
                                selectedTrackIdsInPlaylist.clear()
                                selectedPlaylist = playlist
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Cover art
                            AlbumArtwork(artUri = playlist.coverUri, size = 52.dp, small = true)
                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (playlist.isFavorite) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.Favorite,
                                            contentDescription = "Favorita",
                                            tint = colors.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = "${playlist.trackCount} músicas • ${TimeFormatter.formatDuration(playlist.totalDurationMs)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                )
                            }

                            // Favorite Icon Toggle
                            IconButton(onClick = { onToggleFavorite(playlist.id) }) {
                                Icon(
                                    imageVector = if (playlist.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Favoritar playlist",
                                    tint = if (playlist.isFavorite) colors.primary else colors.textSecondary,
                                )
                            }

                            // More options dropdown
                            Box {
                                IconButton(onClick = { showCardMenu = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = "Opções da playlist", tint = colors.textSecondary)
                                }
                                DropdownMenu(
                                    expanded = showCardMenu,
                                    onDismissRequest = { showCardMenu = false },
                                    containerColor = colors.card,
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Duplicar playlist", color = colors.textPrimary) },
                                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = colors.primary) },
                                        onClick = {
                                            onDuplicatePlaylist(playlist.id)
                                            showCardMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Alterar capa", color = colors.textPrimary) },
                                        leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null, tint = colors.primary) },
                                        onClick = {
                                            playlistToEditCover = playlist
                                            showCardMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Renomear", color = colors.textPrimary) },
                                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.textSecondary) },
                                        onClick = {
                                            renameTarget = playlist
                                            showCardMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Exportar M3U", color = colors.textPrimary) },
                                        leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = colors.primary) },
                                        onClick = {
                                            exportTargetId = playlist.id
                                            exportLauncher.launch("${playlist.name}.m3u")
                                            showCardMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Limpar músicas", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            playlistToClear = playlist
                                            showCardMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Excluir playlist", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            playlistToDelete = playlist
                                            showCardMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Playlist Detail View
            val playlist = selectedPlaylist!!
            val trackIds by observePlaylistTrackIds(playlist.id).collectAsState(initial = emptyList())
            val tracksById = remember(allTracks) { allTracks.associateBy { it.id } }
            val playlistTracks = remember(trackIds, tracksById) { trackIds.mapNotNull { tracksById[it] } }
            var showDetailMenu by remember { mutableStateOf(false) }

            // Action bar / Header
            if (isSelectionModeInPlaylist) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.primary.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { selectedTrackIdsInPlaylist.clear() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancelar seleção", tint = colors.textPrimary)
                    }
                    Text(
                        text = "${selectedTrackIdsInPlaylist.size} selecionada(s)",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            selectedTrackIdsInPlaylist.clear()
                            selectedTrackIdsInPlaylist.addAll(playlistTracks.map { it.id })
                        },
                    ) {
                        Icon(Icons.Rounded.SelectAll, contentDescription = "Selecionar todas", tint = colors.textPrimary)
                    }
                    IconButton(
                        onClick = {
                            val cur = selectedTrackIdsInPlaylist.toSet()
                            val inverted = playlistTracks.map { it.id }.filter { it !in cur }
                            selectedTrackIdsInPlaylist.clear()
                            selectedTrackIdsInPlaylist.addAll(inverted)
                        },
                    ) {
                        Icon(Icons.Rounded.SwapVert, contentDescription = "Inverter seleção", tint = colors.textPrimary)
                    }
                    IconButton(
                        onClick = {
                            onRemoveTracksFromPlaylist(playlist.id, selectedTrackIdsInPlaylist.toList())
                            selectedTrackIdsInPlaylist.clear()
                        },
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Remover da playlist", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { selectedPlaylist = null }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar", tint = colors.textPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${playlistTracks.size} músicas • ${TimeFormatter.formatDuration(playlistTracks.sumOf { it.durationMs })}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    IconButton(onClick = { onToggleFavorite(playlist.id) }) {
                        Icon(
                            imageVector = if (playlist.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favoritar playlist",
                            tint = if (playlist.isFavorite) colors.primary else colors.textSecondary,
                        )
                    }
                    Box {
                        IconButton(onClick = { showDetailMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Mais opções", tint = colors.textSecondary)
                        }
                        DropdownMenu(
                            expanded = showDetailMenu,
                            onDismissRequest = { showDetailMenu = false },
                            containerColor = colors.card,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Adicionar músicas", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null, tint = colors.primary) },
                                onClick = {
                                    showAddSongsSheet = true
                                    showDetailMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Renomear playlist", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.textSecondary) },
                                onClick = {
                                    renameTarget = playlist
                                    showDetailMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Alterar capa", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null, tint = colors.primary) },
                                onClick = {
                                    playlistToEditCover = playlist
                                    showDetailMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicar playlist", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = colors.primary) },
                                onClick = {
                                    onDuplicatePlaylist(playlist.id)
                                    showDetailMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Exportar M3U", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = colors.primary) },
                                onClick = {
                                    exportTargetId = playlist.id
                                    exportLauncher.launch("${playlist.name}.m3u")
                                    showDetailMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Limpar músicas", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    playlistToClear = playlist
                                    showDetailMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Excluir playlist", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    playlistToDelete = playlist
                                    showDetailMenu = false
                                },
                            )
                        }
                    }
                }
            }

            // Action Buttons: Adicionar Músicas, Reproduzir, Aleatório
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { showAddSongsSheet = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Adicionar Músicas", fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Surface(
                    modifier = Modifier
                        .clickable(enabled = playlistTracks.isNotEmpty()) {
                            onPlayPlaylist(playlist, playlistTracks, false)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.card,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Tocar", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    modifier = Modifier
                        .clickable(enabled = playlistTracks.isNotEmpty()) {
                            onPlayPlaylist(playlist, playlistTracks, true)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.card,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = colors.primary)
                    }
                }
            }

            // Playlist Track List
            if (playlistTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Esta playlist está vazia", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text("Toque em \"Adicionar Músicas\" para selecionar faixas da sua biblioteca.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showAddSongsSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                        ) {
                            Text("Adicionar Músicas Agora")
                        }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
                    itemsIndexed(playlistTracks, key = { idx, t -> "${t.id}_$idx" }) { idx, track ->
                        val isSelected = track.id in selectedTrackIdsInPlaylist

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) colors.primary.copy(alpha = 0.16f) else Color.Transparent)
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionModeInPlaylist) {
                                            if (isSelected) selectedTrackIdsInPlaylist.remove(track.id)
                                            else selectedTrackIdsInPlaylist.add(track.id)
                                        } else {
                                            onPlayPlaylist(playlist, playlistTracks.drop(idx) + playlistTracks.take(idx), false)
                                        }
                                    },
                                    onLongClick = {
                                        if (isSelected) selectedTrackIdsInPlaylist.remove(track.id)
                                        else selectedTrackIdsInPlaylist.add(track.id)
                                    },
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isSelectionModeInPlaylist) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (isSelected) colors.primary else colors.textSecondary,
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .size(24.dp),
                                )
                            }

                            AlbumArtwork(artUri = track.albumArtUri, size = 44.dp, small = true)
                            Spacer(Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1)
                            }

                            if (!isSelectionModeInPlaylist) {
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
                                IconButton(
                                    onClick = {
                                        if (idx < trackIds.lastIndex) {
                                            val mutable = trackIds.toMutableList()
                                            val moved = mutable.removeAt(idx)
                                            mutable.add(idx + 1, moved)
                                            onReorderPlaylist(playlist.id, mutable)
                                        }
                                    },
                                    enabled = idx < trackIds.lastIndex,
                                ) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Descer", tint = colors.textSecondary)
                                }
                                IconButton(onClick = { onRemoveTrackFromPlaylist(playlist.id, track.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Remover da playlist", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal to pick and add songs directly into this playlist
    if (showAddSongsSheet && selectedPlaylist != null) {
        val currentPlaylist = selectedPlaylist!!
        val currentTrackIds by observePlaylistTrackIds(currentPlaylist.id).collectAsState(initial = emptyList())
        AddSongsToPlaylistDialog(
            playlist = currentPlaylist,
            existingTrackIds = currentTrackIds.toSet(),
            allTracks = allTracks,
            onDismiss = { showAddSongsSheet = false },
            onAddTracks = { chosenIds, allowDuplicates ->
                onAddTracksToPlaylistsWithResult(listOf(currentPlaylist.id), chosenIds, allowDuplicates)
                showAddSongsSheet = false
            },
        )
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = colors.card,
            title = { Text("Nova Playlist", color = colors.textPrimary) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome da playlist") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val finalName = name.trim().ifBlank { "Minha Playlist" }
                    onCreatePlaylist(finalName)
                    showCreateDialog = false
                }) { Text("Criar", color = colors.primary) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") } },
        )
    }

    // Rename Playlist Dialog
    renameTarget?.let { pl ->
        var name by remember(pl.id) { mutableStateOf(pl.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = colors.card,
            title = { Text("Renomear Playlist", color = colors.textPrimary) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val finalName = name.trim().ifBlank { pl.name }
                    onRenamePlaylist(pl.id, finalName)
                    renameTarget = null
                }) { Text("Salvar", color = colors.primary) }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancelar") } },
        )
    }

    // Clear Playlist Confirmation Dialog
    playlistToClear?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToClear = null },
            containerColor = colors.card,
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Limpar Playlist \"${pl.name}\"?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Todas as músicas serão removidas desta playlist.\n\nOs arquivos de áudio continuarão intactos na memória do seu aparelho.",
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearPlaylist(pl.id)
                    playlistToClear = null
                }) {
                    Text("Limpar Músicas", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { playlistToClear = null }) { Text("Cancelar") } },
        )
    }

    // Delete Playlist Confirmation Dialog (clarifying audio files remain safe)
    playlistToDelete?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = colors.card,
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Excluir Playlist \"${pl.name}\"?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "A playlist \"${pl.name}\" será excluída permanentemente.\n\nIMPORTANTE: Nenhum arquivo de áudio do seu celular será apagado.",
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePlaylist(pl.id)
                    if (selectedPlaylist?.id == pl.id) selectedPlaylist = null
                    playlistToDelete = null
                }) {
                    Text("Excluir Playlist", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { playlistToDelete = null }) { Text("Cancelar") } },
        )
    }

    // Edit Playlist Cover Dialog
    playlistToEditCover?.let { pl ->
        var coverUriInput by remember(pl.id) { mutableStateOf(pl.coverUri ?: "") }
        AlertDialog(
            onDismissRequest = { playlistToEditCover = null },
            containerColor = colors.card,
            title = { Text("Alterar Capa da Playlist", color = colors.textPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = coverUriInput,
                        onValueChange = { coverUriInput = it },
                        label = { Text("URI / Link local da imagem de capa") },
                        placeholder = { Text("Ex: content://... ou deixe em branco") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateCover(pl.id, coverUriInput.trim().ifBlank { null })
                    playlistToEditCover = null
                }) {
                    Text("Salvar", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { playlistToEditCover = null }) { Text("Cancelar") } },
        )
    }
}

/**
 * Dialog to add tracks directly from the library to a specific playlist.
 */
@Composable
fun AddSongsToPlaylistDialog(
    playlist: Playlist,
    existingTrackIds: Set<Long>,
    allTracks: List<MusicTrack>,
    onDismiss: () -> Unit,
    onAddTracks: (trackIds: List<Long>, allowDuplicates: Boolean) -> Unit,
) {
    val colors = LocalMyatoColors.current
    var query by remember { mutableStateOf("") }
    var allowDuplicates by remember { mutableStateOf(false) }
    val chosenIds = remember { mutableStateListOf<Long>() }

    val filteredTracks = remember(allTracks, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) allTracks
        else allTracks.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Column {
                Text(
                    text = "Adicionar a \"${playlist.name}\"",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Text(
                    text = "${chosenIds.size} selecionada(s) • ${existingTrackIds.size} já na playlist",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primary,
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Pesquisar música ou artista...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = null, tint = colors.textSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Permitir músicas repetidas",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    Switch(
                        checked = allowDuplicates,
                        onCheckedChange = { allowDuplicates = it },
                    )
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                ) {
                    items(filteredTracks, key = { it.id }) { track ->
                        val alreadyInPlaylist = track.id in existingTrackIds
                        val isChecked = track.id in chosenIds

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) chosenIds.remove(track.id)
                                    else chosenIds.add(track.id)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (isChecked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (isChecked) colors.primary else colors.textSecondary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            AlbumArtwork(artUri = track.albumArtUri, size = 36.dp, small = true)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1)
                                    if (alreadyInPlaylist) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = colors.primary.copy(alpha = 0.2f),
                                        ) {
                                            Text(
                                                text = "Já na playlist",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.primary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAddTracks(chosenIds.toList(), allowDuplicates)
                },
                enabled = chosenIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
            ) {
                Text("Adicionar (${chosenIds.size})", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = colors.textSecondary)
            }
        },
    )
}
