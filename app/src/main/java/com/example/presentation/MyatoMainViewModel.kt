package com.example.presentation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MyatoApplication
import com.example.core.audio.PlaybackController
import com.example.core.audio.PlaybackControllerHolder
import com.example.core.audio.PlaybackUiState
import com.example.core.common.AudioFormatSupport
import com.example.core.common.BackupWorker
import com.example.core.database.AppUserPreferences
import com.example.core.database.BackupMetadataEntity
import com.example.core.designsystem.AlbumArtPaletteExtractor
import com.example.core.designsystem.BuiltInThemes
import com.example.core.metadata.LrcLyricsEngine
import com.example.core.metadata.TagEditPayload
import com.example.core.storage.DuplicateGroup
import com.example.core.storage.FileOperationResult
import com.example.core.storage.FileSystemNode
import com.example.core.storage.LibraryScanResult
import com.example.core.storage.MyatoBackupBundle
import com.example.domain.models.AlbumItem
import com.example.domain.models.ArtistItem
import com.example.domain.models.AudioFormatCapability
import com.example.domain.models.BackupPreview
import com.example.domain.models.Bookmark
import com.example.domain.models.FolderItem
import com.example.domain.models.GenreItem
import com.example.domain.models.HeadsetButtonMapping
import com.example.domain.models.ListeningStatsSummary
import com.example.domain.models.MusicNote
import com.example.domain.models.MusicTrack
import com.example.domain.models.MyatoTheme
import com.example.domain.models.PlaybackQueue
import com.example.domain.models.Playlist
import com.example.domain.models.QueueOriginType
import com.example.domain.models.TrackSortOrder
import com.example.widgets.WidgetUpdateHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val equalizerState = playbackController.equalizerEngine.state
    val equalizerPresets = playbackController.equalizerEngine.savedPresets
    val spectrumBars = playbackController.equalizerEngine.spectrumBars
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
            _lastScanResult.value = res
            _isScanning.value = false
            _statusMessage.value = "Biblioteca atualizada: ${res.tracksCount} músicas (${res.albumsCount} álbuns)."
        }
    }

    fun importSafFolder(uri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            val count = app.libraryRepository.importSafFolder(uri)
            _isScanning.value = false
            _statusMessage.value = "$count música(s) importada(s) da pasta selecionada."
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

    fun sortTracks(list: List<MusicTrack>, sortOrder: TrackSortOrder, query: String): List<MusicTrack> {
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
            TrackSortOrder.ARTIST_ASC -> filtered.sortedBy { it.artist.lowercase() }
            TrackSortOrder.ALBUM_ASC -> filtered.sortedBy { it.album.lowercase() }
            TrackSortOrder.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            TrackSortOrder.DATE_ADDED_DESC -> filtered.sortedByDescending { it.dateAddedSec }
            TrackSortOrder.MOST_PLAYED -> filtered.sortedByDescending { it.playCount }
        }
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

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            app.playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun reorderPlaylist(playlistId: Long, orderedTrackIds: List<Long>) {
        viewModelScope.launch {
            app.playlistRepository.reorderPlaylist(playlistId, orderedTrackIds)
        }
    }

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

    // Themes & Editor
    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            app.themeRepository.selectTheme(themeId)
            _statusMessage.value = "Tema aplicado."
        }
    }

    fun saveCustomTheme(theme: MyatoTheme) {
        viewModelScope.launch {
            app.themeRepository.saveCustomTheme(theme)
            _statusMessage.value = "Tema \"${theme.name}\" salvo e aplicado."
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

    fun exportThemeJson(theme: MyatoTheme): String = app.themeRepository.exportThemeJson(theme)

    fun importThemeJson(rawJson: String) {
        viewModelScope.launch {
            val res = app.themeRepository.importThemeJson(rawJson)
            _statusMessage.value = res.fold(
                onSuccess = { "Tema \"${it.name}\" importado." },
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
        viewModelScope.launch { app.preferencesStore.setBatterySaverAnimations(enabled) }
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
}
