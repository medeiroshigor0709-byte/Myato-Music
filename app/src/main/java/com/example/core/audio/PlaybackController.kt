package com.example.core.audio

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val volumeStep: Int = 15,
    val volumeMinStep: Int = 0,
    val volumeMaxStep: Int = 15,
    val isVolumeMuted: Boolean = false,
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
    @Volatile
    private var isAppInForeground: Boolean = true
    @Volatile
    private var lastPersistedClockMs: Long = 0L
    @Volatile
    private var isUserDraggingVolume: Boolean = false
    @Volatile
    private var lastInternalVolumeWriteMs: Long = 0L
    @Volatile
    private var lastInternalVolumeStep: Int = -1
    @Volatile
    private var volumeBeforeMuteStep: Int = -1

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _progressPositionMs = MutableStateFlow(0L)
    val progressPositionMs: StateFlow<Long> = _progressPositionMs.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                ACTION_VOLUME_CHANGED, ACTION_STREAM_MUTE_CHANGED -> {
                    val streamType = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, AudioManager.STREAM_MUSIC)
                    if (streamType == AudioManager.STREAM_MUSIC) {
                        syncVolumeFromDevice()
                    }
                }
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    syncVolumeFromDevice(force = true)
                }
            }
        }
    }

    private val volumeContentObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            syncVolumeFromDevice()
        }
    }

    private val volumeDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            syncVolumeFromDevice(force = true)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            syncVolumeFromDevice(force = true)
        }
    }

    init {
        registerVolumeObservers()
        syncVolumeFromDevice(force = true)
        restoreLastActiveQueue()
    }

    companion object {
        const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        const val ACTION_STREAM_MUTE_CHANGED = "android.media.STREAM_MUTE_CHANGED_ACTION"
        const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
    }

    private fun registerVolumeObservers() {
        runCatching {
            val filter = IntentFilter().apply {
                addAction(ACTION_VOLUME_CHANGED)
                addAction(ACTION_STREAM_MUTE_CHANGED)
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            }
            ContextCompat.registerReceiver(context, volumeReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        }
        runCatching {
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                volumeContentObserver,
            )
        }
        runCatching {
            audioManager.registerAudioDeviceCallback(volumeDeviceCallback, mainHandler)
        }
    }

    fun setAppForeground(inForeground: Boolean) {
        if (isAppInForeground == inForeground) return
        isAppInForeground = inForeground
        if (inForeground) {
            syncVolumeFromDevice(force = true)
        }
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            if (inForeground) {
                _progressPositionMs.value = controller.currentPosition.coerceAtLeast(0L)
            }
            refreshProgressTicker(true)
        }
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
            syncVolumeFromDevice(force = true)
            refreshProgressTicker(controller.isPlaying)
            equalizerEngine.onPlaybackStateChanged(
                isPlaying = controller.isPlaying,
                hasTrack = _playbackState.value.currentTrack != null,
            )
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
                val pos = controller.currentPosition.coerceAtLeast(0L)
                _progressPositionMs.value = pos
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = isPlaying,
                    positionMs = pos,
                )
                equalizerEngine.onPlaybackStateChanged(
                    isPlaying = isPlaying,
                    hasTrack = _playbackState.value.currentTrack != null,
                )
                syncVolumeFromDevice(force = true)
                refreshProgressTicker(isPlaying)
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
                _progressPositionMs.value = 0L
                _playbackState.value = _playbackState.value.copy(
                    currentIndex = idx,
                    currentTrack = track,
                    durationMs = track?.durationMs ?: controller.duration.coerceAtLeast(0L),
                    positionMs = 0L,
                    errorMessage = null,
                )
                equalizerEngine.onPlaybackStateChanged(
                    isPlaying = controller.isPlaying,
                    hasTrack = track != null,
                )
                equalizerEngine.onTrackTransitioned()
                syncVolumeFromDevice(force = true)
                persistCurrentQueuePosition()
                pushWidgetUpdate()
            }

            override fun onPlayerError(error: PlaybackException) {
                refreshProgressTicker(false)
                equalizerEngine.onPlaybackStateChanged(
                    isPlaying = false,
                    hasTrack = _playbackState.value.currentTrack != null,
                )
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
                _progressPositionMs.value = 0L
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
            _progressPositionMs.value = queue.currentPositionMs
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
            } else {
                equalizerEngine.onPlaybackStateChanged(isPlaying = false, hasTrack = false)
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
        equalizerEngine.onPlaybackStateChanged(isPlaying = autoPlay, hasTrack = tracks.isNotEmpty())
        equalizerEngine.onTrackTransitioned()
        syncVolumeFromDevice(force = true)
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
        _progressPositionMs.value = safe
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

    private fun readStreamVolumeBounds(): Pair<Int, Int> {
        val minVol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0)
        } else {
            0
        }
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(minVol + 1)
        return minVol to maxVol
    }

    fun onVolumeDragStarted() {
        isUserDraggingVolume = true
    }

    fun onVolumeDragFinished() {
        isUserDraggingVolume = false
        syncVolumeFromDevice(force = true)
    }

    fun setVolumeFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        val (minVol, maxVol) = readStreamVolumeBounds()
        val span = (maxVol - minVol).coerceAtLeast(1)
        val target = (minVol + (span * clamped).roundToInt()).coerceIn(minVol, maxVol)
        val currentSystemVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (target != currentSystemVol || (target == minVol && audioManager.isStreamMute(AudioManager.STREAM_MUSIC).not())) {
            lastInternalVolumeWriteMs = SystemClock.elapsedRealtime()
            lastInternalVolumeStep = target
            runCatching { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) }
        }
        if (target > minVol) {
            volumeBeforeMuteStep = target
        }
        mediaController?.volume = 1f
        _playbackState.value = _playbackState.value.copy(
            volumeFraction = clamped,
            volumeStep = target,
            volumeMinStep = minVol,
            volumeMaxStep = maxVol,
            isVolumeMuted = target <= minVol,
        )
    }

    fun toggleVolumeMute() {
        val (minVol, maxVol) = readStreamVolumeBounds()
        val curr = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(minVol, maxVol)
        val isMutedNow = audioManager.isStreamMute(AudioManager.STREAM_MUSIC) || curr <= minVol
        val targetStep = if (isMutedNow) {
            volumeBeforeMuteStep.takeIf { it > minVol } ?: ((minVol + maxVol) / 2).coerceAtLeast(minVol + 1)
        } else {
            volumeBeforeMuteStep = curr
            minVol
        }
        lastInternalVolumeWriteMs = SystemClock.elapsedRealtime()
        lastInternalVolumeStep = targetStep
        runCatching { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetStep, 0) }
        syncVolumeFromDevice(force = true)
    }

    fun adjustVolumeByStep(delta: Int) {
        val (minVol, maxVol) = readStreamVolumeBounds()
        val curr = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(minVol, maxVol)
        val target = (curr + delta).coerceIn(minVol, maxVol)
        lastInternalVolumeWriteMs = SystemClock.elapsedRealtime()
        lastInternalVolumeStep = target
        runCatching { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) }
        syncVolumeFromDevice(force = true)
    }

    fun syncVolumeFromDevice(force: Boolean = false) {
        val (minVol, maxVol) = readStreamVolumeBounds()
        val span = (maxVol - minVol).coerceAtLeast(1)
        val curr = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(minVol, maxVol)
        val isMuted = runCatching { audioManager.isStreamMute(AudioManager.STREAM_MUSIC) }.getOrDefault(false) || curr <= minVol
        val nowMs = SystemClock.elapsedRealtime()
        if (!force && isUserDraggingVolume && (nowMs - lastInternalVolumeWriteMs) < 220L && curr == lastInternalVolumeStep) {
            return
        }
        val fraction = if (isMuted) 0f else ((curr - minVol).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        if (curr > minVol) {
            volumeBeforeMuteStep = curr
        }
        _playbackState.value = _playbackState.value.copy(
            volumeFraction = fraction,
            volumeStep = curr,
            volumeMinStep = minVol,
            volumeMaxStep = maxVol,
            isVolumeMuted = isMuted,
        )
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
                refreshProgressTicker(false)
                equalizerEngine.onPlaybackStateChanged(isPlaying = false, hasTrack = false)
                _progressPositionMs.value = 0L
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
            _progressPositionMs.value = 0L
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
        val pos = _progressPositionMs.value.takeIf { it > 0L } ?: state.positionMs
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

    private fun refreshProgressTicker(isPlaying: Boolean) {
        progressJob?.cancel()
        if (!isPlaying) {
            progressJob = null
            return
        }
        val intervalMs = if (isAppInForeground) 500L else 5000L
        progressJob = scope.launch {
            while (true) {
                delay(intervalMs)
                val controller = mediaController ?: break
                if (!controller.isPlaying) break
                val pos = controller.currentPosition.coerceAtLeast(0L)
                _progressPositionMs.value = pos

                val dur = controller.duration.takeIf { it > 0L } ?: _playbackState.value.durationMs
                if (dur != _playbackState.value.durationMs) {
                    _playbackState.value = _playbackState.value.copy(
                        durationMs = dur,
                        positionMs = pos,
                    )
                }

                val nowMs = android.os.SystemClock.elapsedRealtime()
                if (nowMs - lastPersistedClockMs >= 5000L) {
                    lastPersistedClockMs = nowMs
                    persistCurrentQueuePosition()
                    pushWidgetUpdate()
                }
            }
        }
    }

    fun pushWidgetUpdate() {
        val s = _playbackState.value
        val pos = _progressPositionMs.value.takeIf { it > 0L } ?: s.positionMs
        val title = s.currentTrack?.title ?: "MYATO MUSIC"
        val artist = s.currentTrack?.artist ?: "Sua música. Seu estilo."
        val album = s.currentTrack?.album ?: "Offline Audio Engine"
        val artUri = s.currentTrack?.albumArtUri
        val isPlaying = s.isPlaying
        val isFavorite = s.currentTrack?.isFavorite ?: false
        val progressPercent = if (s.durationMs > 0L) ((pos * 100L) / s.durationMs).toInt().coerceIn(0, 100) else 0
        scope.launch(Dispatchers.IO) {
            WidgetUpdateHelper.updateAllWidgets(
                context = context,
                title = title,
                artist = artist,
                album = album,
                artUri = artUri,
                isPlaying = isPlaying,
                isFavorite = isFavorite,
                progressPercent = progressPercent,
            )
        }
    }

    fun release() {
        progressJob?.cancel()
        progressJob = null
        runCatching { context.unregisterReceiver(volumeReceiver) }
        runCatching { context.contentResolver.unregisterContentObserver(volumeContentObserver) }
        runCatching { audioManager.unregisterAudioDeviceCallback(volumeDeviceCallback) }
        equalizerEngine.releaseAll()
        controllerFuture?.let { future ->
            runCatching { MediaController.releaseFuture(future) }
        }
        controllerFuture = null
        mediaController = null
    }
}
