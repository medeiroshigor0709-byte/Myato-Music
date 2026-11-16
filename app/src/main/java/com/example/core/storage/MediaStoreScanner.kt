package com.example.core.storage

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.core.common.AudioFormatSupport
import com.example.core.database.AlbumEntity
import com.example.core.database.ArtistEntity
import com.example.core.database.MusicTrackEntity
import com.example.core.database.MyatoDatabase
import com.example.core.metadata.MusicSearchEngine
import com.example.domain.models.AudioFormatCapability
import com.example.domain.models.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class DuplicateGroup(
    val key: String,
    val tracks: List<MusicTrack>,
)

data class LibraryScanResult(
    val tracksCount: Int,
    val albumsCount: Int,
    val artistsCount: Int,
    val unsupportedCount: Int,
)

class MediaStoreScanner(
    private val context: Context,
    private val database: MyatoDatabase,
) {

    suspend fun scanDeviceLibrary(monitoredFolders: Set<String> = emptySet()): LibraryScanResult =
        withContext(Dispatchers.IO) {
            val capabilities = AudioFormatSupport.detectDeviceCapabilities()
            val tracks = mutableListOf<MusicTrackEntity>()
            val existingById = database.trackDao().getAllTracksSync().associateBy { it.id }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = buildList {
                add(MediaStore.Audio.Media._ID)
                add(MediaStore.Audio.Media.TITLE)
                add(MediaStore.Audio.Media.ARTIST)
                add(MediaStore.Audio.Media.ARTIST_ID)
                add(MediaStore.Audio.Media.ALBUM)
                add(MediaStore.Audio.Media.ALBUM_ID)
                add(MediaStore.Audio.Media.DURATION)
                add(MediaStore.Audio.Media.SIZE)
                add(MediaStore.Audio.Media.DATA)
                add(MediaStore.Audio.Media.MIME_TYPE)
                add(MediaStore.Audio.Media.DATE_ADDED)
                add(MediaStore.Audio.Media.TRACK)
                add(MediaStore.Audio.Media.YEAR)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    add(MediaStore.Audio.Media.GENRE)
                    add(MediaStore.Audio.Media.ALBUM_ARTIST)
                    add(MediaStore.Audio.Media.COMPOSER)
                    add(MediaStore.Audio.Media.DISC_NUMBER)
                }
            }.toTypedArray()

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 1000"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            runCatching {
                context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                    val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                    val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                    val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                    val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                    } else -1
                    val albumArtistCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                    } else -1
                    val composerCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        cursor.getColumnIndex(MediaStore.Audio.Media.COMPOSER)
                    } else -1
                    val discCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        cursor.getColumnIndex(MediaStore.Audio.Media.DISC_NUMBER)
                    } else -1

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val filePath = cursor.getString(dataCol).orEmpty()
                        val folderPath = File(filePath).parent ?: ""

                        if (monitoredFolders.isNotEmpty() && monitoredFolders.none { folderPath.startsWith(it) }) {
                            continue
                        }

                        val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                            ?: File(filePath).nameWithoutExtension
                            ?: "Faixa desconhecida"
                        val artist = MusicSearchEngine.sanitizeArtistName(cursor.getString(artistCol))
                        val artistId = cursor.getLong(artistIdCol)
                        val album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() }
                            ?: "Álbum desconhecido"
                        val albumId = cursor.getLong(albumIdCol)
                        val durationMs = cursor.getLong(durCol).coerceAtLeast(0L)
                        val sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L)
                        val mimeType = cursor.getString(mimeCol) ?: "audio/mpeg"
                        val ext = AudioFormatSupport.inferExtension(filePath, mimeType)
                        val playable = isFormatPlayable(ext, capabilities)
                        val contentUri = ContentUris.withAppendedId(collection, id).toString()
                        val artUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()

                        val prev = existingById[id]
                        val trackNumRaw = cursor.getInt(trackCol)
                        val trackNum = if (trackNumRaw >= 1000) trackNumRaw % 1000 else trackNumRaw

                        tracks.add(
                            MusicTrackEntity(
                                id = id,
                                mediaStoreId = id,
                                title = title,
                                artist = artist,
                                album = album,
                                albumId = albumId,
                                artistId = artistId,
                                albumArtist = if (albumArtistCol >= 0) cursor.getString(albumArtistCol).orEmpty() else prev?.albumArtist.orEmpty(),
                                genre = if (genreCol >= 0) cursor.getString(genreCol).orEmpty() else prev?.genre.orEmpty(),
                                year = cursor.getInt(yearCol),
                                trackNumber = trackNum,
                                discNumber = if (discCol >= 0) cursor.getInt(discCol) else prev?.discNumber ?: 0,
                                composer = if (composerCol >= 0) cursor.getString(composerCol).orEmpty() else prev?.composer.orEmpty(),
                                comments = prev?.comments.orEmpty(),
                                durationMs = durationMs,
                                sizeBytes = sizeBytes,
                                bitrateKbps = prev?.bitrateKbps ?: 0,
                                sampleRateHz = prev?.sampleRateHz ?: 0,
                                mimeType = mimeType,
                                formatExtension = ext,
                                filePath = filePath,
                                folderPath = folderPath,
                                contentUri = contentUri,
                                albumArtUri = artUri,
                                dateAddedSec = cursor.getLong(addedCol),
                                isPlayable = playable,
                                embeddedLyrics = prev?.embeddedLyrics.orEmpty(),
                            )
                        )
                    }
                }
            }

            // Preserve tracks imported manually via SAF DocumentTree
            val safTracks = existingById.values.filter { it.mediaStoreId < 0L }
            val mergedTracks = (tracks + safTracks).distinctBy { it.id }

            val albums = mergedTracks
                .groupBy { it.albumId }
                .map { (albumId, albumTracks) ->
                    val first = albumTracks.first()
                    AlbumEntity(
                        id = albumId,
                        title = first.album,
                        artistName = first.artist,
                        trackCount = albumTracks.size,
                        totalDurationMs = albumTracks.sumOf { it.durationMs },
                        artUri = first.albumArtUri,
                        year = albumTracks.maxOfOrNull { it.year } ?: 0,
                    )
                }

            val artists = mergedTracks
                .groupBy { it.artist }
                .map { (name, artistTracks) ->
                    ArtistEntity(
                        id = artistTracks.first().artistId.takeIf { it != 0L } ?: name.hashCode().toLong(),
                        name = name,
                        trackCount = artistTracks.size,
                        albumCount = artistTracks.map { it.albumId }.distinct().size,
                    )
                }

            database.trackDao().replaceLibrarySnapshot(mergedTracks, albums, artists)

            LibraryScanResult(
                tracksCount = mergedTracks.size,
                albumsCount = albums.size,
                artistsCount = artists.size,
                unsupportedCount = mergedTracks.count { !it.isPlayable },
            )
        }

    suspend fun importSafFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val docTree = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext 0
        val capabilities = AudioFormatSupport.detectDeviceCapabilities()
        val imported = mutableListOf<MusicTrackEntity>()
        collectAudioDocuments(docTree, capabilities, imported)
        if (imported.isNotEmpty()) {
            database.trackDao().upsertTracks(imported)
            rebuildAlbumsAndArtists()
        }
        imported.size
    }

    private fun collectAudioDocuments(
        dir: DocumentFile,
        capabilities: List<AudioFormatCapability>,
        out: MutableList<MusicTrackEntity>,
    ) {
        for (file in dir.listFiles()) {
            if (file.isDirectory) {
                collectAudioDocuments(file, capabilities, out)
            } else if (file.isFile) {
                val name = file.name.orEmpty()
                val ext = name.substringAfterLast('.', "").uppercase()
                val mime = file.type.orEmpty()
                val isAudio = mime.startsWith("audio/") ||
                    ext in setOf("MP3", "M4A", "WMA", "FLAC", "OPUS", "AAC", "ALAC", "APE", "DSF", "WAV", "OGG")
                if (!isAudio) continue

                val retriever = MediaMetadataRetriever()
                var title = name.substringBeforeLast('.')
                var artist = MusicSearchEngine.UNKNOWN_ARTIST_LABEL
                var albumArtist = ""
                var album = dir.name ?: "Pasta SAF"
                var duration = 0L
                var bitrate = 0
                var year = 0
                var genre = ""
                try {
                    retriever.setDataSource(context, file.uri)
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: title
                    artist = MusicSearchEngine.sanitizeArtistName(
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    )
                    albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).orEmpty().trim()
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() } ?: album
                    duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    bitrate = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0) / 1000
                    year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull() ?: 0
                    genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE).orEmpty()
                } catch (_: Exception) {
                } finally {
                    runCatching { retriever.release() }
                }

                val stableNegativeId = -kotlin.math.abs(file.uri.toString().hashCode().toLong()).coerceAtLeast(1L)
                out.add(
                    MusicTrackEntity(
                        id = stableNegativeId,
                        mediaStoreId = stableNegativeId,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = album.hashCode().toLong(),
                        artistId = artist.hashCode().toLong(),
                        albumArtist = albumArtist,
                        genre = genre,
                        year = year,
                        durationMs = duration,
                        sizeBytes = file.length(),
                        bitrateKbps = bitrate,
                        mimeType = mime.ifBlank { "audio/*" },
                        formatExtension = ext.ifBlank { "AUDIO" },
                        filePath = file.uri.toString(),
                        folderPath = dir.uri.toString(),
                        contentUri = file.uri.toString(),
                        albumArtUri = null,
                        dateAddedSec = System.currentTimeMillis() / 1000L,
                        isPlayable = isFormatPlayable(ext, capabilities),
                    )
                )
            }
        }
    }

    private suspend fun rebuildAlbumsAndArtists() {
        val tracks = database.trackDao().getAllTracksSync()
        val albums = tracks.groupBy { it.albumId }.map { (id, list) ->
            val f = list.first()
            AlbumEntity(id, f.album, f.artist, list.size, list.sumOf { it.durationMs }, f.albumArtUri, list.maxOfOrNull { it.year } ?: 0)
        }
        val artists = tracks.groupBy { it.artist }.map { (name, list) ->
            ArtistEntity(list.first().artistId, name, list.size, list.map { it.albumId }.distinct().size)
        }
        database.trackDao().upsertAlbums(albums)
        database.trackDao().upsertArtists(artists)
    }

    fun findDuplicateTracks(tracks: List<MusicTrack>): List<DuplicateGroup> {
        return tracks
            .groupBy { track ->
                val normTitle = track.title.trim().lowercase()
                val normArtist = track.artist.trim().lowercase()
                val bucketDuration = (track.durationMs / 2000L)
                "$normTitle|$normArtist|$bucketDuration"
            }
            .filter { it.value.size > 1 }
            .map { (key, list) -> DuplicateGroup(key = key, tracks = list) }
    }

    private fun isFormatPlayable(extension: String, capabilities: List<AudioFormatCapability>): Boolean {
        val match = capabilities.firstOrNull { it.extension.equals(extension, ignoreCase = true) }
        return match?.isSupportedOnDevice ?: false
    }
}
