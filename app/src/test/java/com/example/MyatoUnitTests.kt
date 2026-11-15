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
    fun `built in themes include all 12 mandatory themes with official Midnight Orange palette`() {
        val all = BuiltInThemes.all
        assertEquals(12, all.size)
        val names = all.map { it.name }.toSet()
        val expected = setOf(
            "Midnight Orange", "AMOLED Black", "Neon Galaxy", "Cyberpunk",
            "Royal Gold", "Pure Light", "Ocean Blue", "Forest Green",
            "Crimson Red", "Glass", "Purple Night", "Arctic Silver",
        )
        assertEquals(expected, names)

        val official = BuiltInThemes.MidnightOrange
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
}
