package com.example.feature.queues

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
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
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.audio.PlaybackUiState
import com.example.core.common.TimeFormatter
import com.example.core.designsystem.AlbumArtwork
import com.example.core.designsystem.CreateArtistPlaylistDialog
import com.example.core.designsystem.CreatePlaylistDialog
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.designsystem.StaggeredEntryContainer
import com.example.core.metadata.MusicSearchEngine
import com.example.domain.models.MusicTrack
import com.example.domain.models.PlaybackQueue
import com.example.domain.models.Playlist
import com.example.domain.models.SearchFilterMode
import com.example.domain.models.TrackSortOrder
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
    onCreateArtistPlaylist: (artistName: String, includeCollaborations: Boolean, autoUpdateEnabled: Boolean, customName: String) -> Unit = { _, _, _, _ -> },
    onUpdatePlaylistDetails: (playlistId: Long, newName: String, coverUri: String?, autoUpdateEnabled: Boolean, includeCollaborations: Boolean) -> Unit = { _, _, _, _, _ -> },
    onPickPlaylistCover: (playlistId: Long, imageUri: Uri) -> Unit = { _, _ -> },
    onSetPlaylistAutoUpdate: (playlistId: Long, autoUpdateEnabled: Boolean, includeCollaborations: Boolean) -> Unit = { _, _, _ -> },
    onSyncAutoPlaylists: () -> Unit = {},
) {
    val colors = LocalMyatoColors.current
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    val selectedPlaylist = remember(selectedPlaylistId, playlists) {
        selectedPlaylistId?.let { pid -> playlists.firstOrNull { it.id == pid } }
    }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showArtistPickerForPlaylist by remember { mutableStateOf(false) }
    var artistPlaylistTarget by remember { mutableStateOf<String?>(null) }
    var editTarget by remember { mutableStateOf<Playlist?>(null) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var exportTargetId by remember { mutableStateOf<Long?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var playlistToClear by remember { mutableStateOf<Playlist?>(null) }
    var playlistToEditCover by remember { mutableStateOf<Playlist?>(null) }
    var coverPickerTargetPlaylistId by remember { mutableStateOf<Long?>(null) }
    var playlistSearchQuery by remember { mutableStateOf("") }
    var showAddSongsSheet by remember { mutableStateOf(false) }

    // Multi-selection state inside open playlist
    val selectedTrackIdsInPlaylist = remember { mutableStateListOf<Long>() }
    val isSelectionModeInPlaylist = selectedTrackIdsInPlaylist.isNotEmpty()

    BackHandler(enabled = selectedPlaylist != null) {
        if (isSelectionModeInPlaylist) {
            selectedTrackIdsInPlaylist.clear()
        } else {
            selectedPlaylistId = null
        }
    }

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

    val coverPhotoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val pid = coverPickerTargetPlaylistId
        if (uri != null && pid != null) {
            onPickPlaylistCover(pid, uri)
        }
        coverPickerTargetPlaylistId = null
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
                    modifier = Modifier.weight(1f),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = { importLauncher.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "*/*")) }) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = "Importar M3U", tint = colors.primary)
                    }
                    OutlinedButton(
                        onClick = { showArtistPickerForPlaylist = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("create_artist_playlist_shortcut_button"),
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Por artista", color = colors.primary, maxLines = 1)
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
                val q = MusicSearchEngine.normalizeText(playlistSearchQuery)
                if (q.isBlank()) playlists
                else playlists.filter {
                    MusicSearchEngine.normalizeText(it.name).contains(q) ||
                        MusicSearchEngine.normalizeText(it.artistFilterName.orEmpty()).contains(q)
                }
            }

            if (displayedPlaylists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (playlistSearchQuery.isBlank()) "Nenhuma playlist criada ainda." else "Nenhuma playlist encontrada para \"$playlistSearchQuery\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
                    itemsIndexed(
                        items = displayedPlaylists,
                        key = { _, p -> p.id },
                        contentType = { _, _ -> "playlist_card" },
                    ) { index, playlist ->
                        var showCardMenu by remember { mutableStateOf(false) }

                        StaggeredEntryContainer(index = index) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("playlist_card_${playlist.id}")
                                    .clickable {
                                        selectedTrackIdsInPlaylist.clear()
                                        selectedPlaylistId = playlist.id
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
                                        if (!playlist.artistFilterName.isNullOrBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = colors.primary.copy(alpha = 0.16f),
                                            ) {
                                                Text(
                                                    text = if (playlist.autoUpdateByArtist) {
                                                        "Auto • ${playlist.artistFilterName}"
                                                    } else {
                                                        "Artista • ${playlist.artistFilterName}"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.primary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
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
                                                text = { Text("Editar nome e capa", color = colors.textPrimary) },
                                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.primary) },
                                                onClick = {
                                                    editTarget = playlist
                                                    showCardMenu = false
                                                },
                                            )
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
                }
            }
        } else {
            // Playlist Detail View
            val playlist = selectedPlaylist
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
                    IconButton(onClick = { selectedPlaylistId = null }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar", tint = colors.textPrimary)
                    }
                    AlbumArtwork(artUri = playlist.coverUri, size = 40.dp, small = true)
                    Spacer(Modifier.width(10.dp))
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
                    IconButton(onClick = { editTarget = playlist }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Editar playlist", tint = colors.primary)
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
                                text = { Text("Editar nome e capa", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.primary) },
                                onClick = {
                                    editTarget = playlist
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

            // Smart Artist Playlist Controls Card
            if (!playlist.artistFilterName.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("smart_artist_playlist_card"),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.card,
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Artista vinculado: ${playlist.artistFilterName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            TextButton(
                                onClick = onSyncAutoPlaylists,
                                modifier = Modifier.testTag("sync_artist_playlist_button"),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sincronizar", color = colors.primary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Atualização automática",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                            Switch(
                                checked = playlist.autoUpdateByArtist,
                                onCheckedChange = { checked ->
                                    onSetPlaylistAutoUpdate(playlist.id, checked, playlist.includeCollaborations)
                                },
                                modifier = Modifier.testTag("playlist_auto_update_switch"),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Incluir colaborações (feat.)",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                            Switch(
                                checked = playlist.includeCollaborations,
                                onCheckedChange = { checked ->
                                    onSetPlaylistAutoUpdate(playlist.id, playlist.autoUpdateByArtist, checked)
                                },
                                modifier = Modifier.testTag("playlist_include_collabs_switch"),
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
                    modifier = Modifier.weight(1f).testTag("playlist_add_songs_button"),
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
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Tocar playlist", tint = colors.primary)
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
                        Icon(Icons.Rounded.Shuffle, contentDescription = "Tocar aleatório", tint = colors.primary)
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
                        Text("Toque em \"Adicionar Músicas\" para selecionar faixas da sua biblioteca.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
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
                        val displayArtist = MusicSearchEngine.sanitizeArtistName(track.artist)

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
                                Text(displayArtist, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                if (onTrackMoreClick != null) {
                                    IconButton(onClick = { onTrackMoreClick(track) }) {
                                        Icon(Icons.Rounded.MoreVert, contentDescription = "Opções da música", tint = colors.textSecondary)
                                    }
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
        val currentPlaylist = selectedPlaylist
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

    // Pick Artist to create Smart Artist Playlist
    if (showArtistPickerForPlaylist) {
        var artistSearch by remember { mutableStateOf("") }
        val candidates = remember(allTracks, artistSearch) {
            MusicSearchEngine.extractArtistCandidates(allTracks, artistSearch)
        }
        AlertDialog(
            onDismissRequest = { showArtistPickerForPlaylist = false },
            containerColor = colors.card,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textPrimary,
            title = { Text("Criar playlist de artista", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = artistSearch,
                        onValueChange = { artistSearch = it },
                        placeholder = { Text("Pesquisar artista ou banda...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    if (candidates.isEmpty()) {
                        Text(
                            text = "Nenhum artista encontrado nos metadados.",
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                            items(candidates, key = { it.name }) { cand ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            artistPlaylistTarget = cand.name
                                            showArtistPickerForPlaylist = false
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.primary)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cand.name, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = if (cand.collabTrackCount > 0) {
                                                "${cand.soloTrackCount} solo • ${cand.collabTrackCount} feat."
                                            } else {
                                                "${cand.soloTrackCount} músicas"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArtistPickerForPlaylist = false }) {
                    Text("Fechar", color = colors.primary)
                }
            },
        )
    }

    artistPlaylistTarget?.let { targetArtist ->
        CreateArtistPlaylistDialog(
            artistName = targetArtist,
            allTracks = allTracks,
            onDismiss = { artistPlaylistTarget = null },
            onConfirm = { playlistName, includeCollabs, autoUpdate ->
                onCreateArtistPlaylist(targetArtist, includeCollabs, autoUpdate, playlistName)
            },
        )
    }

    // Unified Edit Playlist Dialog (name + Photo Picker cover + artist auto-update flags)
    editTarget?.let { target ->
        val pl = playlists.firstOrNull { it.id == target.id } ?: target
        var editedName by remember(pl.id) { mutableStateOf(pl.name) }
        var editedCoverUri by remember(pl.id, pl.coverUri) { mutableStateOf(pl.coverUri ?: "") }
        var editedAutoUpdate by remember(pl.id) { mutableStateOf(pl.autoUpdateByArtist) }
        var editedIncludeCollabs by remember(pl.id) { mutableStateOf(pl.includeCollaborations) }

        AlertDialog(
            onDismissRequest = { editTarget = null },
            containerColor = colors.card,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textPrimary,
            title = { Text("Editar Playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AlbumArtwork(artUri = editedCoverUri.ifBlank { null }, size = 60.dp, small = false)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = {
                                    coverPickerTargetPlaylistId = pl.id
                                    coverPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                                modifier = Modifier.testTag("pick_playlist_cover_gallery_button"),
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Escolher capa")
                            }
                            if (editedCoverUri.isNotBlank()) {
                                TextButton(onClick = { editedCoverUri = "" }) {
                                    Text("Remover capa", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Nome da playlist") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_playlist_name_input"),
                    )

                    OutlinedTextField(
                        value = editedCoverUri,
                        onValueChange = { editedCoverUri = it },
                        label = { Text("URI da imagem (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (!pl.artistFilterName.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Atualização automática (${pl.artistFilterName})", style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                            Switch(checked = editedAutoUpdate, onCheckedChange = { editedAutoUpdate = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Incluir colaborações", style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                            Switch(checked = editedIncludeCollabs, onCheckedChange = { editedIncludeCollabs = it })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = editedName.trim().ifBlank { pl.name }
                        val finalCover = editedCoverUri.trim().ifBlank { null }
                        onUpdatePlaylistDetails(pl.id, finalName, finalCover, editedAutoUpdate, editedIncludeCollabs)
                        editTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                    modifier = Modifier.testTag("save_playlist_edits_button"),
                ) {
                    Text("Salvar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
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
                    if (selectedPlaylistId == pl.id) selectedPlaylistId = null
                    playlistToDelete = null
                }) {
                    Text("Excluir Playlist", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { playlistToDelete = null }) { Text("Cancelar") } },
        )
    }

    // Edit Playlist Cover Dialog
    playlistToEditCover?.let { target ->
        val pl = playlists.firstOrNull { it.id == target.id } ?: target
        var coverUriInput by remember(pl.id, pl.coverUri) { mutableStateOf(pl.coverUri ?: "") }
        AlertDialog(
            onDismissRequest = { playlistToEditCover = null },
            containerColor = colors.card,
            title = { Text("Alterar Capa da Playlist", color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            coverPickerTargetPlaylistId = pl.id
                            coverPhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                            playlistToEditCover = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Selecionar da Galeria", fontWeight = FontWeight.Bold)
                    }
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
 * Uses MusicSearchEngine for accurate artist/track matching and displays a confirmation list
 * with song title + artist before adding (never adding unrelated tracks automatically).
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
    var filterMode by remember { mutableStateOf(SearchFilterMode.ALL) }
    var allowDuplicates by remember { mutableStateOf(false) }
    var showConfirmationStep by remember { mutableStateOf(false) }
    val chosenIds = remember { mutableStateListOf<Long>() }

    val searchResult = remember(allTracks, query, filterMode) {
        MusicSearchEngine.searchTracks(
            tracks = allTracks,
            query = query,
            filterMode = filterMode,
            selectedArtist = null,
            sortOrder = TrackSortOrder.TITLE_ASC,
        )
    }
    val filteredHits = searchResult.hits
    val tracksById = remember(allTracks) { allTracks.associateBy { it.id } }
    val selectedTracks = remember(chosenIds.toList(), tracksById) {
        chosenIds.mapNotNull { tracksById[it] }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("add_songs_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            contentColor = colors.textPrimary,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Text(
                    text = if (showConfirmationStep) {
                        "Confirmar adição em \"${playlist.name}\""
                    } else {
                        "Adicionar a \"${playlist.name}\""
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${chosenIds.size} selecionada(s) • ${existingTrackIds.size} já na playlist",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primary,
                )

                Spacer(Modifier.height(8.dp))

                if (showConfirmationStep) {
                    Text(
                        text = "Revise as músicas e artistas selecionados antes de confirmar:",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("manual_add_confirmation_list"),
                    ) {
                        items(selectedTracks, key = { it.id }) { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AlbumArtwork(artUri = track.albumArtUri, size = 36.dp, small = true)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = MusicSearchEngine.sanitizeArtistName(track.artist),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(onClick = {
                                    chosenIds.remove(track.id)
                                    if (chosenIds.isEmpty()) showConfirmationStep = false
                                }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Remover da seleção", tint = colors.textSecondary)
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Pesquisar música, artista, álbum...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Limpar pesquisa", tint = colors.textSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_songs_search_input"),
                        shape = RoundedCornerShape(12.dp),
                    )

                    Spacer(Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(SearchFilterMode.entries) { mode ->
                            FilterChip(
                                selected = filterMode == mode,
                                onClick = { filterMode = mode },
                                label = { Text(mode.label, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor = Color.Black,
                                ),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permitir repetidas • ${filteredHits.size} música(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                        Switch(
                            checked = allowDuplicates,
                            onCheckedChange = { allowDuplicates = it },
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    if (filteredHits.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.SearchOff, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Nenhuma música correspondente encontrada.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("add_songs_list"),
                        ) {
                            items(filteredHits, key = { it.track.id }) { hit ->
                                val track = hit.track
                                val alreadyInPlaylist = track.id in existingTrackIds
                                val isChecked = track.id in chosenIds
                                val displayArtist = MusicSearchEngine.sanitizeArtistName(track.artist)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("add_song_row_${track.id}")
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
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = displayArtist,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false),
                                            )
                                            if (hit.parsedArtists.isCollaboration) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = colors.secondary.copy(alpha = 0.2f),
                                                ) {
                                                    Text(
                                                        text = "Feat.",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colors.secondary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    )
                                                }
                                            }
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
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showConfirmationStep) {
                        TextButton(onClick = { showConfirmationStep = false }) {
                            Text("Voltar", color = colors.textSecondary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onAddTracks(chosenIds.toList(), allowDuplicates)
                            },
                            enabled = chosenIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                            modifier = Modifier.testTag("confirm_manual_add_tracks_button"),
                        ) {
                            Text("Confirmar adição (${chosenIds.size})", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar", color = colors.textSecondary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { showConfirmationStep = true },
                            enabled = chosenIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                            modifier = Modifier.testTag("review_manual_add_tracks_button"),
                        ) {
                            Text("Revisar e Adicionar (${chosenIds.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
