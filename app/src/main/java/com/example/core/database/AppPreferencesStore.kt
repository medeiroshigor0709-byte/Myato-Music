package com.example.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.designsystem.BuiltInThemes
import com.example.domain.models.HeadsetButtonMapping
import com.example.domain.models.TrackSortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.myatoDataStore: DataStore<Preferences> by preferencesDataStore(name = "myato_preferences")

data class AppUserPreferences(
    val hasCompletedOnboarding: Boolean = false,
    val activeThemeId: String = BuiltInThemes.MidnightOrange.id,
    val activeQueueId: Long = 1L,
    val lastTrackId: Long = -1L,
    val lastPositionMs: Long = 0L,
    val gaplessEnabled: Boolean = true,
    val batterySaverAnimations: Boolean = false,
    val visualizerEnabled: Boolean = true,
    val monitoredFolders: Set<String> = emptySet(),
    val singleClickAction: HeadsetButtonMapping = HeadsetButtonMapping.PLAY_PAUSE,
    val doubleClickAction: HeadsetButtonMapping = HeadsetButtonMapping.NEXT_TRACK,
    val tripleClickAction: HeadsetButtonMapping = HeadsetButtonMapping.PREV_TRACK,
    val quadClickAction: HeadsetButtonMapping = HeadsetButtonMapping.SEEK_FORWARD,
    val notificationCompactActions: Boolean = true,
    val notificationShowPlaylistButton: Boolean = true,
    val autoBackupIntervalDays: Int = 0, // 0 = disabled, 1 = daily, 7 = weekly
    val autoBackupDirectoryUri: String = "",
    val savedSortOrder: TrackSortOrder = TrackSortOrder.TITLE_ASC,
    val savedFormatFilter: String = "Todas",
    val visualizerStyle: String = "BARS", // BARS, WAVE, PEAKS
    val visualizerAnimationIntensity: Float = 1.0f, // 0.5f = Suave, 1.0f = Normal, 1.5f = Intensa
    val playerParticlesEnabled: Boolean = true,
    val playerAmbientBackgroundEnabled: Boolean = true,
    val playerShowStats: Boolean = true,
    val playerQueueVisibleCount: Int = 4, // 3..5
    val playerAnimatedTransitions: Boolean = true,
)

class AppPreferencesStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val ACTIVE_THEME_ID = stringPreferencesKey("active_theme_id")
        val ACTIVE_QUEUE_ID = longPreferencesKey("active_queue_id")
        val LAST_TRACK_ID = longPreferencesKey("last_track_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val GAPLESS_ENABLED = booleanPreferencesKey("gapless_enabled")
        val BATTERY_SAVER_ANIM = booleanPreferencesKey("battery_saver_anim")
        val VISUALIZER_ENABLED = booleanPreferencesKey("visualizer_enabled")
        val MONITORED_FOLDERS = stringSetPreferencesKey("monitored_folders")
        val CLICK_1 = stringPreferencesKey("headset_click_1")
        val CLICK_2 = stringPreferencesKey("headset_click_2")
        val CLICK_3 = stringPreferencesKey("headset_click_3")
        val CLICK_4 = stringPreferencesKey("headset_click_4")
        val NOTIF_COMPACT = booleanPreferencesKey("notif_compact")
        val NOTIF_PLAYLIST_BTN = booleanPreferencesKey("notif_playlist_btn")
        val AUTO_BACKUP_DAYS = intPreferencesKey("auto_backup_days")
        val AUTO_BACKUP_URI = stringPreferencesKey("auto_backup_uri")
        val LIBRARY_SORT_ORDER = stringPreferencesKey("library_sort_order")
        val LIBRARY_FORMAT_FILTER = stringPreferencesKey("library_format_filter")
        val VISUALIZER_STYLE = stringPreferencesKey("visualizer_style")
        val VISUALIZER_INTENSITY = floatPreferencesKey("visualizer_intensity")
        val PLAYER_PARTICLES = booleanPreferencesKey("player_particles")
        val PLAYER_AMBIENT_BG = booleanPreferencesKey("player_ambient_bg")
        val PLAYER_SHOW_STATS = booleanPreferencesKey("player_show_stats")
        val PLAYER_QUEUE_COUNT = intPreferencesKey("player_queue_count")
        val PLAYER_ANIM_TRANSITIONS = booleanPreferencesKey("player_anim_transitions")
    }

    val preferencesFlow: Flow<AppUserPreferences> = context.myatoDataStore.data.map { prefs ->
        AppUserPreferences(
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_DONE] ?: false,
            activeThemeId = prefs[Keys.ACTIVE_THEME_ID] ?: BuiltInThemes.MidnightOrange.id,
            activeQueueId = prefs[Keys.ACTIVE_QUEUE_ID] ?: 1L,
            lastTrackId = prefs[Keys.LAST_TRACK_ID] ?: -1L,
            lastPositionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,
            gaplessEnabled = prefs[Keys.GAPLESS_ENABLED] ?: true,
            batterySaverAnimations = prefs[Keys.BATTERY_SAVER_ANIM] ?: false,
            visualizerEnabled = prefs[Keys.VISUALIZER_ENABLED] ?: true,
            monitoredFolders = prefs[Keys.MONITORED_FOLDERS] ?: emptySet(),
            singleClickAction = parseMapping(prefs[Keys.CLICK_1], HeadsetButtonMapping.PLAY_PAUSE),
            doubleClickAction = parseMapping(prefs[Keys.CLICK_2], HeadsetButtonMapping.NEXT_TRACK),
            tripleClickAction = parseMapping(prefs[Keys.CLICK_3], HeadsetButtonMapping.PREV_TRACK),
            quadClickAction = parseMapping(prefs[Keys.CLICK_4], HeadsetButtonMapping.SEEK_FORWARD),
            notificationCompactActions = prefs[Keys.NOTIF_COMPACT] ?: true,
            notificationShowPlaylistButton = prefs[Keys.NOTIF_PLAYLIST_BTN] ?: true,
            autoBackupIntervalDays = prefs[Keys.AUTO_BACKUP_DAYS] ?: 0,
            autoBackupDirectoryUri = prefs[Keys.AUTO_BACKUP_URI] ?: "",
            savedSortOrder = parseSortOrder(prefs[Keys.LIBRARY_SORT_ORDER]),
            savedFormatFilter = prefs[Keys.LIBRARY_FORMAT_FILTER] ?: "Todas",
            visualizerStyle = prefs[Keys.VISUALIZER_STYLE] ?: "BARS",
            visualizerAnimationIntensity = prefs[Keys.VISUALIZER_INTENSITY] ?: 1.0f,
            playerParticlesEnabled = prefs[Keys.PLAYER_PARTICLES] ?: true,
            playerAmbientBackgroundEnabled = prefs[Keys.PLAYER_AMBIENT_BG] ?: true,
            playerShowStats = prefs[Keys.PLAYER_SHOW_STATS] ?: true,
            playerQueueVisibleCount = prefs[Keys.PLAYER_QUEUE_COUNT] ?: 4,
            playerAnimatedTransitions = prefs[Keys.PLAYER_ANIM_TRANSITIONS] ?: true,
        )
    }

    private fun parseMapping(raw: String?, fallback: HeadsetButtonMapping): HeadsetButtonMapping {
        return raw?.let { runCatching { HeadsetButtonMapping.valueOf(it) }.getOrNull() } ?: fallback
    }

    private fun parseSortOrder(raw: String?): TrackSortOrder {
        return raw?.let { runCatching { TrackSortOrder.valueOf(it) }.getOrNull() } ?: TrackSortOrder.TITLE_ASC
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.myatoDataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setActiveThemeId(themeId: String) {
        context.myatoDataStore.edit { it[Keys.ACTIVE_THEME_ID] = themeId }
    }

    suspend fun setActiveQueueState(queueId: Long, trackId: Long, positionMs: Long) {
        context.myatoDataStore.edit {
            it[Keys.ACTIVE_QUEUE_ID] = queueId
            it[Keys.LAST_TRACK_ID] = trackId
            it[Keys.LAST_POSITION_MS] = positionMs
        }
    }

    suspend fun setGaplessEnabled(enabled: Boolean) {
        context.myatoDataStore.edit { it[Keys.GAPLESS_ENABLED] = enabled }
    }

    suspend fun setBatterySaverAnimations(enabled: Boolean) {
        context.myatoDataStore.edit { it[Keys.BATTERY_SAVER_ANIM] = enabled }
    }

    suspend fun setVisualizerEnabled(enabled: Boolean) {
        context.myatoDataStore.edit { it[Keys.VISUALIZER_ENABLED] = enabled }
    }

    suspend fun setMonitoredFolders(folders: Set<String>) {
        context.myatoDataStore.edit { it[Keys.MONITORED_FOLDERS] = folders }
    }

    suspend fun setHeadsetMappings(
        single: HeadsetButtonMapping,
        doubleClick: HeadsetButtonMapping,
        triple: HeadsetButtonMapping,
        quad: HeadsetButtonMapping,
    ) {
        context.myatoDataStore.edit {
            it[Keys.CLICK_1] = single.name
            it[Keys.CLICK_2] = doubleClick.name
            it[Keys.CLICK_3] = triple.name
            it[Keys.CLICK_4] = quad.name
        }
    }

    suspend fun setNotificationPreferences(compact: Boolean, showPlaylistBtn: Boolean) {
        context.myatoDataStore.edit {
            it[Keys.NOTIF_COMPACT] = compact
            it[Keys.NOTIF_PLAYLIST_BTN] = showPlaylistBtn
        }
    }

    suspend fun setAutoBackupConfig(intervalDays: Int, directoryUri: String) {
        context.myatoDataStore.edit {
            it[Keys.AUTO_BACKUP_DAYS] = intervalDays
            it[Keys.AUTO_BACKUP_URI] = directoryUri
        }
    }

    suspend fun setLibrarySortOrder(order: TrackSortOrder) {
        context.myatoDataStore.edit {
            it[Keys.LIBRARY_SORT_ORDER] = order.name
        }
    }

    suspend fun setLibraryFormatFilter(format: String) {
        context.myatoDataStore.edit {
            it[Keys.LIBRARY_FORMAT_FILTER] = format
        }
    }

    suspend fun setVisualizerStyle(style: String) {
        context.myatoDataStore.edit {
            it[Keys.VISUALIZER_STYLE] = style
        }
    }

    suspend fun setVisualizerAnimationIntensity(intensity: Float) {
        context.myatoDataStore.edit {
            it[Keys.VISUALIZER_INTENSITY] = intensity
        }
    }

    suspend fun setPlayerParticlesEnabled(enabled: Boolean) {
        context.myatoDataStore.edit {
            it[Keys.PLAYER_PARTICLES] = enabled
        }
    }

    suspend fun setPlayerAmbientBackgroundEnabled(enabled: Boolean) {
        context.myatoDataStore.edit {
            it[Keys.PLAYER_AMBIENT_BG] = enabled
        }
    }

    suspend fun setPlayerShowStats(enabled: Boolean) {
        context.myatoDataStore.edit {
            it[Keys.PLAYER_SHOW_STATS] = enabled
        }
    }

    suspend fun setPlayerQueueVisibleCount(count: Int) {
        context.myatoDataStore.edit {
            it[Keys.PLAYER_QUEUE_COUNT] = count.coerceIn(3, 5)
        }
    }

    suspend fun setPlayerAnimatedTransitions(enabled: Boolean) {
        context.myatoDataStore.edit {
            it[Keys.PLAYER_ANIM_TRANSITIONS] = enabled
        }
    }
}
