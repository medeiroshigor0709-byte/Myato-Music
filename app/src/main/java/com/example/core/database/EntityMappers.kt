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
import com.example.domain.models.ThemeAnimationType
import com.example.domain.models.ThemeBorderStyle
import com.example.domain.models.ThemeCategory
import com.example.domain.models.ThemeEffectPreset
import com.example.domain.models.ThemeExtendedConfig
import com.example.domain.models.ThemeGradientStyle
import com.example.domain.models.ThemeIconStyle
import com.example.domain.models.WallpaperStyle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val entityJson = Json { ignoreUnknownKeys = true }

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
    fallbackArtUri: String? = null,
): Playlist = Playlist(
    id = id,
    name = name,
    description = description,
    coverUri = coverUri ?: fallbackArtUri,
    isFavorite = isFavorite,
    lastUsedAt = lastUsedAt,
    trackCount = trackCount,
    totalDurationMs = totalDurationMs,
    createdAt = createdAt,
    updatedAt = updatedAt,
    artistFilterName = artistFilterName,
    autoUpdateByArtist = autoUpdateByArtist,
    includeCollaborations = includeCollaborations,
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

fun ThemeEntity.toDomain(): MyatoTheme {
    val ext = runCatching {
        if (extendedConfigJson.isBlank() || extendedConfigJson == "{}") null
        else entityJson.decodeFromString<ThemeExtendedConfig>(extendedConfigJson)
    }.getOrNull()

    return MyatoTheme(
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
        category = if (ext != null) ThemeCategory.fromName(ext.category) else if (isBuiltIn) ThemeCategory.MYATO_EXCLUSIVES else ThemeCategory.CUSTOM,
        description = ext?.description?.ifBlank { null } ?: if (isBuiltIn) "Tema oficial Myato Themes." else "Tema personalizado criado pelo usuário.",
        accentColorHex = ext?.accentColorHex ?: secondaryColorHex,
        buttonColorHex = ext?.buttonColorHex ?: primaryColorHex,
        iconColorHex = ext?.iconColorHex ?: primaryColorHex,
        borderColorHex = ext?.borderColorHex ?: secondaryColorHex,
        gradientEndColorHex = ext?.gradientEndColorHex ?: altBackgroundColorHex,
        surfaceAlpha = ext?.surfaceAlpha ?: 0.94f,
        backgroundAlpha = ext?.backgroundAlpha ?: 1.0f,
        shadowElevationDp = ext?.shadowElevationDp ?: 4f,
        borderWidthDp = ext?.borderWidthDp ?: 1f,
        gradientStyle = ThemeGradientStyle.fromName(ext?.gradientStyle),
        borderStyle = ThemeBorderStyle.fromName(ext?.borderStyle),
        iconStyle = ThemeIconStyle.fromName(ext?.iconStyle),
        effectPreset = ThemeEffectPreset.fromName(ext?.effectPreset),
        wallpaperStyle = WallpaperStyle.fromName(ext?.wallpaperStyle),
        wallpaperPatternSeed = ext?.wallpaperPatternSeed ?: 1,
        wallpaperImagePath = ext?.wallpaperImagePath,
        wallpaperDimAlpha = ext?.wallpaperDimAlpha ?: 0.42f,
        animationType = ThemeAnimationType.fromName(ext?.animationType),
        isFeatured = ext?.isFeatured ?: false,
        seasonalTag = ext?.seasonalTag,
    )
}

fun MyatoTheme.toEntity(): ThemeEntity {
    val ext = ThemeExtendedConfig(
        category = category.name,
        description = description,
        accentColorHex = accentColorHex,
        buttonColorHex = buttonColorHex,
        iconColorHex = iconColorHex,
        borderColorHex = borderColorHex,
        gradientEndColorHex = gradientEndColorHex,
        surfaceAlpha = surfaceAlpha,
        backgroundAlpha = backgroundAlpha,
        shadowElevationDp = shadowElevationDp,
        borderWidthDp = borderWidthDp,
        gradientStyle = gradientStyle.name,
        borderStyle = borderStyle.name,
        iconStyle = iconStyle.name,
        effectPreset = effectPreset.name,
        wallpaperStyle = wallpaperStyle.name,
        wallpaperPatternSeed = wallpaperPatternSeed,
        wallpaperImagePath = wallpaperImagePath,
        wallpaperDimAlpha = wallpaperDimAlpha,
        animationType = animationType.name,
        isFeatured = isFeatured,
        seasonalTag = seasonalTag,
    )
    return ThemeEntity(
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
        extendedConfigJson = entityJson.encodeToString(ext),
    )
}
