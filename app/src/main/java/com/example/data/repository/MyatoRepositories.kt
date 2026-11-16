package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.core.database.AppPreferencesStore
import com.example.core.database.BookmarkEntity
import com.example.core.database.FavoriteEntity
import com.example.core.database.MusicNoteEntity
import com.example.core.database.MyatoDatabase
import com.example.core.database.PlaybackQueueEntity
import com.example.core.database.PlaylistEntity
import com.example.core.database.PlaylistTrackEntity
import com.example.core.database.toDomain
import com.example.core.database.toEntity
import com.example.core.designsystem.BuiltInThemes
import com.example.core.metadata.MusicSearchEngine
import com.example.core.storage.DuplicateGroup
import com.example.core.storage.LibraryScanResult
import com.example.core.storage.MediaStoreScanner
import com.example.domain.models.AlbumItem
import com.example.domain.models.AlbumStatItem
import com.example.domain.models.ArtistItem
import com.example.domain.models.ArtistStatItem
import com.example.domain.models.Bookmark
import com.example.domain.models.HistoryStatItem
import com.example.domain.models.ListeningStatsSummary
import com.example.domain.models.MusicNote
import com.example.domain.models.MusicTrack
import com.example.domain.models.MyatoTheme
import com.example.domain.models.PlaybackQueue
import com.example.domain.models.Playlist
import com.example.domain.models.PlaylistAddResult
import com.example.domain.models.TrackStatItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class LibraryRepository(
    private val database: MyatoDatabase,
    private val scanner: MediaStoreScanner,
    private val preferencesStore: AppPreferencesStore,
) {
    val tracksFlow: Flow<List<MusicTrack>> = combine(
        database.trackDao().observeAllTracks(),
        database.engagementDao().observeFavorites(),
        database.engagementDao().observeAllStats(),
    ) { entities, favorites, stats ->
        val favIds = favorites.map { it.trackId }.toSet()
        val statsMap = stats.associateBy { it.trackId }
        entities.map { entity ->
            val st = statsMap[entity.id]
            entity.toDomain(
                isFavorite = entity.id in favIds,
                playCount = st?.playCount ?: 0,
                lastPlayedAt = st?.lastPlayedAt ?: 0L,
            )
        }
    }

    val albumsFlow: Flow<List<AlbumItem>> = database.trackDao().observeAlbums()
        .map { list -> list.map { it.toDomain() } }

    val artistsFlow: Flow<List<ArtistItem>> = database.trackDao().observeArtists()
        .map { list -> list.map { it.toDomain() } }

    suspend fun scanLibrary(): LibraryScanResult {
        val monitored = preferencesStore.preferencesFlow.first().monitoredFolders
        return scanner.scanDeviceLibrary(monitored)
    }

    suspend fun importSafFolder(uri: Uri): Int = scanner.importSafFolder(uri)

    fun findDuplicates(tracks: List<MusicTrack>): List<DuplicateGroup> = scanner.findDuplicateTracks(tracks)

    suspend fun removeTrackFromLibrary(trackId: Long) {
        database.trackDao().deleteTrackById(trackId)
    }

    suspend fun toggleFavorite(trackId: Long) {
        if (database.engagementDao().isFavorite(trackId)) {
            database.engagementDao().removeFavorite(trackId)
        } else {
            database.engagementDao().addFavorite(FavoriteEntity(trackId))
        }
    }
}

