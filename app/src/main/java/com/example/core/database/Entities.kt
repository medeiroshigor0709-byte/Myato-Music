package com.example.core.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "music_tracks",
    indices = [
        Index(value = ["albumId"]),
        Index(value = ["artistId"]),
        Index(value = ["folderPath"]),
        Index(value = ["title"]),
        Index(value = ["dateAddedSec"]),
    ]
)
data class MusicTrackEntity(
    @PrimaryKey val id: Long,
    val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val artistId: Long,
    val albumArtist: String = "",
    val genre: String = "",
    val year: Int = 0,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val composer: String = "",
    val comments: String = "",
    val durationMs: Long,
    val sizeBytes: Long,
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
    val mimeType: String = "audio/mpeg",
    val formatExtension: String = "MP3",
    val filePath: String,
    val folderPath: String,
    val contentUri: String,
    val albumArtUri: String? = null,
    val dateAddedSec: Long = 0L,
    val isPlayable: Boolean = true,
    val embeddedLyrics: String = "",
)

@Entity(
    tableName = "artists",
    indices = [Index(value = ["name"], unique = true)]
)
data class ArtistEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
)

@Entity(
    tableName = "albums",
    indices = [Index(value = ["title"]), Index(value = ["artistName"])]
)
data class AlbumEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artistName: String,
    val trackCount: Int,
    val totalDurationMs: Long,
    val artUri: String?,
    val year: Int = 0,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    val coverUri: String? = null,
    val lastUsedAt: Long = 0L,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val artistFilterName: String? = null,
    val autoUpdateByArtist: Boolean = false,
    val includeCollaborations: Boolean = false,
)

@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["trackId"]),
    ]
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "playback_queues",
    indices = [Index(value = ["name"]), Index(value = ["isActive"])]
)
data class PlaybackQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val originType: String = "CUSTOM",
    val originKey: String = "",
    val currentTrackId: Long? = null,
    val currentPositionMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: String = "OFF",
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false,
)

@Entity(
    tableName = "queue_tracks",
    primaryKeys = ["queueId", "trackId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = PlaybackQueueEntity::class,
            parentColumns = ["id"],
            childColumns = ["queueId"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = ["queueId"]),
        Index(value = ["trackId"]),
    ]
)
data class QueueTrackEntity(
    val queueId: Long,
    val trackId: Long,
    val position: Int,
)

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["addedAt"])]
)
data class FavoriteEntity(
    @PrimaryKey val trackId: Long,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["trackId"])]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackId: Long,
    val label: String,
    val positionMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "music_notes",
    indices = [Index(value = ["trackId"], unique = true)]
)
data class MusicNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackId: Long,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "playback_history",
    indices = [Index(value = ["trackId"]), Index(value = ["playedAt"])]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackId: Long,
    val trackTitle: String,
    val trackArtist: String,
    val playedAt: Long = System.currentTimeMillis(),
    val durationListenedMs: Long = 0L,
)

@Entity(
    tableName = "playback_statistics",
    indices = [Index(value = ["playCount"]), Index(value = ["lastPlayedAt"])]
)
data class PlaybackStatisticsEntity(
    @PrimaryKey val trackId: Long,
    val playCount: Int = 0,
    val totalListenMs: Long = 0L,
    val lastPlayedAt: Long = 0L,
)

@Entity(tableName = "themes")
data class ThemeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isBuiltIn: Boolean,
    val isLight: Boolean,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val backgroundColorHex: Long,
    val altBackgroundColorHex: Long,
    val cardColorHex: Long,
    val textColorHex: Long,
    val secondaryTextColorHex: Long,
    val coverShape: String = "ROUNDED",
    val buttonStyle: String = "CIRCULAR",
    val animationMode: String = "FULL",
    val playerAppearance: String = "CINEMATIC",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "audio_presets",
    indices = [Index(value = ["outputDeviceType"])]
)
data class AudioPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val outputDeviceType: String,
    val isEnabled: Boolean = false,
    val bandLevelsJson: String = "[]",
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val trebleBoostDb: Int = 0,
    val isCustom: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "backup_metadata",
    indices = [Index(value = ["createdAt"])]
)
data class BackupMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val fileName: String,
    val uriString: String,
    val isAutomatic: Boolean = false,
    val itemCount: Int = 0,
    val sizeBytes: Long = 0L,
    val statusMessage: String = "Concluído",
    val createdAt: Long = System.currentTimeMillis(),
)

data class QueueWithEntries(
    @Embedded val queue: PlaybackQueueEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "queueId"
    )
    val entries: List<QueueTrackEntity>,
)
