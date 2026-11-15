package com.example.core.metadata

import android.content.Context
import com.example.core.common.TimeFormatter
import com.example.domain.models.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Offline Lyrics & LRC Engine supporting synchronized .lrc files, embedded ID3 USLT lyrics,
 * manual time-sync, global time offset shift, and saving sidecar .lrc files.
 */
object LrcLyricsEngine {

    private val lrcLineRegex = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    fun parseLyrics(rawContent: String): List<LyricLine> {
        if (rawContent.isBlank()) return emptyList()
        val parsed = mutableListOf<LyricLine>()
        val lines = rawContent.lines()

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue
            val matches = lrcLineRegex.findAll(trimmed).toList()
            if (matches.isEmpty()) {
                // Skip pure metadata tags like [ar:...] or [ti:...]
                if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                    parsed.add(LyricLine(timestampMs = null, text = trimmed))
                }
            } else {
                val cleanText = lrcLineRegex.replace(trimmed, "").trim()
                for (match in matches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val fracStr = match.groupValues[3]
                    val millis = when (fracStr.length) {
                        0 -> 0L
                        1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                        2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                        else -> (fracStr.take(3).toLongOrNull() ?: 0L)
                    }
                    val totalMs = (minutes * 60_000L) + (seconds * 1000L) + millis
                    parsed.add(LyricLine(timestampMs = totalMs, text = cleanText))
                }
            }
        }
        return if (parsed.any { it.timestampMs != null }) {
            parsed.sortedBy { it.timestampMs ?: Long.MAX_VALUE }
        } else {
            parsed
        }
    }

    fun serializeToLrc(lines: List<LyricLine>, title: String = "", artist: String = ""): String {
        val sb = StringBuilder()
        if (title.isNotBlank()) sb.appendLine("[ti:$title]")
        if (artist.isNotBlank()) sb.appendLine("[ar:$artist]")
        lines.forEach { line ->
            if (line.timestampMs != null) {
                sb.appendLine("${TimeFormatter.formatLrcTimestamp(line.timestampMs)}${line.text}")
            } else {
                sb.appendLine(line.text)
            }
        }
        return sb.toString().trimEnd()
    }

    fun shiftTimestamps(lines: List<LyricLine>, offsetMs: Long): List<LyricLine> {
        return lines.map { line ->
            val ts = line.timestampMs
            if (ts != null) line.copy(timestampMs = (ts + offsetMs).coerceAtLeast(0L))
            else line
        }
    }

    fun findActiveLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        var active = -1
        for (i in lines.indices) {
            val ts = lines[i].timestampMs ?: continue
            if (ts <= positionMs + 120L) {
                active = i
            } else {
                break
            }
        }
        return active
    }

    suspend fun loadLyricsForTrack(
        context: Context,
        trackId: Long,
        audioFilePath: String,
        embeddedLyrics: String,
    ): String = withContext(Dispatchers.IO) {
        // 1. Check sidecar .lrc next to the audio file
        if (!audioFilePath.startsWith("content://")) {
            val audioFile = File(audioFilePath)
            val lrcFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                val content = runCatching { lrcFile.readText(Charsets.UTF_8) }.getOrNull()
                if (!content.isNullOrBlank()) return@withContext content
            }
        }
        // 2. Check app-local lyrics store (guaranteed writable on all Android versions)
        val localLrc = File(lyricsDirectory(context), "track_$trackId.lrc")
        if (localLrc.exists()) {
            val content = runCatching { localLrc.readText(Charsets.UTF_8) }.getOrNull()
            if (!content.isNullOrBlank()) return@withContext content
        }
        // 3. Fallback to embedded tag lyrics
        embeddedLyrics
    }

    suspend fun saveLyricsForTrack(
        context: Context,
        trackId: Long,
        audioFilePath: String,
        lrcContent: String,
    ): Boolean = withContext(Dispatchers.IO) {
        var wroteSidecar = false
        if (!audioFilePath.startsWith("content://")) {
            val audioFile = File(audioFilePath)
            val parent = audioFile.parentFile
            if (parent != null && parent.canWrite()) {
                val sidecar = File(parent, "${audioFile.nameWithoutExtension}.lrc")
                wroteSidecar = runCatching {
                    sidecar.writeText(lrcContent, Charsets.UTF_8)
                    true
                }.getOrDefault(false)
            }
        }
        // Always also persist in app-local lyrics folder so Scoped Storage never loses edits
        val localLrc = File(lyricsDirectory(context), "track_$trackId.lrc")
        val wroteLocal = runCatching {
            localLrc.writeText(lrcContent, Charsets.UTF_8)
            true
        }.getOrDefault(false)
        wroteSidecar || wroteLocal
    }

    private fun lyricsDirectory(context: Context): File {
        return File(context.filesDir, "lyrics").apply { if (!exists()) mkdirs() }
    }
}
