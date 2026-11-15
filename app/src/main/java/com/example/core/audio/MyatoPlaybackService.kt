package com.example.core.audio

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.example.core.database.AppPreferencesStore
import com.example.core.database.MyatoDatabase
import com.example.domain.models.HeadsetButtonMapping
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Media3 MediaLibraryService providing:
 * - Real ExoPlayer gapless playback & background ForegroundService
 * - AudioFocus + AudioBecomingNoisy interruption recovery
 * - MediaSession for lockscreen controls & notification actions (including Add/Remove Playlist action)
 * - Headset multi-click hardware button detection (1x, 2x, 3x, 4x+)
 * - Android Auto browsable media tree (Songs, Albums, Artists, Folders, Playlists, Queues)
 */
@OptIn(UnstableApi::class)
class MyatoPlaybackService : MediaLibraryService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: MyatoDatabase
    private lateinit var preferencesStore: AppPreferencesStore
    private lateinit var player: ExoPlayer
    private var mediaLibrarySession: MediaLibrarySession? = null

    private var clickCount = 0
    private val clickHandler = Handler(Looper.getMainLooper())
    private val clickResetRunnable = Runnable {
        val count = clickCount
        clickCount = 0
        dispatchHeadsetClicks(count)
    }

    companion object {
        const val ROOT_ID = "myato_root"
        const val NODE_ALL_SONGS = "myato_songs"
        const val NODE_ALBUMS = "myato_albums"
        const val NODE_ARTISTS = "myato_artists"
        const val NODE_FOLDERS = "myato_folders"
        const val NODE_PLAYLISTS = "myato_playlists"
        const val NODE_QUEUES = "myato_queues"

        const val CMD_SEEK_BACK = "com.example.myato.CMD_SEEK_BACK"
        const val CMD_SEEK_FORWARD = "com.example.myato.CMD_SEEK_FORWARD"
        const val CMD_TOGGLE_FAVORITE = "com.example.myato.CMD_TOGGLE_FAVORITE"
        const val CMD_ADD_TO_PLAYLIST = "com.example.myato.CMD_ADD_TO_PLAYLIST"
        const val EXTRA_OPEN_PLAYER = "extra_open_player"
    }

    override fun onCreate() {
        super.onCreate()
        database = MyatoDatabase.getInstance(this)
        preferencesStore = AppPreferencesStore(this)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
            .apply {
                pauseAtEndOfMediaItems = false // True gapless transitions
                addListener(object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        PlaybackControllerHolder.getOrNull(applicationContext)
                            ?.onExoAudioSessionIdChanged(audioSessionId)
                    }
                })
            }

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            1001,
            Intent(this, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_PLAYER, true)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val seekBackBtn = CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
            .setDisplayName("Retroceder 10s")
            .setSessionCommand(SessionCommand(CMD_SEEK_BACK, Bundle.EMPTY))
            .build()

        val seekFwdBtn = CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_10)
            .setDisplayName("Avançar 10s")
            .setSessionCommand(SessionCommand(CMD_SEEK_FORWARD, Bundle.EMPTY))
            .build()

        val playlistBtn = CommandButton.Builder(CommandButton.ICON_PLAYLIST_ADD)
            .setDisplayName("Adicionar/Remover Playlist")
            .setSessionCommand(SessionCommand(CMD_ADD_TO_PLAYLIST, Bundle.EMPTY))
            .build()

        val favoriteBtn = CommandButton.Builder(CommandButton.ICON_HEART_UNFILLED)
            .setDisplayName("Favoritar")
            .setSessionCommand(SessionCommand(CMD_TOGGLE_FAVORITE, Bundle.EMPTY))
            .build()

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(sessionActivityIntent)
            .setMediaButtonPreferences(listOf(seekBackBtn, seekFwdBtn, playlistBtn, favoriteBtn))
            .build()

        PlaybackControllerHolder.getOrNull(applicationContext)
            ?.onExoAudioSessionIdChanged(player.audioSessionId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = mediaLibrarySession?.player
        if (currentPlayer == null || !currentPlayer.playWhenReady || currentPlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        clickHandler.removeCallbacks(clickResetRunnable)
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun dispatchHeadsetClicks(clicks: Int) {
        if (clicks <= 0) return
        serviceScope.launch(Dispatchers.Main) {
            val prefs = preferencesStore.preferencesFlow.first()
            val action = when (clicks) {
                1 -> prefs.singleClickAction
                2 -> prefs.doubleClickAction
                3 -> prefs.tripleClickAction
                else -> prefs.quadClickAction
            }
            when (action) {
                HeadsetButtonMapping.PLAY_PAUSE -> {
                    if (player.isPlaying) player.pause() else player.play()
                }
                HeadsetButtonMapping.NEXT_TRACK -> player.seekToNextMediaItem()
                HeadsetButtonMapping.PREV_TRACK -> player.seekToPreviousMediaItem()
                HeadsetButtonMapping.SEEK_FORWARD -> player.seekForward()
                HeadsetButtonMapping.SEEK_BACKWARD -> player.seekBack()
                HeadsetButtonMapping.TOGGLE_FAVORITE -> {
                    PlaybackControllerHolder.getOrNull(applicationContext)?.toggleCurrentTrackFavorite()
                }
            }
        }
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(SessionCommand(CMD_SEEK_BACK, Bundle.EMPTY))
                .add(SessionCommand(CMD_SEEK_FORWARD, Bundle.EMPTY))
                .add(SessionCommand(CMD_TOGGLE_FAVORITE, Bundle.EMPTY))
                .add(SessionCommand(CMD_ADD_TO_PLAYLIST, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent,
        ): Boolean {
            val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            if (keyEvent != null &&
                keyEvent.action == KeyEvent.ACTION_DOWN &&
                keyEvent.repeatCount == 0 &&
                (keyEvent.keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            ) {
                clickCount++
                clickHandler.removeCallbacks(clickResetRunnable)
                clickHandler.postDelayed(clickResetRunnable, 420L)
                return true
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_SEEK_BACK -> player.seekBack()
                CMD_SEEK_FORWARD -> player.seekForward()
                CMD_TOGGLE_FAVORITE -> PlaybackControllerHolder.getOrNull(applicationContext)?.toggleCurrentTrackFavorite()
                CMD_ADD_TO_PLAYLIST -> {
                    val intent = Intent(applicationContext, MainActivity::class.java).apply {
                        putExtra(EXTRA_OPEN_PLAYER, true)
                        putExtra("extra_open_playlist_picker", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(R.string.app_name))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                val items = when (parentId) {
                    ROOT_ID -> listOf(
                        browsableNode(NODE_ALL_SONGS, "Músicas"),
                        browsableNode(NODE_ALBUMS, "Álbuns"),
                        browsableNode(NODE_ARTISTS, "Artistas"),
                        browsableNode(NODE_FOLDERS, "Pastas"),
                        browsableNode(NODE_PLAYLISTS, "Playlists"),
                        browsableNode(NODE_QUEUES, "Filas"),
                    )
                    NODE_ALL_SONGS -> database.trackDao().getAllTracksSync()
                        .filter { it.isPlayable }
                        .drop(page * pageSize)
                        .take(pageSize)
                        .map { it.toMediaItem() }
                    NODE_ALBUMS -> database.trackDao().getAllTracksSync()
                        .groupBy { it.albumId }
                        .values
                        .drop(page * pageSize)
                        .take(pageSize)
                        .map { list ->
                            val first = list.first()
                            browsableNode("album_${first.albumId}", first.album, first.artist)
                        }
                    NODE_ARTISTS -> database.trackDao().getAllTracksSync()
                        .groupBy { it.artist }
                        .entries
                        .drop(page * pageSize)
                        .take(pageSize)
                        .map { (name, _) -> browsableNode("artist_$name", name) }
                    NODE_FOLDERS -> database.trackDao().getAllTracksSync()
                        .groupBy { it.folderPath }
                        .entries
                        .drop(page * pageSize)
                        .take(pageSize)
                        .map { (folder, _) ->
                            val label = folder.substringAfterLast('/').ifBlank { folder }
                            browsableNode("folder_$folder", label, folder)
                        }
                    NODE_PLAYLISTS -> database.playlistDao().getPlaylistsSync()
                        .drop(page * pageSize)
                        .take(pageSize)
                        .map { browsableNode("playlist_${it.id}", it.name) }
                    NODE_QUEUES -> database.queueDao().getQueuesSync()
                        .drop(page * pageSize)
                        .take(pageSize)
                        .map { browsableNode("queue_${it.id}", it.name) }
                    else -> resolveSubtreeChildren(parentId)
                }
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
            }
            return future
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<MediaItem>>()
            serviceScope.launch {
                val trackId = mediaId.removePrefix("track_").toLongOrNull()
                val track = trackId?.let { database.trackDao().getTrackById(it) }
                if (track != null) {
                    future.set(LibraryResult.ofItem(track.toMediaItem(), null))
                } else {
                    future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                }
            }
            return future
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            serviceScope.launch {
                val count = database.trackDao().getAllTracksSync().count {
                    it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true) ||
                        it.album.contains(query, ignoreCase = true)
                }
                session.notifySearchResultChanged(browser, query, count, params)
            }
            return Futures.immediateFuture(LibraryResult.ofVoid(params))
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                val matches = database.trackDao().getAllTracksSync()
                    .filter {
                        it.isPlayable && (
                            it.title.contains(query, ignoreCase = true) ||
                                it.artist.contains(query, ignoreCase = true) ||
                                it.album.contains(query, ignoreCase = true)
                            )
                    }
                    .drop(page * pageSize)
                    .take(pageSize)
                    .map { it.toMediaItem() }
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(matches), params))
            }
            return future
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val future = com.google.common.util.concurrent.SettableFuture.create<MutableList<MediaItem>>()
            serviceScope.launch {
                val all = database.trackDao().getAllTracksSync().associateBy { "track_${it.id}" }
                val resolved = mediaItems.map { item ->
                    all[item.mediaId]?.toMediaItem() ?: item
                }.toMutableList()
                future.set(resolved)
            }
            return future
        }

        private suspend fun resolveSubtreeChildren(parentId: String): List<MediaItem> {
            val tracks = database.trackDao().getAllTracksSync().filter { it.isPlayable }
            return when {
                parentId.startsWith("album_") -> {
                    val albumId = parentId.removePrefix("album_").toLongOrNull() ?: return emptyList()
                    tracks.filter { it.albumId == albumId }.map { it.toMediaItem() }
                }
                parentId.startsWith("artist_") -> {
                    val artist = parentId.removePrefix("artist_")
                    tracks.filter { it.artist == artist }.map { it.toMediaItem() }
                }
                parentId.startsWith("folder_") -> {
                    val folder = parentId.removePrefix("folder_")
                    tracks.filter { it.folderPath == folder }.map { it.toMediaItem() }
                }
                parentId.startsWith("playlist_") -> {
                    val pid = parentId.removePrefix("playlist_").toLongOrNull() ?: return emptyList()
                    val byId = tracks.associateBy { it.id }
                    database.playlistDao().getPlaylistEntriesSync(pid)
                        .mapNotNull { byId[it.trackId]?.toMediaItem() }
                }
                parentId.startsWith("queue_") -> {
                    val qid = parentId.removePrefix("queue_").toLongOrNull() ?: return emptyList()
                    val byId = tracks.associateBy { it.id }
                    database.queueDao().getQueueTracksSync(qid)
                        .mapNotNull { byId[it.trackId]?.toMediaItem() }
                }
                else -> emptyList()
            }
        }

        private fun browsableNode(id: String, title: String, subtitle: String? = null): MediaItem {
            return MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }

        private fun com.example.core.database.MusicTrackEntity.toMediaItem(): MediaItem {
            return MediaItem.Builder()
                .setMediaId("track_$id")
                .setUri(Uri.parse(contentUri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setAlbumTitle(album)
                        .setArtworkUri(albumArtUri?.let { Uri.parse(it) })
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }
    }
}
