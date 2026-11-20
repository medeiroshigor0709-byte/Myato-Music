package com.example.presentation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MyatoApplication
import com.example.core.audio.AudioVisualizerFrame
import com.example.core.audio.PlaybackController
import com.example.core.audio.PlaybackControllerHolder
import com.example.core.audio.PlaybackUiState
import com.example.core.common.AudioFormatSupport
import com.example.core.common.BackupWorker
import com.example.core.database.AppUserPreferences
import com.example.core.database.BackupMetadataEntity
import com.example.core.designsystem.AlbumArtPaletteExtractor
import com.example.core.designsystem.AppFontCatalog
import com.example.core.designsystem.BuiltInThemes
import com.example.core.metadata.LrcLyricsEngine
import com.example.core.metadata.MusicSearchEngine
import com.example.core.metadata.TagEditPayload
import com.example.core.storage.DuplicateGroup
import com.example.core.storage.FileOperationResult
import com.example.core.storage.FileSystemNode
import com.example.core.storage.LibraryScanResult
import com.example.core.storage.MyatoBackupBundle
import com.example.domain.models.AlbumItem
import com.example.domain.models.AppFontOption
import com.example.domain.models.ArtistFilterCandidate
import com.example.domain.models.ArtistItem
import com.example.domain.models.AudioFormatCapability
import com.example.domain.models.BackupPreview
import com.example.domain.models.Bookmark
import com.example.domain.models.FolderItem
import com.example.domain.models.FontApplyTarget
import com.example.domain.models.FontImportResult
import com.example.domain.models.GenreItem
import com.example.domain.models.HeadsetButtonMapping
import com.example.domain.models.ListeningStatsSummary
import com.example.domain.models.MusicNote
import com.example.domain.models.MusicTrack
import com.example.domain.models.MyatoTheme
import com.example.domain.models.PlaybackQueue
import com.example.domain.models.PlayerVisualBackgroundStyle
import com.example.domain.models.Playlist
import com.example.domain.models.PlaylistAddResult
import com.example.domain.models.QueueOriginType
import com.example.domain.models.SearchFilterMode
import com.example.domain.models.SmartSearchResult
import com.example.domain.models.SmartSelectionCriteria
import com.example.domain.models.ThemeAnimationIntensity
import com.example.domain.models.ThemeAutoScheduleMode
import com.example.domain.models.TrackSortOrder
import com.example.widgets.WidgetUpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyatoMainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyatoApplication
    val playbackController: PlaybackController = PlaybackControllerHolder.get(app)

    val preferences: StateFlow<AppUserPreferences> = app.preferencesStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppUserPreferences())

    val activeTheme: StateFlow<MyatoTheme> = app.themeRepository.activeThemeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, BuiltInThemes.MidnightOrange)

    val allThemes: StateFlow<List<MyatoTheme>> = app.themeRepository.themesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BuiltInThemes.all)

    val tracks: StateFlow<List<MusicTrack>> = app.libraryRepository.tracksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<AlbumItem>> = app.libraryRepository.albumsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<ArtistItem>> = app.libraryRepository.artistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderItem>> = tracks.map { list ->
        val monitored = preferences.value.monitoredFolders
        list.groupBy { it.folderPath }.map { (path, items) ->
            FolderItem(
                path = path,
                name = path.substringAfterLast('/').ifBlank { path },
                trackCount = items.size,
                totalDurationMs = items.sumOf { it.durationMs },
                isMonitored = monitored.isEmpty() || monitored.any { path.startsWith(it) },
            )
        }.sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genres: StateFlow<List<GenreItem>> = tracks.map { list ->
        list.groupBy { it.genre.ifBlank { "Sem gênero" } }
            .map { (genre, items) -> GenreItem(name = genre, trackCount = items.size) }
            .sortedByDescending { it.trackCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<MusicTrack>> = tracks.map { list -> list.filter { it.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAddedTracks: StateFlow<List<MusicTrack>> = tracks.map { list ->
        list.sortedByDescending { it.dateAddedSec }.take(30)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedTracks: StateFlow<List<MusicTrack>> = tracks.map { list ->
        list.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(30)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = app.playlistRepository.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queues: StateFlow<List<PlaybackQueue>> = app.queueRepository.queuesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statsSummary: StateFlow<ListeningStatsSummary> = app.statsRepository.statsSummaryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListeningStatsSummary())

    val backupHistory: StateFlow<List<BackupMetadataEntity>> = app.database.settingsBackupDao().observeBackups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState: StateFlow<PlaybackUiState> = playbackController.playbackState
    val playbackProgressMs: StateFlow<Long> = playbackController.progressPositionMs
    val equalizerState = playbackController.equalizerEngine.state
    val equalizerPresets = playbackController.equalizerEngine.savedPresets
    val spectrumBars = playbackController.equalizerEngine.spectrumBars
    val hasRealFftSignal = playbackController.equalizerEngine.hasRealFftSignal
    val visualizerFrame: StateFlow<AudioVisualizerFrame> = playbackController.equalizerEngine.visualizerFrame
    val sleepTimerState = playbackController.sleepTimerManager.state

    val audioCapabilities: List<AudioFormatCapability> by lazy { AudioFormatSupport.detectDeviceCapabilities() }

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _lastScanResult = MutableStateFlow<LibraryScanResult?>(null)
    val lastScanResult: StateFlow<LibraryScanResult?> = _lastScanResult.asStateFlow()

    private val _duplicates = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicates: StateFlow<List<DuplicateGroup>> = _duplicates.asStateFlow()

    private val _previewedBackup = MutableStateFlow<Pair<BackupPreview, MyatoBackupBundle>?>(null)
    val previewedBackup: StateFlow<Pair<BackupPreview, MyatoBackupBundle>?> = _previewedBackup.asStateFlow()

    init {
        viewModelScope.launch {
            app.database.trackDao().observeAllTracks()
                .map { list -> list.map { Triple(it.id, it.artist, it.title) } }
                .distinctUntilChanged()
                .collect { snapshot ->
                    if (snapshot.isNotEmpty() && !_isScanning.value) {
                        app.playlistRepository.syncAutoUpdateArtistPlaylists()
                    }
                }
        }
        // Pre-warm search index off the main thread whenever library changes
        viewModelScope.launch {
            tracks.collect { trackList ->
                if (trackList.isNotEmpty()) {
                    withContext(Dispatchers.Default) {
                        MusicSearchEngine.prewarmIndex(trackList)
                    }
                }
            }
        }
        // Keep visualizer hardware attachment synchronized with saved preference on startup & updates
        viewModelScope.launch {
            preferences
                .map { it.visualizerEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) playbackController.equalizerEngine.startVisualizerIfPermitted()
                    else playbackController.equalizerEngine.stopVisualizer()
                }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showStatus(msg: String) {
        _statusMessage.value = msg
    }

    fun completeOnboarding() {
        viewModelScope.launch { app.preferencesStore.setOnboardingDone(true) }
    }

    fun refreshLibrary() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            val res = app.libraryRepository.scanLibrary()
            val synced = app.playlistRepository.syncAutoUpdateArtistPlaylists()
            _lastScanResult.value = res
            _isScanning.value = false
            _statusMessage.value = if (synced > 0) {
                "Biblioteca atualizada: ${res.tracksCount} músicas • $synced nova(s) adicionada(s) às playlists automáticas."
            } else {
                "Biblioteca atualizada: ${res.tracksCount} músicas (${res.albumsCount} álbuns)."
            }
        }
    }

    fun importSafFolder(uri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            val count = app.libraryRepository.importSafFolder(uri)
            val synced = app.playlistRepository.syncAutoUpdateArtistPlaylists()
            _isScanning.value = false
            _statusMessage.value = if (synced > 0) {
                "$count música(s) importada(s) e $synced adicionada(s) às playlists automáticas."
            } else {
                "$count música(s) importada(s) da pasta selecionada."
            }
        }
    }

    fun scanDuplicates() {
        _duplicates.value = app.libraryRepository.findDuplicates(tracks.value)
    }

    fun removeTrackFromLibrary(trackId: Long) {
        viewModelScope.launch {
            app.libraryRepository.removeTrackFromLibrary(trackId)
            _statusMessage.value = "Música removida da biblioteca."
        }
    }

    fun toggleFavorite(trackId: Long) {
        viewModelScope.launch {
            app.libraryRepository.toggleFavorite(trackId)
            if (playbackState.value.currentTrack?.id == trackId) {
                playbackController.toggleCurrentTrackFavorite()
            }
        }
    }

    fun sortTracks(list: List<MusicTrack>, sortOrder: TrackSortOrder, query: String): List<MusicTrack> =
        filterAndSortTracks(list, sortOrder, query)

    fun smartSearchTracks(
        list: List<MusicTrack>,
        query: String,
        filterMode: SearchFilterMode,
        selectedArtist: String?,
        sortOrder: TrackSortOrder,
    ): SmartSearchResult = MusicSearchEngine.searchTracks(
        tracks = list,
        query = query,
        filterMode = filterMode,
        selectedArtist = selectedArtist,
        sortOrder = sortOrder,
    )

    suspend fun searchTracksOffMain(
        list: List<MusicTrack>,
        query: String,
        filterMode: SearchFilterMode,
        selectedArtist: String?,
        sortOrder: TrackSortOrder,
    ): SmartSearchResult = withContext(Dispatchers.Default) {
        MusicSearchEngine.searchTracks(
            tracks = list,
            query = query,
            filterMode = filterMode,
            selectedArtist = selectedArtist,
            sortOrder = sortOrder,
        )
    }

    suspend fun extractArtistCandidatesOffMain(
        list: List<MusicTrack>,
        query: String = "",
    ): List<ArtistFilterCandidate> = withContext(Dispatchers.Default) {
        MusicSearchEngine.extractArtistCandidates(list, query)
    }

    // Playback & Multi-Queue
    fun playTracks(
        trackList: List<MusicTrack>,
        startIndex: Int,
        queueName: String,
        originType: QueueOriginType,
        originKey: String = "",
    ) {
        playbackController.playTracks(trackList, startIndex, queueName, originType, originKey)
    }

    fun observeQueueTrackIds(queueId: Long): Flow<List<Long>> = app.queueRepository.observeQueueTrackIds(queueId)

    fun createQueue(name: String, trackIds: List<Long>) {
        viewModelScope.launch {
            app.queueRepository.createQueue(name, trackIds)
            _statusMessage.value = "Fila \"$name\" criada."
        }
    }

    fun renameQueue(queueId: Long, newName: String) {
        viewModelScope.launch {
            app.queueRepository.renameQueue(queueId, newName)
            _statusMessage.value = "Fila renomeada."
        }
    }

    fun duplicateQueue(queueId: Long) {
        viewModelScope.launch {
            app.queueRepository.duplicateQueue(queueId)
            _statusMessage.value = "Fila duplicada."
        }
    }

    fun deleteQueue(queueId: Long) {
        viewModelScope.launch {
            app.queueRepository.deleteQueue(queueId)
            _statusMessage.value = "Fila excluída."
        }
    }

    fun mergeQueues(sourceId: Long, targetId: Long) {
        viewModelScope.launch {
            app.queueRepository.mergeQueues(sourceId, targetId)
            _statusMessage.value = "Filas mescladas com sucesso."
        }
    }

    fun replaceQueueWithTracks(targetQueueId: Long, trackIds: List<Long>) {
        viewModelScope.launch {
            app.queueRepository.replaceQueueTracks(targetQueueId, trackIds)
            _statusMessage.value = "Conteúdo da fila substituído."
        }
    }

    // Playlists
    fun observePlaylistTrackIds(playlistId: Long): Flow<List<Long>> =
        app.playlistRepository.observePlaylistTrackIds(playlistId)

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            app.playlistRepository.createPlaylist(name, description)
            _statusMessage.value = "Playlist \"$name\" criada."
        }
    }

    fun createPlaylistWithTracks(name: String, trackIds: List<Long>, description: String = "") {
        viewModelScope.launch {
            app.playlistRepository.createPlaylistWithTracks(name, trackIds, description)
            _statusMessage.value = "Playlist \"$name\" criada com ${trackIds.size} música(s)."
        }
    }

    fun createArtistPlaylist(
        artistName: String,
        includeCollaborations: Boolean,
        autoUpdateEnabled: Boolean,
        customName: String? = null,
    ) {
        viewModelScope.launch {
            val cleanArtist = artistName.trim()
            val finalName = customName?.trim()?.ifBlank { cleanArtist } ?: cleanArtist
            val pid = app.playlistRepository.createArtistPlaylist(
                artistName = cleanArtist,
                includeCollaborations = includeCollaborations,
                autoUpdateEnabled = autoUpdateEnabled,
                customName = finalName,
            )
            val addedCount = app.playlistRepository.getPlaylistTrackIdsSync(pid).size
            _statusMessage.value = if (addedCount > 0) {
                "Playlist \"$finalName\" criada com $addedCount música(s) de $cleanArtist."
            } else {
                "Playlist \"$finalName\" criada para $cleanArtist (nenhuma música encontrada ainda)."
            }
        }
    }

    fun updatePlaylistDetails(
        playlistId: Long,
        newName: String,
        coverUri: String?,
        autoUpdateEnabled: Boolean,
        includeCollaborations: Boolean,
    ) {
        viewModelScope.launch {
            app.playlistRepository.updatePlaylistDetails(
                playlistId = playlistId,
                newName = newName,
                coverUri = coverUri,
                autoUpdateEnabled = autoUpdateEnabled,
                includeCollaborations = includeCollaborations,
            )
            _statusMessage.value = "Playlist atualizada."
        }
    }

    fun updatePlaylistCoverFromPicker(playlistId: Long, imageUri: Uri) {
        viewModelScope.launch {
            val saved = app.playlistRepository.persistPlaylistCoverImage(playlistId, imageUri)
            _statusMessage.value = if (saved != null) {
                "Capa da playlist atualizada."
            } else {
                "Não foi possível salvar a imagem selecionada."
            }
        }
    }

    fun setPlaylistAutoUpdate(
        playlistId: Long,
        autoUpdateEnabled: Boolean,
        includeCollaborations: Boolean,
    ) {
        viewModelScope.launch {
            val added = app.playlistRepository.setPlaylistAutoUpdate(
                playlistId = playlistId,
                autoUpdateEnabled = autoUpdateEnabled,
                includeCollaborations = includeCollaborations,
            )
            _statusMessage.value = when {
                autoUpdateEnabled && added > 0 -> "Atualização automática ativada ($added nova(s) música(s) adicionada(s))."
                autoUpdateEnabled -> "Atualização automática ativada para esta playlist."
                else -> "Atualização automática desativada."
            }
        }
    }

    fun syncAutoUpdatePlaylists() {
        viewModelScope.launch {
            val added = app.playlistRepository.syncAutoUpdateArtistPlaylists()
            _statusMessage.value = if (added > 0) {
                "$added nova(s) música(s) adicionada(s) às playlists automáticas."
            } else {
                "Playlists automáticas já estão atualizadas."
            }
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            app.playlistRepository.renamePlaylist(playlistId, newName)
            _statusMessage.value = "Playlist renomeada."
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            app.playlistRepository.deletePlaylist(playlistId)
            _statusMessage.value = "Playlist excluída."
        }
    }

    fun addTracksToPlaylists(playlistIds: List<Long>, trackIds: List<Long>) {
        viewModelScope.launch {
            app.playlistRepository.addTracksToMultiplePlaylists(playlistIds, trackIds)
            _statusMessage.value = "${trackIds.size} música(s) adicionada(s) em ${playlistIds.size} playlist(s)."
        }
    }

    fun addTracksToPlaylistsWithResult(
        playlistIds: List<Long>,
        trackIds: List<Long>,
        allowDuplicates: Boolean = false,
    ) {
        viewModelScope.launch {
            val result = app.playlistRepository.addTracksWithResult(playlistIds, trackIds, allowDuplicates)
            val namesStr = result.playlistNames.joinToString(", ")
            _statusMessage.value = if (result.addedCount > 0) {
                if (result.duplicateCount > 0) {
                    "${result.addedCount} música(s) adicionada(s) a $namesStr (${result.duplicateCount} já existiam)."
                } else {
                    "${result.addedCount} música(s) adicionada(s) a $namesStr com sucesso."
                }
            } else if (result.duplicateCount > 0) {
                "As músicas selecionadas já existem em $namesStr."
            } else {
                "Nenhuma música adicionada."
            }
        }
    }

    fun duplicatePlaylist(playlistId: Long) {
        viewModelScope.launch {
            app.playlistRepository.duplicatePlaylist(playlistId)
            _statusMessage.value = "Playlist duplicada com sucesso."
        }
    }

    fun clearPlaylist(playlistId: Long) {
        viewModelScope.launch {
            app.playlistRepository.clearPlaylist(playlistId)
            _statusMessage.value = "Playlist esvaziada."
        }
    }

    fun updatePlaylistCover(playlistId: Long, coverUri: String?) {
        viewModelScope.launch {
            app.playlistRepository.updatePlaylistCover(playlistId, coverUri)
            _statusMessage.value = "Capa da playlist atualizada."
        }
    }

    fun togglePlaylistFavorite(playlistId: Long, currentIsFavorite: Boolean? = null) {
        viewModelScope.launch {
            val isFav = currentIsFavorite ?: (playlists.value.find { it.id == playlistId }?.isFavorite == true)
            app.playlistRepository.setPlaylistFavorite(playlistId, !isFav)
        }
    }

    suspend fun getPlaylistTrackIdsSync(playlistId: Long): List<Long> {
        return app.playlistRepository.getPlaylistTrackIdsSync(playlistId)
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            app.playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
            _statusMessage.value = "Música removida da playlist."
        }
    }

    fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>) {
        viewModelScope.launch {
            app.playlistRepository.removeTracksFromPlaylist(playlistId, trackIds)
            _statusMessage.value = "${trackIds.size} música(s) removida(s) da playlist."
        }
    }

    fun reorderPlaylist(playlistId: Long, orderedTrackIds: List<Long>) {
        viewModelScope.launch {
            app.playlistRepository.reorderPlaylist(playlistId, orderedTrackIds)
        }
    }

    // Queue enhancements
    fun addTracksToActiveQueue(tracksToAdd: List<MusicTrack>) {
        playbackController.appendTracksToActiveQueue(tracksToAdd)
        _statusMessage.value = "${tracksToAdd.size} música(s) adicionada(s) à fila."
    }

    fun addTrackNextInActiveQueue(track: MusicTrack) {
        playbackController.addTrackNextInActiveQueue(track)
        _statusMessage.value = "\"${track.title}\" tocará a seguir."
    }

    fun clearActiveQueue() {
        playbackController.clearActiveQueue()
        _statusMessage.value = "Fila de reprodução limpa."
    }

    fun playTrackAtIndexInQueue(index: Int) {
        playbackController.playTrackAtIndex(index)
    }

    fun saveQueueAsPlaylist(name: String, trackIds: List<Long>) {
        viewModelScope.launch {
            app.playlistRepository.createPlaylistWithTracks(name, trackIds)
            _statusMessage.value = "Fila salva como playlist \"$name\"."
        }
    }

    // Permanent file & library deletion with feedback
    fun deleteTracksPermanently(tracksToDelete: List<MusicTrack>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val ids = tracksToDelete.map { it.id }
            tracksToDelete.forEach { t ->
                val f = java.io.File(t.filePath)
                if (f.exists()) {
                    runCatching { f.delete() }
                }
            }
            app.database.trackDao().deleteTracksByIds(ids)
            app.database.playlistDao().removeTracksFromAllPlaylists(ids)
            app.database.queueDao().removeTracksFromAllQueues(ids)
            _statusMessage.value = if (tracksToDelete.size == 1) {
                "Música \"${tracksToDelete.first().title}\" excluída do dispositivo."
            } else {
                "${tracksToDelete.size} músicas excluídas do dispositivo."
            }
            onDone()
        }
    }

    fun setLibrarySortOrder(order: TrackSortOrder) {
        viewModelScope.launch {
            app.preferencesStore.setLibrarySortOrder(order)
        }
    }

    fun setLibraryFormatFilter(format: String) {
        viewModelScope.launch {
            app.preferencesStore.setLibraryFormatFilter(format)
        }
    }

    fun setVisualizerStyle(style: String) {
        viewModelScope.launch { app.preferencesStore.setVisualizerStyle(style) }
    }

    fun setVisualizerAnimationIntensity(intensity: Float) {
        viewModelScope.launch { app.preferencesStore.setVisualizerAnimationIntensity(intensity) }
    }

    fun setPlayerParticlesEnabled(enabled: Boolean) {
        viewModelScope.launch { app.preferencesStore.setPlayerParticlesEnabled(enabled) }
    }

    fun setPlayerAmbientBackgroundEnabled(enabled: Boolean) {
        viewModelScope.launch { app.preferencesStore.setPlayerAmbientBackgroundEnabled(enabled) }
    }

    fun setPlayerShowStats(enabled: Boolean) {
        viewModelScope.launch { app.preferencesStore.setPlayerShowStats(enabled) }
    }

    fun setPlayerQueueVisibleCount(count: Int) {
        viewModelScope.launch { app.preferencesStore.setPlayerQueueVisibleCount(count) }
    }

    fun setPlayerAnimatedTransitions(enabled: Boolean) {
        viewModelScope.launch { app.preferencesStore.setPlayerAnimatedTransitions(enabled) }
    }

    fun setPlayerVisualStyle(style: PlayerVisualBackgroundStyle) {
        viewModelScope.launch { app.preferencesStore.setPlayerVisualStyle(style) }
    }

    fun setAnimationsDisabled(disabled: Boolean) {
        viewModelScope.launch { app.preferencesStore.setAnimationsDisabled(disabled) }
    }

    fun setReduceMotion(reduced: Boolean) {
        viewModelScope.launch { app.preferencesStore.setReduceMotion(reduced) }
    }

    fun setReduceEffectsOnLowBattery(enabled: Boolean) {
        viewModelScope.launch { app.preferencesStore.setReduceEffectsOnLowBattery(enabled) }
    }

    fun setSearchDebounceMs(debounceMs: Long) {
        viewModelScope.launch { app.preferencesStore.setSearchDebounceMs(debounceMs) }
    }

    fun refreshVisualizerState() {
        playbackController.equalizerEngine.refreshVisualizerState()
    }

    fun setAppForeground(inForeground: Boolean) {
        playbackController.setAppForeground(inForeground)
        playbackController.equalizerEngine.setVisualizerAppForeground(inForeground)
    }

    fun selectTracksByCriteria(
        allTracksList: List<MusicTrack>,
        criteria: SmartSelectionCriteria,
        referenceTrack: MusicTrack? = null,
    ): List<Long> = Companion.selectTracksByCriteria(allTracksList, criteria, referenceTrack)


    fun exportPlaylistM3u(playlistId: Long, targetUri: Uri) {
        viewModelScope.launch {
            val res = app.playlistRepository.exportPlaylistM3u(playlistId, targetUri)
            _statusMessage.value = res.fold(
                onSuccess = { "Playlist exportada ($it faixas) em M3U." },
                onFailure = { "Erro ao exportar playlist: ${it.localizedMessage}" },
            )
        }
    }

    fun importPlaylistM3u(sourceUri: Uri, name: String) {
        viewModelScope.launch {
            val res = app.playlistRepository.importPlaylistM3u(sourceUri, name)
            _statusMessage.value = res.fold(
                onSuccess = { "Playlist \"$name\" importada com sucesso." },
                onFailure = { "Erro ao importar M3U: ${it.localizedMessage}" },
            )
        }
    }

    // Bookmarks & Notes
    fun observeBookmarks(trackId: Long): Flow<List<Bookmark>> = app.statsRepository.observeBookmarksForTrack(trackId)
    fun observeNote(trackId: Long): Flow<MusicNote?> = app.statsRepository.observeNoteForTrack(trackId)

    fun addBookmark(trackId: Long, label: String, posMs: Long) {
        viewModelScope.launch {
            app.statsRepository.addBookmark(trackId, label, posMs)
            _statusMessage.value = "Marcador \"$label\" salvo."
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch { app.statsRepository.deleteBookmark(bookmarkId) }
    }

    fun saveNote(trackId: Long, content: String) {
        viewModelScope.launch {
            app.statsRepository.saveNote(trackId, content)
            _statusMessage.value = "Nota salva."
        }
    }

    fun deleteNote(trackId: Long) {
        viewModelScope.launch {
            app.statsRepository.deleteNote(trackId)
            _statusMessage.value = "Nota excluída."
        }
    }

    // Lyrics
    suspend fun loadTrackLyrics(track: MusicTrack): String {
        return LrcLyricsEngine.loadLyricsForTrack(app, track.id, track.filePath, track.embeddedLyrics)
    }

    fun saveTrackLyrics(track: MusicTrack, lrcContent: String) {
        viewModelScope.launch {
            val ok = LrcLyricsEngine.saveLyricsForTrack(app, track.id, track.filePath, lrcContent)
            val entity = app.database.trackDao().getTrackById(track.id)
            if (entity != null) {
                app.database.trackDao().upsertTrack(entity.copy(embeddedLyrics = lrcContent))
            }
            _statusMessage.value = if (ok) "Letras salvas com sucesso." else "Falha ao salvar letras."
        }
    }

    // Tag Editor
    suspend fun inspectTags(track: MusicTrack): TagEditPayload = app.tagEditorEngine.inspectTrackMetadata(track)

    fun saveTagsBatch(targetTracks: List<MusicTrack>, payload: TagEditPayload, isBatch: Boolean) {
        viewModelScope.launch {
            var directCount = 0
            var lastMsg = ""
            targetTracks.forEach { track ->
                val adjustedPayload = if (isBatch) {
                    payload.copy(
                        title = track.title,
                        trackNumber = track.trackNumber,
                    )
                } else payload
                when (val res = app.tagEditorEngine.saveTagsForTrack(track, adjustedPayload)) {
                    is com.example.core.metadata.TagSaveResult.Success -> {
                        if (res.wroteToAudioFile) directCount++
                        lastMsg = res.message
                    }
                    is com.example.core.metadata.TagSaveResult.Error -> {
                        lastMsg = res.message
                    }
                }
            }
            app.playlistRepository.syncAutoUpdateArtistPlaylists()
            _statusMessage.value = if (isBatch) {
                "${targetTracks.size} faixas atualizadas ($directCount gravadas diretamente no arquivo)."
            } else lastMsg
        }
    }

    // File Manager
    fun defaultRootDir(): String = app.fileManagerRepository.defaultRootDirectory().absolutePath
    suspend fun listDirectory(path: String): List<FileSystemNode> = app.fileManagerRepository.listDirectory(path)
    suspend fun listAllAudioLinear(): List<FileSystemNode> = app.fileManagerRepository.listAllAudioFilesLinear()

    fun createFolder(parentPath: String, name: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = app.fileManagerRepository.createFolder(parentPath, name)
            _statusMessage.value = when (res) {
                is FileOperationResult.Success -> res.message
                is FileOperationResult.Error -> res.message
            }
            onDone()
        }
    }

    fun renameFileOrFolder(path: String, newName: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = app.fileManagerRepository.renameNode(path, newName)
            _statusMessage.value = when (res) {
                is FileOperationResult.Success -> res.message
                is FileOperationResult.Error -> res.message
            }
            refreshLibrary()
            onDone()
        }
    }

    fun copyFiles(paths: List<String>, destDir: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = app.fileManagerRepository.copyFiles(paths, destDir)
            _statusMessage.value = when (res) {
                is FileOperationResult.Success -> res.message
                is FileOperationResult.Error -> res.message
            }
            refreshLibrary()
            onDone()
        }
    }

    fun moveFiles(paths: List<String>, destDir: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = app.fileManagerRepository.moveFiles(paths, destDir)
            _statusMessage.value = when (res) {
                is FileOperationResult.Success -> res.message
                is FileOperationResult.Error -> res.message
            }
            refreshLibrary()
            onDone()
        }
    }

    fun deleteFiles(paths: List<String>, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = app.fileManagerRepository.deleteFiles(paths)
            _statusMessage.value = when (res) {
                is FileOperationResult.Success -> res.message
                is FileOperationResult.Error -> res.message
            }
            refreshLibrary()
            onDone()
        }
    }

    // Themes & Studio
    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            val themeName = allThemes.value.firstOrNull { it.id == BuiltInThemes.resolveThemeId(themeId) }?.name
            app.themeRepository.selectTheme(themeId, themeName)
            _statusMessage.value = if (themeName != null) "Tema \"$themeName\" aplicado!" else "Tema aplicado!"
        }
    }

    fun toggleFavoriteTheme(themeId: String) {
        viewModelScope.launch {
            app.themeRepository.toggleFavoriteTheme(themeId)
        }
    }

    fun applyRandomTheme() {
        viewModelScope.launch {
            val picked = app.themeRepository.pickRandomTheme(allThemes.value, activeTheme.value.id)
            app.themeRepository.selectTheme(picked.id, picked.name)
            _statusMessage.value = "Tema aleatório aplicado: \"${picked.name}\""
        }
    }

    fun applySeasonalTheme() {
        viewModelScope.launch {
            val picked = app.themeRepository.pickSeasonalTheme(allThemes.value)
            app.themeRepository.selectTheme(picked.id, picked.name)
            _statusMessage.value = "Tema sazonal aplicado: \"${picked.name}\""
        }
    }

    fun configureThemeAutoSchedule(
        mode: ThemeAutoScheduleMode,
        dayThemeId: String,
        nightThemeId: String,
    ) {
        viewModelScope.launch {
            app.preferencesStore.setThemeAutoSchedule(mode, dayThemeId, nightThemeId)
            val updatedPrefs = preferences.value.copy(
                themeAutoScheduleMode = mode,
                themeAutoDayThemeId = dayThemeId,
                themeAutoNightThemeId = nightThemeId,
            )
            app.themeRepository.evaluateAutoScheduleIfNeeded(allThemes.value, updatedPrefs)
            _statusMessage.value = "Troca automática definida: ${mode.label}."
        }
    }

    fun evaluateAutoThemeSchedule() {
        viewModelScope.launch {
            app.themeRepository.evaluateAutoScheduleIfNeeded(allThemes.value, preferences.value)
        }
    }

    fun setThemeAnimationIntensity(intensity: ThemeAnimationIntensity) {
        viewModelScope.launch {
            app.preferencesStore.setThemeAnimationIntensity(intensity)
        }
    }

    fun saveCustomTheme(theme: MyatoTheme) {
        viewModelScope.launch {
            val saved = app.themeRepository.saveCustomTheme(theme)
            _statusMessage.value = "Tema \"${saved.name}\" salvo e aplicado."
        }
    }

    fun renameCustomTheme(themeId: String, newName: String) {
        viewModelScope.launch {
            app.themeRepository.renameCustomTheme(themeId, newName)
            _statusMessage.value = "Tema renomeado para \"$newName\"."
        }
    }

    fun duplicateTheme(source: MyatoTheme, newName: String) {
        viewModelScope.launch {
            app.themeRepository.duplicateTheme(source, newName)
            _statusMessage.value = "Tema duplicado como \"$newName\"."
        }
    }

    fun deleteCustomTheme(themeId: String) {
        viewModelScope.launch {
            app.themeRepository.deleteCustomTheme(themeId)
            _statusMessage.value = "Tema personalizado excluído."
        }
    }

    fun generateThemeFromCurrentCover(onGenerated: (MyatoTheme) -> Unit) {
        viewModelScope.launch {
            val track = playbackState.value.currentTrack
            val extracted = AlbumArtPaletteExtractor.extractThemeFromTrack(
                context = app,
                artUri = track?.albumArtUri,
                filePath = track?.filePath,
                baseTheme = activeTheme.value,
                customName = track?.album?.let { "Paleta: $it" } ?: "Paleta da Capa",
            )
            onGenerated(extracted)
        }
    }

    fun createThemeFromGalleryImage(
        imageUri: Uri,
        attachAsWallpaper: Boolean,
        customName: String,
        preferLight: Boolean,
        baseTheme: MyatoTheme,
        onGenerated: (MyatoTheme) -> Unit,
    ) {
        viewModelScope.launch {
            val savedPath = if (attachAsWallpaper) app.themeRepository.persistGalleryWallpaper(imageUri) else null
            val extracted = AlbumArtPaletteExtractor.extractThemeFromImageUri(
                context = app,
                imageUri = imageUri,
                baseTheme = baseTheme,
                customName = customName.ifBlank { "Tema da Imagem" },
                preferLight = preferLight,
                persistedWallpaperPath = savedPath,
            )
            onGenerated(extracted)
        }
    }

    fun persistWallpaperImage(imageUri: Uri, onSaved: (String?) -> Unit) {
        viewModelScope.launch {
            val path = app.themeRepository.persistGalleryWallpaper(imageUri)
            onSaved(path)
        }
    }

    fun exportThemeJson(theme: MyatoTheme): String = app.themeRepository.exportThemeJson(theme)

    fun createShareThemeIntent(theme: MyatoTheme): android.content.Intent =
        app.themeRepository.createShareThemeIntent(theme)

    fun importThemeJson(rawJson: String) {
        viewModelScope.launch {
            val res = app.themeRepository.importThemeJson(rawJson)
            _statusMessage.value = res.fold(
                onSuccess = { "Tema \"${it.name}\" importado!" },
                onFailure = { "JSON de tema inválido: ${it.localizedMessage}" },
            )
        }
    }

    // Backup & CSV Stats
    fun exportManualBackup(uri: Uri) {
        viewModelScope.launch {
            val res = app.backupManager.exportBackupToUri(uri, isAutomatic = false)
            _statusMessage.value = res.fold(
                onSuccess = { "Backup salvo (${it.itemCount} itens)." },
                onFailure = { "Falha no backup: ${it.localizedMessage}" },
            )
        }
    }

    fun loadBackupPreview(uri: Uri) {
        viewModelScope.launch {
            val res = app.backupManager.readBackupFromUri(uri)
            res.fold(
                onSuccess = { _previewedBackup.value = it },
                onFailure = { _statusMessage.value = "Arquivo de backup inválido: ${it.localizedMessage}" },
            )
        }
    }

    fun confirmRestoreBackup(preserveExisting: Boolean) {
        val bundle = _previewedBackup.value?.second ?: return
        viewModelScope.launch {
            val res = app.backupManager.restoreBackup(bundle, preserveExistingData = preserveExisting)
            _previewedBackup.value = null
            _statusMessage.value = res.fold(
                onSuccess = { "Backup restaurado com sucesso!" },
                onFailure = { "Erro na restauração: ${it.localizedMessage}" },
            )
        }
    }

    fun dismissBackupPreview() {
        _previewedBackup.value = null
    }

    fun configureAutoBackup(intervalDays: Int, dirUri: String) {
        viewModelScope.launch {
            app.preferencesStore.setAutoBackupConfig(intervalDays, dirUri)
            BackupWorker.schedule(app, intervalDays)
            _statusMessage.value = if (intervalDays > 0) {
                "Backup automático agendado a cada $intervalDays dia(s)."
            } else {
                "Backup automático desativado."
            }
        }
    }

    fun exportTopTracksCsv(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val csv = app.backupManager.buildTopTracksCsv()
                app.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                    out.flush()
                } ?: error("Destino inválido")
            }.fold(
                onSuccess = { _statusMessage.value = "Estatísticas exportadas em CSV com sucesso." },
                onFailure = { _statusMessage.value = "Falha ao exportar CSV: ${it.localizedMessage}" },
            )
        }
    }

    // Settings & Shortcuts
    fun setGapless(enabled: Boolean) {
        viewModelScope.launch { app.preferencesStore.setGaplessEnabled(enabled) }
    }

    fun setBatterySaverAnimations(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesStore.setBatterySaverAnimations(enabled)
            app.preferencesStore.setReduceMotion(enabled)
        }
    }

    fun setVisualizerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesStore.setVisualizerEnabled(enabled)
            if (enabled) playbackController.equalizerEngine.startVisualizerIfPermitted()
            else playbackController.equalizerEngine.stopVisualizer()
        }
    }

    fun setHeadsetMappings(
        single: HeadsetButtonMapping,
        doubleClick: HeadsetButtonMapping,
        triple: HeadsetButtonMapping,
        quad: HeadsetButtonMapping,
    ) {
        viewModelScope.launch {
            app.preferencesStore.setHeadsetMappings(single, doubleClick, triple, quad)
            _statusMessage.value = "Comandos de fone atualizados."
        }
    }

    fun setNotificationPreferences(compact: Boolean, showPlaylistBtn: Boolean) {
        viewModelScope.launch { app.preferencesStore.setNotificationPreferences(compact, showPlaylistBtn) }
    }

    fun setMonitoredFolders(folders: Set<String>) {
        viewModelScope.launch {
            app.preferencesStore.setMonitoredFolders(folders)
            refreshLibrary()
        }
    }

    // Typography & Custom Fonts
    fun selectAppFont(fontId: String) {
        applyAppFont(fontId, FontApplyTarget.BOTH)
    }

    fun applyAppFont(fontId: String, target: FontApplyTarget) {
        viewModelScope.launch {
            app.preferencesStore.setFontSelection(fontId, target)
            val option = AppFontCatalog.findFontOption(fontId, preferences.value.customFonts)
            val label = when (target) {
                FontApplyTarget.GENERAL -> "Fonte Geral"
                FontApplyTarget.DECORATIVE -> "Fonte Decorativa"
                FontApplyTarget.BOTH -> "Fonte Geral e Decorativa"
            }
            _statusMessage.value = "$label definida para \"${option.name}\"."
        }
    }

    fun toggleFavoriteFont(fontId: String) {
        viewModelScope.launch {
            app.preferencesStore.toggleFavoriteFont(fontId)
        }
    }

    fun resetDefaultAppFont() {
        viewModelScope.launch {
            app.preferencesStore.resetDefaultFont()
            _statusMessage.value = "Fonte original do MYATO MUSIC restaurada."
        }
    }

    fun importCustomFont(uri: Uri, onImported: (AppFontOption) -> Unit = {}) {
        viewModelScope.launch {
            when (val res = app.customFontManager.importFontFromUri(uri, preferences.value.customFonts)) {
                is FontImportResult.Success -> {
                    app.preferencesStore.addCustomFont(res.font)
                    _statusMessage.value = "Fonte \"${res.font.name}\" importada! Veja a prévia e aplique quando desejar."
                    onImported(res.font)
                }
                is FontImportResult.Error -> {
                    _statusMessage.value = res.message
                }
            }
        }
    }

    fun deleteCustomFont(font: AppFontOption) {
        viewModelScope.launch {
            app.customFontManager.deleteCustomFontFile(font)
            app.preferencesStore.removeCustomFont(font.id)
            _statusMessage.value = "Fonte personalizada \"${font.name}\" removida."
        }
    }

    fun pinShortcut(type: String, key: String, label: String) {
        val ok = WidgetUpdateHelper.pinHomeShortcut(
            context = app,
            shortcutId = "myato_${type}_${key.hashCode()}",
            label = label,
            shortcutType = type,
            shortcutKey = key,
        )
        _statusMessage.value = if (ok) {
            "Atalho \"$label\" solicitado à tela inicial."
        } else {
            "Seu launcher não suporta fixação automática de atalhos."
        }
    }

    companion object {
        fun filterAndSortTracks(
            list: List<MusicTrack>,
            sortOrder: TrackSortOrder,
            query: String,
        ): List<MusicTrack> {
            val filtered = if (query.isBlank()) list else {
                val q = query.trim().lowercase()
                list.filter {
                    it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q) ||
                        it.folderPath.lowercase().contains(q)
                }
            }
            return when (sortOrder) {
                TrackSortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
                TrackSortOrder.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
                TrackSortOrder.ARTIST_ASC -> filtered.sortedBy { it.artist.lowercase() }
                TrackSortOrder.ALBUM_ASC -> filtered.sortedBy { it.album.lowercase() }
                TrackSortOrder.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
                TrackSortOrder.DATE_ADDED_DESC -> filtered.sortedByDescending { it.dateAddedSec }
                TrackSortOrder.DATE_ADDED_ASC -> filtered.sortedBy { it.dateAddedSec }
                TrackSortOrder.MOST_PLAYED -> filtered.sortedByDescending { it.playCount }
                TrackSortOrder.LEAST_PLAYED -> filtered.sortedBy { it.playCount }
                TrackSortOrder.RECENTLY_ADDED -> filtered.sortedByDescending { it.dateAddedSec }
            }
        }

        fun selectTracksByCriteria(
            allTracksList: List<MusicTrack>,
            criteria: SmartSelectionCriteria,
            referenceTrack: MusicTrack? = null,
        ): List<Long> {
            return when (criteria) {
                SmartSelectionCriteria.BY_ARTIST -> {
                    val refArtist = referenceTrack?.artist
                    if (!refArtist.isNullOrBlank()) {
                        allTracksList.filter { it.artist.equals(refArtist, ignoreCase = true) }.map { it.id }
                    } else emptyList()
                }
                SmartSelectionCriteria.BY_ALBUM -> {
                    val refAlbum = referenceTrack?.album
                    if (!refAlbum.isNullOrBlank()) {
                        allTracksList.filter { it.album.equals(refAlbum, ignoreCase = true) }.map { it.id }
                    } else emptyList()
                }
                SmartSelectionCriteria.BY_FOLDER -> {
                    val refFolder = referenceTrack?.folderPath
                    if (!refFolder.isNullOrBlank()) {
                        allTracksList.filter { it.folderPath.equals(refFolder, ignoreCase = true) }.map { it.id }
                    } else emptyList()
                }
                SmartSelectionCriteria.BY_FORMAT -> {
                    val refFormat = referenceTrack?.formatExtension
                    if (!refFormat.isNullOrBlank()) {
                        allTracksList.filter { it.formatExtension.equals(refFormat, ignoreCase = true) }.map { it.id }
                    } else emptyList()
                }
                SmartSelectionCriteria.FAVORITES -> {
                    allTracksList.filter { it.isFavorite }.map { it.id }
                }
                SmartSelectionCriteria.NEVER_PLAYED -> {
                    allTracksList.filter { it.playCount == 0 }.map { it.id }
                }
            }
        }
    }
}
