package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.core.audio.EqualizerEngine
import com.example.core.audio.PlaybackUiState
import com.example.core.database.AppPreferencesStore
import com.example.core.database.AppUserPreferences
import com.example.core.database.MyatoDatabase
import com.example.core.designsystem.AlbumArtPaletteExtractor
import com.example.core.designsystem.BuiltInThemes
import com.example.core.designsystem.LocalMotionSettings
import com.example.core.designsystem.MyatoMusicTheme
import com.example.core.designsystem.SlantedStripesTransitionOverlay
import com.example.core.designsystem.toExtendedColors
import com.example.domain.models.FontApplyTarget
import com.example.domain.models.MotionSettingsState
import com.example.domain.models.MusicTrack
import com.example.domain.models.PlayerVisualBackgroundStyle
import com.example.feature.player.FullPlayerScreen
import com.example.feature.player.PlayerVisualStyleSelectorSection
import com.example.feature.statistics.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MyatoVisualAnimationsTests {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: MyatoDatabase
    private lateinit var prefs: AppPreferencesStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = MyatoDatabase.createInMemoryForTest(context)
        prefs = AppPreferencesStore(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sampleTrack(id: Long = 101L, artUri: String? = null): MusicTrack {
        return MusicTrack(
            id = id,
            mediaStoreId = id,
            title = "Midnight Pulse",
            artist = "MYATO Artist",
            album = "Neon Horizon",
            albumId = 7L,
            artistId = 17L,
            durationMs = 215_000L,
            filePath = "/storage/emulated/0/Music/midnight_pulse.flac",
            folderPath = "/storage/emulated/0/Music",
            contentUri = "content://media/external/audio/media/$id",
            albumArtUri = artUri,
            trackNumber = 1,
            year = 2025,
            genre = "Synthwave",
            formatExtension = "FLAC",
            mimeType = "audio/flac",
            bitrateKbps = 960,
            sampleRateHz = 44100,
            sizeBytes = 24_000_000L,
            dateAddedSec = 1_700_000_000L,
            isFavorite = false,
        )
    }

    @Test
    fun `slanted stripes transition triggers on route switch, settles cleanly, and unmounts`() {
        composeRule.mainClock.autoAdvance = false
        var currentRoute by mutableStateOf("home")

        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SlantedStripesTransitionOverlay(
                        routeKey = currentRoute,
                        enabled = true,
                        intensity = 1.0f,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.waitForIdle()
        // Initial launch must not show a transition overlay
        composeRule.onNodeWithTag("slanted_stripes_overlay").assertDoesNotExist()

        // Switch tab: Início -> Playlists
        currentRoute = "playlists"
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(80)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("slanted_stripes_overlay").assertExists()

        // Rapid consecutive switches: Playlists -> Álbuns -> Biblioteca
        currentRoute = "albums"
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(50)
        currentRoute = "library"
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(120)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("slanted_stripes_overlay").assertExists()

        // After 400ms+ completion, overlay must unmount cleanly so it never blocks UI
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("slanted_stripes_overlay").assertDoesNotExist()
    }

    @Test
    fun `slanted stripes transition is disabled when enabled flag is false`() {
        composeRule.mainClock.autoAdvance = false
        var currentRoute by mutableStateOf("home")

        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                SlantedStripesTransitionOverlay(
                    routeKey = currentRoute,
                    enabled = false,
                    intensity = 1.0f,
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(50)
        currentRoute = "playlists"
        composeRule.mainClock.advanceTimeBy(120)
        composeRule.onNodeWithTag("slanted_stripes_overlay").assertDoesNotExist()
        composeRule.mainClock.autoAdvance = true
    }

    @Test
    fun `motion settings state resolves effective style, intensity, and low battery behavior accurately`() {
        val normal = MotionSettingsState(
            playerVisualStyle = PlayerVisualBackgroundStyle.AURORA_LIQUID_WAVES,
            effectIntensity = 1.2f,
            animationsDisabled = false,
            reduceMotion = false,
            reduceEffectsOnLowBattery = true,
            systemReducedMotion = false,
            isLowBatteryOrPowerSave = false,
            isAppInForeground = true,
        )
        assertTrue(normal.isAnimationsActive)
        assertFalse(normal.isReducedMotionActive)
        assertTrue(normal.shouldAnimateTabTransitions)
        assertEquals(PlayerVisualBackgroundStyle.AURORA_LIQUID_WAVES, normal.effectiveStyle)
        assertEquals(1.2f, normal.effectiveIntensity, 0.001f)

        val lowBattery = normal.copy(isLowBatteryOrPowerSave = true)
        assertTrue(lowBattery.isReducedMotionActive)
        assertFalse(lowBattery.shouldAnimateTabTransitions)
        assertEquals(PlayerVisualBackgroundStyle.MINIMAL_MOTION, lowBattery.effectiveStyle)
        assertTrue(lowBattery.effectiveIntensity < 0.65f)

        val disabled = normal.copy(animationsDisabled = true)
        assertFalse(disabled.isAnimationsActive)
        assertEquals(PlayerVisualBackgroundStyle.MINIMAL_MOTION, disabled.effectiveStyle)
        assertEquals(0f, disabled.effectiveIntensity, 0.001f)

        val backgrounded = normal.copy(isAppInForeground = false)
        assertFalse(backgrounded.isAnimationsActive)
    }

    @Test
    fun `preferences store persists visual styles and motion toggles while preserving custom fonts`(): Unit = runBlocking {
        prefs.setFontSelection("orbitron", FontApplyTarget.DECORATIVE)
        prefs.toggleFavoriteFont("orbitron")

        prefs.setPlayerVisualStyle(PlayerVisualBackgroundStyle.AURORA_LIQUID_WAVES)
        prefs.setVisualizerAnimationIntensity(1.5f)
        prefs.setAnimationsDisabled(true)
        prefs.setReduceMotion(true)
        prefs.setReduceEffectsOnLowBattery(false)

        val saved = prefs.preferencesFlow.first()
        assertEquals(PlayerVisualBackgroundStyle.AURORA_LIQUID_WAVES, saved.playerVisualStyle)
        assertEquals(1.5f, saved.visualizerAnimationIntensity, 0.001f)
        assertTrue(saved.animationsDisabled)
        assertTrue(saved.reduceMotion)
        assertFalse(saved.reduceEffectsOnLowBattery)

        // Font preferences remain completely untouched
        assertEquals("orbitron", saved.activeDecorativeFontId)
        assertTrue("orbitron" in saved.favoriteFontIds)
    }

    @Test
    fun `album art palette extractor returns theme fallback without cover and extracts valid palette with cover`(): Unit = runBlocking {
        val fallbackThemeColors = BuiltInThemes.MidnightOrange.toExtendedColors()

        // Case 1: no cover -> clean fallback to active theme colors
        val noCoverPalette = AlbumArtPaletteExtractor.extractAuroraPalette(
            context = context,
            trackId = 999L,
            artUri = null,
            filePath = null,
            fallbackColors = fallbackThemeColors,
        )
        assertFalse(noCoverPalette.hasArtwork)
        assertEquals(fallbackThemeColors.primary, noCoverPalette.primary)
        assertEquals(fallbackThemeColors.secondary, noCoverPalette.secondary)

        // Case 2: real local bitmap artwork -> extracts saturated colors & caches result
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.rgb(255, 64, 129))
        val coverFile = File(context.cacheDir, "test_aurora_cover.png")
        FileOutputStream(coverFile).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bmp.recycle()

        val artPalette = AlbumArtPaletteExtractor.extractAuroraPalette(
            context = context,
            trackId = 888L,
            artUri = Uri.fromFile(coverFile).toString(),
            filePath = null,
            fallbackColors = fallbackThemeColors,
        )
        assertTrue(artPalette.hasArtwork)
        assertTrue(artPalette.primary.alpha > 0.9f)
        coverFile.delete()
        Unit
    }

    @Test
    fun `full player switches across all three visual background styles and playback states without errors`() {
        var motionState by mutableStateOf(
            MotionSettingsState(
                playerVisualStyle = PlayerVisualBackgroundStyle.AUDIO_SPECTRUM,
                effectIntensity = 1.0f,
            )
        )
        var playbackState by mutableStateOf(
            PlaybackUiState(
                currentTrack = sampleTrack(),
                isPlaying = true,
                positionMs = 45_000L,
                durationMs = 215_000L,
            )
        )

        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                CompositionLocalProvider(LocalMotionSettings provides motionState) {
                    FullPlayerScreen(
                        playbackState = playbackState,
                        spectrumBars = List(16) { idx -> 0.25f + (idx % 4) * 0.18f },
                        visualizerEnabled = true,
                        hasRealFftSignal = true,
                        playerVisualStyle = motionState.playerVisualStyle,
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
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("full_player_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("player_bg_audio_spectrum").assertExists()

        // Switch to Aurora + Liquid Waves
        motionState = motionState.copy(playerVisualStyle = PlayerVisualBackgroundStyle.AURORA_LIQUID_WAVES)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("player_bg_aurora_waves").assertExists()

        // Pause track and switch to Minimal Motion
        playbackState = playbackState.copy(isPlaying = false)
        motionState = motionState.copy(playerVisualStyle = PlayerVisualBackgroundStyle.MINIMAL_MOTION)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("player_bg_minimal_motion").assertExists()

        // Low battery force-reduces Aurora to Minimal Motion
        motionState = MotionSettingsState(
            playerVisualStyle = PlayerVisualBackgroundStyle.AURORA_LIQUID_WAVES,
            reduceEffectsOnLowBattery = true,
            isLowBatteryOrPowerSave = true,
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("player_bg_minimal_motion").assertExists()

        // Stop playback (null track) safely
        playbackState = PlaybackUiState(currentTrack = null, isPlaying = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("full_player_screen").assertIsDisplayed()
    }

    @Test
    fun `visual style selector renders all cards, intensity controls, and toggles cleanly`() {
        var selectedStyle by mutableStateOf(PlayerVisualBackgroundStyle.AUDIO_SPECTRUM)
        var intensity by mutableStateOf(1.0f)
        var disabled by mutableStateOf(false)
        var reduced by mutableStateOf(false)
        var lowBattery by mutableStateOf(true)

        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    PlayerVisualStyleSelectorSection(
                        selectedStyle = selectedStyle,
                        effectIntensity = intensity,
                        animationsDisabled = disabled,
                        reduceMotion = reduced,
                        reduceEffectsOnLowBattery = lowBattery,
                        onSelectStyle = { selectedStyle = it },
                        onIntensityChange = { intensity = it },
                        onSetAnimationsDisabled = { disabled = it },
                        onSetReduceMotion = { reduced = it },
                        onSetReduceEffectsOnLowBattery = { lowBattery = it },
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("visual_style_audio_spectrum").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("visual_style_aurora_liquid_waves").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("visual_style_minimal_motion").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("slider_effect_intensity").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("visual_style_aurora_liquid_waves").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(PlayerVisualBackgroundStyle.AURORA_LIQUID_WAVES, selectedStyle)

        composeRule.onNodeWithTag("toggle_disable_animations").performScrollTo().performClick()
        composeRule.onNodeWithTag("toggle_reduce_motion").performScrollTo().performClick()
        composeRule.onNodeWithTag("toggle_low_battery_effects").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(disabled)
        assertTrue(reduced)
        assertFalse(lowBattery)
    }

    @Test
    fun `settings screen opens visual style sheet and switches player style without affecting font row`() {
        var prefsState by mutableStateOf(AppUserPreferences())

        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                SettingsScreen(
                    preferences = prefsState,
                    capabilities = emptyList(),
                    onSetGapless = {},
                    onSetBatterySaverAnim = {},
                    onSetNotificationPrefs = { _, _ -> },
                    onSetHeadsetMappings = { _, _, _, _ -> },
                    onOpenPermissions = {},
                    onOpenBackup = {},
                    onOpenWidgets = {},
                    onOpenAbout = {},
                    onSetPlayerVisualStyle = { prefsState = prefsState.copy(playerVisualStyle = it) },
                    onSetEffectIntensity = { prefsState = prefsState.copy(visualizerAnimationIntensity = it) },
                    onSetAnimationsDisabled = { prefsState = prefsState.copy(animationsDisabled = it) },
                    onSetReduceMotion = { prefsState = prefsState.copy(reduceMotion = it) },
                    onSetReduceEffectsOnLowBattery = { prefsState = prefsState.copy(reduceEffectsOnLowBattery = it) },
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_visual_style_row").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("visual_style_audio_spectrum").assertExists()
        composeRule.onNodeWithTag("visual_style_minimal_motion").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(PlayerVisualBackgroundStyle.MINIMAL_MOTION, prefsState.playerVisualStyle)
    }

    @Test
    fun `player animated background renders safely across all 50 themes and compact screen width`() {
        assertEquals(50, BuiltInThemes.all.size)
        var themeIndex by mutableStateOf(0)

        composeRule.setContent {
            val activeTheme = BuiltInThemes.all[themeIndex]
            MyatoMusicTheme(activeTheme = activeTheme) {
                Box(modifier = Modifier.width(320.dp)) {
                    FullPlayerScreen(
                        playbackState = PlaybackUiState(currentTrack = sampleTrack(), isPlaying = true),
                        spectrumBars = List(16) { 0.5f },
                        visualizerEnabled = true,
                        playerVisualStyle = PlayerVisualBackgroundStyle.AURORA_LIQUID_WAVES,
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
                    )
                }
            }
        }

        BuiltInThemes.all.indices.forEach { idx ->
            themeIndex = idx
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("full_player_screen").assertIsDisplayed()
        }
    }

    @Test
    fun `equalizer engine handles foreground background lifecycle transitions safely without audio permission`() {
        val engine = EqualizerEngine(context, db)
        engine.startVisualizerIfPermitted()
        assertFalse(engine.hasRealFftSignal.value)

        engine.setVisualizerAppForeground(false)
        assertFalse(engine.hasRealFftSignal.value)
        assertTrue(engine.spectrumBars.value.all { it == 0f })

        engine.setVisualizerAppForeground(true)
        assertFalse(engine.hasRealFftSignal.value)
        engine.releaseEffects()
    }
}
