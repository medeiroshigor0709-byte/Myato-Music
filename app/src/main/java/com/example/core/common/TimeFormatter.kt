package com.example.core.common

import java.util.Locale

object TimeFormatter {

    fun formatDuration(ms: Long): String {
        val safeMs = ms.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatHoursMinutes(ms: Long): String {
        val totalMinutes = (ms.coerceAtLeast(0L) / 60_000L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
    }

    fun formatLrcTimestamp(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        val minutes = safe / 60_000L
        val seconds = (safe % 60_000L) / 1000L
        val centis = (safe % 1000L) / 10L
        return String.format(Locale.US, "[%02d:%02d.%02d]", minutes, seconds, centis)
    }

    fun formatFileSize(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        return when {
            safe < 1024 -> "$safe B"
            safe < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", safe / 1024.0)
            safe < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", safe / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.2f GB", safe / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