class PlaylistRepository(
    private val context: Context,
    private val database: MyatoDatabase,
) {
    private val autoSyncMutex = Mutex()
    val playlistsFlow: Flow<List<Playlist>> = combine(
        database.playlistDao().observePlaylists(),
        database.playlistDao().observeAllPlaylistEntries(),
        database.trackDao().observeAllTracks(),
    ) { playlists, entries, tracks ->
        val trackDur = tracks.associate { it.id to it.durationMs }
        val grouped = entries.groupBy { it.playlistId }
        playlists.map { p ->
            val pEntries = grouped[p.id].orEmpty()
            p.toDomain(
                trackCount = pEntries.size,
                totalDurationMs = pEntries.sumOf { trackDur[it.trackId] ?: 0L },
            )
        }
    }

    fun observePlaylistTrackIds(playlistId: Long): Flow<List<Long>> =
        database.playlistDao().observePlaylistEntries(playlistId).map { list -> list.map { it.trackId } }

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        val clean = name.trim().ifBlank { "Nova Playlist" }
        return database.playlistDao().insertPlaylist(PlaylistEntity(name = clean, description = description))
    }

    suspend fun createPlaylistWithTracks(name: String, trackIds: List<Long>, description: String = ""): Long {
        val pid = createPlaylist(name, description)
        if (trackIds.isNotEmpty()) {
            database.playlistDao().replacePlaylistOrder(pid, trackIds.distinct())
        }
        return pid
    }

    suspend fun createArtistPlaylist(
        artistName: String,
        includeCollaborations: Boolean = false,
        autoUpdateEnabled: Boolean = true,
        customName: String? = null,
    ): Long {
        val cleanArtist = artistName.trim().ifBlank { MusicSearchEngine.UNKNOWN_ARTIST_LABEL }
        val finalName = customName?.trim()?.ifBlank { cleanArtist } ?: cleanArtist
        val allTracks = database.trackDao().getAllTracksSync().map { it.toDomain() }
        val matchedTracks = MusicSearchEngine.tracksForArtist(
            tracks = allTracks,
            artistName = cleanArtist,
            includeCollaborations = includeCollaborations,
        )
        val matchedIds = matchedTracks.map { it.id }.distinct()
        val firstCover = matchedTracks.firstOrNull { !it.albumArtUri.isNullOrBlank() }?.albumArtUri
        val now = System.currentTimeMillis()
        val pid = database.playlistDao().insertPlaylist(
            PlaylistEntity(
                name = finalName,
                description = if (includeCollaborations) {
                    "Playlist do artista $cleanArtist (com colaborações)"
                } else {
                    "Playlist exclusiva de $cleanArtist"
                },
                coverUri = firstCover,
                lastUsedAt = now,
                createdAt = now,
                updatedAt = now,
                artistFilterName = cleanArtist,
                autoUpdateByArtist = autoUpdateEnabled,
                includeCollaborations = includeCollaborations,
            )
        )
        if (matchedIds.isNotEmpty()) {
            database.playlistDao().replacePlaylistOrder(pid, matchedIds)
        }
        return pid
    }

    suspend fun setPlaylistAutoUpdate(
        playlistId: Long,
        autoUpdateEnabled: Boolean,
        includeCollaborations: Boolean? = null,
    ): Int {
        val existing = database.playlistDao().getPlaylistById(playlistId) ?: return 0
        val updated = existing.copy(
            autoUpdateByArtist = autoUpdateEnabled,
            includeCollaborations = includeCollaborations ?: existing.includeCollaborations,
            updatedAt = System.currentTimeMillis(),
        )
        database.playlistDao().updatePlaylist(updated)
        return if (updated.autoUpdateByArtist && !updated.artistFilterName.isNullOrBlank()) {
            syncPlaylistForArtistEntity(updated)
        } else {
            0
        }
    }

    suspend fun updatePlaylistDetails(
        playlistId: Long,
        newName: String,
        coverUri: String?,
        autoUpdateEnabled: Boolean? = null,
        includeCollaborations: Boolean? = null,
    ) {
        val existing = database.playlistDao().getPlaylistById(playlistId) ?: return
        val updated = existing.copy(
            name = newName.trim().ifBlank { existing.name },
            coverUri = coverUri?.trim()?.ifBlank { null },
            autoUpdateByArtist = autoUpdateEnabled ?: existing.autoUpdateByArtist,
            includeCollaborations = includeCollaborations ?: existing.includeCollaborations,
            updatedAt = System.currentTimeMillis(),
        )
        database.playlistDao().updatePlaylist(updated)
        if (updated.autoUpdateByArtist && !updated.artistFilterName.isNullOrBlank()) {
            syncPlaylistForArtistEntity(updated)
        }
    }

    suspend fun persistPlaylistCoverImage(playlistId: Long, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val coversDir = File(context.filesDir, "playlist_covers").apply { mkdirs() }
            val destFile = File(coversDir, "playlist_${playlistId}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            val fileUri = Uri.fromFile(destFile).toString()
            updatePlaylistCover(playlistId, fileUri)
            fileUri
        }.getOrNull()
    }

    suspend fun syncAutoUpdateArtistPlaylists(): Int = autoSyncMutex.withLock {
        val playlists = database.playlistDao().getPlaylistsSync()
            .filter { it.autoUpdateByArtist && !it.artistFilterName.isNullOrBlank() }
        if (playlists.isEmpty()) return@withLock 0

        val allTracks = database.trackDao().getAllTracksSync().map { it.toDomain() }
        var totalAdded = 0
        playlists.forEach { pl ->
            totalAdded += syncPlaylistForArtistEntity(pl, allTracks)
        }
        totalAdded
    }

    private suspend fun syncPlaylistForArtistEntity(
        playlist: PlaylistEntity,
        cachedTracks: List<MusicTrack>? = null,
    ): Int {
        val artistName = playlist.artistFilterName?.takeIf { it.isNotBlank() } ?: return 0
        val allTracks = cachedTracks ?: database.trackDao().getAllTracksSync().map { it.toDomain() }
        val matched = MusicSearchEngine.tracksForArtist(
            tracks = allTracks,
            artistName = artistName,
            includeCollaborations = playlist.includeCollaborations,
        )
        val existingIds = database.playlistDao().getPlaylistEntriesSync(playlist.id).map { it.trackId }
        val existingSet = existingIds.toSet()
        val newIds = matched.map { it.id }.filter { it !in existingSet }.distinct()
        if (newIds.isNotEmpty()) {
            database.playlistDao().replacePlaylistOrder(playlist.id, existingIds + newIds)
            if (playlist.coverUri.isNullOrBlank()) {
                val art = matched.firstOrNull { !it.albumArtUri.isNullOrBlank() }?.albumArtUri
                if (!art.isNullOrBlank()) {
                    database.playlistDao().updatePlaylistCover(playlist.id, art)
                }
            }
        }
        return newIds.size
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        val existing = database.playlistDao().getPlaylistById(playlistId) ?: return
        database.playlistDao().updatePlaylist(
            existing.copy(name = newName.trim().ifBlank { existing.name }, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deletePlaylist(playlistId: Long) {
        database.playlistDao().deletePlaylist(playlistId)
    }

    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>) {
        val existing = database.playlistDao().getPlaylistEntriesSync(playlistId).map { it.trackId }
        val merged = (existing + trackIds).distinct()
        database.playlistDao().replacePlaylistOrder(playlistId, merged)
    }

    suspend fun addTracksToMultiplePlaylists(playlistIds: List<Long>, trackIds: List<Long>) {
        playlistIds.forEach { pid -> addTracksToPlaylist(pid, trackIds) }
    }

    suspend fun addTracksWithResult(
        playlistIds: List<Long>,
        trackIds: List<Long>,
        allowDuplicates: Boolean = false,
    ): PlaylistAddResult {
        var totalAdded = 0
        val duplicates = mutableListOf<Long>()
        val playlistNames = mutableListOf<String>()

        playlistIds.forEach { pid ->
            val playlist = database.playlistDao().getPlaylistById(pid) ?: return@forEach
            playlistNames.add(playlist.name)
            val existing = database.playlistDao().getPlaylistEntriesSync(pid).map { it.trackId }
            val existingSet = existing.toSet()

            val toAdd = mutableListOf<Long>()
            trackIds.forEach { tid ->
                if (tid in existingSet && !allowDuplicates) {
                    if (tid !in duplicates) duplicates.add(tid)
                } else {
                    toAdd.add(tid)
                }
            }

            if (toAdd.isNotEmpty() || allowDuplicates) {
                val finalOrder = existing + toAdd
                database.playlistDao().replacePlaylistOrder(pid, finalOrder)
                totalAdded += toAdd.size
            }
        }

        return PlaylistAddResult(
            playlistIds = playlistIds,
            playlistNames = playlistNames,
            requestedTrackIds = trackIds,
            addedCount = totalAdded,
            duplicateTrackIds = duplicates,
            duplicateCount = duplicates.size,
        )
    }

    suspend fun duplicatePlaylist(playlistId: Long): Long? {
        val src = database.playlistDao().getPlaylistById(playlistId) ?: return null
        val entries = database.playlistDao().getPlaylistEntriesSync(playlistId).map { it.trackId }
        val newId = database.playlistDao().insertPlaylist(
            src.copy(
                id = 0L,
                name = "${src.name} (Cópia)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis(),
            )
        )
        database.playlistDao().replacePlaylistOrder(newId, entries)
        return newId
    }

    suspend fun clearPlaylist(playlistId: Long) {
        database.playlistDao().clearPlaylistTracks(playlistId)
        database.playlistDao().getPlaylistById(playlistId)?.let {
            database.playlistDao().updatePlaylist(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun updatePlaylistCover(playlistId: Long, coverUri: String?) {
        database.playlistDao().updatePlaylistCover(playlistId, coverUri)
    }

    suspend fun setPlaylistFavorite(playlistId: Long, isFavorite: Boolean) {
        database.playlistDao().setPlaylistFavorite(playlistId, isFavorite)
    }

    suspend fun getPlaylistTrackIdsSync(playlistId: Long): List<Long> {
        return database.playlistDao().getPlaylistEntriesSync(playlistId).map { it.trackId }
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        database.playlistDao().removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>) {
        database.playlistDao().removeTracksFromPlaylist(playlistId, trackIds)
    }

    suspend fun reorderPlaylist(playlistId: Long, orderedTrackIds: List<Long>) {
        database.playlistDao().replacePlaylistOrder(playlistId, orderedTrackIds)
    }

    suspend fun exportPlaylistM3u(playlistId: Long, targetUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val playlist = database.playlistDao().getPlaylistById(playlistId) ?: error("Playlist não encontrada.")
            val entries = database.playlistDao().getPlaylistEntriesSync(playlistId)
            val tracksById = database.trackDao().getTracksByIds(entries.map { it.trackId }).associateBy { it.id }

            val sb = StringBuilder()
            sb.appendLine("#EXTM3U")
            sb.appendLine("#PLAYLIST:${playlist.name}")
            var count = 0
            entries.forEach { e ->
                val t = tracksById[e.trackId] ?: return@forEach
                val sec = (t.durationMs / 1000L).coerceAtLeast(0L)
                sb.appendLine("#EXTINF:$sec,${t.artist} - ${t.title}")
                sb.appendLine(t.filePath.ifBlank { t.contentUri })
                count++
            }
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: error("Não foi possível gravar o arquivo M3U.")
            count
        }
    }

    suspend fun importPlaylistM3u(sourceUri: Uri, playlistName: String): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(sourceUri)?.bufferedReader()?.use { it.readText() }
                ?: error("Não foi possível ler o arquivo de playlist.")
            val allTracks = database.trackDao().getAllTracksSync()
            val byPath = allTracks.associateBy { it.filePath.lowercase() }
            val byFileName = allTracks.associateBy { File(it.filePath).name.lowercase() }

            val matchedIds = mutableListOf<Long>()
            content.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .forEach { ref ->
                    val normalized = ref.lowercase()
                    val nameOnly = File(ref).name.lowercase()
                    val hit = byPath[normalized] ?: byFileName[nameOnly]
                    if (hit != null) matchedIds.add(hit.id)
                }

            val finalName = playlistName.trim().ifBlank { "Playlist Importada" }
            val newId = createPlaylist(finalName, "Importada via M3U")
            if (matchedIds.isNotEmpty()) {
                addTracksToPlaylist(newId, matchedIds)
            }
            newId
        }
    }
}

class QueueRepository(private val database: MyatoDatabase) {
    val queuesFlow: Flow<List<PlaybackQueue>> = combine(
        database.queueDao().observeQueues(),
        database.queueDao().observeAllQueueTracks(),
    ) { queues, tracks ->
        val counts = tracks.groupBy { it.queueId }
        queues.map { q -> q.toDomain(trackCount = counts[q.id]?.size ?: 0) }
    }

    fun observeQueueTrackIds(queueId: Long): Flow<List<Long>> =
        database.queueDao().observeQueueTracks(queueId).map { list -> list.map { it.trackId } }

    suspend fun createQueue(name: String, trackIds: List<Long> = emptyList()): Long {
        val clean = name.trim().ifBlank { "Nova Fila" }
        val qid = database.queueDao().insertQueue(
            PlaybackQueueEntity(
                name = clean,
                currentTrackId = trackIds.firstOrNull(),
                isActive = false,
            )
        )
        if (trackIds.isNotEmpty()) {
            database.queueDao().replaceQueueTracks(qid, trackIds)
        }
        return qid
    }

    suspend fun renameQueue(queueId: Long, newName: String) {
        val q = database.queueDao().getQueueById(queueId) ?: return
        database.queueDao().updateQueue(q.copy(name = newName.trim().ifBlank { q.name }))
    }

    suspend fun deleteQueue(queueId: Long) {
        database.queueDao().deleteQueue(queueId)
    }

    suspend fun duplicateQueue(queueId: Long): Long? {
        val src = database.queueDao().getQueueById(queueId) ?: return null
        val tracks = database.queueDao().getQueueTracksSync(queueId).map { it.trackId }
        val newId = database.queueDao().insertQueue(
            src.copy(
                id = 0L,
                name = "${src.name} (Cópia)",
                isActive = false,
                createdAt = System.currentTimeMillis(),
            )
        )
        database.queueDao().replaceQueueTracks(newId, tracks)
        return newId
    }

    suspend fun mergeQueues(sourceQueueId: Long, targetQueueId: Long) {
        if (sourceQueueId == targetQueueId) return
        val srcTracks = database.queueDao().getQueueTracksSync(sourceQueueId).map { it.trackId }
        val dstTracks = database.queueDao().getQueueTracksSync(targetQueueId).map { it.trackId }
        database.queueDao().replaceQueueTracks(targetQueueId, dstTracks + srcTracks)
    }

    suspend fun replaceQueueTracks(queueId: Long, trackIds: List<Long>) {
        database.queueDao().replaceQueueTracks(queueId, trackIds)
    }
}

class ThemeRepository(
    private val database: MyatoDatabase,
    private val preferencesStore: AppPreferencesStore,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    val themesFlow: Flow<List<MyatoTheme>> = database.themeDao().observeThemes().map { list ->
        if (list.isEmpty()) BuiltInThemes.all else list.map { it.toDomain() }
    }

    val activeThemeFlow: Flow<MyatoTheme> = combine(
        themesFlow,
        preferencesStore.preferencesFlow,
    ) { themes, prefs ->
        themes.firstOrNull { it.id == prefs.activeThemeId } ?: BuiltInThemes.MidnightOrange
    }

    suspend fun selectTheme(themeId: String) {
        preferencesStore.setActiveThemeId(themeId)
    }

    suspend fun saveCustomTheme(theme: MyatoTheme) {
        val custom = theme.copy(isBuiltIn = false)
        database.themeDao().upsertTheme(custom.toEntity())
        preferencesStore.setActiveThemeId(custom.id)
    }

    suspend fun duplicateTheme(source: MyatoTheme, newName: String): MyatoTheme {
        val copy = source.copy(
            id = "custom_${System.currentTimeMillis()}",
            name = newName.trim().ifBlank { "${source.name} Personalizado" },
            isBuiltIn = false,
        )
        database.themeDao().upsertTheme(copy.toEntity())
        return copy
    }

    suspend fun deleteCustomTheme(themeId: String) {
        database.themeDao().deleteCustomTheme(themeId)
    }

    fun exportThemeJson(theme: MyatoTheme): String = json.encodeToString(MyatoTheme.serializer(), theme)

    suspend fun importThemeJson(rawJson: String): Result<MyatoTheme> = runCatching {
        val parsed = json.decodeFromString(MyatoTheme.serializer(), rawJson)
        val imported = parsed.copy(
            id = "imported_${System.currentTimeMillis()}",
            isBuiltIn = false,
        )
        database.themeDao().upsertTheme(imported.toEntity())
        imported
    }
}

class StatsAndEngagementRepository(private val database: MyatoDatabase) {

    val statsSummaryFlow: Flow<ListeningStatsSummary> = combine(
        database.trackDao().observeAllTracks(),
        database.engagementDao().observeAllStats(),
        database.engagementDao().observeHistory(limit = 100),
    ) { tracks, stats, history ->
        val byId = tracks.associateBy { it.id }
        val topTracks = stats
            .filter { it.playCount > 0 }
            .sortedByDescending { it.playCount }
            .take(25)
            .mapNotNull { s ->
                val t = byId[s.trackId] ?: return@mapNotNull null
                TrackStatItem(
                    trackId = t.id,
                    title = t.title,
                    artist = t.artist,
                    album = t.album,
                    playCount = s.playCount,
                    totalListenMs = s.totalListenMs,
                    artUri = t.albumArtUri,
                )
            }

        val topArtists = stats
            .mapNotNull { s -> byId[s.trackId]?.let { it.artist to s } }
            .groupBy { it.first }
            .map { (artist, pairs) ->
                ArtistStatItem(
                    artist = artist,
                    playCount = pairs.sumOf { it.second.playCount },
                    totalListenMs = pairs.sumOf { it.second.totalListenMs },
                )
            }
            .sortedByDescending { it.playCount }
            .take(15)

        val topAlbums = stats
            .mapNotNull { s -> byId[s.trackId]?.let { it to s } }
            .groupBy { it.first.album }
            .map { (album, pairs) ->
                val firstTrack = pairs.first().first
                AlbumStatItem(
                    album = album,
                    artist = firstTrack.artist,
                    playCount = pairs.sumOf { it.second.playCount },
                    totalListenMs = pairs.sumOf { it.second.totalListenMs },
                    artUri = firstTrack.albumArtUri,
                )
            }
            .sortedByDescending { it.playCount }
            .take(15)

        val recentHistory = history.map { h ->
            HistoryStatItem(
                id = h.id,
                trackId = h.trackId,
                title = h.trackTitle,
                artist = h.trackArtist,
                playedAt = h.playedAt,
                durationListenedMs = h.durationListenedMs,
            )
        }

        ListeningStatsSummary(
            totalListenMs = stats.sumOf { it.totalListenMs },
            totalPlays = stats.sumOf { it.playCount },
            topTracks = topTracks,
            topArtists = topArtists,
            topAlbums = topAlbums,
            recentHistory = recentHistory,
        )
    }

    fun observeBookmarksForTrack(trackId: Long): Flow<List<Bookmark>> =
        database.engagementDao().observeBookmarksForTrack(trackId).map { list -> list.map { it.toDomain() } }

    fun observeAllBookmarks(): Flow<List<Bookmark>> =
        database.engagementDao().observeAllBookmarks().map { list -> list.map { it.toDomain() } }

    suspend fun addBookmark(trackId: Long, label: String, positionMs: Long) {
        database.engagementDao().insertBookmark(
            BookmarkEntity(
                trackId = trackId,
                label = label.trim().ifBlank { "Marcador" },
                positionMs = positionMs.coerceAtLeast(0L),
            )
        )
    }

    suspend fun deleteBookmark(bookmarkId: Long) {
        database.engagementDao().deleteBookmark(bookmarkId)
    }

    fun observeNoteForTrack(trackId: Long): Flow<MusicNote?> =
        database.engagementDao().observeNoteForTrack(trackId).map { it?.toDomain() }

    suspend fun saveNote(trackId: Long, content: String) {
        val existing = database.engagementDao().getAllNotesSync().firstOrNull { it.trackId == trackId }
        database.engagementDao().upsertNote(
            MusicNoteEntity(
                id = existing?.id ?: 0L,
                trackId = trackId,
                content = content,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun deleteNote(trackId: Long) {
        database.engagementDao().deleteNoteByTrackId(trackId)
    }
}
