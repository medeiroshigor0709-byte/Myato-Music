package com.example.core.database

import com.example.domain.models.AlbumItem
import com.example.domain.models.AnimationMode
import com.example.domain.models.ArtistItem
import com.example.domain.models.Bookmark
import com.example.domain.models.ButtonStyle
import com.example.domain.models.CoverShape
import com.example.domain.models.MusicNote
import com.example.domain.models.MusicTrack
import com.example.domain.models.MyatoTheme
import com.example.domain.models.PlaybackQueue
import com.example.domain.models.PlayerAppearance
import com.example.domain.models.Playlist
import com.example.domain.models.QueueOriginType
import com.example.domain.models.RepeatMode

fun MusicTrackEntity.toDomain(
    isFavorite: Boolean = false,
    playCount: Int = 0,
    lastPlayedAt: Long = 0L,
): MusicTrack = MusicTrack(
    id = id,
    mediaStoreId = mediaStoreId,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    artistId = artistId,
    albumArtist = albumArtist,
    genre = genre,
    year = year,
    trackNumber = trackNumber,
    discNumber = discNumber,
    composer = composer,
    comments = comments,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    bitrateKbps = bitrateKbps,
    sampleRateHz = sampleRateHz,
    mimeType = mimeType,
    formatExtension = formatExtension,
    filePath = filePath,
    folderPath = folderPath,
    contentUri = contentUri,
    albumArtUri = albumArtUri,
    dateAddedSec = dateAddedSec,
    isPlayable = isPlayable,
    isFavorite = isFavorite,
    playCount = playCount,
    lastPlayedAt = lastPlayedAt,
    embeddedLyrics = embeddedLyrics,
)

fun MusicTrack.toEntity(): MusicTrackEntity = MusicTrackEntity(
    id = id,
    mediaStoreId = mediaStoreId,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    artistId = artistId,
    albumArtist = albumArtist,
    genre = genre,
    year = year,
    trackNumber = trackNumber,
    discNumber = discNumber,
    composer = composer,
    comments = comments,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    bitrateKbps = bitrateKbps,
    sampleRateHz = sampleRateHz,
    mimeType = mimeType,
    formatExtension = formatExtension,
    filePath = filePath,
    folderPath = folderPath,
    contentUri = contentUri,
    albumArtUri = albumArtUri,
    dateAddedSec = dateAddedSec,
    isPlayable = isPlayable,
    embeddedLyrics = embeddedLyrics,
)

fun AlbumEntity.toDomain(): AlbumItem = AlbumItem(
    id = id,
    title = title,
    artist = artistName,
    trackCount = trackCount,
    totalDurationMs = totalDurationMs,
    artUri = artUri,
    year = year,
)

fun ArtistEntity.toDomain(): ArtistItem = ArtistItem(
    id = id,
    name = name,
    trackCount = trackCount,
    albumCount = albumCount,
)

fun PlaylistEntity.toDomain(
    trackCount: Int = 0,
    totalDurationMs: Long = 0L,
): Playlist = Playlist(
    id = id,
    name = name,
    description = description,
    trackCount = trackCount,
    totalDurationMs = totalDurationMs,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PlaybackQueueEntity.toDomain(trackCount: Int = 0): PlaybackQueue = PlaybackQueue(
    id = id,
    name = name,
    originType = runCatching { QueueOriginType.valueOf(originType) }.getOrDefault(QueueOriginType.CUSTOM),
    originKey = originKey,
    currentTrackId = currentTrackId,
    currentPositionMs = currentPositionMs,
    isShuffleEnabled = isShuffleEnabled,
    repeatMode = runCatching { RepeatMode.valueOf(repeatMode) }.getOrDefault(RepeatMode.OFF),
    trackCount = trackCount,
    createdAt = createdAt,
    lastActiveAt = lastActiveAt,
    isActive = isActive,
)

fun BookmarkEntity.toDomain(): Bookmark = Bookmark(
    id = id,
    trackId = trackId,
    label = label,
    positionMs = positionMs,
    createdAt = createdAt,
)

fun MusicNoteEntity.toDomain(): MusicNote = MusicNote(
    id = id,
    trackId = trackId,
    content = content,
    updatedAt = updatedAt,
)

fun ThemeEntity.toDomain(): MyatoTheme = MyatoTheme(
    id = id,
    name = name,
    isBuiltIn = isBuiltIn,
    isLight = isLight,
    primaryColorHex = primaryColorHex,
    secondaryColorHex = secondaryColorHex,
    backgroundColorHex = backgroundColorHex,
    altBackgroundColorHex = altBackgroundColorHex,
    cardColorHex = cardColorHex,
    textColorHex = textColorHex,
    secondaryTextColorHex = secondaryTextColorHex,
    coverShape = runCatching { CoverShape.valueOf(coverShape) }.getOrDefault(CoverShape.ROUNDED),
    buttonStyle = runCatching { ButtonStyle.valueOf(buttonStyle) }.getOrDefault(ButtonStyle.CIRCULAR),
    animationMode = runCatching { AnimationMode.valueOf(animationMode) }.getOrDefault(AnimationMode.FULL),
    playerAppearance = runCatching { PlayerAppearance.valueOf(playerAppearance) }.getOrDefault(PlayerAppearance.CINEMATIC),
)

fun MyatoTheme.toEntity(): ThemeEntity = ThemeEntity(
    id = id,
    name = name,
    isBuiltIn = isBuiltIn,
    isLight = isLight,
    primaryColorHex = primaryColorHex,
    secondaryColorHex = secondaryColorHex,
    backgroundColorHex = backgroundColorHex,
    altBackgroundColorHex = altBackgroundColorHex,
    cardColorHex = cardColorHex,
    textColorHex = textColorHex,
    secondaryTextColorHex = secondaryTextColorHex,
    coverShape = coverShape.name,
    buttonStyle = buttonStyle.name,
    animationMode = animationMode.name,
    playerAppearance = playerAppearance.name,
)
