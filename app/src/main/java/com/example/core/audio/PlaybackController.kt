package com.example.core.audio

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.core.database.AppPreferencesStore
import com.example.core.database.MyatoDatabase
import com.example.core.database.PlaybackQueueEntity
import com.example.core.database.toDomain
import com.example.domain.models.MusicTrack
import com.example.domain.models.PlaybackQueue
import com.example.domain.models.QueueOriginType
import com.example.domain.models.RepeatMode
import com.example.widgets.WidgetUpdateHelper
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val activeQueue: PlaybackQueue? = null,
    val queueTracks: List<MusicTrack> = emptyList(),
    val currentTrack: MusicTrack? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val volumeFraction: Float = 1f,
    val errorMessage: String? = null,
)

object PlaybackControllerHolder {
    @Volatile
    private var instance: PlaybackController? = null

    fun get(context: Context): PlaybackController {
        return instance ?: synchronized(this) {
            instance ?: PlaybackController(context.applicationContext).also { instance = it }
        }
    }

    fun getOrNull(context: Context): PlaybackController? = instance
}

/**
 * Central coordinator bridging Media3 MediaController, multi-queue state persistence,
 * EqualizerEngine, SleepTimerManager, statistics logging, and AppWidget synchronization.
 */
class PlaybackController(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val database = MyatoDatabase.getInstance(context)
    private val preferencesStore = AppPreferencesStore(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val equalizerEngine = EqualizerEngine(context, database)
    val sleepTimerManager = SleepTimerManager(onTimerFinished = { pause() })

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var activeTrackStartMs: Long = 0L
    private var pendingAutoPlay: Boolean = false

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    init {
        restoreLastActiveQueue()
    }

    fun connectToService() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, MyatoPlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull() ?: return@addListener
            mediaController = controller
            attachPlayerListener(controller)
            syncVolumeFromDevice()
            startProgressTicker()
            // Load existing queue into ExoPlayer if player has no items yet
            val state = _playbackState.value
            if (controller.mediaItemCount == 0 && state.queueTracks.isNotEmpty()) {
                val shouldPlay = pendingAutoPlay
                pendingAutoPlay = false
                loadTracksIntoController(
                    tracks = state.queueTracks,
                    startIndex = state.currentIndex.coerceAtLeast(0),
                    startPositionMs = state.positionMs,
                    autoPlay = shouldPlay,
                )
            }
        }, MoreExecutors.directExecutor())
    }

    fun onExoAudioSessionIdChanged(audioSessionId: Int) {
        equalizerEngine.attachToAudioSession(audioSessionId)
    }

    private fun attachPlayerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                persistCurrentQueuePosition()
                pushWidgetUpdate()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val previousTrack = _playbackState.value.currentTrack
                if (previousTrack != null) {
                    val listened = (System.currentTimeMillis() - activeTrackStartMs).coerceIn(0L, previousTrack.durationMs)
                    if (listened >= 3000L) {
                        recordPlayStats(previousTrack, listened)
                    }
                    sleepTimerManager.onTrackTransitioned()
                }
                activeTrackStartMs = System.currentTimeMillis()
                val idx = controller.currentMediaItemIndex
                val track = _playbackState.value.queueTracks.getOrNull(idx)
                _playbackState.value = _playbackState.value.copy(
                    currentIndex = idx,
                    currentTrack = track,
                    durationMs = track?.durationMs ?: controller.duration.coerceAtLeast(0L),
                    positionMs = 0L,
                    errorMessage = null,
                )
                persistCurrentQueuePosition()
                pushWidgetUpdate()
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    errorMessage = "Não foi possível reproduzir este arquivo (${error.errorCodeName}). Verifique se o arquivo existe ou se o formato é suportado.",
                )
            }
        })
    }

    private fun restoreLastActiveQueue() {
        scope.launch(Dispatchers.IO) {
            val activeQueue = database.queueDao().getActiveQueueSync()
                ?: database.queueDao().getQueuesSync().firstOrNull()
                ?: return@launch
            loadQueueInternal(activeQueue.id, autoPlay = false)
        }
    }

    fun playTracks(
        tracks: List<MusicTrack>,
        startIndex: Int = 0,
        queueName: String = "Fila Atual",
        originType: QueueOriginType = QueueOriginType.CUSTOM,
        originKey: String = "",
    ) {
        val playable = tracks.filter { it.isPlayable }
        if (playable.isEmpty()) {
            _playbackState.value = _playbackState.value.copy(
                errorMessage = "Nenhuma faixa reproduzível encontrada nesta seleção."
            )
            return
        }
        val targetTrack = tracks.getOrNull(startIndex)
        val safeStartIndex = if (targetTrack != null) playable.indexOfFirst { it.id == targetTrack.id }.coerceAtLeast(0) else 0

        scope.launch(Dispatchers.IO) {
            // Save outgoing queue position before switching
            persistCurrentQueuePositionSync()

            val existing = if (originType != QueueOriginType.CUSTOM && originKey.isNotBlank()) {
                database.queueDao().getQueuesSync().firstOrNull {
                    it.originType == originType.name && it.originKey == originKey
                }
            } else null

            val queueId = if (existing != null) {
                database.queueDao().setActiveQueue(existing.id)
                database.queueDao().replaceQueueTracks(existing.id, playable.map { it.id })
                existing.id
            } else {
                val createdId = database.queueDao().insertQueue(
                    PlaybackQueueEntity(
                        name = queueName,
                        originType = originType.name,
                        originKey = originKey,
                        currentTrackId = playable[safeStartIndex].id,
                        currentPositionMs = 0L,
                        isActive = true,
                    )
                )
                database.queueDao().setActiveQueue(createdId)
                database.queueDao().replaceQueueTracks(createdId, playable.map { it.id })
                createdId
            }

            val updatedQueue = database.queueDao().getQueueById(queueId)?.toDomain(playable.size)
            val chosen = playable[safeStartIndex]

            launch(Dispatchers.Main) {
                activeTrackStartMs = System.currentTimeMillis()
                _playbackState.value = _playbackState.value.copy(
                    activeQueue = updatedQueue,
                    queueTracks = playable,
                    currentTrack = chosen,
                    currentIndex = safeStartIndex,
                    positionMs = 0L,
                    durationMs = chosen.durationMs,
                    isShuffleEnabled = updatedQueue?.isShuffleEnabled ?: false,
                    repeatMode = updatedQueue?.repeatMode ?: RepeatMode.OFF,
                    errorMessage = null,
                )
                loadTracksIntoController(playable, safeStartIndex, 0L, autoPlay = true)
                pushWidgetUpdate()
            }
        }
    }

    fun switchToQueue(queueId: Long, autoPlay: Boolean = true) {
        scope.launch(Dispatchers.IO) {
            persistCurrentQueuePositionSync()
            database.queueDao().setActiveQueue(queueId)
            loadQueueInternal(queueId, autoPlay = autoPlay)
        }
    }

    private suspend fun loadQueueInternal(queueId: Long, autoPlay: Boolean) {
        val queue = database.queueDao().getQueueById(queueId) ?: return
        val entries = database.queueDao().getQueueTracksSync(queueId)
        val allTracks = database.trackDao().getTracksByIds(entries.map { it.trackId }).associateBy { it.id }
        val favIds = database.engagementDao().getFavoritesSync().map { it.trackId }.toSet()
        val statsById = database.engagementDao().getAllStatsSync().associateBy { it.trackId }

        val orderedTracks = entries.mapNotNull { entry ->
            val entity = allTracks[entry.trackId] ?: return@mapNotNull null
            val st = statsById[entity.id]
            entity.toDomain(
                isFavorite = entity.id in favIds,
                playCount = st?.playCount ?: 0,
                lastPlayedAt = st?.lastPlayedAt ?: 0L,
            )
        }
        val idx = orderedTracks.indexOfFirst { it.id == queue.currentTrackId }.let {
            if (it >= 0) it else 0
        }
        val currentTrack = orderedTracks.getOrNull(idx)
        val domainQueue = queue.toDomain(orderedTracks.size)

        scope.launch(Dispatchers.Main) {
            activeTrackStartMs = System.currentTimeMillis()
            _playbackState.value = _playbackState.value.copy(
                activeQueue = domainQueue,
                queueTracks = orderedTracks,
                currentTrack = currentTrack,
                currentIndex = if (orderedTracks.isEmpty()) -1 else idx,
                positionMs = queue.currentPositionMs,
                durationMs = currentTrack?.durationMs ?: 0L,
                isShuffleEnabled = domainQueue.isShuffleEnabled,
                repeatMode = domainQueue.repeatMode,
                errorMessage = null,
            )
            if (orderedTracks.isNotEmpty()) {
                loadTracksIntoController(orderedTracks, idx, queue.currentPositionMs, autoPlay = autoPlay)
            }
            pushWidgetUpdate()
        }
    }

    private fun loadTracksIntoController(
        tracks: List<MusicTrack>,
        startIndex: Int,
        startPositionMs: Long,
        autoPlay: Boolean,
    ) {
        val controller = mediaController ?: run {
            if (autoPlay) pendingAutoPlay = true
            connectToService()
            return
        }
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId("track_${track.id}")
                .setUri(Uri.parse(track.contentUri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(track.albumArtUri?.let { Uri.parse(it) })
                        .build()
                )
                .build()
        }
        controller.setMediaItems(mediaItems, startIndex.coerceIn(0, mediaItems.lastIndex), startPositionMs.coerceAtLeast(0L))
        controller.prepare()
        applyShuffleAndRepeatToController(controller)
        if (autoPlay) controller.play()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: run {
            pendingAutoPlay = true
            connectToService()
            return
        }
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.mediaItemCount == 0 && _playbackState.value.queueTracks.isNotEmpty()) {
                loadTracksIntoController(
                    _playbackState.value.queueTracks,
                    _playbackState.value.currentIndex.coerceAtLeast(0),
                    _playbackState.value.positionMs,
                    autoPlay = true,
                )
            } else {
                controller.play()
            }
        }
    }

    fun pause() {
        mediaController?.pause()
    }

    fun skipNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (_playbackState.value.repeatMode == RepeatMode.ALL && controller.mediaItemCount > 0) {
            controller.seekToDefaultPosition(0)
        }
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3500L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else if (_playbackState.value.repeatMode == RepeatMode.ALL && controller.mediaItemCount > 0) {
            controller.seekToDefaultPosition((controller.mediaItemCount - 1).coerceAtLeast(0))
        }
    }

    fun seekTo(positionMs: Long) {
        val safe = positionMs.coerceAtLeast(0L)
        mediaController?.seekTo(safe)
        _playbackState.value = _playbackState.value.copy(positionMs = safe)
        persistCurrentQueuePosition()
    }

    fun seekForward10s() {
        mediaController?.seekForward()
    }

    fun seekBackward10s() {
        mediaController?.seekBack()
    }

    fun toggleShuffle() {
        val next = !_playbackState.value.isShuffleEnabled
        mediaController?.shuffleModeEnabled = next
        _playbackState.value = _playbackState.value.copy(isShuffleEnabled = next)
        updateQueueFlags(shuffle = next, repeatMode = _playbackState.value.repeatMode)
    }

    fun cycleRepeatMode() {
        val next = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        mediaController?.let { applyRepeatMode(it, next) }
        _playbackState.value = _playbackState.value.copy(repeatMode = next)
        updateQueueFlags(shuffle = _playbackState.value.isShuffleEnabled, repeatMode = next)
    }

    fun setVolumeFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (maxVol * clamped).toInt().coerceIn(0, maxVol)
        runCatching { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) }
        mediaController?.volume = 1f
        _playbackState.value = _playbackState.value.copy(volumeFraction = clamped)
    }

    fun syncVolumeFromDevice() {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curr = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        _playbackState.value = _playbackState.value.copy(volumeFraction = curr.toFloat() / maxVol.toFloat())
    }

    fun addTrackNextInActiveQueue(track: MusicTrack) {
        val currentList = _playbackState.value.queueTracks.toMutableList()
        if (currentList.isEmpty()) {
            playTracks(listOf(track), 0, "Fila Principal", QueueOriginType.CUSTOM)
            return
        }
        val insertIdx = (_playbackState.value.currentIndex + 1).coerceIn(0, currentList.size)
        currentList.add(insertIdx, track)
        updateActiveQueueTracks(currentList)
    }

    fun appendTrackToActiveQueue(track: MusicTrack) {
        val currentList = _playbackState.value.queueTracks.toMutableList()
        if (currentList.isEmpty()) {
            playTracks(listOf(track), 0, "Fila Principal", QueueOriginType.CUSTOM)
            return
        }
        currentList.add(track)
        updateActiveQueueTracks(currentList)
    }

    fun appendTracksToActiveQueue(tracks: List<MusicTrack>) {
        if (tracks.isEmpty()) return
        val currentList = _playbackState.value.queueTracks.toMutableList()
        if (currentList.isEmpty()) {
            playTracks(tracks, 0, "Fila Principal", QueueOriginType.CUSTOM)
            return
        }
        currentList.addAll(tracks)
        updateActiveQueueTracks(currentList)
    }

    fun clearActiveQueue() {
        val qid = _playbackState.value.activeQueue?.id ?: return
        scope.launch(Dispatchers.IO) {
            database.queueDao().clearQueueTracks(qid)
            launch(Dispatchers.Main) {
                mediaController?.clearMediaItems()
                _playbackState.value = _playbackState.value.copy(
                    queueTracks = emptyList(),
                    currentIndex = -1,
                    currentTrack = null,
                    positionMs = 0L,
                    durationMs = 0L,
                    isPlaying = false,
                )
                pushWidgetUpdate()
            }
        }
    }

    fun playTrackAtIndex(index: Int) {
        val list = _playbackState.value.queueTracks
        if (index in list.indices) {
            val track = list[index]
            _playbackState.value = _playbackState.value.copy(
                currentIndex = index,
                currentTrack = track,
                positionMs = 0L,
                durationMs = track.durationMs,
            )
            mediaController?.seekTo(index, 0L)
            mediaController?.play()
            persistCurrentQueuePosition()
            pushWidgetUpdate()
        }
    }

    fun removeTrackFromActiveQueue(index: Int) {
        val currentList = _playbackState.value.queueTracks.toMutableList()
        if (index !in currentList.indices) return
        currentList.removeAt(index)
        val currentIdx = _playbackState.value.currentIndex
        val newIdx = when {
            currentList.isEmpty() -> -1
            index < currentIdx -> currentIdx - 1
            index == currentIdx -> currentIdx.coerceAtMost(currentList.lastIndex)
            else -> currentIdx
        }
        _playbackState.value = _playbackState.value.copy(
            queueTracks = currentList,
            currentIndex = newIdx,
            currentTrack = currentList.getOrNull(newIdx),
        )
        mediaController?.removeMediaItem(index)
        persistTrackOrderInActiveQueue(currentList)
    }

    fun moveTrackInActiveQueue(fromIndex: Int, toIndex: Int) {
        val list = _playbackState.value.queueTracks.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        val currentId = _playbackState.value.currentTrack?.id
        val newCurrentIdx = list.indexOfFirst { it.id == currentId }
        _playbackState.value = _playbackState.value.copy(
            queueTracks = list,
            currentIndex = newCurrentIdx,
        )
        mediaController?.moveMediaItem(fromIndex, toIndex)
        persistTrackOrderInActiveQueue(list)
    }

    fun toggleCurrentTrackFavorite() {
        val track = _playbackState.value.currentTrack ?: return
        scope.launch(Dispatchers.IO) {
            val isFav = database.engagementDao().isFavorite(track.id)
            if (isFav) {
                database.engagementDao().removeFavorite(track.id)
            } else {
                database.engagementDao().addFavorite(com.example.core.database.FavoriteEntity(track.id))
            }
            val updatedTrack = track.copy(isFavorite = !isFav)
            val updatedList = _playbackState.value.queueTracks.map {
                if (it.id == track.id) it.copy(isFavorite = !isFav) else it
            }
            launch(Dispatchers.Main) {
                _playbackState.value = _playbackState.value.copy(
                    currentTrack = updatedTrack,
                    queueTracks = updatedList,
                )
                pushWidgetUpdate()
            }
        }
    }

    private fun updateActiveQueueTracks(newList: List<MusicTrack>) {
        _playbackState.value = _playbackState.value.copy(queueTracks = newList)
        val controller = mediaController
        if (controller != null) {
            val pos = controller.currentPosition
            val wasPlaying = controller.isPlaying
            val currentIdx = _playbackState.value.currentIndex.coerceAtLeast(0)
            loadTracksIntoController(newList, currentIdx, pos, autoPlay = wasPlaying)
        }
        persistTrackOrderInActiveQueue(newList)
    }

    private fun persistTrackOrderInActiveQueue(tracks: List<MusicTrack>) {
        val qid = _playbackState.value.activeQueue?.id ?: return
        scope.launch(Dispatchers.IO) {
            database.queueDao().replaceQueueTracks(qid, tracks.map { it.id })
        }
    }

    private fun applyShuffleAndRepeatToController(controller: MediaController) {
        controller.shuffleModeEnabled = _playbackState.value.isShuffleEnabled
        applyRepeatMode(controller, _playbackState.value.repeatMode)
    }

    private fun applyRepeatMode(controller: MediaController, mode: RepeatMode) {
        controller.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    private fun updateQueueFlags(shuffle: Boolean, repeatMode: RepeatMode) {
        val qid = _playbackState.value.activeQueue?.id ?: return
        scope.launch(Dispatchers.IO) {
            val existing = database.queueDao().getQueueById(qid) ?: return@launch
            database.queueDao().updateQueue(
                existing.copy(isShuffleEnabled = shuffle, repeatMode = repeatMode.name)
            )
        }
    }

    private fun persistCurrentQueuePosition() {
        scope.launch(Dispatchers.IO) { persistCurrentQueuePositionSync() }
    }

    private suspend fun persistCurrentQueuePositionSync() {
        val state = _playbackState.value
        val qid = state.activeQueue?.id ?: return
        val trackId = state.currentTrack?.id
        val pos = state.positionMs
        val existing = database.queueDao().getQueueById(qid)
        if (existing != null) {
            database.queueDao().updateQueue(
                existing.copy(
                    currentTrackId = trackId,
                    currentPositionMs = pos,
                    lastActiveAt = System.currentTimeMillis(),
                )
            )
        }
        if (trackId != null) {
            preferencesStore.setActiveQueueState(qid, trackId, pos)
        }
    }

    private fun recordPlayStats(track: MusicTrack, durationListenedMs: Long) {
        scope.launch(Dispatchers.IO) {
            database.engagementDao().recordPlayback(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                durationListenedMs = durationListenedMs,
            )
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                delay(500L)
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0L } ?: _playbackState.value.durationMs
                    _playbackState.value = _playbackState.value.copy(
                        positionMs = pos,
                        durationMs = dur,
                    )
                    if (pos % 5000L < 600L) {
                        persistCurrentQueuePosition()
                        pushWidgetUpdate()
                    }
                }
            }
        }
    }

    fun pushWidgetUpdate() {
        val s = _playbackState.value
        WidgetUpdateHelper.updateAllWidgets(
            context = context,
            title = s.currentTrack?.title ?: "MYATO MUSIC",
            artist = s.currentTrack?.artist ?: "Sua música. Seu estilo.",
            album = s.currentTrack?.album ?: "Offline Audio Engine",
            artUri = s.currentTrack?.albumArtUri,
            isPlaying = s.isPlaying,
            isFavorite = s.currentTrack?.isFavorite ?: false,
            progressPercent = if (s.durationMs > 0L) ((s.positionMs * 100L) / s.durationMs).toInt().coerceIn(0, 100) else 0,
        )
    }
}
