package com.example.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.TimeFormatter
import com.example.core.designsystem.AlbumArtwork
import com.example.core.designsystem.CompactTrackRow
import com.example.core.designsystem.CreateArtistPlaylistDialog
import com.example.core.designsystem.CreatePlaylistDialog
import com.example.core.designsystem.DeleteTracksConfirmDialog
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.designsystem.StaggeredEntryContainer
import com.example.core.metadata.MusicSearchEngine
import com.example.core.storage.DuplicateGroup
import com.example.domain.models.AlbumItem
import com.example.domain.models.ArtistFilterCandidate
import com.example.domain.models.ArtistItem
import com.example.domain.models.FolderItem
import com.example.domain.models.GenreItem
import com.example.domain.models.MusicTrack
import com.example.domain.models.QueueOriginType
import com.example.domain.models.SearchFilterMode
import com.example.domain.models.SearchMatchSource
import com.example.domain.models.SmartSelectionCriteria
import com.example.domain.models.SmartSearchResult
import com.example.domain.models.TrackSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
    val itemsList = remember(tracksCount, albumsCount, artistsCount, foldersCount, genresCount, favoritesCount) {
        listOf(
            Triple("Todas as músicas", "$tracksCount faixas", Icons.Rounded.MusicNote to onOpenAllSongs),
            Triple("Álbuns", "$albumsCount álbuns", Icons.Rounded.Album to onOpenAlbums),
            Triple("Artistas", "$artistsCount artistas", Icons.Rounded.Person to onOpenArtists),
            Triple("Pastas", "$foldersCount pastas", Icons.Rounded.Folder to onOpenFolders),
            Triple("Gêneros", "$genresCount gêneros", Icons.Rounded.Category to onOpenGenres),
            Triple("Favoritos", "$favoritesCount favoritas", Icons.Rounded.Favorite to onOpenFavorites),
            Triple("Gerenciador de Arquivos", "Navegar, mover, copiar", Icons.Rounded.FolderOpen to onOpenFileManager),
            Triple("Múltiplas Filas", "Gerenciar filas ativas", Icons.AutoMirrored.Rounded.QueueMusic to onOpenQueues),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            gridItemsIndexed(itemsList, key = { _, item -> item.first }) { idx, (title, subtitle, pair) ->
                val (icon, action) = pair
                StaggeredEntryContainer(index = idx) {
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
    onCreateArtistPlaylist: (artistName: String, includeCollaborations: Boolean, autoUpdateEnabled: Boolean, customName: String) -> Unit = { _, _, _, _ -> },
    searchDebounceMs: Long = MusicSearchEngine.DEFAULT_SEARCH_DEBOUNCE_MS,
) {
    val colors = LocalMyatoColors.current
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchFilterMode by remember { mutableStateOf(SearchFilterMode.ALL) }
    var selectedArtistFilter by remember { mutableStateOf<String?>(null) }
    var sortOrder by remember { mutableStateOf(initialSortOrder) }
    var formatFilter by remember { mutableStateOf(initialFormatFilter) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSmartSelectMenu by remember { mutableStateOf(false) }
    var showArtistPickerMenu by remember { mutableStateOf(false) }
    var showDuplicatesDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var createArtistPlaylistTarget by remember { mutableStateOf<String?>(null) }

    val selectedIds = remember { mutableStateListOf<Long>() }
    val isSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }

    val clampedDebounceMs = remember(searchDebounceMs) {
        if (searchDebounceMs <= 0L) 0L else MusicSearchEngine.clampDebounceMs(searchDebounceMs)
    }

    // Configurable debounce (250–350ms, default 280ms); clearing resets immediately without blocking typing
    LaunchedEffect(query, clampedDebounceMs) {
        if (query.isBlank() || clampedDebounceMs <= 0L) {
            debouncedQuery = query
        } else {
            delay(clampedDebounceMs)
            debouncedQuery = query
        }
    }

    val filteredByFormat = remember(tracks, formatFilter) {
        if (formatFilter == "Todos" || formatFilter == "Todas") tracks
        else tracks.filter { it.formatExtension.equals(formatFilter, ignoreCase = true) }
    }

    var searchResult by remember { mutableStateOf(SmartSearchResult()) }
    LaunchedEffect(filteredByFormat, debouncedQuery, searchFilterMode, selectedArtistFilter, sortOrder) {
        isSearching = true
        val computed = withContext(Dispatchers.Default) {
            MusicSearchEngine.searchTracks(
                tracks = filteredByFormat,
                query = debouncedQuery,
                filterMode = searchFilterMode,
                selectedArtist = selectedArtistFilter,
                sortOrder = sortOrder,
            )
        }
        searchResult = computed
        isSearching = false
    }

    var allArtistCandidates by remember { mutableStateOf<List<ArtistFilterCandidate>>(emptyList()) }
    LaunchedEffect(tracks) {
        allArtistCandidates = withContext(Dispatchers.Default) {
            MusicSearchEngine.extractArtistCandidates(tracks, "")
        }
    }

    val displayedHits = searchResult.hits
    val displayed = remember(displayedHits) { displayedHits.map { it.track } }

    val targetArtistForPlaylist = remember(selectedArtistFilter, searchResult.matchingArtists, debouncedQuery) {
        selectedArtistFilter ?: searchResult.matchingArtists.firstOrNull()?.takeIf {
            val normQ = MusicSearchEngine.normalizeText(debouncedQuery)
            normQ.isNotBlank() && MusicSearchEngine.normalizeText(it.name).let { n ->
                n == normQ || n.startsWith(normQ)
            }
        }?.name
    }

    val availableFormats = remember(tracks) {
        listOf("Todos") + tracks.map { it.formatExtension }.distinct().sorted()
    }

    val selectedTracks by remember(tracks) {
        derivedStateOf {
            val selSet = selectedIds.toSet()
            if (selSet.isEmpty()) emptyList() else tracks.filter { it.id in selSet }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("all_songs_screen"),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar / Multi-selection Action Bar with smooth content size transition
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
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

            // Real-time Intelligent Search Input
            OutlinedTextField(
                value = query,
                onValueChange = { newValue ->
                    query = newValue
                    if (newValue.isBlank()) {
                        selectedArtistFilter = null
                        if (searchFilterMode != SearchFilterMode.ALL) {
                            searchFilterMode = SearchFilterMode.ALL
                        }
                    }
                },
                placeholder = { Text("Buscar música, artista, álbum ou gênero...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary) },
                trailingIcon = {
                    if (query.isNotEmpty() || selectedArtistFilter != null || searchFilterMode != SearchFilterMode.ALL) {
                        IconButton(
                            onClick = {
                                query = ""
                                selectedArtistFilter = null
                                searchFilterMode = SearchFilterMode.ALL
                            },
                            modifier = Modifier.testTag("clear_search_button"),
                        ) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Limpar pesquisa", tint = colors.textSecondary)
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

            if (isSearching || query != debouncedQuery) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(2.dp)
                        .testTag("search_loading_indicator"),
                    color = colors.primary,
                    trackColor = colors.card,
                )
            }

            // Search Filter Mode Row: Todos | Artista exato | Artista e colaborações + Artist Picker
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("search_mode_row"),
            ) {
                items(SearchFilterMode.entries) { mode ->
                    FilterChip(
                        selected = searchFilterMode == mode,
                        onClick = { searchFilterMode = mode },
                        label = { Text(mode.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.onPrimary,
                        ),
                        modifier = Modifier.testTag("search_mode_${mode.name.lowercase()}"),
                    )
                }

                item(key = "artist_picker_chip") {
                    Box {
                        FilterChip(
                            selected = selectedArtistFilter != null,
                            onClick = {
                                if (selectedArtistFilter != null) {
                                    selectedArtistFilter = null
                                } else {
                                    showArtistPickerMenu = true
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (selectedArtistFilter != null) Icons.Rounded.Close else Icons.Rounded.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            label = {
                                Text(
                                    text = selectedArtistFilter?.let { "Artista: $it" } ?: "Filtrar artista",
                                    maxLines = 1,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.secondary,
                                selectedLabelColor = colors.onSecondary,
                                selectedLeadingIconColor = colors.onSecondary,
                            ),
                            modifier = Modifier.testTag("artist_filter_picker_chip"),
                        )

                        DropdownMenu(
                            expanded = showArtistPickerMenu,
                            onDismissRequest = { showArtistPickerMenu = false },
                            containerColor = colors.card,
                        ) {
                            if (allArtistCandidates.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhum artista encontrado", color = colors.textSecondary) },
                                    onClick = { showArtistPickerMenu = false },
                                )
                            } else {
                                allArtistCandidates.take(30).forEach { cand ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${cand.name} (${cand.totalTrackCount})",
                                                color = colors.textPrimary,
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.primary) },
                                        onClick = {
                                            selectedArtistFilter = cand.name
                                            if (searchFilterMode == SearchFilterMode.ALL) {
                                                searchFilterMode = SearchFilterMode.EXACT_ARTIST
                                            }
                                            showArtistPickerMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Matching Artist Suggestion Chips when searching
            if (query.isNotBlank() && searchResult.matchingArtists.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("matching_artists_row"),
                ) {
                    items(searchResult.matchingArtists.take(8), key = { it.name }) { candidate ->
                        val isSelectedArtist = selectedArtistFilter != null &&
                            MusicSearchEngine.normalizeText(selectedArtistFilter!!) == MusicSearchEngine.normalizeText(candidate.name)
                        val countLabel = if (searchFilterMode == SearchFilterMode.EXACT_ARTIST) {
                            "${candidate.soloTrackCount}"
                        } else {
                            "${candidate.totalTrackCount}"
                        }
                        FilterChip(
                            selected = isSelectedArtist,
                            onClick = {
                                if (isSelectedArtist) {
                                    selectedArtistFilter = null
                                } else {
                                    selectedArtistFilter = candidate.name
                                    if (searchFilterMode == SearchFilterMode.ALL) {
                                        searchFilterMode = SearchFilterMode.EXACT_ARTIST
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("${candidate.name} ($countLabel)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = colors.onPrimary,
                                selectedLeadingIconColor = colors.onPrimary,
                            ),
                            modifier = Modifier.testTag("artist_chip_${candidate.name}"),
                        )
                    }
                }
            }

            // Result Count & Create Artist Playlist Action Banner
            if (debouncedQuery.isNotBlank() || selectedArtistFilter != null || searchFilterMode != SearchFilterMode.ALL) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.card,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${displayedHits.size} música(s) encontrada(s)",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("search_result_count"),
                            )
                            val activeLabel = searchResult.activeArtistFilter
                            if (!activeLabel.isNullOrBlank()) {
                                val modeSub = if (searchFilterMode == SearchFilterMode.EXACT_ARTIST) {
                                    "Somente $activeLabel"
                                } else {
                                    "$activeLabel + colaborações"
                                }
                                Text(
                                    text = modeSub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.primary,
                                )
                            }
                        }

                        if (!targetArtistForPlaylist.isNullOrBlank()) {
                            Button(
                                onClick = { createArtistPlaylistTarget = targetArtistForPlaylist },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("create_artist_playlist_button"),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Criar playlist deste artista", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Audio format filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
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
                            selectedLabelColor = colors.onPrimary,
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
                            onPlayTracks(displayed, 0, searchResult.activeArtistFilter ?: "Todas as músicas", QueueOriginType.ALL_SONGS)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.primary,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = colors.onPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text("Tocar tudo", color = colors.onPrimary, fontWeight = FontWeight.Bold)
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

            // Track List or Empty State
            if (displayedHits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(32.dp)
                        .testTag("search_empty_state"),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.SearchOff,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = when {
                                selectedArtistFilter != null -> "Nenhuma música de \"$selectedArtistFilter\" encontrada com este filtro."
                                query.isNotBlank() -> "Nenhum resultado correspondente para \"${query.trim()}\"."
                                else -> "Nenhuma música disponível na biblioteca."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Verifique a grafia ou alterne entre Artista exato, Colaborações ou Todos os resultados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                        if (query.isNotBlank() || selectedArtistFilter != null || searchFilterMode != SearchFilterMode.ALL) {
                            Spacer(Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    query = ""
                                    selectedArtistFilter = null
                                    searchFilterMode = SearchFilterMode.ALL
                                },
                                modifier = Modifier.testTag("empty_clear_search_button"),
                            ) {
                                Icon(Icons.Rounded.Clear, contentDescription = null, tint = colors.primary)
                                Spacer(Modifier.width(6.dp))
                                Text("Limpar pesquisa", color = colors.primary)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = if (isSelectionMode) 180.dp else 140.dp),
                ) {
                    itemsIndexed(
                        items = displayedHits,
                        key = { _, hit -> hit.track.id },
                        contentType = { _, _ -> "track_row" },
                    ) { index, hit ->
                        val track = hit.track
                        val isSelected = track.id in selectedIds
                        val badge = when {
                            hit.parsedArtists.isCollaboration -> "Feat."
                            debouncedQuery.isNotBlank() && hit.matchSource == SearchMatchSource.GENRE -> track.genre
                            debouncedQuery.isNotBlank() && hit.matchSource == SearchMatchSource.ALBUM -> "Álbum"
                            else -> null
                        }
                        val rowContent: @Composable () -> Unit = {
                            CompactTrackRow(
                                track = track,
                                isCurrent = track.id == currentTrackId,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        if (isSelected) selectedIds.remove(track.id) else selectedIds.add(track.id)
                                    } else {
                                        onPlayTracks(displayed, index, searchResult.activeArtistFilter ?: "Todas as músicas", QueueOriginType.ALL_SONGS)
                                    }
                                },
                                onLongClick = {
                                    if (isSelected) selectedIds.remove(track.id) else selectedIds.add(track.id)
                                },
                                onQuickAddToPlaylist = {
                                    onBatchAddToPlaylists(listOf(track.id))
                                },
                                badgeText = badge,
                                onFixUnknownArtist = { onBatchEditTags(listOf(track.id)) },
                                onMoreClick = { onTrackMoreClick(track) },
                                precomputedDisplayArtist = hit.parsedArtists.displayArtist,
                                precomputedIsUnknown = hit.parsedArtists.isUnknownArtist,
                            )
                        }
                        if (debouncedQuery.isNotBlank() || displayedHits.size > 40) {
                            rowContent()
                        } else {
                            StaggeredEntryContainer(index = index) {
                                rowContent()
                            }
                        }
                    }
                }
            }
        }

        // Sticky Bottom Bar when multiple tracks are selected
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 72.dp)
                .fillMaxWidth(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
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
    createArtistPlaylistTarget?.let { targetArtist ->
        CreateArtistPlaylistDialog(
            artistName = targetArtist,
            allTracks = tracks,
            initialIncludeCollaborations = searchFilterMode == SearchFilterMode.ARTIST_AND_COLLABS,
            onDismiss = { createArtistPlaylistTarget = null },
            onConfirm = { playlistName, includeCollabs, autoUpdate ->
                onCreateArtistPlaylist(targetArtist, includeCollabs, autoUpdate, playlistName)
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
    onCreateArtistPlaylist: (artistName: String, includeCollaborations: Boolean, autoUpdateEnabled: Boolean, customName: String) -> Unit = { _, _, _, _ -> },
    onTrackMoreClick: (MusicTrack) -> Unit = {},
    onEditTrackTags: (Long) -> Unit = {},
) {
    val colors = LocalMyatoColors.current
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var artistQuery by remember { mutableStateOf("") }
    var debouncedArtistQuery by remember { mutableStateOf("") }
    var includeCollabs by remember { mutableStateOf(false) }
    var artistPlaylistDialogTarget by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedArtist != null) {
        selectedArtist = null
    }

    LaunchedEffect(artistQuery) {
        if (artistQuery.isBlank()) {
            debouncedArtistQuery = ""
        } else {
            delay(250L)
            debouncedArtistQuery = artistQuery
        }
    }

    var artistCandidates by remember { mutableStateOf<List<ArtistFilterCandidate>>(emptyList()) }
    LaunchedEffect(tracks, debouncedArtistQuery) {
        artistCandidates = withContext(Dispatchers.Default) {
            MusicSearchEngine.extractArtistCandidates(tracks, debouncedArtistQuery)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SimpleHeader(
            title = selectedArtist ?: "Artistas (${artistCandidates.size})",
            onBack = { if (selectedArtist != null) selectedArtist = null else onBack() },
        )

        if (selectedArtist == null) {
            OutlinedTextField(
                value = artistQuery,
                onValueChange = { artistQuery = it },
                placeholder = { Text("Buscar artista ou banda...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary) },
                trailingIcon = {
                    if (artistQuery.isNotBlank()) {
                        IconButton(onClick = { artistQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Limpar busca", tint = colors.textSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )

            if (artistCandidates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (artistQuery.isBlank()) "Nenhum artista encontrado na biblioteca." else "Nenhum artista encontrado para \"$artistQuery\".",
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
                    items(artistCandidates, key = { it.name }) { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedArtist = candidate.name }
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
                                Text(candidate.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                val sub = if (candidate.collabTrackCount > 0) {
                                    "${candidate.soloTrackCount} solo • ${candidate.collabTrackCount} participação(ões)"
                                } else {
                                    "${candidate.soloTrackCount} músicas"
                                }
                                Text(sub, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                            }
                            IconButton(onClick = { artistPlaylistDialogTarget = candidate.name }) {
                                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Criar playlist deste artista", tint = colors.primary)
                            }
                            IconButton(onClick = { onPinShortcut("artist", candidate.name, candidate.name) }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Fixar atalho", tint = colors.primary)
                            }
                        }
                    }
                }
            }
        } else {
            val artistName = selectedArtist!!
            val artistTracks = remember(artistName, tracks, includeCollabs) {
                MusicSearchEngine.tracksForArtist(tracks, artistName, includeCollaborations = includeCollabs)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !includeCollabs,
                    onClick = { includeCollabs = false },
                    label = { Text("Artista exato") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primary,
                        selectedLabelColor = colors.onPrimary,
                    ),
                )
                FilterChip(
                    selected = includeCollabs,
                    onClick = { includeCollabs = true },
                    leadingIcon = { Icon(Icons.Rounded.Group, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Com colaborações") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primary,
                        selectedLabelColor = colors.onPrimary,
                        selectedLeadingIconColor = colors.onPrimary,
                    ),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${artistTracks.size} música(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                )
            }

            Button(
                onClick = { artistPlaylistDialogTarget = artistName },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Criar playlist deste artista", fontWeight = FontWeight.Bold)
            }

            if (artistTracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nenhuma música de \"$artistName\" disponível neste filtro.",
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
                    itemsIndexed(artistTracks, key = { _, t -> t.id }) { idx, track ->
                        val parsed = MusicSearchEngine.parseTrackArtists(track)
                        CompactTrackRow(
                            track = track,
                            isCurrent = false,
                            badgeText = if (parsed.isCollaboration) "Feat." else null,
                            onFixUnknownArtist = { onEditTrackTags(track.id) },
                            onClick = { onPlayArtistQueue(artistName, artistTracks.drop(idx) + artistTracks.take(idx)) },
                            onMoreClick = { onTrackMoreClick(track) },
                        )
                    }
                }
            }
        }
    }

    artistPlaylistDialogTarget?.let { targetArtist ->
        CreateArtistPlaylistDialog(
            artistName = targetArtist,
            allTracks = tracks,
            initialIncludeCollaborations = includeCollabs,
            onDismiss = { artistPlaylistDialogTarget = null },
            onConfirm = { playlistName, withCollabs, autoUpdate ->
                onCreateArtistPlaylist(targetArtist, withCollabs, autoUpdate, playlistName)
            },
        )
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

    Column(modifier = Modifier.fillMaxSize()) {
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
                gridItemsIndexed(albums, key = { _, a -> a.id }, contentType = { _, _ -> "album_card" }) { index, album ->
                    StaggeredEntryContainer(index = index) {
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
    Column(modifier = Modifier.fillMaxSize()) {
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
    Column(modifier = Modifier.fillMaxSize()) {
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
    Column(modifier = Modifier.fillMaxSize()) {
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
