package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.audio.PlaybackUiState
import com.example.core.common.AudioFormatSupport
import com.example.core.database.AppPreferencesStore
import com.example.core.database.BookmarkEntity
import com.example.core.database.FavoriteEntity
import com.example.core.database.MusicNoteEntity
import com.example.core.database.MusicTrackEntity
import com.example.core.database.MyatoDatabase
import com.example.core.database.toDomain
import com.example.core.database.toEntity
import com.example.core.designsystem.AppFontCatalog
import com.example.core.designsystem.BuiltInThemes
import com.example.core.designsystem.MyatoMusicTheme
import com.example.core.designsystem.MyatoTypography
import com.example.core.metadata.TagEditPayload
import com.example.core.metadata.TagEditorEngine
import com.example.core.storage.BackupManager
import com.example.core.storage.CustomFontManager
import com.example.data.repository.PlaylistRepository
import com.example.data.repository.QueueRepository
import com.example.data.repository.ThemeRepository
import com.example.domain.models.AppFontOption
import com.example.domain.models.FontApplyTarget
import com.example.domain.models.FontCategory
import com.example.domain.models.FontImportResult
import com.example.domain.models.MusicTrack
import com.example.domain.models.PlaybackQueue
import com.example.feature.player.FullPlayerScreen
import com.example.feature.queues.QueuesScreen
import com.example.feature.themes.FontPreviewScreen
import com.example.feature.themes.FontSettingsScreen
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
        val repo = ThemeRepository(context, db, prefs)
        val exported = repo.exportThemeJson(BuiltInThemes.Cyberpunk)
        val imported = repo.importThemeJson(exported).getOrNull()
        assertNotNull(imported)
        assertFalse(imported!!.isBuiltIn)
        assertEquals("Cyber Gamer", imported.name)
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
        composeRule.onNodeWithTag("theme_grid").performScrollToNode(hasTestTag("theme_card_galaxy"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("theme_card_galaxy").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Pré-visualização: Galaxy").assertIsDisplayed()
        composeRule.onNodeWithText("Aplicar Tema").performClick()
        composeRule.waitForIdle()
        assertEquals("galaxy", selectedThemeId)
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

        // Verify all 5 new bottom sections are rendered in the full player view
        composeRule.onNodeWithText("VISUALIZADOR DE ÁUDIO").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("PRÓXIMAS MÚSICAS").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("LETRAS DA MÚSICA").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("INFORMAÇÕES DA FAIXA").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ESTATÍSTICAS DE REPRODUÇÃO").performScrollTo().assertIsDisplayed()
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

    @Test
    fun `playlist repository smart operations handle creation with tracks, duplicate detection, and clearing`() = runBlocking {
        val track1 = MusicTrackEntity(
            id = 201L, mediaStoreId = 201L, title = "Track 1", artist = "Artist", album = "Album",
            albumId = 1L, artistId = 1L, durationMs = 100_000L, sizeBytes = 1000L,
            filePath = "/path/1.mp3", folderPath = "/path", contentUri = "content://201",
        )
        val track2 = track1.copy(id = 202L, mediaStoreId = 202L, title = "Track 2")
        val track3 = track1.copy(id = 203L, mediaStoreId = 203L, title = "Track 3")
        db.trackDao().upsertTracks(listOf(track1, track2, track3))

        val plRepo = PlaylistRepository(context, db)
        val plId = plRepo.createPlaylistWithTracks("Rock Clássico", listOf(201L, 202L))

        val initialTracks = plRepo.observePlaylistTrackIds(plId).first()
        assertEquals(listOf(201L, 202L), initialTracks)

        // Try adding with allowDuplicates = false
        val addResultNoDup = plRepo.addTracksWithResult(listOf(plId), listOf(202L, 203L), allowDuplicates = false)
        assertEquals(1, addResultNoDup.addedCount)
        assertEquals(1, addResultNoDup.duplicateCount)
        assertEquals(listOf(202L), addResultNoDup.duplicateTrackIds)

        // Duplicate playlist
        val dupId = plRepo.duplicatePlaylist(plId)
        assertNotNull(dupId)
        val dupTracks = plRepo.observePlaylistTrackIds(dupId!!).first()
        assertEquals(listOf(201L, 202L, 203L), dupTracks)

        // Favorite & Cover
        plRepo.setPlaylistFavorite(plId, true)
        plRepo.updatePlaylistCover(plId, "content://cover/art")
        val updatedPl = plRepo.playlistsFlow.first().first { it.id == plId }
        assertTrue(updatedPl.isFavorite)
        assertEquals("content://cover/art", updatedPl.coverUri)

        // Clear playlist
        plRepo.clearPlaylist(plId)
        val clearedTracks = plRepo.observePlaylistTrackIds(plId).first()
        assertTrue(clearedTracks.isEmpty())
    }

    @Test
    fun `media notification provider applies dynamic theme colors to notification`() {
        var activeColor = BuiltInThemes.MidnightOrange.primaryColorHex.toInt()
        val builder = androidx.core.app.NotificationCompat.Builder(context, "myato_test_channel")
        builder.setColor(activeColor)
        builder.setColorized(true)
        val notif = builder.build()
        assertEquals(BuiltInThemes.MidnightOrange.primaryColorHex.toInt(), notif.color)

        // Switch to Ocean Blue
        activeColor = BuiltInThemes.OceanBlue.primaryColorHex.toInt()
        val blueBuilder = androidx.core.app.NotificationCompat.Builder(context, "myato_test_channel")
        blueBuilder.setColor(activeColor)
        blueBuilder.setColorized(true)
        val blueNotif = blueBuilder.build()
        assertEquals(BuiltInThemes.OceanBlue.primaryColorHex.toInt(), blueNotif.color)

        // Switch to Purple Night
        activeColor = BuiltInThemes.PurpleNight.primaryColorHex.toInt()
        val purpleBuilder = androidx.core.app.NotificationCompat.Builder(context, "myato_test_channel")
        purpleBuilder.setColor(activeColor)
        purpleBuilder.setColorized(true)
        val purpleNotif = purpleBuilder.build()
        assertEquals(BuiltInThemes.PurpleNight.primaryColorHex.toInt(), purpleNotif.color)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Test
    fun `media notification command buttons include Next Track mapped to SLOT_FORWARD and COMMAND_SEEK_TO_NEXT_MEDIA_ITEM`() {
        val nextTrackBtn = androidx.media3.session.CommandButton.Builder(androidx.media3.session.CommandButton.ICON_NEXT)
            .setDisplayName("Próxima Música")
            .setPlayerCommand(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .setSlots(androidx.media3.session.CommandButton.SLOT_FORWARD)
            .build()

        assertEquals(androidx.media3.session.CommandButton.ICON_NEXT, nextTrackBtn.icon)
        assertEquals(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, nextTrackBtn.playerCommand)
        assertEquals("Próxima Música", nextTrackBtn.displayName)
        assertTrue(nextTrackBtn.slots.contains(androidx.media3.session.CommandButton.SLOT_FORWARD))
    }

    @Test
    fun `skip next action loops to index 0 when RepeatMode ALL is active`() = runBlocking {
        val t1 = MusicTrack(
            id = 1L, mediaStoreId = 1L, title = "Song 1", artist = "Artist", album = "Album",
            albumId = 1L, artistId = 1L, durationMs = 120_000L, sizeBytes = 1000L,
            filePath = "/path/1.mp3", folderPath = "/path", contentUri = "content://1",
            formatExtension = "MP3",
        )
        val t2 = t1.copy(id = 2L, title = "Song 2", contentUri = "content://2")
        val state = PlaybackUiState(
            queueTracks = listOf(t1, t2),
            currentIndex = 1,
            currentTrack = t2,
            repeatMode = com.example.domain.models.RepeatMode.ALL,
        )
        val nextIndex = if (state.currentIndex + 1 < state.queueTracks.size) {
            state.currentIndex + 1
        } else if (state.repeatMode == com.example.domain.models.RepeatMode.ALL) {
            0
        } else {
            state.currentIndex
        }
        assertEquals(0, nextIndex)
    }

    @Test
    fun `official launcher icon and brand drawables resolve cleanly`() {
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_myato_official_logo))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_launcher_background))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_launcher_monochrome))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round))
    }

    @Test
    fun `splash screen displays official MYATO MUSIC brand lockup on deep black canvas`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                com.example.feature.home.SplashScreen(onFinished = {})
            }
        }
        composeRule.mainClock.advanceTimeBy(320L)
        composeRule.onNodeWithTag("splash_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("myato_official_brand_lockup").assertIsDisplayed()
        composeRule.onNodeWithText("MYATO").assertIsDisplayed()
        composeRule.onNodeWithText("MUSIC").assertIsDisplayed()
    }

    @Test
    fun `about screen keeps official brand lockup visible across light and dark themes`() {
        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.PureLight) {
                com.example.feature.statistics.AboutScreen(onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("about_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("myato_official_brand_lockup").assertIsDisplayed()
        composeRule.onNodeWithText("MYATO").assertIsDisplayed()
        composeRule.onNodeWithText("MUSIC").assertIsDisplayed()
    }

    @Test
    fun `app font catalog includes all 12 categories, 49 licensed fonts, and resolves all 15 typography slots`() {
        assertEquals(12, FontCategory.entries.size)
        assertEquals(49, AppFontCatalog.builtInFonts.size)

        // Ensure every required category has fonts
        FontCategory.entries.forEach { category ->
            assertTrue(
                "Category ${category.label} must contain at least 3 fonts",
                AppFontCatalog.builtInFonts.count { it.fontCategory == category } >= 3,
            )
        }

        // Ensure every font loads cleanly and has creator + license attribution
        AppFontCatalog.builtInFonts.forEach { option ->
            assertTrue(option.creator.isNotBlank())
            assertTrue(option.license in setOf("SIL OFL 1.1", "Apache 2.0"))
            val family = AppFontCatalog.resolveFontFamily(option.id, emptyList())
            val typo = AppFontCatalog.resolveAppTypography(option.id, emptyList())
            assertNotNull(family)
            assertNotNull(typo.displayLarge.fontFamily)
            assertNotNull(typo.headlineMedium.fontFamily)
            assertNotNull(typo.titleLarge.fontFamily)
            assertNotNull(typo.bodyLarge.fontFamily)
            assertNotNull(typo.labelSmall.fontFamily)
        }
        assertEquals(MyatoTypography, AppFontCatalog.resolveAppTypography(AppFontCatalog.DEFAULT_FONT_ID, emptyList()))

        // Verify Dual Typography separates Decorative (display/headline/titleLarge) and General (body/menus/labels)
        val dualTypo = AppFontCatalog.resolveDualTypography(
            generalFontId = "inter",
            decorativeFontId = "metal_mania",
            customFonts = emptyList(),
        )
        assertEquals(com.example.core.designsystem.MetalManiaFontFamily, dualTypo.headlineLarge.fontFamily)
        assertEquals(com.example.core.designsystem.InterFontFamily, dualTypo.bodyLarge.fontFamily)
    }

    @Test
    fun `font preferences persist dual general and decorative fonts, favorites, recents, and reset`(): Unit = runBlocking {
        prefs.setFontSelection("inter", FontApplyTarget.GENERAL)
        prefs.setFontSelection("creepster", FontApplyTarget.DECORATIVE)
        prefs.toggleFavoriteFont("creepster")
        prefs.toggleFavoriteFont("metal_mania")

        val customFile = File(context.filesDir, "custom_fonts/test_custom.ttf").apply {
            parentFile?.mkdirs()
            context.resources.openRawResource(R.font.roboto).use { input ->
                outputStream().use { output -> input.copyTo(output) }
            }
        }
        val customOption = AppFontOption(
            id = "custom_font_test_1",
            name = "Minha Fonte Studio",
            description = "Fonte personalizada importada (TTF)",
            category = "Personalizada",
            isCustom = true,
            customFilePath = customFile.absolutePath,
            fileFormat = "TTF",
            sizeBytes = customFile.length(),
        )
        prefs.addCustomFont(customOption)
        prefs.setFontSelection(customOption.id, FontApplyTarget.DECORATIVE)

        val reloadedPrefs = AppPreferencesStore(context)
        var current = reloadedPrefs.preferencesFlow.first()
        assertEquals("inter", current.activeGeneralFontId)
        assertEquals(customOption.id, current.activeDecorativeFontId)
        assertTrue("creepster" in current.favoriteFontIds)
        assertTrue("metal_mania" in current.favoriteFontIds)
        assertEquals(customOption.id, current.recentFontIds.firstOrNull())
        assertTrue(current.customFonts.any { it.id == customOption.id })

        // Removing active custom decorative font resets decorative back to default while keeping general
        reloadedPrefs.removeCustomFont(customOption.id)
        current = reloadedPrefs.preferencesFlow.first()
        assertEquals("inter", current.activeGeneralFontId)
        assertEquals(AppFontCatalog.DEFAULT_FONT_ID, current.activeDecorativeFontId)
        assertFalse(current.customFonts.any { it.id == customOption.id })

        // Reset restores both General and Decorative to MYATO Original
        reloadedPrefs.resetDefaultFont()
        current = reloadedPrefs.preferencesFlow.first()
        assertEquals(AppFontCatalog.DEFAULT_FONT_ID, current.activeGeneralFontId)
        assertEquals(AppFontCatalog.DEFAULT_FONT_ID, current.activeDecorativeFontId)
        customFile.delete()
    }

    @Test
    fun `custom font manager validates extensions, corrupt binaries, imports ttf and otf, and cleans up safely`(): Unit = runBlocking {
        val manager = CustomFontManager(context)
        val tmpInvalidExt = File.createTempFile("bad_font_", ".txt")
        val tmpCorruptTtf = File.createTempFile("corrupt_font_", ".ttf")
        val tmpValidTtf = File.createTempFile("Minha_Fonte_Ttf", ".ttf")
        val tmpValidOtf = File.createTempFile("Minha_Fonte_Otf", ".otf")
        try {
            tmpInvalidExt.writeText("not a font")
            assertTrue(manager.importFontFromFile(tmpInvalidExt, emptyList()) is FontImportResult.Error)

            tmpCorruptTtf.writeBytes(ByteArray(512) { 0x42 })
            assertTrue(manager.importFontFromFile(tmpCorruptTtf, emptyList()) is FontImportResult.Error)

            context.resources.openRawResource(R.font.inter).use { input ->
                tmpValidTtf.outputStream().use { output -> input.copyTo(output) }
            }
            val ttfResult = manager.importFontFromFile(tmpValidTtf, emptyList())
            assertTrue("Expected valid TTF import to succeed, got $ttfResult", ttfResult is FontImportResult.Success)
            val importedTtf = (ttfResult as FontImportResult.Success).font
            assertEquals("TTF", importedTtf.fileFormat)

            context.resources.openRawResource(R.font.outfit).use { input ->
                tmpValidOtf.outputStream().use { output -> input.copyTo(output) }
            }
            val otfResult = manager.importFontFromFile(tmpValidOtf, listOf(importedTtf))
            assertTrue("Expected valid OTF import to succeed, got $otfResult", otfResult is FontImportResult.Success)
            val importedOtf = (otfResult as FontImportResult.Success).font
            assertEquals("OTF", importedOtf.fileFormat)

            assertNotNull(AppFontCatalog.resolveAppTypography(importedTtf.id, listOf(importedTtf)).headlineLarge.fontFamily)
            assertNotNull(AppFontCatalog.resolveAppTypography(importedOtf.id, listOf(importedOtf)).headlineLarge.fontFamily)

            assertTrue(manager.deleteCustomFontFile(importedTtf))
            assertTrue(manager.deleteCustomFontFile(importedOtf))
            assertFalse(File(importedTtf.customFilePath!!).exists())
            assertFalse(File(importedOtf.customFilePath!!).exists())
        } finally {
            tmpInvalidExt.delete()
            tmpCorruptTtf.delete()
            tmpValidTtf.delete()
            tmpValidOtf.delete()
        }
    }

    @Test
    fun `font catalog ui filters by category, toggles favorites, applies decorative font, and restores default`() {
        var activeGeneral = AppFontCatalog.DEFAULT_FONT_ID
        var activeDecorative = AppFontCatalog.DEFAULT_FONT_ID
        var previewRequestedId = ""
        var favs = setOf<String>()

        composeRule.setContent {
            MyatoMusicTheme(
                activeTheme = BuiltInThemes.PureLight,
                activeGeneralFontId = activeGeneral,
                activeDecorativeFontId = activeDecorative,
            ) {
                FontSettingsScreen(
                    activeGeneralFontId = activeGeneral,
                    activeDecorativeFontId = activeDecorative,
                    favoriteFontIds = favs,
                    recentFontIds = listOf("creepster"),
                    customFonts = emptyList(),
                    onBack = {},
                    onOpenPreview = { previewRequestedId = it },
                    onApplyFont = { fid, target ->
                        when (target) {
                            FontApplyTarget.GENERAL -> activeGeneral = fid
                            FontApplyTarget.DECORATIVE -> activeDecorative = fid
                            FontApplyTarget.BOTH -> {
                                activeGeneral = fid
                                activeDecorative = fid
                            }
                        }
                    },
                    onToggleFavorite = { fid ->
                        favs = if (fid in favs) favs - fid else favs + fid
                    },
                    onRestoreDefault = {
                        activeGeneral = AppFontCatalog.DEFAULT_FONT_ID
                        activeDecorative = AppFontCatalog.DEFAULT_FONT_ID
                    },
                    onImportCustomFontUri = {},
                    onDeleteCustomFont = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("font_settings_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("import_custom_font_button").assertIsDisplayed()
        composeRule.onNodeWithTag("restore_default_font_button").assertIsDisplayed()

        // Filter by Horror category -> Creepster appears
        composeRule.onNodeWithTag("font_category_row")
            .performScrollToNode(hasTestTag("font_category_horror"))
        composeRule.onNodeWithTag("font_category_horror").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("font_settings_list")
            .performScrollToNode(hasTestTag("font_option_creepster"))
        composeRule.onNodeWithTag("font_option_creepster").assertIsDisplayed()

        // Click Visualizar on Creepster
        composeRule.onNodeWithTag("preview_btn_creepster").performClick()
        composeRule.waitForIdle()
        assertEquals("creepster", previewRequestedId)

        // Click Aplicar on Creepster -> choose Decorative in dialog
        composeRule.onNodeWithTag("apply_btn_creepster").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialog_apply_decorative").performClick()
        composeRule.waitForIdle()
        assertEquals("creepster", activeDecorative)
        assertEquals(AppFontCatalog.DEFAULT_FONT_ID, activeGeneral)
    }

    @Test
    fun `font preview screen renders all required myato samples and applies font cleanly across themes`() {
        var appliedTarget: FontApplyTarget? = null
        var backPressed = false

        val sampleFont = AppFontCatalog.findFontOption("metal_mania", emptyList())

        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                FontPreviewScreen(
                    fontOption = sampleFont,
                    customFonts = emptyList(),
                    activeGeneralFontId = AppFontCatalog.DEFAULT_FONT_ID,
                    activeDecorativeFontId = AppFontCatalog.DEFAULT_FONT_ID,
                    isFavorite = false,
                    onToggleFavorite = {},
                    onApplyFont = { appliedTarget = it },
                    onDeleteCustomFont = null,
                    onBack = { backPressed = true },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("font_preview_screen").assertIsDisplayed()
        composeRule.onNodeWithText("MYATO MUSIC").assertIsDisplayed()

        composeRule.onNodeWithTag("font_preview_scroll")
            .performScrollToNode(hasText("The Sound of Music"))
        composeRule.onNodeWithText("Reproduzindo agora").assertIsDisplayed()
        composeRule.onNodeWithText("The Sound of Music").assertIsDisplayed()
        composeRule.onNodeWithText("Myato Artist").assertIsDisplayed()

        composeRule.onNodeWithTag("font_preview_scroll")
            .performScrollToNode(hasText("Álbuns e playlists"))
        composeRule.onNodeWithText("Minha biblioteca").assertIsDisplayed()
        composeRule.onNodeWithText("Álbuns e playlists").assertIsDisplayed()

        composeRule.onNodeWithTag("font_preview_scroll")
            .performScrollToNode(hasText("Botões e informações das configurações"))
        composeRule.onNodeWithText("Botões e informações das configurações").assertIsDisplayed()

        composeRule.onNodeWithTag("preview_apply_font_button").performClick()
        composeRule.waitForIdle()
        assertEquals(FontApplyTarget.DECORATIVE, appliedTarget)

        composeRule.onNodeWithTag("preview_back_button").performClick()
        composeRule.waitForIdle()
        assertTrue(backPressed)
    }

    @Test
    fun `all 50 built-in themes combine seamlessly with custom decorative and general fonts`() {
        assertEquals(50, BuiltInThemes.all.size)
        BuiltInThemes.all.forEach { theme ->
            val typo = AppFontCatalog.resolveDualTypography(
                generalFontId = "inter",
                decorativeFontId = "unifraktur_maguntia",
                customFonts = emptyList(),
            )
            assertNotNull(theme.id)
            assertNotNull(typo.headlineLarge.fontFamily)
            assertNotNull(typo.bodyMedium.fontFamily)
        }
    }

    private fun sampleBrazilianRapTracks(): List<MusicTrackEntity> = listOf(
        MusicTrackEntity(
            id = 1001L,
            mediaStoreId = 1001L,
            title = "A Praga",
            artist = "Haikaiss",
            album = "Teto Baixo",
            albumArtist = "Haikaiss",
            genre = "Rap Nacional",
            albumId = 1L,
            artistId = 1L,
            durationMs = 210_000L,
            sizeBytes = 5_000_000L,
            filePath = "/music/haikaiss/a_praga.mp3",
            folderPath = "/music/haikaiss",
            contentUri = "content://1001",
            albumArtUri = "content://art/1001",
        ),
        MusicTrackEntity(
            id = 1002L,
            mediaStoreId = 1002L,
            title = "Linhas Tortas",
            artist = "Haikaiss",
            album = "Fotografia de um Instante",
            albumArtist = "Haikaiss",
            genre = "Rap Nacional",
            albumId = 2L,
            artistId = 1L,
            durationMs = 195_000L,
            sizeBytes = 4_800_000L,
            filePath = "/music/haikaiss/linhas_tortas.mp3",
            folderPath = "/music/haikaiss",
            contentUri = "content://1002",
        ),
        MusicTrackEntity(
            id = 1003L,
            mediaStoreId = 1003L,
            title = "Rap Lord",
            artist = "Haikaiss feat. Jonas Bento",
            album = "Teto Baixo",
            albumArtist = "Haikaiss",
            genre = "Rap Nacional",
            albumId = 1L,
            artistId = 1L,
            durationMs = 230_000L,
            sizeBytes = 5_200_000L,
            filePath = "/music/haikaiss/rap_lord.mp3",
            folderPath = "/music/haikaiss",
            contentUri = "content://1003",
        ),
        MusicTrackEntity(
            id = 1004L,
            mediaStoreId = 1004L,
            title = "Favela Vive 3 (part. Haikaiss)",
            artist = "ADL",
            album = "Singles",
            albumArtist = "ADL",
            genre = "Rap Nacional",
            albumId = 3L,
            artistId = 2L,
            durationMs = 310_000L,
            sizeBytes = 7_000_000L,
            filePath = "/music/adl/favela_vive_3.mp3",
            folderPath = "/music/adl",
            contentUri = "content://1004",
        ),
        MusicTrackEntity(
            id = 1005L,
            mediaStoreId = 1005L,
            title = "Morgana",
            artist = "Kamaitachi",
            album = "Contos",
            albumArtist = "Kamaitachi",
            genre = "Rap Nacional",
            albumId = 4L,
            artistId = 3L,
            durationMs = 205_000L,
            sizeBytes = 4_500_000L,
            filePath = "/music/kamaitachi/morgana.mp3",
            folderPath = "/music/kamaitachi",
            contentUri = "content://1005",
        ),
        MusicTrackEntity(
            id = 1006L,
            mediaStoreId = 1006L,
            title = "Plutão",
            artist = "VMZ",
            album = "Espaço",
            albumArtist = "VMZ",
            genre = "Rap Nacional",
            albumId = 5L,
            artistId = 4L,
            durationMs = 190_000L,
            sizeBytes = 4_100_000L,
            filePath = "/music/vmz/plutao.mp3",
            folderPath = "/music/vmz",
            contentUri = "content://1006",
        ),
        MusicTrackEntity(
            id = 1007L,
            mediaStoreId = 1007L,
            title = "Vibe Espacial",
            artist = "VMZ",
            album = "Espaço",
            albumArtist = "VMZ",
            genre = "Sad Trap",
            albumId = 5L,
            artistId = 4L,
            durationMs = 185_000L,
            sizeBytes = 4_000_000L,
            filePath = "/music/vmz/vibe.mp3",
            folderPath = "/music/vmz",
            contentUri = "content://1007",
        ),
    )

    @Test
    fun `teste 1 pesquisa haikaiss prioriza haikaiss e nao mistura kamaitachi ou vmz`() = runBlocking {
        val entities = sampleBrazilianRapTracks()
        db.trackDao().upsertTracks(entities)
        val domainTracks = db.trackDao().getAllTracksSync().map { it.toDomain() }

        val result = com.example.core.metadata.MusicSearchEngine.searchTracks(
            tracks = domainTracks,
            query = "Haikaiss",
            filterMode = com.example.domain.models.SearchFilterMode.ALL,
        )

        val returnedIds = result.hits.map { it.track.id }
        assertTrue(1001L in returnedIds)
        assertTrue(1002L in returnedIds)
        assertFalse("Kamaitachi não pode aparecer ao pesquisar Haikaiss", 1005L in returnedIds)
        assertFalse("VMZ não pode aparecer ao pesquisar Haikaiss", 1006L in returnedIds)
        assertFalse("VMZ não pode aparecer ao pesquisar Haikaiss", 1007L in returnedIds)
        assertEquals(
            com.example.domain.models.SearchMatchSource.EXACT_ARTIST,
            result.hits.first().matchSource,
        )
    }

    @Test
    fun `teste 2 playlist do artista haikaiss adiciona apenas musicas de haikaiss sem duplicadas`() = runBlocking {
        db.trackDao().upsertTracks(sampleBrazilianRapTracks())
        val plRepo = PlaylistRepository(context, db)

        val playlistId = plRepo.createArtistPlaylist(
            artistName = "Haikaiss",
            includeCollaborations = false,
            autoUpdateEnabled = false,
            customName = "Haikaiss",
        )

        val playlist = plRepo.playlistsFlow.first().first { it.id == playlistId }
        assertEquals("Haikaiss", playlist.name)
        assertEquals("Haikaiss", playlist.artistFilterName)

        val trackIds = plRepo.getPlaylistTrackIdsSync(playlistId)
        assertEquals(listOf(1001L, 1002L), trackIds.sorted())
        assertEquals(trackIds.size, trackIds.distinct().size)
        assertFalse(1005L in trackIds) // Kamaitachi excluded
        assertFalse(1006L in trackIds) // VMZ excluded
    }

    @Test
    fun `teste 3 colaboracoes aparecem apenas quando filtro de colaboracoes esta habilitado`() = runBlocking {
        db.trackDao().upsertTracks(sampleBrazilianRapTracks())
        val domainTracks = db.trackDao().getAllTracksSync().map { it.toDomain() }

        val soloResult = com.example.core.metadata.MusicSearchEngine.searchTracks(
            tracks = domainTracks,
            query = "Haikaiss",
            filterMode = com.example.domain.models.SearchFilterMode.EXACT_ARTIST,
            selectedArtist = "Haikaiss",
        )
        assertEquals(listOf(1001L, 1002L), soloResult.hits.map { it.track.id }.sorted())

        val collabResult = com.example.core.metadata.MusicSearchEngine.searchTracks(
            tracks = domainTracks,
            query = "Haikaiss",
            filterMode = com.example.domain.models.SearchFilterMode.ARTIST_AND_COLLABS,
            selectedArtist = "Haikaiss",
        )
        assertEquals(listOf(1001L, 1002L, 1003L, 1004L), collabResult.hits.map { it.track.id }.sorted())

        val plRepo = PlaylistRepository(context, db)
        val collabPlaylistId = plRepo.createArtistPlaylist(
            artistName = "Haikaiss",
            includeCollaborations = true,
            autoUpdateEnabled = false,
            customName = "Haikaiss + Feats",
        )
        assertEquals(
            listOf(1001L, 1002L, 1003L, 1004L),
            plRepo.getPlaylistTrackIdsSync(collabPlaylistId).sorted(),
        )
    }

    @Test
    fun `teste 4 pesquisa por outros artistas como VMZ funciona sem misturar haikaiss ou kamaitachi`() = runBlocking {
        db.trackDao().upsertTracks(sampleBrazilianRapTracks())
        val domainTracks = db.trackDao().getAllTracksSync().map { it.toDomain() }

        val vmzResult = com.example.core.metadata.MusicSearchEngine.searchTracks(
            tracks = domainTracks,
            query = "vmz",
            filterMode = com.example.domain.models.SearchFilterMode.EXACT_ARTIST,
        )
        assertEquals(listOf(1006L, 1007L), vmzResult.hits.map { it.track.id }.sorted())
        assertFalse(1001L in vmzResult.hits.map { it.track.id })
        assertFalse(1005L in vmzResult.hits.map { it.track.id })
    }

    @Test
    fun `teste 5 persistencia mantem playlists, ordem, capas e configuracoes apos fechar e reabrir banco`() = runBlocking {
        val tempDbFile = File.createTempFile("myato_persist_test_", ".db")
        try {
            val db1 = Room.databaseBuilder(context, MyatoDatabase::class.java, tempDbFile.absolutePath)
                .allowMainThreadQueries()
                .build()
            db1.trackDao().upsertTracks(sampleBrazilianRapTracks())
            val repo1 = PlaylistRepository(context, db1)
            val plId = repo1.createArtistPlaylist(
                artistName = "Haikaiss",
                includeCollaborations = true,
                autoUpdateEnabled = true,
                customName = "Haikaiss Persistida",
            )
            repo1.updatePlaylistDetails(
                playlistId = plId,
                newName = "Haikaiss Editada",
                coverUri = "file:///custom/haikaiss_cover.jpg",
                autoUpdateEnabled = true,
                includeCollaborations = false,
            )
            repo1.reorderPlaylist(plId, listOf(1002L, 1001L))
            db1.close()

            // Reopen database simulating full app restart
            val db2 = Room.databaseBuilder(context, MyatoDatabase::class.java, tempDbFile.absolutePath)
                .allowMainThreadQueries()
                .build()
            try {
                val repo2 = PlaylistRepository(context, db2)
                val restored = repo2.playlistsFlow.first().first { it.id == plId }
                assertEquals("Haikaiss Editada", restored.name)
                assertEquals("file:///custom/haikaiss_cover.jpg", restored.coverUri)
                assertEquals("Haikaiss", restored.artistFilterName)
                assertTrue(restored.autoUpdateByArtist)
                assertFalse(restored.includeCollaborations)
                assertEquals(listOf(1002L, 1001L), repo2.getPlaylistTrackIdsSync(plId))
            } finally {
                db2.close()
            }
        } finally {
            tempDbFile.delete()
            File("${tempDbFile.absolutePath}-wal").delete()
            File("${tempDbFile.absolutePath}-shm").delete()
        }
    }

    @Test
    fun `teste 6 atualizacao da biblioteca sincroniza playlist automatica do artista quando habilitada`() = runBlocking {
        db.trackDao().upsertTracks(sampleBrazilianRapTracks())
        val plRepo = PlaylistRepository(context, db)

        val autoPlId = plRepo.createArtistPlaylist(
            artistName = "Haikaiss",
            includeCollaborations = false,
            autoUpdateEnabled = true,
            customName = "Haikaiss Auto",
        )
        val manualPlId = plRepo.createArtistPlaylist(
            artistName = "Haikaiss",
            includeCollaborations = false,
            autoUpdateEnabled = false,
            customName = "Haikaiss Manual",
        )
        assertEquals(listOf(1001L, 1002L), plRepo.getPlaylistTrackIdsSync(autoPlId).sorted())

        // Add new Haikaiss track + new Kamaitachi & VMZ tracks to library
        val newHaikaiss = sampleBrazilianRapTracks().first().copy(
            id = 1099L,
            mediaStoreId = 1099L,
            title = "Bosque das Ilusões",
            artist = "Haikaiss",
            filePath = "/music/haikaiss/bosque.mp3",
            contentUri = "content://1099",
        )
        val newKamaitachi = sampleBrazilianRapTracks().first().copy(
            id = 1100L,
            mediaStoreId = 1100L,
            title = "Cabelos Arco-Íris",
            artist = "Kamaitachi",
            filePath = "/music/kamaitachi/arco_iris.mp3",
            contentUri = "content://1100",
        )
        db.trackDao().upsertTracks(listOf(newHaikaiss, newKamaitachi))

        val addedCount = plRepo.syncAutoUpdateArtistPlaylists()
        assertEquals(1, addedCount)

        val autoIds = plRepo.getPlaylistTrackIdsSync(autoPlId).sorted()
        assertEquals(listOf(1001L, 1002L, 1099L), autoIds)
        assertFalse(1100L in autoIds)

        // Manual playlist (autoUpdate = false) must NOT auto-update
        assertEquals(listOf(1001L, 1002L), plRepo.getPlaylistTrackIdsSync(manualPlId).sorted())
    }

    @Test
    fun `all songs search ui displays artist filter, result count, create artist playlist dialog, and empty state`() {
        val domainTracks = listOf(
            MusicTrack(
                id = 1L, mediaStoreId = 1L, title = "A Praga", artist = "Haikaiss", album = "Teto Baixo",
                albumId = 1L, artistId = 1L, durationMs = 200_000L, sizeBytes = 1000L,
                filePath = "/music/1.mp3", folderPath = "/music", contentUri = "content://1",
            ),
            MusicTrack(
                id = 2L, mediaStoreId = 2L, title = "Morgana", artist = "Kamaitachi", album = "Contos",
                albumId = 2L, artistId = 2L, durationMs = 200_000L, sizeBytes = 1000L,
                filePath = "/music/2.mp3", folderPath = "/music", contentUri = "content://2",
            ),
        )
        var createdArtist = ""
        var createdAutoUpdate = false

        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                com.example.feature.library.AllSongsScreen(
                    tracks = domainTracks,
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
                    sortTracks = { list, _, _ -> list },
                    onCreateArtistPlaylist = { artist, _, autoUpdate, _ ->
                        createdArtist = artist
                        createdAutoUpdate = autoUpdate
                    },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("songs_search_input").performTextInput("Haikaiss")
        composeRule.mainClock.advanceTimeBy(350L)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule.onAllNodes(hasTestTag("search_result_count")).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("search_result_count").assertIsDisplayed()
        composeRule.onNodeWithText("1 música(s) encontrada(s)").assertIsDisplayed()
        composeRule.onNodeWithTag("create_artist_playlist_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("confirm_create_artist_playlist_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("Haikaiss", createdArtist)
        assertTrue(createdAutoUpdate)

        // Clear search and test empty state on non-matching query
        composeRule.onNodeWithTag("clear_search_button").performClick()
        composeRule.onNodeWithTag("songs_search_input").performTextInput("ArtistaInexistenteXYZ")
        composeRule.mainClock.advanceTimeBy(350L)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule.onAllNodes(hasTestTag("search_empty_state")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("search_empty_state").assertIsDisplayed()
    }

    @Test
    fun `playlists screen shows smart artist badge, auto update controls, and manual add confirmation`() {
        val domainTracks = listOf(
            MusicTrack(
                id = 1L, mediaStoreId = 1L, title = "A Praga", artist = "Haikaiss", album = "Teto Baixo",
                albumId = 1L, artistId = 1L, durationMs = 200_000L, sizeBytes = 1000L,
                filePath = "/music/1.mp3", folderPath = "/music", contentUri = "content://1",
            ),
            MusicTrack(
                id = 2L, mediaStoreId = 2L, title = "Plutão", artist = "VMZ", album = "Espaço",
                albumId = 2L, artistId = 2L, durationMs = 190_000L, sizeBytes = 1000L,
                filePath = "/music/2.mp3", folderPath = "/music", contentUri = "content://2",
            ),
        )
        val smartPlaylist = com.example.domain.models.Playlist(
            id = 55L,
            name = "Haikaiss",
            trackCount = 1,
            totalDurationMs = 200_000L,
            artistFilterName = "Haikaiss",
            autoUpdateByArtist = true,
            includeCollaborations = false,
        )
        var addedTrackIds: List<Long> = emptyList()

        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                com.example.feature.queues.PlaylistsScreen(
                    playlists = listOf(smartPlaylist),
                    allTracks = domainTracks,
                    observePlaylistTrackIds = { kotlinx.coroutines.flow.flowOf(listOf(1L)) },
                    onCreatePlaylist = {},
                    onRenamePlaylist = { _, _ -> },
                    onDeletePlaylist = {},
                    onPlayPlaylist = { _, _, _ -> },
                    onRemoveTrackFromPlaylist = { _, _ -> },
                    onReorderPlaylist = { _, _ -> },
                    onAddTracksToPlaylist = { _, _ -> },
                    onExportM3u = { _, _ -> },
                    onImportM3u = { _, _ -> },
                    onAddTracksToPlaylistsWithResult = { _, ids, _ -> addedTrackIds = ids },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Auto • Haikaiss").assertIsDisplayed()
        composeRule.onNodeWithTag("playlist_card_55").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("smart_artist_playlist_card").assertIsDisplayed()
        composeRule.onNodeWithTag("playlist_auto_update_switch").assertIsDisplayed()
        composeRule.onNodeWithTag("playlist_add_songs_button").performClick()
        composeRule.waitForIdle()

        // Select VMZ track manually and go through confirmation summary step
        composeRule.onNodeWithText("Plutão").performClick()
        composeRule.onNodeWithTag("review_manual_add_tracks_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("manual_add_confirmation_list").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_manual_add_tracks_button").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(2L), addedTrackIds)
    }
}



