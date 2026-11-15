package com.example.feature.library

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.SwapVert
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.TimeFormatter
import com.example.core.designsystem.AlbumArtwork
import com.example.core.designsystem.CompactTrackRow
import com.example.core.designsystem.CreatePlaylistDialog
import com.example.core.designsystem.DeleteTracksConfirmDialog
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.storage.DuplicateGroup
import com.example.domain.models.AlbumItem
import com.example.domain.models.ArtistItem
import com.example.domain.models.FolderItem
import com.example.domain.models.GenreItem
import com.example.domain.models.MusicTrack
import com.example.domain.models.QueueOriginType
import com.example.domain.models.SmartSelectionCriteria
import com.example.domain.models.TrackSortOrder

@Composable
fun LibraryHubScreen(
    tracksCount: Int,
    albumsCount: Int,
    artistsCount: Int,
    foldersCount: Int,
    genresCount: Int,
    favoritesCount: Int,
    onOpenAllSongs: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenGenres: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenFileManager: () -> Unit,
    onOpenQueues: () -> Unit,
    onRefreshLibrary: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    val itemsList = listOf(
        Triple("Todas as músicas", "$tracksCount faixas", Icons.Rounded.MusicNote to onOpenAllSongs),
        Triple("Álbuns", "$albumsCount álbuns", Icons.Rounded.Album to onOpenAlbums),
        Triple("Artistas", "$artistsCount artistas", Icons.Rounded.Person to onOpenArtists),
        Triple("Pastas", "$foldersCount pastas", Icons.Rounded.Folder to onOpenFolders),
        Triple("Gêneros", "$genresCount gêneros", Icons.Rounded.Category to onOpenGenres),
        Triple("Favoritos", "$favoritesCount favoritas", Icons.Rounded.Favorite to onOpenFavorites),
        Triple("Gerenciador de Arquivos", "Navegar, mover, copiar", Icons.Rounded.FolderOpen to onOpenFileManager),
        Triple("Múltiplas Filas", "Gerenciar filas ativas", Icons.AutoMirrored.Rounded.QueueMusic to onOpenQueues),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("library_hub_screen"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Biblioteca", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            IconButton(onClick = onRefreshLibrary, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar biblioteca", tint = colors.primary)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(itemsList) { (title, subtitle, pair) ->
                val (icon, action) = pair
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = action),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(icon, contentDescription = null, tint = colors.primary)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * 5. All Songs Screen with real-time search, sorting, smart selection, quick add to playlist, and batch actions.
 */
@Composable
fun AllSongsScreen(
    tracks: List<MusicTrack>,
    currentTrackId: Long?,
    duplicates: List<DuplicateGroup>,
    initialSortOrder: TrackSortOrder = TrackSortOrder.TITLE_ASC,
    initialFormatFilter: String = "Todos",
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onScanDuplicates: () -> Unit,
    onPlayTracks: (List<MusicTrack>, Int, String, QueueOriginType) -> Unit,
    onBatchAddToPlaylists: (List<Long>) -> Unit,
    onCreatePlaylistWithTracks: (String, List<Long>) -> Unit,
    onBatchAddToQueue: (List<MusicTrack>) -> Unit,
    onBatchShare: (List<MusicTrack>) -> Unit,
    onBatchDeleteFiles: (List<MusicTrack>) -> Unit,
    onBatchEditTags: (List<Long>) -> Unit,
    onTrackMoreClick: (MusicTrack) -> Unit,
    onSaveSortOrder: (TrackSortOrder) -> Unit = {},
    onSaveFormatFilter: (String) -> Unit = {},
    onSmartSelect: (SmartSelectionCriteria, MusicTrack?) -> List<Long>,
    sortTracks: (List<MusicTrack>, TrackSortOrder, String) -> List<MusicTrack>,
) {
    val colors = LocalMyatoColors.current
    var query by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(initialSortOrder) }
    var formatFilter by remember { mutableStateOf(initialFormatFilter) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSmartSelectMenu by remember { mutableStateOf(false) }
    var showDuplicatesDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val selectedIds = remember { mutableStateListOf<Long>() }
    val isSelectionMode = selectedIds.isNotEmpty()

    val filteredByFormat = remember(tracks, formatFilter) {
        if (formatFilter == "Todos") tracks
        else tracks.filter { it.formatExtension.equals(formatFilter, ignoreCase = true) }
    }
    val displayed = remember(filteredByFormat, sortOrder, query) {
        sortTracks(filteredByFormat, sortOrder, query)
    }

    val availableFormats = remember(tracks) {
        listOf("Todos") + tracks.map { it.formatExtension }.distinct().sorted()
    }

    val selectedTracks = remember(selectedIds.toList(), tracks) {
        val selSet = selectedIds.toSet()
        tracks.filter { it.id in selSet }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("all_songs_screen"),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar / Multi-selection Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelectionMode) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { if (isSelectionMode) selectedIds.clear() else onBack() }) {
                    Icon(
                        imageVector = if (isSelectionMode) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = colors.textPrimary,
                    )
                }

                Text(
                    text = if (isSelectionMode) "${selectedIds.size} selecionada(s)" else "Músicas (${displayed.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelectionMode) colors.primary else colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )

                if (isSelectionMode) {
                    IconButton(
                        onClick = {
                            selectedIds.clear()
                            selectedIds.addAll(displayed.map { it.id })
                        },
                    ) {
                        Icon(Icons.Rounded.SelectAll, contentDescription = "Selecionar todas", tint = colors.textPrimary)
                    }

                    IconButton(
                        onClick = {
                            val cur = selectedIds.toSet()
                            val inverted = displayed.map { it.id }.filter { it !in cur }
                            selectedIds.clear()
                            selectedIds.addAll(inverted)
                        },
                    ) {
                        Icon(Icons.Rounded.SwapVert, contentDescription = "Inverter seleção", tint = colors.textPrimary)
                    }

                    Box {
                        IconButton(onClick = { showSmartSelectMenu = true }) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = "Seleção Inteligente", tint = colors.primary)
                        }
                        DropdownMenu(
                            expanded = showSmartSelectMenu,
                            onDismissRequest = { showSmartSelectMenu = false },
                            containerColor = colors.card,
                        ) {
                            val firstSelected = selectedTracks.firstOrNull() ?: displayed.firstOrNull()
                            SmartSelectionCriteria.entries.forEach { criteria ->
                                DropdownMenuItem(
                                    text = { Text(criteria.label, color = colors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Rounded.Checklist, contentDescription = null, tint = colors.primary) },
                                    onClick = {
                                        val matched = onSmartSelect(criteria, firstSelected)
                                        selectedIds.clear()
                                        selectedIds.addAll(matched)
                                        showSmartSelectMenu = false
                                    },
                                )
                            }
                        }
                    }

                    IconButton(onClick = { onBatchAddToQueue(selectedTracks); selectedIds.clear() }) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Adicionar à fila", tint = colors.primary)
                    }

                    IconButton(onClick = { onBatchAddToPlaylists(selectedIds.toList()) }) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Adicionar à playlist", tint = colors.primary)
                    }

                    IconButton(onClick = { showCreatePlaylistDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Criar playlist com as músicas selecionadas", tint = colors.primary)
                    }

                    IconButton(onClick = { onBatchShare(selectedTracks) }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Compartilhar músicas selecionadas", tint = colors.textSecondary)
                    }

                    IconButton(onClick = { onBatchEditTags(selectedIds.toList()); selectedIds.clear() }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Editar informações", tint = colors.textSecondary)
                    }

                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = "Excluir arquivos", tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    IconButton(onClick = { onScanDuplicates(); showDuplicatesDialog = true }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Detectar duplicadas", tint = colors.textSecondary)
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Rounded.Sort, contentDescription = "Ordenar", tint = colors.primary)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            containerColor = colors.card,
                        ) {
                            TrackSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = order.label,
                                            color = if (sortOrder == order) colors.primary else colors.textPrimary,
                                            fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        sortOrder = order
                                        onSaveSortOrder(order)
                                        showSortMenu = false
                                    },
                                )
                            }
                        }
                    }

                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar", tint = colors.primary)
                    }
                }
            }

            // Real-time Search Input
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Buscar título, artista, álbum ou pasta...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Limpar busca", tint = colors.textSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("songs_search_input"),
            )

            // Audio format filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(availableFormats) { fmt ->
                    FilterChip(
                        selected = formatFilter == fmt,
                        onClick = {
                            formatFilter = fmt
                            onSaveFormatFilter(fmt)
                        },
                        label = { Text(fmt) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = Color.Black,
                        ),
                    )
                }
            }

            // Quick Play All & Shuffle Play Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = displayed.isNotEmpty()) {
                            onPlayTracks(displayed, 0, "Todas as músicas", QueueOriginType.ALL_SONGS)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.primary,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("Tocar tudo", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = displayed.isNotEmpty()) {
                            onPlayTracks(displayed.shuffled(), 0, "Aleatório Geral", QueueOriginType.ALL_SONGS)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.card,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Aleatório", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Track List with quick "+ Playlist" button and selection
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = if (isSelectionMode) 180.dp else 140.dp),
            ) {
                itemsIndexed(displayed, key = { _, t -> t.id }) { index, track ->
                    val isSelected = track.id in selectedIds
                    CompactTrackRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) selectedIds.remove(track.id) else selectedIds.add(track.id)
                            } else {
                                onPlayTracks(displayed, index, "Todas as músicas", QueueOriginType.ALL_SONGS)
                            }
                        },
                        onLongClick = {
                            if (isSelected) selectedIds.remove(track.id) else selectedIds.add(track.id)
                        },
                        onQuickAddToPlaylist = {
                            onBatchAddToPlaylists(listOf(track.id))
                        },
                        onMoreClick = { onTrackMoreClick(track) },
                    )
                }
            }
        }

        // Sticky Bottom Bar when multiple tracks are selected
        if (isSelectionMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 72.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = colors.card,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "${selectedIds.size} selecionada(s)",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Ações disponíveis",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Button(
                        onClick = { onBatchAddToPlaylists(selectedIds.toList()) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Adicionar à playlist")
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            initialName = if (query.isNotBlank()) "Playlist $query" else "Seleção (${selectedIds.size})",
            trackCount = selectedIds.size,
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                onCreatePlaylistWithTracks(name, selectedIds.toList())
                selectedIds.clear()
            },
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteTracksConfirmDialog(
            tracks = selectedTracks,
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                onBatchDeleteFiles(selectedTracks)
                selectedIds.clear()
            },
        )
    }

    if (showDuplicatesDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicatesDialog = false },
            containerColor = colors.card,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textPrimary,
            title = { Text("Músicas Duplicadas (${duplicates.size} grupos)") },
            text = {
                if (duplicates.isEmpty()) {
                    Text("Nenhuma música duplicada encontrada na biblioteca atual.", color = colors.textSecondary)
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(duplicates) { group ->
                            Text(
                                text = "${group.tracks.first().title} (${group.tracks.size} cópias)",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.primary,
                            )
                            group.tracks.forEach { copy ->
                                Text(
                                    text = "• ${copy.filePath} (${TimeFormatter.formatFileSize(copy.sizeBytes)})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDuplicatesDialog = false }) {
                    Text("Fechar", color = colors.primary)
                }
            },
        )
    }
}

