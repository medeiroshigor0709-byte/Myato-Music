package com.example.core.storage

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.domain.models.MusicTrack
import java.io.File

object TrackShareAndDeleteHelper {

    fun shareTracks(context: Context, tracks: List<MusicTrack>): Result<Unit> = runCatching {
        if (tracks.isEmpty()) error("Nenhuma música selecionada para compartilhar.")
        val uris = tracks.mapNotNull { resolveShareableUri(context, it) }
        if (uris.isEmpty()) error("Não foi possível acessar os arquivos para compartilhamento.")

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = tracks.first().mimeType.ifBlank { "audio/*" }
                putExtra(Intent.EXTRA_STREAM, uris.first())
                putExtra(Intent.EXTRA_SUBJECT, "${tracks.first().title} - ${tracks.first().artist}")
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                putExtra(Intent.EXTRA_SUBJECT, "${tracks.size} músicas - MYATO MUSIC")
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(intent, "Compartilhar música(s)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun sharePlaylist(context: Context, playlistName: String, tracks: List<MusicTrack>): Result<Unit> = runCatching {
        if (tracks.isEmpty()) error("A playlist está vazia.")
        val dir = File(context.cacheDir, "shared_playlists").apply { mkdirs() }
        val safeName = playlistName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "playlist" }
        val m3uFile = File(dir, "$safeName.m3u")
        val sb = StringBuilder().apply {
            appendLine("#EXTM3U")
            appendLine("#PLAYLIST:$playlistName")
            tracks.forEach { t ->
                val sec = (t.durationMs / 1000L).coerceAtLeast(0L)
                appendLine("#EXTINF:$sec,${t.artist} - ${t.title}")
                appendLine(t.filePath.ifBlank { t.contentUri })
            }
        }
        m3uFile.writeText(sb.toString(), Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", m3uFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/x-mpegurl"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Playlist: $playlistName")
            putExtra(
                Intent.EXTRA_TEXT,
                "Playlist \"$playlistName\" (${tracks.size} músicas):\n" +
                    tracks.take(25).joinToString("\n") { "• ${it.title} — ${it.artist}" }
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Compartilhar playlist \"$playlistName\"").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun resolveShareableUri(context: Context, track: MusicTrack): Uri? {
        val file = File(track.filePath)
        if (file.exists() && file.canRead()) {
            val fpUri = runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }.getOrNull()
            if (fpUri != null) return fpUri
        }
        return track.contentUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
    }

    fun createMediaStoreDeleteRequestIfNeeded(
        context: Context,
        tracks: List<MusicTrack>,
    ): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val mediaUris = tracks
            .mapNotNull { t ->
                t.contentUri.takeIf { it.startsWith("content://media/") }?.let { Uri.parse(it) }
            }
        if (mediaUris.isEmpty()) return null
        return runCatching {
            MediaStore.createDeleteRequest(context.contentResolver, mediaUris).intentSender
        }.getOrNull()
    }
}
