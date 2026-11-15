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
import com.example.core.designsystem.AppFontCatalog
import com.example.core.designsystem.BuiltInThemes
import com.example.domain.models.AppFontOption
import com.example.domain.models.FontApplyTarget
import com.example.domain.models.HeadsetButtonMapping
import com.example.domain.models.PlayerVisualBackgroundStyle
import com.example.domain.models.TrackSortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

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
    val playerVisualStyle: PlayerVisualBackgroundStyle = PlayerVisualBackgroundStyle.AUDIO_SPECTRUM,
    val animationsDisabled: Boolean = false,
    val reduceMotion: Boolean = false,
    val reduceEffectsOnLowBattery: Boolean = true,
    val activeFontId: String = AppFontCatalog.DEFAULT_FONT_ID,
    val activeGeneralFontId: String = AppFontCatalog.DEFAULT_FONT_ID,
    val activeDecorativeFontId: String = AppFontCatalog.DEFAULT_FONT_ID,
    val favoriteFontIds: Set<String> = emptySet(),
    val recentFontIds: List<String> = emptyList(),
    val customFonts: List<AppFontOption> = emptyList(),
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
        val PLAYER_VISUAL_STYLE = stringPreferencesKey("player_visual_style")
        val ANIMATIONS_DISABLED = booleanPreferencesKey("animations_disabled")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val REDUCE_EFFECTS_LOW_BATTERY = booleanPreferencesKey("reduce_effects_low_battery")
        val ACTIVE_FONT_ID = stringPreferencesKey("active_font_id")
        val ACTIVE_GENERAL_FONT_ID = stringPreferencesKey("active_general_font_id")
        val ACTIVE_DECORATIVE_FONT_ID = stringPreferencesKey("active_decorative_font_id")
        val FAVORITE_FONT_IDS = stringSetPreferencesKey("favorite_font_ids")
        val RECENT_FONT_IDS_JSON = stringPreferencesKey("recent_font_ids_json")
        val CUSTOM_FONTS_JSON = stringPreferencesKey("custom_fonts_json")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val preferencesFlow: Flow<AppUserPreferences> = context.myatoDataStore.data.map { prefs ->
        val legacyFontId = prefs[Keys.ACTIVE_FONT_ID] ?: AppFontCatalog.DEFAULT_FONT_ID
        val generalId = prefs[Keys.ACTIVE_GENERAL_FONT_ID] ?: legacyFontId
        val decorativeId = prefs[Keys.ACTIVE_DECORATIVE_FONT_ID] ?: legacyFontId
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
            playerVisualStyle = PlayerVisualBackgroundStyle.fromId(prefs[Keys.PLAYER_VISUAL_STYLE]),
            animationsDisabled = prefs[Keys.ANIMATIONS_DISABLED] ?: false,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: (prefs[Keys.BATTERY_SAVER_ANIM] ?: false),
            reduceEffectsOnLowBattery = prefs[Keys.REDUCE_EFFECTS_LOW_BATTERY] ?: true,
            activeFontId = generalId,
            activeGeneralFontId = generalId,
            activeDecorativeFontId = decorativeId,
            favoriteFontIds = prefs[Keys.FAVORITE_FONT_IDS] ?: emptySet(),
            recentFontIds = parseStringList(prefs[Keys.RECENT_FONT_IDS_JSON]),
            customFonts = parseCustomFonts(prefs[Keys.CUSTOM_FONTS_JSON]),
        )
    }

    private fun parseStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<String>>(raw)
        }.getOrDefault(emptyList())
    }

    private fun parseCustomFonts(raw: String?): List<AppFontOption> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<AppFontOption>>(raw).filter { font ->
                val path = font.customFilePath
                !path.isNullOrBlank() && File(path).let { it.exists() && it.isFile && it.length() > 0L }
            }
        }.getOrDefault(emptyList())
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

    suspend fun setPlayerVisualStyle(style: PlayerVisualBackgroundStyle) {
        context.myatoDataStore.edit {
            it[Keys.PLAYER_VISUAL_STYLE] = style.id
        }
    }

    suspend fun setAnimationsDisabled(disabled: Boolean) {
        context.myatoDataStore.edit {
            it[Keys.ANIMATIONS_DISABLED] = disabled
        }
    }

    suspend fun setReduceMotion(reduced: Boolean) {
        context.myatoDataStore.edit {
            it[Keys.REDUCE_MOTION] = reduced
            it[Keys.BATTERY_SAVER_ANIM] = reduced
        }
    }

    suspend fun setReduceEffectsOnLowBattery(enabled: Boolean) {
        context.myatoDataStore.edit {
            it[Keys.REDUCE_EFFECTS_LOW_BATTERY] = enabled
        }
    }

    suspend fun setActiveFontId(fontId: String) {
        setFontSelection(fontId, FontApplyTarget.BOTH)
    }

    suspend fun setFontSelection(fontId: String, target: FontApplyTarget) {
        context.myatoDataStore.edit { prefs ->
            when (target) {
                FontApplyTarget.GENERAL -> {
                    prefs[Keys.ACTIVE_FONT_ID] = fontId
                    prefs[Keys.ACTIVE_GENERAL_FONT_ID] = fontId
                }
                FontApplyTarget.DECORATIVE -> {
                    prefs[Keys.ACTIVE_DECORATIVE_FONT_ID] = fontId
                }
                FontApplyTarget.BOTH -> {
                    prefs[Keys.ACTIVE_FONT_ID] = fontId
                    prefs[Keys.ACTIVE_GENERAL_FONT_ID] = fontId
                    prefs[Keys.ACTIVE_DECORATIVE_FONT_ID] = fontId
                }
            }
            val currentRecents = parseStringList(prefs[Keys.RECENT_FONT_IDS_JSON]).filterNot { it == fontId }
            val updatedRecents = (listOf(fontId) + currentRecents).take(10)
            prefs[Keys.RECENT_FONT_IDS_JSON] = json.encodeToString(updatedRecents)
        }
    }

    suspend fun toggleFavoriteFont(fontId: String) {
        context.myatoDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_FONT_IDS] ?: emptySet()
            prefs[Keys.FAVORITE_FONT_IDS] = if (fontId in current) current - fontId else current + fontId
        }
    }

    suspend fun resetDefaultFont() {
        context.myatoDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_FONT_ID] = AppFontCatalog.DEFAULT_FONT_ID
            prefs[Keys.ACTIVE_GENERAL_FONT_ID] = AppFontCatalog.DEFAULT_FONT_ID
            prefs[Keys.ACTIVE_DECORATIVE_FONT_ID] = AppFontCatalog.DEFAULT_FONT_ID
        }
    }

    suspend fun addCustomFont(font: AppFontOption) {
        context.myatoDataStore.edit { prefs ->
            val current = parseCustomFonts(prefs[Keys.CUSTOM_FONTS_JSON]).filterNot { it.id == font.id }
            prefs[Keys.CUSTOM_FONTS_JSON] = json.encodeToString(current + font)
        }
    }

    suspend fun removeCustomFont(fontId: String) {
        context.myatoDataStore.edit { prefs ->
            val updated = parseCustomFonts(prefs[Keys.CUSTOM_FONTS_JSON]).filterNot { it.id == fontId }
            prefs[Keys.CUSTOM_FONTS_JSON] = json.encodeToString(updated)
            if (prefs[Keys.ACTIVE_FONT_ID] == fontId) {
                prefs[Keys.ACTIVE_FONT_ID] = AppFontCatalog.DEFAULT_FONT_ID
            }
            if (prefs[Keys.ACTIVE_GENERAL_FONT_ID] == fontId) {
                prefs[Keys.ACTIVE_GENERAL_FONT_ID] = AppFontCatalog.DEFAULT_FONT_ID
            }
            if (prefs[Keys.ACTIVE_DECORATIVE_FONT_ID] == fontId) {
                prefs[Keys.ACTIVE_DECORATIVE_FONT_ID] = AppFontCatalog.DEFAULT_FONT_ID
            }
            val favs = prefs[Keys.FAVORITE_FONT_IDS] ?: emptySet()
            if (fontId in favs) {
                prefs[Keys.FAVORITE_FONT_IDS] = favs - fontId
            }
            val recents = parseStringList(prefs[Keys.RECENT_FONT_IDS_JSON]).filterNot { it == fontId }
            prefs[Keys.RECENT_FONT_IDS_JSON] = json.encodeToString(recents)
        }
    }
}