/**
 * 6. Artists Screen
 */
@Composable
fun ArtistsScreen(
    artists: List<ArtistItem>,
    tracks: List<MusicTrack>,
    onBack: () -> Unit,
    onPlayArtistQueue: (String, List<MusicTrack>) -> Unit,
    onPinShortcut: (String, String, String) -> Unit,
) {
    val colors = LocalMyatoColors.current
    var selectedArtist by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        SimpleHeader(
            title = selectedArtist ?: "Artistas (${artists.size})",
            onBack = { if (selectedArtist != null) selectedArtist = null else onBack() },
        )

        if (selectedArtist == null) {
            LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
                items(artists, key = { it.id }) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedArtist = artist.name }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.card),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(artist.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("${artist.trackCount} músicas • ${artist.albumCount} álbuns", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        }
                        IconButton(onClick = { onPinShortcut("artist", artist.name, artist.name) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Fixar atalho", tint = colors.primary)
                        }
                    }
                }
            }
        } else {
            val artistTracks = remember(selectedArtist, tracks) { tracks.filter { it.artist == selectedArtist } }
            LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
                itemsIndexed(artistTracks, key = { _, t -> t.id }) { idx, track ->
                    CompactTrackRow(
                        track = track,
                        isCurrent = false,
                        onClick = { onPlayArtistQueue(selectedArtist!!, artistTracks.drop(idx) + artistTracks.take(idx)) },
                        onMoreClick = {},
                    )
                }
            }
        }
    }
}

