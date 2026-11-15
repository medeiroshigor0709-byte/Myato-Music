package com.example.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose destinations covering all 25 required MYATO MUSIC screens.
 */
sealed interface MyatoRoute {
    @Serializable data object Splash : MyatoRoute
    @Serializable data object Onboarding : MyatoRoute
    @Serializable data object Permissions : MyatoRoute

    // Bottom Navigation Top-Level Tabs
    @Serializable data object Home : MyatoRoute
    @Serializable data object LibraryHub : MyatoRoute
    @Serializable data object Playlists : MyatoRoute
    @Serializable data object ThemeCenter : MyatoRoute
    @Serializable data object Settings : MyatoRoute

    // Library Sub-sections
    @Serializable data object AllSongs : MyatoRoute
    @Serializable data object Artists : MyatoRoute
    @Serializable data object Albums : MyatoRoute
    @Serializable data object Folders : MyatoRoute
    @Serializable data object Genres : MyatoRoute
    @Serializable data object Favorites : MyatoRoute

    // Core Player & Tools
    @Serializable data object Queues : MyatoRoute
    @Serializable data object FullPlayer : MyatoRoute
    @Serializable data class Lyrics(val trackId: Long) : MyatoRoute
    @Serializable data class TagEditor(val trackIdsCsv: String) : MyatoRoute
    @Serializable data object FileManager : MyatoRoute
    @Serializable data object Equalizer : MyatoRoute
    @Serializable data object SleepTimer : MyatoRoute
    @Serializable data object Statistics : MyatoRoute
    @Serializable data class ThemeEditor(val sourceThemeId: String) : MyatoRoute
    @Serializable data object WidgetsShortcuts : MyatoRoute
    @Serializable data object Backup : MyatoRoute
    @Serializable data object About : MyatoRoute
}
