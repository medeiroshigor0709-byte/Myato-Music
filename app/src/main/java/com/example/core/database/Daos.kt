package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM music_tracks ORDER BY title COLLATE NOCASE ASC")
    fun observeAllTracks(): Flow<List<MusicTrackEntity>>

    @Query("SELECT * FROM music_tracks ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllTracksSync(): List<MusicTrackEntity>

    @Query("SELECT * FROM music_tracks WHERE id = :trackId LIMIT 1")
    suspend fun getTrackById(trackId: Long): MusicTrackEntity?

    @Query("SELECT * FROM music_tracks WHERE id IN (:ids)")
    suspend fun getTracksByIds(ids: List<Long>): List<MusicTrackEntity>

    @Query("SELECT * FROM albums ORDER BY title COLLATE NOCASE ASC")
    fun observeAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE ASC")
    fun observeArtists(): Flow<List<ArtistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<MusicTrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: MusicTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbums(albums: List<AlbumEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtists(artists: List<ArtistEntity>)

    @Query("DELETE FROM music_tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: Long)

    @Query("DELETE FROM music_tracks WHERE id IN (:trackIds)")
    suspend fun deleteTracksByIds(trackIds: List<Long>)

    @Query("DELETE FROM music_tracks WHERE id NOT IN (:activeIds)")
    suspend fun deleteTracksNotIn(activeIds: List<Long>)

    @Query("DELETE FROM music_tracks")
    suspend fun clearAllTracks()

    @Query("DELETE FROM albums")
    suspend fun clearAlbums()

    @Query("DELETE FROM artists")
    suspend fun clearArtists()

    @Transaction
    suspend fun replaceLibrarySnapshot(
        tracks: List<MusicTrackEntity>,
        albums: List<AlbumEntity>,
        artists: List<ArtistEntity>,
    ) {
        clearAllTracks()
        clearAlbums()
        clearArtists()
        if (tracks.isNotEmpty()) upsertTracks(tracks)
        if (albums.isNotEmpty()) upsertAlbums(albums)
        if (artists.isNotEmpty()) upsertArtists(artists)
    }
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    suspend fun getPlaylistsSync(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observePlaylistEntries(playlistId: Long): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT * FROM playlist_tracks ORDER BY playlistId ASC, position ASC")
    fun observeAllPlaylistEntries(): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistEntriesSync(playlistId: Long): List<PlaylistTrackEntity>

    @Query("SELECT * FROM playlist_tracks")
    suspend fun getAllPlaylistEntriesSync(): List<PlaylistTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylistTracks(entries: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId IN (:trackIds)")
    suspend fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>)

    @Query("DELETE FROM playlist_tracks WHERE trackId IN (:trackIds)")
    suspend fun removeTracksFromAllPlaylists(trackIds: List<Long>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)

    @Query("UPDATE playlists SET lastUsedAt = :timestamp, updatedAt = :timestamp WHERE id = :playlistId")
    suspend fun touchPlaylistLastUsed(playlistId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE playlists SET coverUri = :coverUri, updatedAt = :timestamp WHERE id = :playlistId")
    suspend fun updatePlaylistCover(playlistId: Long, coverUri: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE playlists SET isFavorite = :isFavorite, updatedAt = :timestamp WHERE id = :playlistId")
    suspend fun setPlaylistFavorite(playlistId: Long, isFavorite: Boolean, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun replacePlaylistOrder(playlistId: Long, orderedTrackIds: List<Long>) {
        clearPlaylistTracks(playlistId)
        val entries = orderedTrackIds.mapIndexed { index, trackId ->
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = trackId,
                position = index,
            )
        }
        if (entries.isNotEmpty()) upsertPlaylistTracks(entries)
        getPlaylistById(playlistId)?.let {
            val now = System.currentTimeMillis()
            updatePlaylist(it.copy(updatedAt = now, lastUsedAt = now))
        }
    }
}

@Dao
interface QueueDao {
    @Query("SELECT * FROM playback_queues ORDER BY isActive DESC, lastActiveAt DESC")
    fun observeQueues(): Flow<List<PlaybackQueueEntity>>

    @Query("SELECT * FROM playback_queues ORDER BY isActive DESC, lastActiveAt DESC")
    suspend fun getQueuesSync(): List<PlaybackQueueEntity>

    @Query("SELECT * FROM playback_queues WHERE id = :queueId LIMIT 1")
    suspend fun getQueueById(queueId: Long): PlaybackQueueEntity?

    @Query("SELECT * FROM playback_queues WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveQueueSync(): PlaybackQueueEntity?

    @Query("SELECT * FROM playback_queues WHERE isActive = 1 LIMIT 1")
    fun observeActiveQueue(): Flow<PlaybackQueueEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(queue: PlaybackQueueEntity): Long

    @Update
    suspend fun updateQueue(queue: PlaybackQueueEntity)

    @Query("DELETE FROM playback_queues WHERE id = :queueId")
    suspend fun deleteQueue(queueId: Long)

    @Query("UPDATE playback_queues SET isActive = 0")
    suspend fun clearActiveFlags()

    @Query("SELECT * FROM queue_tracks WHERE queueId = :queueId ORDER BY position ASC")
    fun observeQueueTracks(queueId: Long): Flow<List<QueueTrackEntity>>

    @Query("SELECT * FROM queue_tracks ORDER BY queueId ASC, position ASC")
    fun observeAllQueueTracks(): Flow<List<QueueTrackEntity>>

    @Query("SELECT * FROM queue_tracks WHERE queueId = :queueId ORDER BY position ASC")
    suspend fun getQueueTracksSync(queueId: Long): List<QueueTrackEntity>

    @Query("SELECT * FROM queue_tracks")
    suspend fun getAllQueueTracksSync(): List<QueueTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueTracks(entries: List<QueueTrackEntity>)

    @Query("DELETE FROM queue_tracks WHERE queueId = :queueId")
    suspend fun clearQueueTracks(queueId: Long)

    @Query("DELETE FROM queue_tracks WHERE trackId IN (:trackIds)")
    suspend fun removeTracksFromAllQueues(trackIds: List<Long>)

    @Transaction
    suspend fun replaceQueueTracks(queueId: Long, trackIds: List<Long>) {
        clearQueueTracks(queueId)
        val entries = trackIds.mapIndexed { index, trackId ->
            QueueTrackEntity(queueId = queueId, trackId = trackId, position = index)
        }
        if (entries.isNotEmpty()) insertQueueTracks(entries)
    }

    @Transaction
    suspend fun setActiveQueue(queueId: Long) {
        clearActiveFlags()
        val target = getQueueById(queueId) ?: return
        updateQueue(target.copy(isActive = true, lastActiveAt = System.currentTimeMillis()))
    }
}

@Dao
interface EngagementDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites")
    suspend fun getFavoritesSync(): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :trackId)")
    suspend fun isFavorite(trackId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun removeFavorite(trackId: Long)

    @Query("SELECT * FROM bookmarks WHERE trackId = :trackId ORDER BY positionMs ASC")
    fun observeBookmarksForTrack(trackId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarksSync(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: Long)

    @Query("SELECT * FROM music_notes WHERE trackId = :trackId LIMIT 1")
    fun observeNoteForTrack(trackId: Long): Flow<MusicNoteEntity?>

    @Query("SELECT * FROM music_notes ORDER BY updatedAt DESC")
    fun observeAllNotes(): Flow<List<MusicNoteEntity>>

    @Query("SELECT * FROM music_notes")
    suspend fun getAllNotesSync(): List<MusicNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: MusicNoteEntity): Long

    @Query("DELETE FROM music_notes WHERE trackId = :trackId")
    suspend fun deleteNoteByTrackId(trackId: Long)

    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit")
    fun observeHistory(limit: Int = 200): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC")
    suspend fun getHistorySync(): List<PlaybackHistoryEntity>

    @Insert
    suspend fun insertHistory(entry: PlaybackHistoryEntity): Long

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM playback_statistics")
    fun observeAllStats(): Flow<List<PlaybackStatisticsEntity>>

    @Query("SELECT * FROM playback_statistics")
    suspend fun getAllStatsSync(): List<PlaybackStatisticsEntity>

    @Query("SELECT * FROM playback_statistics WHERE trackId = :trackId LIMIT 1")
    suspend fun getStatsForTrack(trackId: Long): PlaybackStatisticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(stats: PlaybackStatisticsEntity)

    @Transaction
    suspend fun recordPlayback(
        trackId: Long,
        title: String,
        artist: String,
        durationListenedMs: Long,
    ) {
        val now = System.currentTimeMillis()
        insertHistory(
            PlaybackHistoryEntity(
                trackId = trackId,
                trackTitle = title,
                trackArtist = artist,
                playedAt = now,
                durationListenedMs = durationListenedMs,
            )
        )
        val current = getStatsForTrack(trackId)
        val updated = if (current == null) {
            PlaybackStatisticsEntity(
                trackId = trackId,
                playCount = 1,
                totalListenMs = durationListenedMs.coerceAtLeast(0L),
                lastPlayedAt = now,
            )
        } else {
            current.copy(
                playCount = current.playCount + 1,
                totalListenMs = current.totalListenMs + durationListenedMs.coerceAtLeast(0L),
                lastPlayedAt = now,
            )
        }
        upsertStats(updated)
    }
}

@Dao
interface ThemeDao {
    @Query("SELECT * FROM themes ORDER BY isBuiltIn DESC, createdAt ASC")
    fun observeThemes(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes ORDER BY isBuiltIn DESC, createdAt ASC")
    suspend fun getThemesSync(): List<ThemeEntity>

    @Query("SELECT * FROM themes WHERE id = :id LIMIT 1")
    suspend fun getThemeById(id: String): ThemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTheme(theme: ThemeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThemes(themes: List<ThemeEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingBuiltInThemes(themes: List<ThemeEntity>)

    @Query("DELETE FROM themes WHERE isBuiltIn = 1 AND id NOT IN (:validIds)")
    suspend fun deleteObsoleteBuiltInThemes(validIds: List<String>)

    @Query("DELETE FROM themes WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomTheme(id: String)
}

@Dao
interface AudioPresetDao {
    @Query("SELECT * FROM audio_presets WHERE outputDeviceType = :deviceType ORDER BY isCustom ASC, name ASC")
    fun observePresetsForDevice(deviceType: String): Flow<List<AudioPresetEntity>>

    @Query("SELECT * FROM audio_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: String): AudioPresetEntity?

    @Query("SELECT * FROM audio_presets")
    suspend fun getAllPresetsSync(): List<AudioPresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreset(preset: AudioPresetEntity)

    @Query("DELETE FROM audio_presets WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomPreset(id: String)
}

@Dao
interface SettingsBackupDao {
    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettingsSync(): List<AppSettingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: AppSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: List<AppSettingsEntity>)

    @Query("SELECT * FROM backup_metadata ORDER BY createdAt DESC")
    fun observeBackups(): Flow<List<BackupMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupMetadata(metadata: BackupMetadataEntity): Long
}