/**
 * 7. Albums Screen
 */
@Composable
fun AlbumsScreen(
    albums: List<AlbumItem>,
    tracks: List<MusicTrack>,
    onBack: () -> Unit,
    onPlayAlbumQueue: (AlbumItem, List<MusicTrack>, Int) -> Unit,
) {
    val colors = LocalMyatoColors.current
    var selectedAlbum by remember { mutableStateOf<AlbumItem?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        SimpleHeader(
            title = selectedAlbum?.title ?: "Álbuns (${albums.size})",
            onBack = { if (selectedAlbum != null) selectedAlbum = null else onBack() },
        )

        if (selectedAlbum == null) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(156.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(albums, key = { it.id }) { album ->
                    Card(
                        modifier = Modifier.clickable { selectedAlbum = album },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            AlbumArtwork(artUri = album.artUri, size = 128.dp, small = false, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            Text(album.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(album.artist, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${album.trackCount} faixas", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                        }
                    }
                }
            }
        } else {
            val albumTracks = remember(selectedAlbum, tracks) {
                tracks.filter { it.albumId == selectedAlbum!!.id }.sortedBy { it.trackNumber }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
                itemsIndexed(albumTracks, key = { _, t -> t.id }) { idx, track ->
                    CompactTrackRow(
                        track = track,
                        isCurrent = false,
                        onClick = { onPlayAlbumQueue(selectedAlbum!!, albumTracks, idx) },
                        onMoreClick = {},
                    )
                }
            }
        }
    }
}

/**
 * 8. Folders Screen (with monitored folders toggle & auto-queue playback)
 */
@Composable
fun FoldersScreen(
    folders: List<FolderItem>,
    tracks: List<MusicTrack>,
    monitoredFolders: Set<String>,
    onUpdateMonitoredFolders: (Set<String>) -> Unit,
    onBack: () -> Unit,
    onPlayFolder: (FolderItem, List<MusicTrack>) -> Unit,
) {
    val colors = LocalMyatoColors.current
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        SimpleHeader(title = "Pastas de Música (${folders.size})", onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
            items(folders, key = { it.path }) { folder ->
                val isMonitored = monitoredFolders.isEmpty() || folder.path in monitoredFolders
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable {
                            val folderTracks = tracks.filter { it.folderPath == folder.path }
                            onPlayFolder(folder, folderTracks)
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folder.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("${folder.trackCount} músicas • ${folder.path}", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Switch(
                            checked = isMonitored,
                            onCheckedChange = { checked ->
                                val next = if (monitoredFolders.isEmpty()) {
                                    folders.map { it.path }.toMutableSet()
                                } else {
                                    monitoredFolders.toMutableSet()
                                }
                                if (checked) next.add(folder.path) else next.remove(folder.path)
                                onUpdateMonitoredFolders(next)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 9. Genres Screen
 */
@Composable
fun GenresScreen(
    genres: List<GenreItem>,
    tracks: List<MusicTrack>,
    onBack: () -> Unit,
    onPlayGenre: (String, List<MusicTrack>) -> Unit,
) {
    val colors = LocalMyatoColors.current
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        SimpleHeader(title = "Gêneros (${genres.size})", onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
            items(genres, key = { it.name }) { genre ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable {
                            val list = tracks.filter { it.genre.ifBlank { "Sem gênero" } == genre.name }
                            onPlayGenre(genre.name, list)
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Category, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(genre.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("${genre.trackCount} músicas", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        }
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = colors.primary)
                    }
                }
            }
        }
    }
}

/**
 * 11. Favorites Screen
 */
@Composable
fun FavoritesScreen(
    favorites: List<MusicTrack>,
    currentTrackId: Long?,
    onBack: () -> Unit,
    onPlayFavorites: (List<MusicTrack>, Int) -> Unit,
    onTrackMoreClick: (MusicTrack) -> Unit,
) {
    val colors = LocalMyatoColors.current
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        SimpleHeader(title = "Favoritos (${favorites.size})", onBack = onBack)
        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Marque músicas com coração no player para vê-las aqui.", color = colors.textSecondary)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
                itemsIndexed(favorites, key = { _, t -> t.id }) { idx, track ->
                    CompactTrackRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        onClick = { onPlayFavorites(favorites, idx) },
                        onMoreClick = { onTrackMoreClick(track) },
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleHeader(title: String, onBack: () -> Unit) {
    val colors = LocalMyatoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = colors.textPrimary)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
