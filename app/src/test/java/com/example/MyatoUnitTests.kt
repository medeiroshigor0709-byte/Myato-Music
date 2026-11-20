package com.example

import com.example.core.audio.SleepTimerManager
import com.example.core.common.AudioFormatSupport
import com.example.core.common.TimeFormatter
import com.example.core.designsystem.BuiltInThemes
import com.example.core.metadata.LrcLyricsEngine
import com.example.core.metadata.TagEditPayload
import com.example.domain.models.LyricLine
import com.example.domain.models.MusicTrack
import com.example.domain.models.SleepTimerMode
import com.example.domain.models.SmartSelectionCriteria
import com.example.domain.models.TrackSortOrder
import com.example.presentation.MyatoMainViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MyatoUnitTests {

    @Test
    fun `time formatter formats durations and lrc timestamps accurately`() {
        assertEquals("03:45", TimeFormatter.formatDuration(225_000L))
        assertEquals("1:02:05", TimeFormatter.formatDuration(3_725_000L))
        assertEquals("[01:23.45]", TimeFormatter.formatLrcTimestamp(83_450L))
        assertEquals("1h 30min", TimeFormatter.formatHoursMinutes(5_400_000L))
    }

    @Test
    fun `lrc parser handles synced lines, time shifting, and active line index`() {
        val raw = """
            [ti:Neon Skyline]
            [ar:Myato]
            [00:05.00]Primeira linha
            [00:12.50]Segunda linha
            [00:20.00]Terceira linha
        """.trimIndent()

        val parsed = LrcLyricsEngine.parseLyrics(raw)
        assertEquals(3, parsed.size)
        assertEquals(5_000L, parsed[0].timestampMs)
        assertEquals("Segunda linha", parsed[1].text)
        assertEquals(1, LrcLyricsEngine.findActiveLineIndex(parsed, 13_000L))

        val shifted = LrcLyricsEngine.shiftTimestamps(parsed, -2_000L)
        assertEquals(3_000L, shifted[0].timestampMs)
        assertEquals(10_500L, shifted[1].timestampMs)

        val serialized = LrcLyricsEngine.serializeToLrc(shifted, "Neon Skyline", "Myato")
        assertTrue(serialized.contains("[00:03.00]Primeira linha"))
    }

    @Test
    fun `built in themes include all 50 mandatory themes across 10 categories with official Myato Original palette`() {
        val all = BuiltInThemes.all
        assertEquals(50, all.size)
        val nonCustomCategories = com.example.domain.models.ThemeCategory.entries.filter {
            it != com.example.domain.models.ThemeCategory.CUSTOM
        }
        assertEquals(10, nonCustomCategories.size)
        nonCustomCategories.forEach { cat ->
            val inCat = all.filter { it.category == cat }
            assertEquals("Category ${cat.displayName} should have 5 themes", 5, inCat.size)
        }
        assertEquals(50, all.map { it.id }.toSet().size)
        assertEquals(50, all.map { it.name }.toSet().size)

        val official = BuiltInThemes.MidnightOrange
        assertEquals("Myato Original", official.name)
        assertEquals(0xFFFF8500L, official.primaryColorHex)
        assertEquals(0xFFFF9D24L, official.secondaryColorHex)
        assertEquals(0xFF000000L, official.backgroundColorHex)
        assertEquals(0xFF1C1C1CL, official.cardColorHex)
        assertEquals(0xFFFFFFFFL, official.textColorHex)
        assertEquals(0xFFA5A5A5L, official.secondaryTextColorHex)
    }

    @Test
    fun `sleep timer track count decrements and completes`() {
        var finished = false
        val timer = SleepTimerManager(Dispatchers.Unconfined) { finished = true }

        timer.startTrackCountTimer(3)
        assertEquals(SleepTimerMode.TRACK_COUNT, timer.state.value.mode)
        assertEquals(3, timer.state.value.remainingTracks)

        timer.onTrackTransitioned()
        assertEquals(2, timer.state.value.remainingTracks)
        assertFalse(finished)

        timer.pauseOrResume()
        assertTrue(timer.state.value.isPaused)
        timer.onTrackTransitioned() // paused, should not decrement
        assertEquals(2, timer.state.value.remainingTracks)

        timer.pauseOrResume()
        timer.onTrackTransitioned()
        timer.onTrackTransitioned()
        assertTrue(finished)
        assertEquals(SleepTimerMode.NONE, timer.state.value.mode)
    }

    @Test
    fun `audio format support infers extensions cleanly`() {
        assertEquals("FLAC", AudioFormatSupport.inferExtension("/music/track.flac", null))
        assertEquals("DSF", AudioFormatSupport.inferExtension("/music/track.dsf", null))
        assertEquals("OPUS", AudioFormatSupport.inferExtension("unknown", "audio/opus"))
    }

    @Test
    fun `tag edit payload preserves metadata fields accurately`() {
        val tmp = File.createTempFile("myato_test_", ".mp3")
        try {
            val dummyAudioPayload = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64.toByte(), 1, 2, 3, 4)
            tmp.writeBytes(dummyAudioPayload)
            val header = tmp.readBytes()
            assertEquals(8, header.size)
            val payload = TagEditPayload(
                title = "Faixa Teste",
                artist = "MYATO",
                album = "Offline Studio",
                albumArtist = "MYATO",
                genre = "Electronic",
                year = 2026,
                trackNumber = 1,
                discNumber = 1,
                composer = "Composer",
                comments = "Nota",
                embeddedLyrics = "[00:01.00]Teste",
            )
            assertEquals("Faixa Teste", payload.title)
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun `filterAndSortTracks handles all TrackSortOrder options and search query`() {
        val sampleTracks = listOf(
            MusicTrack(
                id = 1L,
                mediaStoreId = 1L,
                title = "Beta Song",
                artist = "Zed Artist",
                album = "Mid Album",
                albumId = 1L,
                artistId = 1L,
                durationMs = 120_000L,
                sizeBytes = 1_000_000L,
                filePath = "/music/rock/beta.mp3",
                folderPath = "/music/rock",
                contentUri = "content://1",
                dateAddedSec = 200L,
                playCount = 5,
            ),
            MusicTrack(
                id = 2L,
                mediaStoreId = 2L,
                title = "Alpha Song",
                artist = "Amber Artist",
                album = "Zebra Album",
                albumId = 2L,
                artistId = 2L,
                durationMs = 300_000L,
                sizeBytes = 2_000_000L,
                filePath = "/music/jazz/alpha.flac",
                folderPath = "/music/jazz",
                contentUri = "content://2",
                dateAddedSec = 100L,
                playCount = 1,
            ),
            MusicTrack(
                id = 3L,
                mediaStoreId = 3L,
                title = "Gamma Song",
                artist = "Mid Artist",
                album = "Alpha Album",
                albumId = 3L,
                artistId = 3L,
                durationMs = 200_000L,
                sizeBytes = 3_000_000L,
                filePath = "/music/pop/gamma.mp3",
                folderPath = "/music/pop",
                contentUri = "content://3",
                dateAddedSec = 300L,
                playCount = 10,
            ),
        )

        assertEquals(
            listOf(2L, 1L, 3L),
            MyatoMainViewModel.filterAndSortTracks(sampleTracks, TrackSortOrder.TITLE_ASC, "").map { it.id },
        )
        assertEquals(
            listOf(3L, 1L, 2L),
            MyatoMainViewModel.filterAndSortTracks(sampleTracks, TrackSortOrder.TITLE_DESC, "").map { it.id },
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            MyatoMainViewModel.filterAndSortTracks(sampleTracks, TrackSortOrder.ARTIST_ASC, "").map { it.id },
        )
        assertEquals(
            listOf(3L, 1L, 2L),
            MyatoMainViewModel.filterAndSortTracks(sampleTracks, TrackSortOrder.ALBUM_ASC, "").map { it.id },
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            MyatoMainViewModel.filterAndSortTracks(sampleTracks, TrackSortOrder.DURATION_DESC, "").map { it.id },
        )
        assertEquals(
            listOf(3L, 1L, 2L),
            MyatoMainViewModel.filterAndSortTracks(sampleTracks, TrackSortOrder.DATE_ADDED_DESC, "").map { it.id },
        )
        assertEquals(
            listOf(2L, 1L, 3L),
            MyatoMainViewModel.filterAndSortTracks(sampleTracks, TrackSortOrder.DATE_ADDED_ASC, "").map { it.id },
        )
        assertEquals(
            listOf(3L, 1L, 2L),
            MyatoMainViewModel.filterAndSortTracks(sampleTracks, TrackSortOrder.MOST_PLAYED, "").map { it.id },
        )
        assertEquals(
            listOf(2L, 1L, 3L),
            MyatoMainViewModel.filterAndSortTracks(sampleTracks, TrackSortOrder.LEAST_PLAYED, "").map { it.id },
        )
        assertEquals(
            listOf(3L, 1L, 2L),
            MyatoMainViewModel.filterAndSortTracks(sampleTracks, TrackSortOrder.RECENTLY_ADDED, "").map { it.id },
        )
        assertEquals(
            listOf(2L),
            MyatoMainViewModel.filterAndSortTracks(sampleTracks, TrackSortOrder.TITLE_ASC, "jazz").map { it.id },
        )
    }

    @Test
    fun `smart selection criteria selects accurately by artist, album, folder, format, favorites and unplayed`() {
        val tracks = listOf(
            MusicTrack(
                id = 10L, mediaStoreId = 10L, title = "Song A", artist = "Artist One", album = "Album X",
                albumId = 1L, artistId = 1L, durationMs = 180_000L, sizeBytes = 1000L,
                filePath = "/storage/music/rock/song_a.mp3", folderPath = "/storage/music/rock",
                contentUri = "content://10", formatExtension = "MP3", isFavorite = true, playCount = 12,
            ),
            MusicTrack(
                id = 20L, mediaStoreId = 20L, title = "Song B", artist = "Artist One", album = "Album Y",
                albumId = 2L, artistId = 1L, durationMs = 210_000L, sizeBytes = 2000L,
                filePath = "/storage/music/pop/song_b.flac", folderPath = "/storage/music/pop",
                contentUri = "content://20", formatExtension = "FLAC", isFavorite = false, playCount = 0,
            ),
            MusicTrack(
                id = 30L, mediaStoreId = 30L, title = "Song C", artist = "Artist Two", album = "Album X",
                albumId = 1L, artistId = 2L, durationMs = 195_000L, sizeBytes = 3000L,
                filePath = "/storage/music/rock/song_c.mp3", folderPath = "/storage/music/rock",
                contentUri = "content://30", formatExtension = "MP3", isFavorite = true, playCount = 0,
            ),
        )

        // BY_ARTIST using track 10 (Artist One)
        val artistMatches = MyatoMainViewModel.selectTracksByCriteria(tracks, SmartSelectionCriteria.BY_ARTIST, tracks[0])
        assertEquals(listOf(10L, 20L), artistMatches)

        // BY_ALBUM using track 10 (Album X)
        val albumMatches = MyatoMainViewModel.selectTracksByCriteria(tracks, SmartSelectionCriteria.BY_ALBUM, tracks[0])
        assertEquals(listOf(10L, 30L), albumMatches)

        // BY_FOLDER using track 10 (/storage/music/rock)
        val folderMatches = MyatoMainViewModel.selectTracksByCriteria(tracks, SmartSelectionCriteria.BY_FOLDER, tracks[0])
        assertEquals(listOf(10L, 30L), folderMatches)

        // BY_FORMAT using track 20 (FLAC)
        val formatMatches = MyatoMainViewModel.selectTracksByCriteria(tracks, SmartSelectionCriteria.BY_FORMAT, tracks[1])
        assertEquals(listOf(20L), formatMatches)

        // FAVORITES
        val favMatches = MyatoMainViewModel.selectTracksByCriteria(tracks, SmartSelectionCriteria.FAVORITES, null)
        assertEquals(listOf(10L, 30L), favMatches)

        // NEVER_PLAYED (playCount == 0)
        val unplayedMatches = MyatoMainViewModel.selectTracksByCriteria(tracks, SmartSelectionCriteria.NEVER_PLAYED, null)
        assertEquals(listOf(20L, 30L), unplayedMatches)
    }

    @Test
    fun `multi-selection inversion logic inverts correctly against displayed list`() {
        val allIds = listOf(1L, 2L, 3L, 4L, 5L)
        val selectedIds = mutableSetOf(2L, 4L)

        val inverted = allIds.filter { it !in selectedIds }
        assertEquals(listOf(1L, 3L, 5L), inverted)
    }

    @Test
    fun `duplicate detection calculates duplicates and unique items accurately`() {
        val existingInPlaylist = setOf(101L, 102L, 105L)
        val toAdd = listOf(102L, 103L, 104L, 105L)

        val duplicates = toAdd.filter { it in existingInPlaylist }
        val uniqueToAdd = toAdd.filter { it !in existingInPlaylist }

        assertEquals(listOf(102L, 105L), duplicates)
        assertEquals(listOf(103L, 104L), uniqueToAdd)
    }

    @Test
    fun `music search engine normalizes accents, case, whitespace and isolates unknown artists`() {
        assertEquals("haikaiss", com.example.core.metadata.MusicSearchEngine.normalizeText("  Haïkaíss   "))
        assertEquals("kamaitachi", com.example.core.metadata.MusicSearchEngine.normalizeText("KAMAITACHI"))

        assertTrue(com.example.core.metadata.MusicSearchEngine.isUnknownArtist(""))
        assertTrue(com.example.core.metadata.MusicSearchEngine.isUnknownArtist("<unknown>"))
        assertTrue(com.example.core.metadata.MusicSearchEngine.isUnknownArtist("Unknown Artist"))
        assertEquals("Artista desconhecido", com.example.core.metadata.MusicSearchEngine.sanitizeArtistName("<unknown>"))
        assertEquals("Artista desconhecido", com.example.core.metadata.MusicSearchEngine.sanitizeArtistName("   "))
        assertEquals("Haikaiss", com.example.core.metadata.MusicSearchEngine.sanitizeArtistName("  Haikaiss  "))
    }

    @Test
    fun `music search engine separates solo tracks from collaborations and preserves named duos`() {
        val solo = MusicTrack(
            id = 1L, mediaStoreId = 1L, title = "A Praga", artist = "Haikaiss", album = "Teto Baixo",
            albumId = 1L, artistId = 1L, durationMs = 200_000L, sizeBytes = 1000L,
            filePath = "/music/haikaiss.mp3", folderPath = "/music", contentUri = "content://1",
        )
        val collabArtistField = solo.copy(
            id = 2L, mediaStoreId = 2L, title = "Rap Lord", artist = "Haikaiss feat. Rashid",
        )
        val collabTitleField = solo.copy(
            id = 3L, mediaStoreId = 3L, title = "Sessão Cypher (part. Haikaiss)", artist = "ADL",
        )
        val namedDuo = solo.copy(
            id = 4L, mediaStoreId = 4L, title = "The Sound of Silence", artist = "Simon & Garfunkel",
            albumArtist = "Simon & Garfunkel",
        )

        val parsedSolo = com.example.core.metadata.MusicSearchEngine.parseTrackArtists(solo)
        assertFalse(parsedSolo.isCollaboration)
        assertEquals(listOf("Haikaiss"), parsedSolo.primaryArtists)

        val parsedCollab1 = com.example.core.metadata.MusicSearchEngine.parseTrackArtists(collabArtistField)
        assertTrue(parsedCollab1.isCollaboration)
        assertEquals(listOf("Haikaiss"), parsedCollab1.primaryArtists)
        assertEquals(listOf("Rashid"), parsedCollab1.featuredArtists)

        val parsedCollab2 = com.example.core.metadata.MusicSearchEngine.parseTrackArtists(collabTitleField)
        assertTrue(parsedCollab2.isCollaboration)
        assertEquals(listOf("ADL"), parsedCollab2.primaryArtists)
        assertEquals(listOf("Haikaiss"), parsedCollab2.featuredArtists)

        val parsedDuo = com.example.core.metadata.MusicSearchEngine.parseTrackArtists(namedDuo)
        assertFalse(parsedDuo.isCollaboration)
        assertEquals(listOf("Simon & Garfunkel"), parsedDuo.primaryArtists)

        assertTrue(com.example.core.metadata.MusicSearchEngine.matchesArtist(solo, "Haikaiss", includeCollaborations = false))
        assertFalse(com.example.core.metadata.MusicSearchEngine.matchesArtist(collabArtistField, "Haikaiss", includeCollaborations = false))
        assertTrue(com.example.core.metadata.MusicSearchEngine.matchesArtist(collabArtistField, "Haikaiss", includeCollaborations = true))
        assertTrue(com.example.core.metadata.MusicSearchEngine.matchesArtist(collabTitleField, "Haikaiss", includeCollaborations = true))
    }

    @Test
    fun `music search engine ranks exact artist above collab, title, album, and genre without folder pollution`() {
        val exactArtist = MusicTrack(
            id = 1L, mediaStoreId = 1L, title = "Ouroboros", artist = "Haikaiss", album = "Teto Baixo",
            albumId = 1L, artistId = 1L, durationMs = 200_000L, sizeBytes = 1000L,
            filePath = "/music/a.mp3", folderPath = "/music", contentUri = "content://1", genre = "Rap",
        )
        val collabTrack = exactArtist.copy(
            id = 2L, mediaStoreId = 2L, title = "Noite Fria", artist = "CortesiaDaCasa feat. Haikaiss",
        )
        val unrelatedSameFolder = exactArtist.copy(
            id = 3L, mediaStoreId = 3L, title = "Morgana", artist = "Kamaitachi", album = "Contos",
            filePath = "/music/haikaiss_mix/morgana.mp3", folderPath = "/music/haikaiss_mix", genre = "Rap",
        )
        val unrelatedSameGenre = exactArtist.copy(
            id = 4L, mediaStoreId = 4L, title = "Plutão", artist = "VMZ", album = "Espaço",
            filePath = "/music/vmz.mp3", folderPath = "/music", genre = "Rap",
        )

        val result = com.example.core.metadata.MusicSearchEngine.searchTracks(
            tracks = listOf(unrelatedSameGenre, collabTrack, unrelatedSameFolder, exactArtist),
            query = "  haïkaíss ",
            filterMode = com.example.domain.models.SearchFilterMode.ALL,
        )

        assertEquals(listOf(1L, 2L), result.hits.map { it.track.id })
        assertEquals(com.example.domain.models.SearchMatchSource.EXACT_ARTIST, result.hits[0].matchSource)
        assertEquals(com.example.domain.models.SearchMatchSource.COLLAB_ARTIST, result.hits[1].matchSource)
    }
}
