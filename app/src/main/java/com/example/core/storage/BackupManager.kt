package com.example.core.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import com.example.core.database.AppPreferencesStore
import com.example.core.database.AppSettingsEntity
import com.example.core.database.BackupMetadataEntity
import com.example.core.database.BookmarkEntity
import com.example.core.database.FavoriteEntity
import com.example.core.database.MusicNoteEntity
import com.example.core.database.MyatoDatabase
import com.example.core.database.PlaybackHistoryEntity
import com.example.core.database.PlaybackQueueEntity
import com.example.core.database.PlaybackStatisticsEntity
import com.example.core.database.PlaylistEntity
import com.example.core.database.PlaylistTrackEntity
import com.example.core.database.QueueTrackEntity
import com.example.core.database.toDomain
import com.example.core.database.toEntity
import com.example.domain.models.BackupPreview
import com.example.domain.models.MyatoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupPlaylistDto(
    val id: Long,
    val name: String,
    val description: String,
    val trackIds: List<Long>,
    val createdAt: Long,
    val coverUri: String? = null,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long = 0L,
    val artistFilterName: String? = null,
    val autoUpdateByArtist: Boolean = false,
    val includeCollaborations: Boolean = false,
)

@Serializable
data class BackupQueueDto(
    val id: Long,
    val name: String,
    val originType: String,
    val originKey: String,
    val currentTrackId: Long?,
    val currentPositionMs: Long,
    val isShuffleEnabled: Boolean,
    val repeatMode: String,
    val isActive: Boolean,
    val trackIds: List<Long>,
)

@Serializable
data class BackupBookmarkDto(
    val trackId: Long,
    val label: String,
    val positionMs: Long,
    val createdAt: Long,
)

@Serializable
data class BackupNoteDto(
    val trackId: Long,
    val content: String,
    val updatedAt: Long,
)

@Serializable
data class BackupHistoryDto(
    val trackId: Long,
    val trackTitle: String,
    val trackArtist: String,
    val playedAt: Long,
    val durationListenedMs: Long,
)

@Serializable
data class BackupStatDto(
    val trackId: Long,
    val playCount: Int,
    val totalListenMs: Long,
    val lastPlayedAt: Long,
)

@Serializable
data class MyatoBackupBundle(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val activeThemeId: String = "midnight_orange",
    val gaplessEnabled: Boolean = true,
    val batterySaverAnimations: Boolean = false,
    val playlists: List<BackupPlaylistDto> = emptyList(),
    val queues: List<BackupQueueDto> = emptyList(),
    val favoriteTrackIds: List<Long> = emptyList(),
    val bookmarks: List<BackupBookmarkDto> = emptyList(),
    val notes: List<BackupNoteDto> = emptyList(),
    val history: List<BackupHistoryDto> = emptyList(),
    val statistics: List<BackupStatDto> = emptyList(),
    val customThemes: List<MyatoTheme> = emptyList(),
    val favoriteThemeIds: List<String> = emptyList(),
)

