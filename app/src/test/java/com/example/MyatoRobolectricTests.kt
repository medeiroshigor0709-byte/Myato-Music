package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.core.audio.PlaybackUiState
import com.example.core.common.AudioFormatSupport
import com.example.core.database.AppPreferencesStore
import com.example.core.database.BookmarkEntity
import com.example.core.database.FavoriteEntity
import com.example.core.database.MusicNoteEntity
import com.example.core.database.MusicTrackEntity
import com.example.core.database.MyatoDatabase
import com.example.core.database.toEntity
import com.example.core.designsystem.BuiltInThemes
import com.example.core.designsystem.MyatoMusicTheme
import com.example.core.metadata.TagEditPayload
import com.example.core.metadata.TagEditorEngine
import com.example.core.storage.BackupManager
import com.example.data.repository.PlaylistRepository
import com.example.data.repository.QueueRepository
import com.example.data.repository.ThemeRepository
import com.example.domain.models.MusicTrack
import com.example.domain.models.PlaybackQueue
import com.example.feature.player.FullPlayerScreen
import com.example.feature.queues.QueuesScreen
import com.example.feature.themes.ThemeCenterScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MyatoRobolectricTests {

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

    @Test
    fun `manifest declares zero internet permission for strict offline privacy`() {
        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val permissions = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertFalse(
            "INTERNET permission must NOT be present in offline MYATO MUSIC",
            permissions.contains(Manifest.permission.INTERNET),
        )
        assertTrue(permissions.contains(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK))
    }

    @Test
    fun `database persists tracks, playlists, multi-queues, bookmarks, notes, stats, and themes`() = runBlocking {
        val track1 = MusicTrackEntity(
            id = 101L,
            mediaStoreId = 101L,
            title = "Midnight Drive",
            artist = "Synthwave",
            album = "Nightfall",
            albumId = 10L,
            artistId = 20L,
            durationMs = 210_000L,
            sizeBytes = 5_000_000L,
            filePath = "/storage/emulated/0/Music/midnight.mp3",
            folderPath = "/storage/emulated/0/Music",
            contentUri = "content://media/external/audio/media/101",
        )
        val track2 = track1.copy(id = 102L, mediaStoreId = 102L, title = "Orange Horizon")
        db.trackDao().upsertTracks(listOf(track1, track2))

        // Playlist operations
        val plRepo = PlaylistRepository(context, db)
        val plId = plRepo.createPlaylist("Treino", "Pesado")
        plRepo.addTracksToPlaylist(plId, listOf(101L, 102L))
        val plTracks = plRepo.observePlaylistTrackIds(plId).first()
        assertEquals(listOf(101L, 102L), plTracks)

        // Multi-Queue operations (including same-name queues & duplicate & merge)
        val qRepo = QueueRepository(db)
        val q1 = qRepo.createQueue("Foco", listOf(101L))
        val q2 = qRepo.createQueue("Foco", listOf(102L)) // identical name allowed
        assertTrue(q1 != q2)
        qRepo.mergeQueues(q2, q1)
        val mergedIds = qRepo.observeQueueTrackIds(q1).first()
        assertEquals(listOf(101L, 102L), mergedIds)

        // Favorites, Bookmarks, Notes, Stats
        db.engagementDao().addFavorite(FavoriteEntity(101L))
        assertTrue(db.engagementDao().isFavorite(101L))
        db.engagementDao().insertBookmark(BookmarkEntity(trackId = 101L, label = "Solo", positionMs = 95_000L))
        assertEquals(1, db.engagementDao().observeBookmarksForTrack(101L).first().size)
        db.engagementDao().upsertNote(MusicNoteEntity(trackId = 101L, content = "Afinação Drop D"))
        assertEquals("Afinação Drop D", db.engagementDao().observeNoteForTrack(101L).first()?.content)
        db.engagementDao().recordPlayback(101L, "Midnight Drive", "Synthwave", 180_000L)
        assertEquals(1, db.engagementDao().getStatsForTrack(101L)?.playCount)

        // Backup roundtrip
        db.themeDao().insertMissingBuiltInThemes(BuiltInThemes.all.map { it.toEntity() })
        val customTheme = BuiltInThemes.MidnightOrange.copy(id = "custom_1", name = "Meu Tema", isBuiltIn = false)
        db.themeDao().upsertTheme(customTheme.toEntity())

        val backupManager = BackupManager(context, db, prefs)
        val json = backupManager.createBackupJson()
        val (preview, bundle) = backupManager.previewBackupJson(json).getOrThrow()
        assertEquals(1, preview.playlistsCount)
        assertEquals(2, preview.queuesCount)
        assertEquals(1, preview.favoritesCount)
        assertEquals(1, preview.customThemesCount)

        val restoreResult = backupManager.restoreBackup(bundle, preserveExistingData = false)
        assertTrue(restoreResult.isSuccess)

        val csv = backupManager.buildTopTracksCsv()
        assertTrue(csv.contains("Midnight Drive"))
    }

    @Test
    fun `tag editor writes real id3v23 tag into mp3 file`() {
        val tmp = File.createTempFile("myato_id3_", ".mp3")
        try {
            tmp.writeBytes(byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64.toByte(), 10, 20, 30, 40))
            val engine = TagEditorEngine(context, db)
            engine.writeId3v23TagToMp3(
                tmp,
                TagEditPayload(
                    title = "Aurora",
                    artist = "Myato Band",
                    album = "Studio I",
                    albumArtist = "Myato Band",
                    genre = "Rock",
                    year = 2026,
                    trackNumber = 3,
                    discNumber = 1,
                    composer = "Myato",
                    comments = "Offline",
                    embeddedLyrics = "[00:05.00]Aurora",
                ),
            )
            val bytes = tmp.readBytes()
            assertEquals('I'.code.toByte(), bytes[0])
            assertEquals('D'.code.toByte(), bytes[1])
            assertEquals('3'.code.toByte(), bytes[2])
            assertEquals(3.toByte(), bytes[3])
            assertTrue(bytes.size > 40)
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun `theme repository exports and imports custom themes`() = runBlocking {
        val repo = ThemeRepository(db, prefs)
        val exported = repo.exportThemeJson(BuiltInThemes.Cyberpunk)
        val imported = repo.importThemeJson(exported).getOrNull()
        assertNotNull(imported)
        assertFalse(imported!!.isBuiltIn)
        assertEquals("Cyberpunk", imported.name)
    }

    @Test
    fun `device audio format capability check covers all 11 formats`() {
        val caps = AudioFormatSupport.detectDeviceCapabilities()
        assertEquals(11, caps.size)
        val exts = caps.map { it.extension }.toSet()
        assertTrue(exts.containsAll(listOf("MP3", "M4A", "WMA", "FLAC", "OPUS", "AAC", "ALAC", "APE", "DSF", "WAV", "OGG")))
    }

    @Test
    fun `theme center ui shows themes and opens live preview dialog before applying`() {
        var selectedThemeId = ""
        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                ThemeCenterScreen(
                    themes = BuiltInThemes.all,
                    activeThemeId = BuiltInThemes.MidnightOrange.id,
                    onSelectTheme = { selectedThemeId = it },
                    onOpenEditor = {},
                    onDuplicateTheme = { _, _ -> },
                    onDeleteCustomTheme = {},
                    onImportThemeJson = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("theme_center_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_grid").performScrollToNode(hasText("Neon Galaxy"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Neon Galaxy").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Pré-visualização: Neon Galaxy").assertIsDisplayed()
        composeRule.onNodeWithText("Aplicar Tema").performClick()
        composeRule.waitForIdle()
        assertEquals("neon_galaxy", selectedThemeId)
    }

    @Test
    fun `full player screen renders track controls and triggers playback actions`() {
        var playPauseClicks = 0
        var nextClicks = 0
        var favClicks = 0
        val sampleTrack = MusicTrack(
            id = 501L,
            mediaStoreId = 501L,
            title = "Laranja Mecânica",
            artist = "Estúdio MYATO",
            album = "Offline Sessions",
            albumId = 1L,
            artistId = 1L,
            durationMs = 240_000L,
            sizeBytes = 6_000_000L,
            filePath = "/storage/emulated/0/Music/laranja.flac",
            folderPath = "/storage/emulated/0/Music",
            contentUri = "content://media/external/audio/media/501",
            formatExtension = "FLAC",
        )
        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                FullPlayerScreen(
                    playbackState = PlaybackUiState(
                        currentTrack = sampleTrack,
                        queueTracks = listOf(sampleTrack),
                        currentIndex = 0,
                        isPlaying = true,
                        positionMs = 60_000L,
                        durationMs = 240_000L,
                    ),
                    spectrumBars = listOf(0.4f, 0.7f, 0.9f, 0.5f),
                    visualizerEnabled = true,
                    bookmarks = emptyList(),
                    note = null,
                    onClose = {},
                    onPlayPause = { playPauseClicks++ },
                    onNext = { nextClicks++ },
                    onPrevious = {},
                    onSeekTo = {},
                    onSeekForward10s = {},
                    onSeekBack10s = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleFavorite = { favClicks++ },
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
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("full_player_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Laranja Mecânica").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("player_play_pause_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("player_next_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("player_favorite_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(1, playPauseClicks)
        assertEquals(1, nextClicks)
        assertEquals(1, favClicks)
    }

    @Test
    fun `queues screen displays independent queues including identical names`() {
        val queues = listOf(
            PlaybackQueue(id = 1L, name = "Principal", isActive = true, trackCount = 2),
            PlaybackQueue(id = 2L, name = "Corrida", isActive = false, trackCount = 5),
            PlaybackQueue(id = 3L, name = "Corrida", isActive = false, trackCount = 3),
        )
        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                QueuesScreen(
                    queues = queues,
                    allTracks = emptyList(),
                    playbackState = PlaybackUiState(activeQueue = queues.first()),
                    onBack = {},
                    onSwitchQueue = {},
                    onCreateQueue = { _, _ -> },
                    onRenameQueue = { _, _ -> },
                    onDuplicateQueue = {},
                    onDeleteQueue = {},
                    onMergeQueues = { _, _ -> },
                    onReplaceQueue = { _, _ -> },
                    onMoveActiveTrack = { _, _ -> },
                    onRemoveActiveTrack = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("queues_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("create_queue_button").assertIsDisplayed()
        composeRule.onNodeWithText("Múltiplas Filas (3)").assertIsDisplayed()
    }
}
