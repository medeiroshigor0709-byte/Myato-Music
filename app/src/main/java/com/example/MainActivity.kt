package com.example

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.core.audio.MyatoPlaybackService
import com.example.presentation.MyatoMainViewModel
import com.example.presentation.MyatoRootApp

class MainActivity : ComponentActivity() {

    private val viewModel: MyatoMainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        volumeControlStream = AudioManager.STREAM_MUSIC
        handleIntentExtras(intent)
        setContent {
            MyatoRootApp(
                viewModel = viewModel,
                openPlayerOnStart = intent?.getBooleanExtra(MyatoPlaybackService.EXTRA_OPEN_PLAYER, false) == true,
                openPlaylistPickerOnStart = intent?.getBooleanExtra("extra_open_playlist_picker", false) == true,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.playbackController.syncVolumeFromDevice(force = true)
        viewModel.refreshVisualizerState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentExtras(intent)
    }

    private fun handleIntentExtras(intent: Intent?) {
        // Ensures service connection is warmed when opened from notification/widget/shortcut
        viewModel.playbackController.connectToService()
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data!!
            val uriStr = uri.toString()
            val all = viewModel.tracks.value
            val matched = all.firstOrNull { it.contentUri == uriStr || it.filePath == uri.path }
            if (matched != null) {
                viewModel.playTracks(
                    listOf(matched),
                    0,
                    matched.title,
                    com.example.domain.models.QueueOriginType.ALL_SONGS,
                    matched.contentUri,
                )
            } else {
                val displayTitle = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Áudio Externo"
                val externalTrack = com.example.domain.models.MusicTrack(
                    id = uriStr.hashCode().toLong(),
                    mediaStoreId = 0L,
                    title = displayTitle,
                    artist = "Arquivo Local",
                    album = "Aberto externamente",
                    albumId = 0L,
                    artistId = 0L,
                    durationMs = 0L,
                    sizeBytes = 0L,
                    filePath = uri.path ?: uriStr,
                    folderPath = "",
                    contentUri = uriStr,
                )
                viewModel.playTracks(
                    listOf(externalTrack),
                    0,
                    displayTitle,
                    com.example.domain.models.QueueOriginType.ALL_SONGS,
                    uriStr,
                )
            }
            return
        }
        val shortcutType = intent?.getStringExtra("extra_shortcut_type") ?: return
        val shortcutKey = intent.getStringExtra("extra_shortcut_key") ?: return
        val all = viewModel.tracks.value
        when (shortcutType) {
            "artist" -> {
                val list = all.filter { it.artist == shortcutKey }
                if (list.isNotEmpty()) viewModel.playTracks(list, 0, shortcutKey, com.example.domain.models.QueueOriginType.ARTIST, shortcutKey)
            }
            "album" -> {
                val aid = shortcutKey.toLongOrNull() ?: return
                val list = all.filter { it.albumId == aid }
                if (list.isNotEmpty()) viewModel.playTracks(list, 0, list.first().album, com.example.domain.models.QueueOriginType.ALBUM, shortcutKey)
            }
            "folder" -> {
                val list = all.filter { it.folderPath == shortcutKey }
                if (list.isNotEmpty()) viewModel.playTracks(list, 0, shortcutKey.substringAfterLast('/'), com.example.domain.models.QueueOriginType.FOLDER, shortcutKey)
            }
        }
    }
}