class BackupManager(
    private val context: Context,
    private val database: MyatoDatabase,
    private val preferencesStore: AppPreferencesStore,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val prefs = preferencesStore.preferencesFlow.first()
        val playlists = database.playlistDao().getPlaylistsSync()
        val playlistEntries = database.playlistDao().getAllPlaylistEntriesSync().groupBy { it.playlistId }
        val queues = database.queueDao().getQueuesSync()
        val queueEntries = database.queueDao().getAllQueueTracksSync().groupBy { it.queueId }
        val favorites = database.engagementDao().getFavoritesSync()
        val bookmarks = database.engagementDao().getAllBookmarksSync()
        val notes = database.engagementDao().getAllNotesSync()
        val history = database.engagementDao().getHistorySync()
        val stats = database.engagementDao().getAllStatsSync()
        val customThemes = database.themeDao().getThemesSync().filter { !it.isBuiltIn }.map { it.toDomain() }

        val bundle = MyatoBackupBundle(
            activeThemeId = prefs.activeThemeId,
            gaplessEnabled = prefs.gaplessEnabled,
            batterySaverAnimations = prefs.batterySaverAnimations,
            playlists = playlists.map { p ->
                BackupPlaylistDto(
                    id = p.id,
                    name = p.name,
                    description = p.description,
                    trackIds = playlistEntries[p.id]?.sortedBy { it.position }?.map { it.trackId }.orEmpty(),
                    createdAt = p.createdAt,
                    coverUri = p.coverUri,
                    isFavorite = p.isFavorite,
                    lastUsedAt = p.lastUsedAt,
                    artistFilterName = p.artistFilterName,
                    autoUpdateByArtist = p.autoUpdateByArtist,
                    includeCollaborations = p.includeCollaborations,
                )
            },
            queues = queues.map { q ->
                BackupQueueDto(
                    id = q.id,
                    name = q.name,
                    originType = q.originType,
                    originKey = q.originKey,
                    currentTrackId = q.currentTrackId,
                    currentPositionMs = q.currentPositionMs,
                    isShuffleEnabled = q.isShuffleEnabled,
                    repeatMode = q.repeatMode,
                    isActive = q.isActive,
                    trackIds = queueEntries[q.id]?.sortedBy { it.position }?.map { it.trackId }.orEmpty(),
                )
            },
            favoriteTrackIds = favorites.map { it.trackId },
            bookmarks = bookmarks.map { BackupBookmarkDto(it.trackId, it.label, it.positionMs, it.createdAt) },
            notes = notes.map { BackupNoteDto(it.trackId, it.content, it.updatedAt) },
            history = history.map { BackupHistoryDto(it.trackId, it.trackTitle, it.trackArtist, it.playedAt, it.durationListenedMs) },
            statistics = stats.map { BackupStatDto(it.trackId, it.playCount, it.totalListenMs, it.lastPlayedAt) },
            customThemes = customThemes,
            favoriteThemeIds = prefs.favoriteThemeIds.toList(),
        )
        json.encodeToString(MyatoBackupBundle.serializer(), bundle)
    }

    suspend fun exportBackupToUri(targetUri: Uri, isAutomatic: Boolean = false): Result<BackupMetadataEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = createBackupJson()
                val bytes = payload.toByteArray(Charsets.UTF_8)
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    out.write(bytes)
                    out.flush()
                } ?: error("Não foi possível abrir destino para escrita.")

                val bundle = json.decodeFromString(MyatoBackupBundle.serializer(), payload)
                val count = bundle.playlists.size + bundle.queues.size + bundle.favoriteTrackIds.size +
                    bundle.bookmarks.size + bundle.notes.size + bundle.customThemes.size
                val fileName = DocumentFile.fromSingleUri(context, targetUri)?.name ?: "myato_backup.json"
                val meta = BackupMetadataEntity(
                    fileName = fileName,
                    uriString = targetUri.toString(),
                    isAutomatic = isAutomatic,
                    itemCount = count,
                    sizeBytes = bytes.size.toLong(),
                    statusMessage = "Backup exportado com sucesso",
                )
                val id = database.settingsBackupDao().insertBackupMetadata(meta)
                meta.copy(id = id)
            }
        }

    suspend fun exportBackupToDirectory(dirUri: Uri, isAutomatic: Boolean): Result<BackupMetadataEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = DocumentFile.fromTreeUri(context, dirUri) ?: error("Diretório SAF indisponível.")
                val timestamp = System.currentTimeMillis()
                val fileName = "myato_backup_$timestamp.json"
                val doc = dir.createFile("application/json", fileName) ?: error("Falha ao criar arquivo de backup.")
                exportBackupToUri(doc.uri, isAutomatic = isAutomatic).getOrThrow()
            }
        }

    fun previewBackupJson(rawJson: String): Result<Pair<BackupPreview, MyatoBackupBundle>> {
        return runCatching {
            val bundle = json.decodeFromString(MyatoBackupBundle.serializer(), rawJson)
            val preview = BackupPreview(
                version = bundle.version,
                createdAt = bundle.createdAt,
                deviceName = bundle.deviceName,
                playlistsCount = bundle.playlists.size,
                queuesCount = bundle.queues.size,
                favoritesCount = bundle.favoriteTrackIds.size,
                bookmarksCount = bundle.bookmarks.size,
                notesCount = bundle.notes.size,
                historyCount = bundle.history.size,
                statsCount = bundle.statistics.size,
                customThemesCount = bundle.customThemes.size,
            )
            preview to bundle
        }
    }

    suspend fun readBackupFromUri(uri: Uri): Result<Pair<BackupPreview, MyatoBackupBundle>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Não foi possível ler o arquivo de backup.")
                previewBackupJson(raw).getOrThrow()
            }
        }

    suspend fun restoreBackup(bundle: MyatoBackupBundle, preserveExistingData: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                preferencesStore.setActiveThemeId(bundle.activeThemeId)
                preferencesStore.setGaplessEnabled(bundle.gaplessEnabled)
                preferencesStore.setBatterySaverAnimations(bundle.batterySaverAnimations)

                bundle.customThemes.forEach { theme ->
                    database.themeDao().upsertTheme(theme.copy(isBuiltIn = false).toEntity())
                }
                bundle.favoriteThemeIds.forEach { favId ->
                    if (favId !in preferencesStore.preferencesFlow.first().favoriteThemeIds) {
                        preferencesStore.toggleFavoriteTheme(favId)
                    }
                }

                if (!preserveExistingData) {
                    database.playlistDao().getPlaylistsSync().forEach {
                        database.playlistDao().deletePlaylist(it.id)
                    }
                    database.queueDao().getQueuesSync().forEach {
                        database.queueDao().deleteQueue(it.id)
                    }
                    database.engagementDao().clearHistory()
                }

                bundle.playlists.forEach { p ->
                    val newId = database.playlistDao().insertPlaylist(
                        PlaylistEntity(
                            name = p.name,
                            description = p.description,
                            coverUri = p.coverUri,
                            isFavorite = p.isFavorite,
                            lastUsedAt = p.lastUsedAt,
                            createdAt = p.createdAt,
                            artistFilterName = p.artistFilterName,
                            autoUpdateByArtist = p.autoUpdateByArtist,
                            includeCollaborations = p.includeCollaborations,
                        )
                    )
                    database.playlistDao().replacePlaylistOrder(newId, p.trackIds)
                }

                bundle.queues.forEach { q ->
                    val newQueueId = database.queueDao().insertQueue(
                        PlaybackQueueEntity(
                            name = q.name,
                            originType = q.originType,
                            originKey = q.originKey,
                            currentTrackId = q.currentTrackId,
                            currentPositionMs = q.currentPositionMs,
                            isShuffleEnabled = q.isShuffleEnabled,
                            repeatMode = q.repeatMode,
                            isActive = q.isActive,
                        )
                    )
                    database.queueDao().replaceQueueTracks(newQueueId, q.trackIds)
                }

                bundle.favoriteTrackIds.forEach { trackId ->
                    database.engagementDao().addFavorite(FavoriteEntity(trackId = trackId))
                }

                bundle.bookmarks.forEach { b ->
                    database.engagementDao().insertBookmark(
                        BookmarkEntity(trackId = b.trackId, label = b.label, positionMs = b.positionMs, createdAt = b.createdAt)
                    )
                }

                bundle.notes.forEach { n ->
                    database.engagementDao().upsertNote(
                        MusicNoteEntity(trackId = n.trackId, content = n.content, updatedAt = n.updatedAt)
                    )
                }

                bundle.history.forEach { h ->
                    database.engagementDao().insertHistory(
                        PlaybackHistoryEntity(
                            trackId = h.trackId,
                            trackTitle = h.trackTitle,
                            trackArtist = h.trackArtist,
                            playedAt = h.playedAt,
                            durationListenedMs = h.durationListenedMs,
                        )
                    )
                }

                bundle.statistics.forEach { s ->
                    database.engagementDao().upsertStats(
                        PlaybackStatisticsEntity(
                            trackId = s.trackId,
                            playCount = s.playCount,
                            totalListenMs = s.totalListenMs,
                            lastPlayedAt = s.lastPlayedAt,
                        )
                    )
                }

                database.settingsBackupDao().upsertSetting(
                    AppSettingsEntity(key = "last_restore_at", value = System.currentTimeMillis().toString())
                )
            }
        }

    suspend fun buildTopTracksCsv(): String = withContext(Dispatchers.IO) {
        val tracks = database.trackDao().getAllTracksSync().associateBy { it.id }
        val stats = database.engagementDao().getAllStatsSync().sortedByDescending { it.playCount }
        val sb = StringBuilder()
        sb.appendLine("Posicao,Titulo,Artista,Album,Reproducoes,TempoOuvidoMs")
        stats.forEachIndexed { index, stat ->
            val track = tracks[stat.trackId]
            val title = (track?.title ?: "Track #${stat.trackId}").replace("\"", "\"\"")
            val artist = (track?.artist ?: "Desconhecido").replace("\"", "\"\"")
            val album = (track?.album ?: "Desconhecido").replace("\"", "\"\"")
            sb.appendLine("${index + 1},\"$title\",\"$artist\",\"$album\",${stat.playCount},${stat.totalListenMs}")
        }
        sb.toString()
    }
}
