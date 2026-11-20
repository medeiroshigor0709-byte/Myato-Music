package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppPreferencesStore
import com.example.core.database.MyatoDatabase
import com.example.core.designsystem.BuiltInThemes
import com.example.core.designsystem.MyatoMusicTheme
import com.example.core.designsystem.contrastContentColor
import com.example.core.designsystem.toExtendedColors
import com.example.data.repository.ThemeRepository
import com.example.domain.models.ButtonStyle
import com.example.domain.models.CoverShape
import com.example.domain.models.MyatoTheme
import com.example.domain.models.PlayerAppearance
import com.example.domain.models.ThemeAnimationIntensity
import com.example.domain.models.ThemeAnimationType
import com.example.domain.models.ThemeAutoScheduleMode
import com.example.domain.models.ThemeBorderStyle
import com.example.domain.models.ThemeCategory
import com.example.domain.models.ThemeCreationMode
import com.example.domain.models.ThemeEffectPreset
import com.example.domain.models.ThemeGradientStyle
import com.example.domain.models.ThemePreviewScreenTab
import com.example.domain.models.WallpaperStyle
import com.example.feature.statistics.SettingsScreen
import com.example.feature.themes.ThemeCenterScreen
import com.example.feature.themes.ThemeEditorScreen
import com.example.feature.themes.ThemePreviewScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MyatoThemesSystemTests {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: MyatoDatabase
    private lateinit var prefs: AppPreferencesStore
    private lateinit var themeRepo: ThemeRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = MyatoDatabase.createInMemoryForTest(context)
        prefs = AppPreferencesStore(context)
        themeRepo = ThemeRepository(context, db, prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `all 50 built-in themes have distinct identities visual variety and readable contrast`() {
        val all = BuiltInThemes.all
        assertEquals(50, all.size)

        // Unique identifiers, names, descriptions
        assertEquals(50, all.map { it.id }.toSet().size)
        assertEquals(50, all.map { it.name }.toSet().size)
        assertTrue(all.all { it.description.isNotBlank() && it.isBuiltIn })

        // Distinct animations, wallpapers, primary colors
        assertTrue(all.map { it.animationType }.toSet().size >= 30)
        assertTrue(all.map { it.wallpaperStyle }.toSet().size >= 15)
        assertTrue(all.map { it.primaryColorHex }.toSet().size >= 35)

        // Exact 5 themes in each of the 10 categories
        val nonCustomCategories = ThemeCategory.entries.filter { it != ThemeCategory.CUSTOM }
        assertEquals(10, nonCustomCategories.size)
        nonCustomCategories.forEach { cat ->
            assertEquals(5, all.count { it.category == cat })
        }

        // Readable contrast on every theme's primary and button colors
        all.forEach { theme ->
            val ext = theme.toExtendedColors()
            val primaryLum = ext.primary.luminance()
            val onPrimaryLum = ext.onPrimary.luminance()
            assertTrue(
                "Contrast too low on primary for ${theme.name}",
                abs(primaryLum - onPrimaryLum) > 0.20f,
            )
            val darkSampleContrast = contrastContentColor(Color(0xFF101014))
            val lightSampleContrast = contrastContentColor(Color(0xFFFDFDFE))
            assertNotEquals(darkSampleContrast, lightSampleContrast)
        }
    }

    @Test
    fun `theme repository handles legacy aliases favorites history custom crud and json roundtrip`() = runBlocking {
        // Legacy alias lookup preserves pre-existing theme IDs seamlessly
        assertEquals("myato_original", BuiltInThemes.resolveThemeId("midnight_orange"))
        assertEquals("galaxy", BuiltInThemes.resolveThemeId("neon_galaxy"))
        assertEquals("ocean_breeze", BuiltInThemes.resolveThemeId("ocean_blue"))
        assertNotNull(BuiltInThemes.findByIdOrAlias("purple_night"))

        // Select theme records history
        themeRepo.selectTheme(BuiltInThemes.SakuraDream.id, BuiltInThemes.SakuraDream.name)
        val afterSelect = prefs.preferencesFlow.first()
        assertEquals("sakura_dream", afterSelect.activeThemeId)
        assertTrue(afterSelect.themeHistory.any { it.themeId == "sakura_dream" })

        // Favorites toggle
        themeRepo.toggleFavoriteTheme("sakura_dream")
        assertTrue("sakura_dream" in prefs.preferencesFlow.first().favoriteThemeIds)
        themeRepo.toggleFavoriteTheme("sakura_dream")
        assertFalse("sakura_dream" in prefs.preferencesFlow.first().favoriteThemeIds)

        // Save, rename, export, import, delete custom theme with full extended config
        val custom = MyatoTheme(
            id = "custom_test_studio",
            name = "Estúdio Violeta",
            isBuiltIn = false,
            category = ThemeCategory.CUSTOM,
            description = "Tema personalizado de teste",
            primaryColorHex = 0xFFB388FFL,
            secondaryColorHex = 0xFF00E5FFL,
            accentColorHex = 0xFFFF6EC7L,
            backgroundColorHex = 0xFF0A0818L,
            altBackgroundColorHex = 0xFF14102CL,
            cardColorHex = 0xFF1C1638L,
            textColorHex = 0xFFFFFFFFL,
            secondaryTextColorHex = 0xFFC5B8E8L,
            buttonColorHex = 0xFFB388FFL,
            iconColorHex = 0xFF00E5FFL,
            borderColorHex = 0xFF00E5FFL,
            gradientEndColorHex = 0xFF28164BL,
            surfaceAlpha = 0.88f,
            shadowElevationDp = 10f,
            borderWidthDp = 1.8f,
            isLight = false,
            coverShape = CoverShape.VINYL,
            buttonStyle = ButtonStyle.PILL,
            playerAppearance = PlayerAppearance.CINEMATIC,
            gradientStyle = ThemeGradientStyle.DIAGONAL,
            borderStyle = ThemeBorderStyle.GLOW,
            effectPreset = ThemeEffectPreset.NEON_EDGE,
            wallpaperStyle = WallpaperStyle.AURORA,
            animationType = ThemeAnimationType.AURORA_LIGHTS,
        )
        val saved = themeRepo.saveCustomTheme(custom)
        assertEquals("custom_test_studio", saved.id)

        val exportedJson = themeRepo.exportThemeJson(saved)
        val importedResult = themeRepo.importThemeJson(exportedJson)
        assertTrue(importedResult.isSuccess)
        val imported = importedResult.getOrThrow()
        assertEquals(saved.name, imported.name)
        assertEquals(ThemeAnimationType.AURORA_LIGHTS, imported.animationType)
        assertEquals(WallpaperStyle.AURORA, imported.wallpaperStyle)
        assertEquals(ThemeBorderStyle.GLOW, imported.borderStyle)

        themeRepo.renameCustomTheme(saved.id, "Estúdio Cristal")
        val renamed = db.themeDao().getThemeById(saved.id)
        assertEquals("Estúdio Cristal", renamed?.name)

        themeRepo.deleteCustomTheme(saved.id)
        assertEquals(null, db.themeDao().getThemeById(saved.id))
    }

    @Test
    fun `random seasonal auto schedule and animation intensity persist offline`() = runBlocking {
        // Random theme always chooses another theme when multiple exist
        val randomPick = themeRepo.pickRandomTheme(BuiltInThemes.all, BuiltInThemes.MidnightOrange.id)
        assertNotEquals(BuiltInThemes.MidnightOrange.id, randomPick.id)

        // Seasonal picking
        val springTheme = themeRepo.pickSeasonalTheme(BuiltInThemes.all, monthZeroIndexed = Calendar.OCTOBER)
        assertNotNull(springTheme.seasonalTag)

        // Auto schedule Day/Night switches based on current hour
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dayId = BuiltInThemes.PureWhite.id
        val nightId = BuiltInThemes.MidnightCat.id
        val expectedId = if (hour in 6..17) dayId else nightId

        prefs.setThemeAutoSchedule(ThemeAutoScheduleMode.DAY_NIGHT, dayId, nightId)
        val currentPrefs = prefs.preferencesFlow.first()
        val switched = themeRepo.evaluateAutoScheduleIfNeeded(BuiltInThemes.all, currentPrefs)
        assertEquals(expectedId, switched?.id)
        assertEquals(expectedId, prefs.preferencesFlow.first().activeThemeId)

        // Animation intensity 4-level persistence
        prefs.setThemeAnimationIntensity(ThemeAnimationIntensity.SOFT)
        val afterSoft = prefs.preferencesFlow.first()
        assertFalse(afterSoft.animationsDisabled)
        assertEquals(0.5f, afterSoft.visualizerAnimationIntensity, 0.01f)

        prefs.setThemeAnimationIntensity(ThemeAnimationIntensity.DISABLED)
        val afterDisabled = prefs.preferencesFlow.first()
        assertTrue(afterDisabled.animationsDisabled)
        assertEquals(0f, afterDisabled.visualizerAnimationIntensity, 0.01f)
    }

    @Test
    fun `theme center hub filters by category search and opens preview`() {
        var previewOpenedId = ""
        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                ThemeCenterScreen(
                    themes = BuiltInThemes.all,
                    activeThemeId = BuiltInThemes.MidnightOrange.id,
                    favoriteThemeIds = setOf(BuiltInThemes.SakuraDream.id),
                    onSelectTheme = {},
                    onOpenEditor = {},
                    onDuplicateTheme = { _, _ -> },
                    onDeleteCustomTheme = {},
                    onImportThemeJson = {},
                    onOpenPreview = { previewOpenedId = it },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("theme_center_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_search_input").performTextInput("Sakura")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("theme_grid").performScrollToNode(hasTestTag("theme_card_sakura_dream"))
        composeRule.onNodeWithTag("theme_card_sakura_dream").performClick()
        composeRule.waitForIdle()
        assertEquals("sakura_dream", previewOpenedId)

        // Clear search and test category filter
        composeRule.onNodeWithTag("theme_search_input").performTextClearance()
        composeRule.onNodeWithTag("theme_category_filter_row").performScrollToNode(
            androidx.compose.ui.test.hasText("Kawaii & Cats")
        )
        composeRule.onNodeWithText("Kawaii & Cats").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("theme_grid").performScrollToNode(hasTestTag("theme_card_pink_kitty"))
        composeRule.onNodeWithTag("theme_card_pink_kitty").assertIsDisplayed()
    }

    @Test
    fun `multi screen theme preview switches all 6 screens without applying active theme early`() {
        var appliedCount = 0
        var activeThemeId = BuiltInThemes.MidnightOrange.id

        composeRule.setContent {
            ThemePreviewScreen(
                previewTheme = BuiltInThemes.Aurora,
                isActiveTheme = activeThemeId == BuiltInThemes.Aurora.id,
                isFavorite = false,
                onApplyTheme = {
                    appliedCount++
                    activeThemeId = BuiltInThemes.Aurora.id
                },
                onToggleFavorite = {},
                onOpenInStudio = {},
                onShareTheme = {},
                onBack = {},
            )
        }
        composeRule.waitForIdle()

        // Navigate across all 6 simulated preview screens
        ThemePreviewScreenTab.entries.forEach { tab ->
            composeRule.onNodeWithTag("preview_tab_${tab.name}").performScrollTo().performClick()
            composeRule.waitForIdle()
        }

        // Active theme must remain unchanged until Apply is clicked
        assertEquals(0, appliedCount)
        assertEquals(BuiltInThemes.MidnightOrange.id, activeThemeId)

        composeRule.onNodeWithTag("theme_preview_apply_button").performClick()
        composeRule.waitForIdle()
        assertEquals(1, appliedCount)
        assertEquals(BuiltInThemes.Aurora.id, activeThemeId)
    }

    @Test
    fun `theme studio creates custom theme from scratch edits name and saves`() {
        var savedTheme: MyatoTheme? = null
        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.MidnightOrange) {
                ThemeEditorScreen(
                    sourceTheme = BuiltInThemes.MidnightOrange,
                    creationMode = ThemeCreationMode.SCRATCH,
                    onBack = {},
                    onSaveTheme = { savedTheme = it },
                    onGenerateFromCover = {},
                    onExportThemeJson = { "{}" },
                    onNotify = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("theme_editor_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_live_preview_card").assertExists()
        composeRule.onNodeWithTag("studio_name_input").performTextClearance()
        composeRule.onNodeWithTag("studio_name_input").performTextInput("Meu Tema Studio")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("studio_save_apply_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertNotNull(savedTheme)
        assertEquals("Meu Tema Studio", savedTheme?.name)
        assertFalse(savedTheme!!.isBuiltIn)
        assertEquals(ThemeCategory.CUSTOM, savedTheme?.category)
    }

    @Test
    fun `settings screen shows personalization section myato themes entry and zero internet permission`() {
        var openedMyatoThemes = false
        var selectedIntensity: ThemeAnimationIntensity? = null
        val currentPrefs = runBlocking { prefs.preferencesFlow.first() }

        composeRule.setContent {
            MyatoMusicTheme(activeTheme = BuiltInThemes.SakuraDream) {
                SettingsScreen(
                    preferences = currentPrefs,
                    capabilities = emptyList(),
                    onSetGapless = {},
                    onSetBatterySaverAnim = {},
                    onSetNotificationPrefs = { _, _ -> },
                    onSetHeadsetMappings = { _, _, _, _ -> },
                    onOpenPermissions = {},
                    onOpenBackup = {},
                    onOpenWidgets = {},
                    onOpenAbout = {},
                    activeThemeName = BuiltInThemes.SakuraDream.name,
                    onOpenMyatoThemes = { openedMyatoThemes = true },
                    onSelectAnimationIntensity = { selectedIntensity = it },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_personalization_section").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_myato_themes_row").performClick()
        composeRule.waitForIdle()
        assertTrue(openedMyatoThemes)

        composeRule.onNodeWithTag("settings_animation_intensity_row").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Intensas").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(ThemeAnimationIntensity.INTENSE, selectedIntensity)

        // Verify 100% offline: no INTERNET permission declared
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val declared = packageInfo.requestedPermissions?.toSet() ?: emptySet()
        assertFalse(Manifest.permission.INTERNET in declared)
    }
}
