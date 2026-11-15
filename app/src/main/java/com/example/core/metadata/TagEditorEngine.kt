package com.example.core.metadata

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.example.core.database.MyatoDatabase
import com.example.domain.models.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

data class TagEditPayload(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val year: Int,
    val trackNumber: Int,
    val discNumber: Int,
    val composer: String,
    val comments: String,
    val embeddedLyrics: String,
    val newCoverBytes: ByteArray? = null,
)

sealed interface TagSaveResult {
    data class Success(val message: String, val wroteToAudioFile: Boolean) : TagSaveResult
    data class Error(val message: String) : TagSaveResult
}

/**
 * Real offline metadata & ID3v2.3 tag reader/writer.
 * Writes real ID3v2.3 frames (TIT2, TPE1, TALB, TPE2, TCON, TYER, TRCK, TPOS, TCOM, COMM, USLT, APIC)
 * to writable MP3 files and updates Room/MediaStore; reports clearly when a format does not support direct editing.
 */
class TagEditorEngine(
    private val context: Context,
    private val database: MyatoDatabase,
) {

    fun isDirectFileEditingSupported(formatExtension: String): Boolean {
        return formatExtension.equals("MP3", ignoreCase = true)
    }

    suspend fun inspectTrackMetadata(track: MusicTrack): TagEditPayload = withContext(Dispatchers.IO) {
        var title = track.title
        var artist = track.artist
        var album = track.album
        var albumArtist = track.albumArtist
        var genre = track.genre
        var year = track.year
        var trackNum = track.trackNumber
        var discNum = track.discNumber
        var composer = track.composer
        var lyrics = track.embeddedLyrics

        val retriever = MediaMetadataRetriever()
        try {
            if (track.filePath.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(track.contentUri))
            } else if (File(track.filePath).exists()) {
                retriever.setDataSource(track.filePath)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let { artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }?.let { album = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.takeIf { it.isNotBlank() }?.let { albumArtist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.takeIf { it.isNotBlank() }?.let { genre = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()?.let { year = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.substringBefore('/')?.toIntOrNull()?.let { trackNum = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.substringBefore('/')?.toIntOrNull()?.let { discNum = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)?.takeIf { it.isNotBlank() }?.let { composer = it }
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }

        val loadedLyrics = LrcLyricsEngine.loadLyricsForTrack(context, track.id, track.filePath, lyrics)
        TagEditPayload(
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            genre = genre,
            year = year,
            trackNumber = trackNum,
            discNumber = discNum,
            composer = composer,
            comments = track.comments,
            embeddedLyrics = loadedLyrics,
        )
    }

    suspend fun saveTagsForTrack(track: MusicTrack, payload: TagEditPayload): TagSaveResult =
        withContext(Dispatchers.IO) {
            var wroteAudioFile = false
            var formatWarning = ""

            if (isDirectFileEditingSupported(track.formatExtension)) {
                val file = File(track.filePath)
                if (file.exists() && file.canWrite()) {
                    wroteAudioFile = runCatching {
                        writeId3v23TagToMp3(file, payload)
                        true
                    }.getOrDefault(false)
                }
                if (!wroteAudioFile) {
                    formatWarning = "Permissão de escrita direta negada pelo sistema neste diretório; metadados salvos na biblioteca MYATO."
                }
            } else {
                formatWarning = "O formato ${track.formatExtension} não suporta reescrita direta de ID3 neste dispositivo; alterações salvas na biblioteca do aplicativo."
            }

            // Persist cover art locally if user selected a new image
            var newArtUri = track.albumArtUri
            if (payload.newCoverBytes != null && payload.newCoverBytes.isNotEmpty()) {
                val coverFile = File(File(context.filesDir, "covers").apply { mkdirs() }, "cover_${track.id}.jpg")
                runCatching {
                    coverFile.writeBytes(payload.newCoverBytes)
                    newArtUri = Uri.fromFile(coverFile).toString()
                }
            }

            // Update MediaStore metadata where allowed
            if (track.mediaStoreId > 0) {
                runCatching {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.TITLE, payload.title)
                        put(MediaStore.Audio.Media.ARTIST, payload.artist)
                        put(MediaStore.Audio.Media.ALBUM, payload.album)
                        if (payload.year > 0) put(MediaStore.Audio.Media.YEAR, payload.year)
                    }
                    context.contentResolver.update(
                        Uri.parse(track.contentUri),
                        values,
                        null,
                        null,
                    )
                }
            }

            // Update lyrics sidecar/local store if non-blank
            if (payload.embeddedLyrics.isNotBlank()) {
                LrcLyricsEngine.saveLyricsForTrack(context, track.id, track.filePath, payload.embeddedLyrics)
            }

            // Always update Room entity
            val existing = database.trackDao().getTrackById(track.id)
            if (existing != null) {
                database.trackDao().upsertTrack(
                    existing.copy(
                        title = payload.title,
                        artist = payload.artist,
                        album = payload.album,
                        albumArtist = payload.albumArtist,
                        genre = payload.genre,
                        year = payload.year,
                        trackNumber = payload.trackNumber,
                        discNumber = payload.discNumber,
                        composer = payload.composer,
                        comments = payload.comments,
                        embeddedLyrics = payload.embeddedLyrics,
                        albumArtUri = newArtUri,
                    )
                )
            }

            val message = if (wroteAudioFile) {
                "Metadados gravados diretamente no arquivo MP3 e na biblioteca."
            } else {
                formatWarning
            }
            TagSaveResult.Success(message = message, wroteToAudioFile = wroteAudioFile)
        }

    /**
     * Pure-Kotlin ID3v2.3 writer for MP3 files. Preserves the audio bitstream intact and prepends/replaces ID3v2.3 header.
     */
    fun writeId3v23TagToMp3(file: File, payload: TagEditPayload) {
        val framesOut = ByteArrayOutputStream()
        fun addTextFrame(id: String, value: String) {
            if (value.isBlank()) return
            val utf16Bytes = value.toByteArray(StandardCharsets.UTF_16)
            val body = ByteArray(1 + utf16Bytes.size)
            body[0] = 0x01 // UTF-16 with BOM
            System.arraycopy(utf16Bytes, 0, body, 1, utf16Bytes.size)
            writeFrame(framesOut, id, body)
        }

        addTextFrame("TIT2", payload.title)
        addTextFrame("TPE1", payload.artist)
        addTextFrame("TALB", payload.album)
        addTextFrame("TPE2", payload.albumArtist)
        addTextFrame("TCON", payload.genre)
        if (payload.year > 0) addTextFrame("TYER", payload.year.toString())
        if (payload.trackNumber > 0) addTextFrame("TRCK", payload.trackNumber.toString())
        if (payload.discNumber > 0) addTextFrame("TPOS", payload.discNumber.toString())
        addTextFrame("TCOM", payload.composer)

        if (payload.comments.isNotBlank()) {
            val textBytes = payload.comments.toByteArray(StandardCharsets.UTF_8)
            val commBody = ByteArray(1 + 3 + 1 + textBytes.size)
            commBody[0] = 0x03 // UTF-8 (compatible fallback)
            commBody[1] = 'p'.code.toByte()
            commBody[2] = 'o'.code.toByte()
            commBody[3] = 'r'.code.toByte()
            commBody[4] = 0x00 // empty short description
            System.arraycopy(textBytes, 0, commBody, 5, textBytes.size)
            writeFrame(framesOut, "COMM", commBody)
        }

        if (payload.embeddedLyrics.isNotBlank()) {
            val lyrBytes = payload.embeddedLyrics.toByteArray(StandardCharsets.UTF_8)
            val usltBody = ByteArray(1 + 3 + 1 + lyrBytes.size)
            usltBody[0] = 0x03
            usltBody[1] = 'p'.code.toByte()
            usltBody[2] = 'o'.code.toByte()
            usltBody[3] = 'r'.code.toByte()
            usltBody[4] = 0x00
            System.arraycopy(lyrBytes, 0, usltBody, 5, lyrBytes.size)
            writeFrame(framesOut, "USLT", usltBody)
        }

        if (payload.newCoverBytes != null && payload.newCoverBytes.isNotEmpty()) {
            val apicOut = ByteArrayOutputStream()
            apicOut.write(0x00) // ISO-8859-1
            apicOut.write("image/jpeg".toByteArray(StandardCharsets.ISO_8859_1))
            apicOut.write(0x00)
            apicOut.write(0x03) // Cover (front)
            apicOut.write(0x00) // Empty description
            apicOut.write(payload.newCoverBytes)
            writeFrame(framesOut, "APIC", apicOut.toByteArray())
        }

        val frameBytes = framesOut.toByteArray()
        val existingOffset = findId3v2AudioOffset(file)
        val audioBytes = RandomAccessFile(file, "r").use { raf ->
            val len = (raf.length() - existingOffset).toInt().coerceAtLeast(0)
            ByteArray(len).also { buf ->
                raf.seek(existingOffset.toLong())
                raf.readFully(buf)
            }
        }

        val header = ByteArray(10)
        header[0] = 'I'.code.toByte()
        header[1] = 'D'.code.toByte()
        header[2] = '3'.code.toByte()
        header[3] = 3 // v2.3
        header[4] = 0
        header[5] = 0
        val size = frameBytes.size
        header[6] = ((size shr 21) and 0x7F).toByte()
        header[7] = ((size shr 14) and 0x7F).toByte()
        header[8] = ((size shr 7) and 0x7F).toByte()
        header[9] = (size and 0x7F).toByte()

        val tempFile = File(file.parentFile, "${file.name}.tmp")
        tempFile.outputStream().use { out ->
            out.write(header)
            out.write(frameBytes)
            out.write(audioBytes)
        }
        if (!tempFile.renameTo(file)) {
            tempFile.copyTo(file, overwrite = true)
            tempFile.delete()
        }
    }

    private fun writeFrame(out: ByteArrayOutputStream, frameId: String, body: ByteArray) {
        out.write(frameId.toByteArray(StandardCharsets.ISO_8859_1).take(4).toByteArray())
        val size = body.size
        out.write((size shr 24) and 0xFF)
        out.write((size shr 16) and 0xFF)
        out.write((size shr 8) and 0xFF)
        out.write(size and 0xFF)
        out.write(0)
        out.write(0)
        out.write(body)
    }

    private fun findId3v2AudioOffset(file: File): Int {
        if (file.length() < 10) return 0
        return RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(10)
            raf.readFully(header)
            if (header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
                val tagSize = ((header[6].toInt() and 0x7F) shl 21) or
                    ((header[7].toInt() and 0x7F) shl 14) or
                    ((header[8].toInt() and 0x7F) shl 7) or
                    (header[9].toInt() and 0x7F)
                10 + tagSize
            } else {
                0
            }
        }
    }
}
