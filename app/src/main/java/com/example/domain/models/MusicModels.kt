package com.example.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicTrack(
    val id: Long,
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
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val embeddedLyrics: String = "",
)

@Serializable
data class AlbumItem(
    val id: Long,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val totalDurationMs: Long,
    val artUri: String?,
    val year: Int = 0,
)

@Serializable
data class ArtistItem(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
)

@Serializable
data class FolderItem(
    val path: String,
    val name: String,
    val trackCount: Int,
    val totalDurationMs: Long,
    val isMonitored: Boolean = true,
)

@Serializable
data class GenreItem(
    val name: String,
    val trackCount: Int,
)

@Serializable
data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val coverUri: String? = null,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long = 0L,
    val trackCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class PlaylistAddResult(
    val playlistIds: List<Long>,
    val playlistNames: List<String>,
    val requestedTrackIds: List<Long>,
    val addedCount: Int,
    val duplicateTrackIds: List<Long>,
    val duplicateCount: Int,
)

@Serializable
enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

@Serializable
enum class QueueOriginType {
    CUSTOM,
    ALL_SONGS,
    ALBUM,
    ARTIST,
    FOLDER,
    PLAYLIST,
    FAVORITES
}

@Serializable
data class PlaybackQueue(
    val id: Long,
    val name: String,
    val originType: QueueOriginType = QueueOriginType.CUSTOM,
    val originKey: String = "",
    val currentTrackId: Long? = null,
    val currentPositionMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val trackCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false,
)

@Serializable
data class Bookmark(
    val id: Long = 0L,
    val trackId: Long,
    val label: String,
    val positionMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class MusicNote(
    val id: Long = 0L,
    val trackId: Long,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class LyricLine(
    val timestampMs: Long?,
    val text: String,
)

@Serializable
enum class CoverShape {
    ROUNDED,
    CIRCLE,
    SHARP,
    VINYL
}

@Serializable
enum class ButtonStyle {
    CIRCULAR,
    PILL,
    OUTLINED
}

@Serializable
enum class AnimationMode {
    FULL,
    REDUCED,
    BATTERY_SAVER
}

@Serializable
enum class PlayerAppearance {
    CINEMATIC,
    COMPACT,
    VINYL_DISC
}

@Serializable
data class MyatoTheme(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = true,
    val isLight: Boolean = false,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val backgroundColorHex: Long,
    val altBackgroundColorHex: Long,
    val cardColorHex: Long,
    val textColorHex: Long,
    val secondaryTextColorHex: Long,
    val coverShape: CoverShape = CoverShape.ROUNDED,
    val buttonStyle: ButtonStyle = ButtonStyle.CIRCULAR,
    val animationMode: AnimationMode = AnimationMode.FULL,
    val playerAppearance: PlayerAppearance = PlayerAppearance.CINEMATIC,
)

@Serializable
enum class AudioOutputDeviceType(val displayName: String) {
    SPEAKER("Alto-falante"),
    WIRED_HEADSET("Fones com Fio"),
    BLUETOOTH("Bluetooth"),
    OTHER("Outro Dispositivo")
}

@Serializable
data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val levelMillibels: Int,
    val minMillibels: Int,
    val maxMillibels: Int,
)

@Serializable
data class AudioEqualizerState(
    val isAvailable: Boolean = false,
    val availabilityReason: String = "",
    val isEnabled: Boolean = false,
    val outputDevice: AudioOutputDeviceType = AudioOutputDeviceType.SPEAKER,
    val presetName: String = "Personalizado",
    val bands: List<EqualizerBand> = emptyList(),
    val bassBoostStrength: Int = 0, // 0..1000
    val virtualizerStrength: Int = 0, // 0..1000
    val loudnessGainMb: Int = 0, // 0..1500
    val trebleBoostDb: Int = 0, // -6..+6 visual/high-band helper
)

@Serializable
enum class SleepTimerMode {
    NONE,
    DURATION_OR_TIME,
    TRACK_COUNT
}

@Serializable
data class SleepTimerState(
    val mode: SleepTimerMode = SleepTimerMode.NONE,
    val remainingMs: Long = 0L,
    val totalMs: Long = 0L,
    val remainingTracks: Int = 0,
    val targetTracks: Int = 0,
    val isPaused: Boolean = false,
    val targetClockLabel: String = "",
)

@Serializable
data class ListeningStatsSummary(
    val totalListenMs: Long = 0L,
    val totalPlays: Int = 0,
    val topTracks: List<TrackStatItem> = emptyList(),
    val topArtists: List<ArtistStatItem> = emptyList(),
    val topAlbums: List<AlbumStatItem> = emptyList(),
    val recentHistory: List<HistoryStatItem> = emptyList(),
)

@Serializable
data class TrackStatItem(
    val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val playCount: Int,
    val totalListenMs: Long,
    val artUri: String?,
)

@Serializable
data class ArtistStatItem(
    val artist: String,
    val playCount: Int,
    val totalListenMs: Long,
)

@Serializable
data class AlbumStatItem(
    val album: String,
    val artist: String,
    val playCount: Int,
    val totalListenMs: Long,
    val artUri: String?,
)

@Serializable
data class HistoryStatItem(
    val id: Long,
    val trackId: Long,
    val title: String,
    val artist: String,
    val playedAt: Long,
    val durationListenedMs: Long,
)

@Serializable
data class AudioFormatCapability(
    val extension: String,
    val mimeTypes: List<String>,
    val isSupportedOnDevice: Boolean,
    val decoderName: String,
    val supportsTagEditing: Boolean,
    val notes: String,
)

@Serializable
data class BackupPreview(
    val version: Int,
    val createdAt: Long,
    val deviceName: String,
    val playlistsCount: Int,
    val queuesCount: Int,
    val favoritesCount: Int,
    val bookmarksCount: Int,
    val notesCount: Int,
    val historyCount: Int,
    val statsCount: Int,
    val customThemesCount: Int,
)

enum class TrackSortOrder(val label: String) {
    TITLE_ASC("Nome crescente (A-Z)"),
    TITLE_DESC("Nome decrescente (Z-A)"),
    ARTIST_ASC("Artista"),
    ALBUM_ASC("Álbum"),
    DURATION_DESC("Duração"),
    DATE_ADDED_DESC("Mais recentes"),
    DATE_ADDED_ASC("Mais antigas"),
    MOST_PLAYED("Mais reproduzidas"),
    LEAST_PLAYED("Menos reproduzidas"),
    RECENTLY_ADDED("Adicionadas recentemente"),
}

enum class SmartSelectionCriteria(val label: String) {
    BY_ARTIST("Todas do mesmo artista"),
    BY_ALBUM("Todas do mesmo álbum"),
    BY_FOLDER("Todas da mesma pasta"),
    BY_FORMAT("Mesmo formato de áudio"),
    FAVORITES("Músicas favoritas"),
    NEVER_PLAYED("Músicas nunca reproduzidas"),
}

enum class HeadsetButtonMapping(val label: String) {
    PLAY_PAUSE("Reproduzir / Pausar"),
    NEXT_TRACK("Próxima música"),
    PREV_TRACK("Música anterior"),
    SEEK_FORWARD("Avançar 10s"),
    SEEK_BACKWARD("Retroceder 10s"),
    TOGGLE_FAVORITE("Favoritar música atual")
}
