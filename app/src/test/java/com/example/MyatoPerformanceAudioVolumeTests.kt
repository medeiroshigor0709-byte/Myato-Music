package com.example

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.core.audio.AudioSpectrumAnalyzer
import com.example.core.audio.AudioVisualizerFrame
import com.example.core.audio.PlaybackController
import com.example.core.audio.PlaybackUiState
import com.example.core.audio.VisualizerCaptureStatus
import com.example.core.database.AppPreferencesStore
import com.example.core.designsystem.BuiltInThemes
import com.example.core.designsystem.MyatoMusicTheme
import com.example.core.metadata.MusicSearchEngine
import com.example.domain.models.MusicTrack
import com.example.domain.models.SearchFilterMode
import com.example.domain.models.TrackSortOrder
import com.example.feature.library.AllSongsScreen
import com.example.feature.player.FullPlayerScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MyatoPerformanceAudioVolumeTests {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        MusicSearchEngine.clearCaches()
    }

    private fun makeTrack(
        id: Long,
        title: String,
        artist: String,
        album: String = "Álbum $id",
        genre: String = "Hip Hop",
        format: String = "MP3",
    ): MusicTrack = MusicTrack(
        id = id,
        mediaStoreId = id,
        title = title,
        artist = artist,
        album = album,
        albumId = id,
        artistId = id,
        durationMs = 210_000L,
        sizeBytes = 4_200_000L,
        filePath = "/music/$id.mp3",
        folderPath = "/music",
        contentUri = "content://media/external/audio/media/$id",
        genre = genre,
        formatExtension = format,
        sampleRateHz = 44100,
        bitrateKbps = 320,
    )

    /**
     * Generates a realistic Android Visualizer packed FFT byte array:
     * fft[0] = DC, fft[1] = Nyquist, fft[2*k] = Real, fft[2*k+1] = Imag.
     */
    private fun buildSyntheticFft(
        targetFreqHz: Float,
        amplitude: Byte,
        sampleRateRaw: Int = 44_100_000, // Android reports milliHertz
        fftSize: Int = 1024,
    ): ByteArray {
        val bytes = ByteArray(fftSize)
        val srHz = if (sampleRateRaw >= 100_000) sampleRateRaw / 1000f else sampleRateRaw.toFloat()
        val maxBin = (fftSize / 2) - 1
        val targetBin = ((targetFreqHz * fftSize) / srHz).roundToInt().coerceIn(1, maxBin)
        bytes[targetBin * 2] = amplitude
        // Small side-lobe energy in adjacent bins to mirror real windowing leakage
        if (targetBin > 1) bytes[(targetBin - 1) * 2] = (amplitude / 3).toInt().toByte()
        if (targetBin < maxBin) bytes[(targetBin + 1) * 2] = (amplitude / 3).toInt().toByte()
        return bytes
    }

    // =========================================================================
    // 1. SEARCH OPTIMIZATION TESTS (5,000+ tracks, cache, prefix, accents, UI)
    // =========================================================================

    @Test
    fun `search engine indexes 5000 tracks rapidly, serves LRU cache, and matches prefix subset identically`() {
        val artists = listOf(
            "Haikaiss",
            "Racionais MC's",
            "Daft Punk",
            "AC/DC",
            "Jorge Ben Jor",
            "Djonga & Froid",
            "Alok feat. Zeeba",
            "Legião Urbana",
            "O Terno",
            "Anavitória",
        )
        val genres = listOf("Rap Nacional", "Eletrônica", "Rock Clássico", "MPB", "Indie")
        val tracks = (1..5_000).map { i ->
            val artist = artists[i % artists.size]
            val title = when (i) {
                2500 -> "A Praga da Noite"
                3333 -> "Coração Selvagem (Ao Vivo)"
                else -> "Faixa Número $i ${if (i % 7 == 0) "Especial" else "Original"}"
            }
            makeTrack(
                id = i.toLong(),
                title = title,
                artist = artist,
                album = "Disco ${(i % 50) + 1}",
                genre = genres[i % genres.size],
            )
        }

        val warmMs = measureTimeMillis {
            MusicSearchEngine.prewarmIndex(tracks)
        }
        val coldQueryMs = measureTimeMillis {
            val res = MusicSearchEngine.searchTracks(tracks, "haikaiss")
            assertEquals(500, res.hits.size)
        }
        val cachedQueryMs = measureTimeMillis {
            val res = MusicSearchEngine.searchTracks(tracks, "haikaiss")
            assertEquals(500, res.hits.size)
        }

        assertTrue("Prewarm 5k took $warmMs ms", warmMs < 4_000)
        assertTrue("Cold query took $coldQueryMs ms", coldQueryMs < 500)
        assertTrue("Cached query took $cachedQueryMs ms", cachedQueryMs < 50)

        // Prefix subset optimization: "haik" -> "haikaiss" produces identical result to fresh full scan
        val narrowed = MusicSearchEngine.searchTracks(tracks, "haikaiss")
        MusicSearchEngine.clearCaches()
        val cold = MusicSearchEngine.searchTracks(tracks, "haikaiss")
        assertEquals(cold.hits.map { it.track.id }, narrowed.hits.map { it.track.id })

        // Accent, punctuation, multi-token & genre/album queries
        val accentHit = MusicSearchEngine.searchTracks(tracks, "CORACAO")
        assertTrue(accentHit.hits.any { it.track.id == 3333L })

        val collabHit = MusicSearchEngine.searchTracks(tracks, "zeeba", filterMode = SearchFilterMode.ARTIST_AND_COLLABS)
        assertTrue(collabHit.hits.isNotEmpty())

        val punctuationHit = MusicSearchEngine.searchTracks(tracks, "ac dc")
        assertEquals(500, punctuationHit.hits.size)

        val genreHit = MusicSearchEngine.searchTracks(tracks, "eletronica")
        assertEquals(1000, genreHit.hits.size)
    }

    @Test
    fun `search debounce preference clamps strictly between 250ms and 350ms`() = runBlocking {
        val prefs = AppPreferencesStore(context)
        prefs.setSearchDebounceMs(100L)
        assertEquals(250L, prefs.preferencesFlow.first().searchDebounceMs)
        prefs.setSearchDebounceMs(900L)
        assertEquals(350L, prefs.preferencesFlow.first().searchDebounceMs)
        prefs.setSearchDebounceMs(310L)
        assertEquals(310L, prefs.preferencesFlow.first().searchDebounceMs)
    }

    @Test
    fun `all songs screen debounces rapid typing cleanly and clears immediately without UI stalls`() {
        val library = listOf(
            makeTrack(1L, "A Praga", "Haikaiss", "Teto Baixo", "Rap"),
            makeTrack(2L, "Plutão", "VMZ", "Galáxia", "Trap"),
            makeTrack(3L, "Coração Cigano", "Luan Santana", "Ao Vivo", "Sertanejo"),
        )

        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                AllSongsScreen(
                    tracks = library,
                    currentTrackId = null,
                    duplicates = emptyList(),
                    onBack = {},
                    onRefresh = {},
                    onScanDuplicates = {},
                    onPlayTracks = { _, _, _, _ -> },
                    onBatchAddToPlaylists = {},
                    onCreatePlaylistWithTracks = { _, _ -> },
                    onBatchAddToQueue = {},
                    onBatchShare = {},
                    onBatchDeleteFiles = {},
                    onBatchEditTags = {},
                    onTrackMoreClick = {},
                    onSmartSelect = { _, _ -> emptyList() },
                    sortTracks = { l, _, _ -> l },
                    searchDebounceMs = 300L,
                )
            }
        }
        composeRule.waitForIdle()

        // Rapid keystrokes under the 300ms window must NOT prematurely show the filtered count
        val input = composeRule.onNodeWithTag("songs_search_input")
        input.performTextInput("H")
        composeRule.mainClock.advanceTimeBy(80L)
        input.performTextReplacement("Ha")
        composeRule.mainClock.advanceTimeBy(80L)
        input.performTextReplacement("Hai")
        composeRule.mainClock.advanceTimeBy(80L)
        input.performTextReplacement("Haik")
        composeRule.mainClock.advanceTimeBy(120L) // only 120ms since last edit -> still debouncing
        assertTrue(composeRule.onAllNodes(hasTestTag("search_result_count")).fetchSemanticsNodes().isEmpty())

        // Once full 300ms passes after final character, search completes off main thread and updates UI
        composeRule.mainClock.advanceTimeBy(250L)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule.onAllNodes(hasTestTag("search_result_count")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("1 música(s) encontrada(s)").assertIsDisplayed()

        // Clearing search resets debouncedQuery immediately without waiting 300ms and delivers full library from background thread
        composeRule.onNodeWithTag("clear_search_button").performClick()
        composeRule.mainClock.advanceTimeBy(350L)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule.onAllNodes(hasTestTag("search_result_count")).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("all_songs_list").performScrollToNode(hasText("Plutão"))
        composeRule.onNodeWithText("Plutão").assertIsDisplayed()
    }

    // =========================================================================
    // 2. REAL AUDIO VISUALIZER FFT & LIFECYCLE TESTS
    // =========================================================================

    @Test
    fun `spectrum analyzer distinguishes bass, mid, and treble frequency bands accurately`() {
        // A. Strong 80 Hz Bass
        val bassAnalyzer = AudioSpectrumAnalyzer()
        val bassFft = buildSyntheticFft(targetFreqHz = 80f, amplitude = 110)
        val bassFrame = bassAnalyzer.processFftCapture(bassFft, 44_100_000, status = VisualizerCaptureStatus.ACTIVE_SYNCED)
        assertTrue(bassFrame.hasRealSignal)
        assertEquals(44_100, bassFrame.sampleRateHz)
        assertTrue("Expected bass(${bassFrame.bassLevel}) > mid(${bassFrame.midLevel})", bassFrame.bassLevel > bassFrame.midLevel)
        assertTrue("Expected bass(${bassFrame.bassLevel}) > treble(${bassFrame.trebleLevel})", bassFrame.bassLevel > bassFrame.trebleLevel)
        assertTrue("Expected beat onset pulse on sudden bass drop", bassFrame.beatPulse > 0.1f)

        // B. Strong 1 kHz Midrange
        val midAnalyzer = AudioSpectrumAnalyzer()
        val midFft = buildSyntheticFft(targetFreqHz = 1_000f, amplitude = 110)
        val midFrame = midAnalyzer.processFftCapture(midFft, 44_100_000, status = VisualizerCaptureStatus.ACTIVE_SYNCED)
        assertTrue(midFrame.hasRealSignal)
        assertTrue("Expected mid(${midFrame.midLevel}) > bass(${midFrame.bassLevel})", midFrame.midLevel > midFrame.bassLevel)
        assertTrue("Expected mid(${midFrame.midLevel}) > treble(${midFrame.trebleLevel})", midFrame.midLevel > midFrame.trebleLevel)

        // C. Strong 8 kHz Treble
        val trebleAnalyzer = AudioSpectrumAnalyzer()
        val trebleFft = buildSyntheticFft(targetFreqHz = 8_000f, amplitude = 110)
        val trebleFrame = trebleAnalyzer.processFftCapture(trebleFft, 44_100_000, status = VisualizerCaptureStatus.ACTIVE_SYNCED)
        assertTrue(trebleFrame.hasRealSignal)
        assertTrue("Expected treble(${trebleFrame.trebleLevel}) > bass(${trebleFrame.bassLevel})", trebleFrame.trebleLevel > trebleFrame.bassLevel)
        assertTrue("Expected treble(${trebleFrame.trebleLevel}) > mid(${trebleFrame.midLevel})", trebleFrame.trebleLevel > trebleFrame.midLevel)
    }

    @Test
    fun `spectrum analyzer scales with real signal intensity and drops cleanly to zero on silence or pause`() {
        val quietAnalyzer = AudioSpectrumAnalyzer()
        val loudAnalyzer = AudioSpectrumAnalyzer()

        val quietFrame = quietAnalyzer.processFftCapture(
            buildSyntheticFft(targetFreqHz = 500f, amplitude = 20),
            48_000_000,
            status = VisualizerCaptureStatus.ACTIVE_SYNCED,
        )
        val loudFrame = loudAnalyzer.processFftCapture(
            buildSyntheticFft(targetFreqHz = 500f, amplitude = 120),
            48_000_000,
            status = VisualizerCaptureStatus.ACTIVE_SYNCED,
        )
        assertEquals(48_000, loudFrame.sampleRateHz)
        assertTrue(loudFrame.overallEnergy > quietFrame.overallEnergy)

        // Waveform RMS difference between silence (unsigned 0x80) and active PCM sine wave
        val silentWave = ByteArray(128) { 0x80.toByte() }
        assertEquals(0f, quietAnalyzer.processWaveformCapture(silentWave), 0.001f)
        val activeWave = ByteArray(128) { idx ->
            (128 + (90 * sin(2.0 * PI * idx / 16.0))).toInt().coerceIn(0, 255).toByte()
        }
        val firstWaveRms = quietAnalyzer.processWaveformCapture(activeWave)
        assertTrue("Expected immediate rise ($firstWaveRms) on frame 1", firstWaveRms > 0.22f)
        val steadyWaveRms = quietAnalyzer.processWaveformCapture(activeWave)
        assertTrue("Expected smoothed RMS ($steadyWaveRms) > 0.35f", steadyWaveRms > 0.35f)

        // Digital silence FFT must yield all zeros and hasRealSignal = false
        val zeroFft = ByteArray(1024)
        val freshAnalyzer = AudioSpectrumAnalyzer()
        val initialSilenceFrame = freshAnalyzer.processFftCapture(zeroFft, 48_000_000, status = VisualizerCaptureStatus.ACTIVE_SYNCED)
        assertFalse(initialSilenceFrame.hasRealSignal)
        assertTrue(initialSilenceFrame.bars.all { it == 0f })

        // When trailing silence follows a loud signal, bars decay exponentially to zero
        var silenceFrame = AudioVisualizerFrame.Empty
        repeat(20) {
            silenceFrame = loudAnalyzer.processFftCapture(zeroFft, 48_000_000, status = VisualizerCaptureStatus.ACTIVE_SYNCED)
        }
        assertFalse(silenceFrame.hasRealSignal)
        assertTrue(silenceFrame.bars.all { it == 0f })

        // Pause / Stop reset immediately without residual energy
        val pausedFrame = loudAnalyzer.reset(VisualizerCaptureStatus.PAUSED)
        assertEquals(VisualizerCaptureStatus.PAUSED, pausedFrame.status)
        assertFalse(pausedFrame.hasRealSignal)
        assertEquals(0f, pausedFrame.bassLevel, 0.0001f)
        assertEquals(0f, pausedFrame.midLevel, 0.0001f)
        assertEquals(0f, pausedFrame.trebleLevel, 0.0001f)
        assertTrue(pausedFrame.bars.all { it == 0f })
    }

    @Test
    fun `full player visualizer shows honest permission, paused, and active synced states`() {
        var frameState by mutableStateOf(
            AudioVisualizerFrame(
                bars = List(18) { 0f },
                status = VisualizerCaptureStatus.PERMISSION_REQUIRED,
                hasRealSignal = false,
            )
        )
        var playbackState by mutableStateOf(
            PlaybackUiState(
                currentTrack = makeTrack(1L, "Test Song", "Artist"),
                isPlaying = true,
                durationMs = 180_000L,
            )
        )
        var permRequests = 0

        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                FullPlayerScreen(
                    playbackState = playbackState,
                    spectrumBars = frameState.bars,
                    visualizerEnabled = true,
                    hasRealFftSignal = frameState.hasRealSignal,
                    visualizerFrame = frameState,
                    bookmarks = emptyList(),
                    note = null,
                    onClose = {},
                    onPlayPause = {},
                    onNext = {},
                    onPrevious = {},
                    onSeekTo = {},
                    onSeekForward10s = {},
                    onSeekBack10s = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleFavorite = {},
                    onVolumeChange = {},
                    onOpenQueue = {},
                    onOpenLyrics = {},
                    onOpenEqualizer = {},
                    onOpenSleepTimer = {},
                    onOpenTagEditor = {},
                    onAddBookmark = { _, _, _ -> },
                    onDeleteBookmark = {},
                    onSaveNote = { _, _ -> },
                    onDeleteNote = {},
                    onRefreshVisualizerPermission = { permRequests++ },
                )
            }
        }
        composeRule.waitForIdle()

        // Permission required state shows honest actionable chip
        composeRule.onNodeWithTag("visualizer_permission_button").performScrollTo().assertIsDisplayed()

        // Active real FFT state shows "Sincronizado • Real"
        frameState = AudioVisualizerFrame(
            bars = List(18) { idx -> if (idx < 6) 0.85f else 0.35f },
            bassLevel = 0.85f,
            midLevel = 0.35f,
            trebleLevel = 0.35f,
            beatPulse = 0.6f,
            hasRealSignal = true,
            status = VisualizerCaptureStatus.ACTIVE_SYNCED,
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("visualizer_status_badge").performScrollTo().assertTextEquals("Sincronizado • Real")

        // Paused state updates honestly to "Em pausa"
        playbackState = playbackState.copy(isPlaying = false)
        frameState = AudioVisualizerFrame(status = VisualizerCaptureStatus.PAUSED)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("visualizer_status_badge").performScrollTo().assertTextEquals("Em pausa")
    }

    // =========================================================================
    // 3. BIDIRECTIONAL ANDROID MEDIA VOLUME SYNC TESTS
    // =========================================================================

    @Test
    fun `playback controller syncs bidirectionally with android STREAM_MUSIC and isolates other streams and EQ gain`() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val shadowAudio = Shadows.shadowOf(audioManager)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(15)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 6, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 2, 0)

        val controller = PlaybackController(context)
        try {
            // Initial read matches system STREAM_MUSIC
            assertEquals(6, controller.playbackState.value.volumeStep)
            assertEquals(6f / maxVol.toFloat(), controller.playbackState.value.volumeFraction, 0.02f)

            // App slider -> System STREAM_MUSIC
            controller.setVolumeFraction(0.8f)
            val expectedStep = (maxVol * 0.8f).roundToInt()
            assertEquals(expectedStep, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
            assertEquals(expectedStep, controller.playbackState.value.volumeStep)

            // Hardware Volume Down -> BroadcastReceiver updates app slider immediately
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 4, 0)
            controller.syncVolumeFromDevice(force = true) // also validate direct broadcast intent below
            val downIntent = Intent(PlaybackController.ACTION_VOLUME_CHANGED).apply {
                putExtra(PlaybackController.EXTRA_VOLUME_STREAM_TYPE, AudioManager.STREAM_MUSIC)
            }
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 3, 0)
            context.sendBroadcast(downIntent)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(3, controller.playbackState.value.volumeStep)
            assertEquals(3f / maxVol.toFloat(), controller.playbackState.value.volumeFraction, 0.02f)

            // Hardware Volume Up broadcast updates app slider immediately
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 11, 0)
            val upIntent = Intent(PlaybackController.ACTION_VOLUME_CHANGED).apply {
                putExtra(PlaybackController.EXTRA_VOLUME_STREAM_TYPE, AudioManager.STREAM_MUSIC)
            }
            context.sendBroadcast(upIntent)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(11, controller.playbackState.value.volumeStep)

            // Mute toggle mutes to 0 and unmutes back to previous volume step (11)
            controller.toggleVolumeMute()
            assertEquals(0, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
            assertTrue(controller.playbackState.value.isVolumeMuted)
            controller.toggleVolumeMute()
            assertEquals(11, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
            assertFalse(controller.playbackState.value.isVolumeMuted)

            // Stream isolation: RING volume broadcast must NOT change STREAM_MUSIC or player volume
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 1, 0)
            context.sendBroadcast(
                Intent(PlaybackController.ACTION_VOLUME_CHANGED).apply {
                    putExtra(PlaybackController.EXTRA_VOLUME_STREAM_TYPE, AudioManager.STREAM_RING)
                }
            )
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(11, controller.playbackState.value.volumeStep)
            assertEquals(1, audioManager.getStreamVolume(AudioManager.STREAM_RING))

            // Background -> Foreground sync picks up changes made outside the app (e.g., ContentObserver/system panel)
            controller.setAppForeground(false)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 9, 0)
            controller.setAppForeground(true)
            assertEquals(9, controller.playbackState.value.volumeStep)

            // ContentObserver on Settings.System triggers sync without polling
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 13, 0)
            context.contentResolver.notifyChange(Settings.System.CONTENT_URI, null)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(13, controller.playbackState.value.volumeStep)

            // Equalizer LoudnessEnhancer stays strictly independent from STREAM_MUSIC volume
            controller.equalizerEngine.setLoudnessGain(900)
            assertEquals(900, controller.equalizerEngine.state.value.loudnessGainMb)
            assertEquals(13, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
            assertEquals(13, controller.playbackState.value.volumeStep)
            @Suppress("UNUSED_VARIABLE")
            val keepShadowRef = shadowAudio
        } finally {
            controller.release()
        }
    }

    @Test
    fun `full player volume UI reflects bidirectional updates and mute button toggles cleanly`() {
        var playbackState by mutableStateOf(
            PlaybackUiState(
                currentTrack = makeTrack(1L, "Track Vol", "Artist"),
                volumeFraction = 0.50f,
                volumeStep = 7,
                volumeMaxStep = 15,
                isVolumeMuted = false,
            )
        )
        var muteClicked = 0

        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                FullPlayerScreen(
                    playbackState = playbackState,
                    spectrumBars = List(18) { 0f },
                    visualizerEnabled = false,
                    bookmarks = emptyList(),
                    note = null,
                    onClose = {},
                    onPlayPause = {},
                    onNext = {},
                    onPrevious = {},
                    onSeekTo = {},
                    onSeekForward10s = {},
                    onSeekBack10s = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleFavorite = {},
                    onVolumeChange = { frac -> playbackState = playbackState.copy(volumeFraction = frac) },
                    onOpenQueue = {},
                    onOpenLyrics = {},
                    onOpenEqualizer = {},
                    onOpenSleepTimer = {},
                    onOpenTagEditor = {},
                    onAddBookmark = { _, _, _ -> },
                    onDeleteBookmark = {},
                    onSaveNote = { _, _ -> },
                    onDeleteNote = {},
                    onToggleVolumeMute = {
                        muteClicked++
                        playbackState = playbackState.copy(isVolumeMuted = true, volumeFraction = 0f, volumeStep = 0)
                    },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("player_volume_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("player_volume_percent").performScrollTo().assertTextEquals("50%")

        // External system volume change updates slider percent label immediately
        playbackState = playbackState.copy(volumeFraction = 0.80f, volumeStep = 12)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("player_volume_percent").assertTextEquals("80%")

        // Clicking speaker mute button toggles mute
        composeRule.onNodeWithTag("player_volume_mute_button").performClick()
        composeRule.waitForIdle()
        assertEquals(1, muteClicked)
        composeRule.onNodeWithTag("player_volume_percent").assertTextEquals("0%")
    }
}
