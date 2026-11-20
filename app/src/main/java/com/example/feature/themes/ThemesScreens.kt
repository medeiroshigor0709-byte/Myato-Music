package com.example.feature.themes

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.designsystem.ThemeThumbnailArtwork
import com.example.core.designsystem.contrastContentColor
import com.example.domain.models.MyatoTheme
import com.example.domain.models.ThemeAnimationIntensity
import com.example.domain.models.ThemeAutoScheduleMode
import com.example.domain.models.ThemeCategory
import com.example.domain.models.ThemeCreationMode
import com.example.domain.models.ThemeUsageHistoryEntry

private enum class HubSectionFilter(val label: String) {
    ALL("Todos"),
    FEATURED("Destaques"),
    FAVORITES("Favoritos"),
    CUSTOM("Personalizados"),
    SEASONAL("Sazonais"),
    HISTORY("Histórico"),
    OFFICIAL("Oficiais"),
}

/**
 * Hub Central Myato Themes: Destaques, 50 temas em 10 categorias, pesquisa,
 * favoritos, histórico, tema aleatório, sazonais, agendador automático,
 * criação (do zero / derivado / de imagem) e pré-visualização multi-tela.
 */
@Composable
fun ThemeCenterScreen(
    themes: List<MyatoTheme>,
    activeThemeId: String,
    onSelectTheme: (String) -> Unit,
    onOpenEditor: (String) -> Unit,
    onDuplicateTheme: (MyatoTheme, String) -> Unit,
    onDeleteCustomTheme: (String) -> Unit,
    onImportThemeJson: (String) -> Unit,
    activeFontName: String = "MYATO Original",
    onOpenFontSettings: () -> Unit = {},
    favoriteThemeIds: Set<String> = emptySet(),
    themeHistory: List<ThemeUsageHistoryEntry> = emptyList(),
    autoScheduleMode: ThemeAutoScheduleMode = ThemeAutoScheduleMode.MANUAL,
    autoDayThemeId: String = activeThemeId,
    autoNightThemeId: String = activeThemeId,
    currentAnimationIntensity: ThemeAnimationIntensity = ThemeAnimationIntensity.NORMAL,
    onOpenPreview: ((String) -> Unit)? = null,
    onOpenEditorWithMode: ((String, ThemeCreationMode) -> Unit)? = null,
    onToggleFavoriteTheme: (String) -> Unit = {},
    onApplyRandomTheme: () -> Unit = {},
    onApplySeasonalTheme: () -> Unit = {},
    onConfigureAutoSchedule: (ThemeAutoScheduleMode, String, String) -> Unit = { _, _, _ -> },
    onSelectAnimationIntensity: (ThemeAnimationIntensity) -> Unit = {},
    onRenameCustomTheme: (String, String) -> Unit = { _, _ -> },
    onShareTheme: (MyatoTheme) -> Unit = {},
) {
    val colors = LocalMyatoColors.current
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var sectionFilter by remember { mutableStateOf(HubSectionFilter.ALL) }
    var selectedCategory by remember { mutableStateOf<ThemeCategory?>(null) }

    var quickPreviewTarget by remember { mutableStateOf<MyatoTheme?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showCreateModeDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<MyatoTheme?>(null) }

    val jsonFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { raw ->
                onImportThemeJson(raw)
                showImportDialog = false
            }
        }
    }

    val activeThemeObj = remember(themes, activeThemeId) {
        themes.firstOrNull { it.id == activeThemeId }
    }

    val featuredThemes = remember(themes) {
        themes.filter { it.isFeatured }.ifEmpty { themes.take(6) }
    }

    val filteredThemes = remember(themes, searchQuery, sectionFilter, selectedCategory, favoriteThemeIds, themeHistory) {
        val base = when (sectionFilter) {
            HubSectionFilter.ALL -> themes
            HubSectionFilter.FEATURED -> themes.filter { it.isFeatured }
            HubSectionFilter.FAVORITES -> themes.filter { it.id in favoriteThemeIds }
            HubSectionFilter.CUSTOM -> themes.filter { !it.isBuiltIn }
            HubSectionFilter.SEASONAL -> themes.filter { !it.seasonalTag.isNullOrBlank() }
            HubSectionFilter.HISTORY -> {
                val order = themeHistory.mapIndexed { idx, e -> e.themeId to idx }.toMap()
                themes.filter { it.id in order }.sortedBy { order[it.id] ?: Int.MAX_VALUE }
            }
            HubSectionFilter.OFFICIAL -> themes.filter { it.isBuiltIn }
        }
        val byCat = if (selectedCategory != null) base.filter { it.category == selectedCategory } else base
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) byCat else {
            byCat.filter {
                it.name.lowercase().contains(q) ||
                    it.category.displayName.lowercase().contains(q) ||
                    it.description.lowercase().contains(q) ||
                    (it.seasonalTag?.lowercase()?.contains(q) == true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("theme_center_screen"),
    ) {
        // Header + Top Tools
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Myato Themes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${themes.size} temas • Ativo: ${activeThemeObj?.name ?: colors.themeName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row {
                IconButton(
                    onClick = { onApplyRandomTheme() },
                    modifier = Modifier.testTag("theme_random_button"),
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = "Tema aleatório", tint = colors.primary)
                }
                IconButton(
                    onClick = { showScheduleDialog = true },
                    modifier = Modifier.testTag("theme_schedule_button"),
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = "Troca automática", tint = colors.primary)
                }
                IconButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.testTag("theme_import_button"),
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = "Importar JSON", tint = colors.primary)
                }
                IconButton(
                    onClick = { showCreateModeDialog = true },
                    modifier = Modifier.testTag("theme_create_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Criar tema", tint = colors.primary)
                }
            }
        }

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar tema por nome, categoria ou estação...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Limpar busca")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("theme_search_input"),
        )

        Spacer(Modifier.height(8.dp))

        // Section chips (Todos, Destaques, Favoritos, Personalizados, Sazonais, Histórico, Oficiais)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("theme_section_filter_row"),
        ) {
            items(HubSectionFilter.entries) { filter ->
                val selected = sectionFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { sectionFilter = filter },
                    label = { Text(filter.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primary,
                        selectedLabelColor = colors.onPrimary,
                    ),
                )
            }
        }

        // 10 Categories chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("theme_category_filter_row"),
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Todas categorias") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.secondary,
                        selectedLabelColor = colors.onSecondary,
                    ),
                )
            }
            items(ThemeCategory.entries.filter { it != ThemeCategory.CUSTOM || themes.any { t -> !t.isBuiltIn } }) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = if (selectedCategory == category) null else category },
                    label = { Text(category.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.secondary,
                        selectedLabelColor = colors.onSecondary,
                    ),
                )
            }
        }

        // Main grid with optional Destaques carousel + Studio CTA + Fonts shortcut
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 164.dp),
            modifier = Modifier.testTag("theme_grid"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 148.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (sectionFilter == HubSectionFilter.ALL && selectedCategory == null && searchQuery.isBlank()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Quick Actions Row (Myato Theme Studio + Tema Sazonal + Fontes)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { showCreateModeDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.buttonContainer,
                                    contentColor = colors.buttonContent,
                                ),
                                shape = colors.resolveButtonShape(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_theme_studio_cta"),
                            ) {
                                Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Theme Studio", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = onApplySeasonalTheme,
                                shape = colors.resolveButtonShape(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sazonal", color = colors.textPrimary, maxLines = 1)
                            }
                        }

                        // Fonts & Intensity Quick Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(onClick = onOpenFontSettings)
                                .testTag("theme_center_font_card"),
                            shape = RoundedCornerShape(18.dp),
                            border = colors.borderStroke(),
                            colors = CardDefaults.cardColors(containerColor = colors.cardSurfaceColor()),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.TextFields, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Fontes e tipografia", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                                        Text("Ativa: $activeFontName", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text("Alterar", color = colors.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Intensidade das animações dos temas", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(ThemeAnimationIntensity.entries) { level ->
                                        FilterChip(
                                            selected = currentAnimationIntensity == level,
                                            onClick = { onSelectAnimationIntensity(level) },
                                            label = { Text(level.label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = colors.primary,
                                                selectedLabelColor = colors.onPrimary,
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                        // Destaques Carousel
                        Text(
                            "Temas em Destaque",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(featuredThemes, key = { "feat_${it.id}" }) { feat ->
                                FeaturedThemeBannerCard(
                                    theme = feat,
                                    isActive = feat.id == activeThemeId,
                                    onClick = {
                                        if (onOpenPreview != null) onOpenPreview(feat.id)
                                        else quickPreviewTarget = feat
                                    },
                                )
                            }
                        }
                        Text(
                            "Catálogo Completo (${filteredThemes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (filteredThemes.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.cardSurfaceColor()),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Nenhum tema encontrado", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text("Tente limpar os filtros ou crie um tema exclusivo no Theme Studio.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        }
                    }
                }
            } else {
                items(filteredThemes, key = { it.id }) { theme ->
                    val isActive = theme.id == activeThemeId
                    val isFavorite = theme.id in favoriteThemeIds
                    ThemeCatalogCard(
                        theme = theme,
                        isActive = isActive,
                        isFavorite = isFavorite,
                        onOpenPreview = {
                            if (onOpenPreview != null) onOpenPreview(theme.id)
                            else quickPreviewTarget = theme
                        },
                        onToggleFavorite = { onToggleFavoriteTheme(theme.id) },
                        onDuplicate = { onDuplicateTheme(theme, "${theme.name} Cópia") },
                        onEdit = {
                            if (onOpenEditorWithMode != null) onOpenEditorWithMode(theme.id, ThemeCreationMode.FROM_THEME)
                            else onOpenEditor(theme.id)
                        },
                        onShare = { onShareTheme(theme) },
                        onRename = if (!theme.isBuiltIn) { { renameTarget = theme } } else null,
                        onDelete = if (!theme.isBuiltIn) { { onDeleteCustomTheme(theme.id) } } else null,
                    )
                }
            }
        }
    }

    // Legacy quick preview dialog fallback (preserved when onOpenPreview is null)
    quickPreviewTarget?.let { target ->
        val targetOnPrimary = contrastContentColor(Color(target.primaryColorHex))
        val targetOnSecondary = contrastContentColor(Color(target.secondaryColorHex))
        AlertDialog(
            onDismissRequest = { quickPreviewTarget = null },
            containerColor = Color(target.backgroundColorHex),
            title = {
                Text("Pré-visualização: ${target.name}", color = Color(target.textColorHex), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(target.cardColorHex))
                        .padding(14.dp),
                ) {
                    Text(target.category.displayName.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color(target.primaryColorHex))
                    Spacer(Modifier.height(4.dp))
                    Text(target.description, style = MaterialTheme.typography.bodySmall, color = Color(target.secondaryTextColorHex))
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = CircleShape, color = Color(target.primaryColorHex)) {
                            Text("Principal", color = targetOnPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                        Surface(shape = CircleShape, color = Color(target.secondaryColorHex)) {
                            Text("Secundária", color = targetOnSecondary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSelectTheme(target.id)
                        quickPreviewTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(target.primaryColorHex), contentColor = targetOnPrimary),
                ) {
                    Text("Aplicar Tema")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        quickPreviewTarget = null
                        onOpenEditor(target.id)
                    }) { Text("Editar", color = Color(target.secondaryColorHex)) }
                    TextButton(onClick = { quickPreviewTarget = null }) {
                        Text("Fechar", color = Color(target.secondaryTextColorHex))
                    }
                }
            },
        )
    }

    // Create Mode Selection Dialog (SCRATCH / FROM_THEME / FROM_IMAGE)
    if (showCreateModeDialog) {
        AlertDialog(
            onDismissRequest = { showCreateModeDialog = false },
            containerColor = colors.card,
            title = { Text("Criar Tema no Studio", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CreationModeOptionRow(
                        icon = Icons.Rounded.Add,
                        title = "Criar do Zero",
                        subtitle = "Comece com uma tela limpa e defina cada cor, borda, fundo e animação.",
                        onClick = {
                            showCreateModeDialog = false
                            if (onOpenEditorWithMode != null) onOpenEditorWithMode(activeThemeId, ThemeCreationMode.SCRATCH)
                            else onOpenEditor(activeThemeId)
                        },
                    )
                    CreationModeOptionRow(
                        icon = Icons.Rounded.Edit,
                        title = "Personalizar Tema Existente",
                        subtitle = "Use \"${activeThemeObj?.name ?: "Myato Original"}\" como base editável.",
                        onClick = {
                            showCreateModeDialog = false
                            if (onOpenEditorWithMode != null) onOpenEditorWithMode(activeThemeId, ThemeCreationMode.FROM_THEME)
                            else onOpenEditor(activeThemeId)
                        },
                    )
                    CreationModeOptionRow(
                        icon = Icons.Rounded.Image,
                        title = "Gerar de uma Imagem da Galeria",
                        subtitle = "Escolha uma foto offline para extrair paleta de cores e/ou usar como fundo.",
                        onClick = {
                            showCreateModeDialog = false
                            if (onOpenEditorWithMode != null) onOpenEditorWithMode(activeThemeId, ThemeCreationMode.FROM_IMAGE)
                            else onOpenEditor(activeThemeId)
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCreateModeDialog = false }) { Text("Fechar", color = colors.primary) }
            },
        )
    }

    // Auto Schedule Dialog
    if (showScheduleDialog) {
        var modeDraft by remember { mutableStateOf(autoScheduleMode) }
        var dayThemeDraft by remember { mutableStateOf(autoDayThemeId) }
        var nightThemeDraft by remember { mutableStateOf(autoNightThemeId) }

        val lightCandidates = remember(themes) { themes.filter { it.isLight }.ifEmpty { themes } }
        val darkCandidates = remember(themes) { themes.filter { !it.isLight }.ifEmpty { themes } }

        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            containerColor = colors.card,
            title = { Text("Agendamento de Temas", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeAutoScheduleMode.entries.forEach { mode ->
                        val selected = modeDraft == mode
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { modeDraft = mode },
                            border = if (selected) BorderStroke(1.5.dp, colors.primary) else null,
                            colors = CardDefaults.cardColors(containerColor = if (selected) colors.primary.copy(alpha = 0.14f) else colors.altBackground),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(mode.label, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                Text(mode.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                    }
                    if (modeDraft == ThemeAutoScheduleMode.DAY_NIGHT) {
                        Text("Tema diurno (06h–18h):", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(lightCandidates.take(8), key = { "day_${it.id}" }) { t ->
                                FilterChip(
                                    selected = dayThemeDraft == t.id,
                                    onClick = { dayThemeDraft = t.id },
                                    label = { Text(t.name) },
                                )
                            }
                        }
                        Text("Tema noturno (18h–06h):", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(darkCandidates.take(10), key = { "night_${it.id}" }) { t ->
                                FilterChip(
                                    selected = nightThemeDraft == t.id,
                                    onClick = { nightThemeDraft = t.id },
                                    label = { Text(t.name) },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfigureAutoSchedule(modeDraft, dayThemeDraft, nightThemeDraft)
                        showScheduleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                ) {
                    Text("Salvar Agenda")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) { Text("Cancelar") }
            },
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = colors.card,
            title = { Text("Importar Tema (JSON)", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { jsonFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Selecionar Arquivo .json", color = colors.textPrimary)
                    }
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        label = { Text("Ou cole o JSON exportado") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onImportThemeJson(jsonInput); showImportDialog = false },
                    enabled = jsonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                ) {
                    Text("Importar")
                }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancelar") } },
        )
    }

    // Rename Custom Theme Dialog
    renameTarget?.let { target ->
        var newName by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = colors.card,
            title = { Text("Renomear Tema", color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Novo nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameCustomTheme(target.id, newName)
                    renameTarget = null
                }) { Text("Salvar", color = colors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun FeaturedThemeBannerCard(
    theme: MyatoTheme,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val primary = Color(theme.primaryColorHex)
    val onPrimary = contrastContentColor(primary)
    Card(
        modifier = Modifier
            .width(196.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (isActive) 2.dp else 1.dp, if (isActive) primary else Color(theme.borderColorHex).copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = Color(theme.backgroundColorHex)),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
            ) {
                ThemeThumbnailArtwork(
                    theme = theme,
                    enableParticles = false,
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    shape = CircleShape,
                    color = primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                ) {
                    Text(
                        text = if (isActive) "Ativo" else theme.seasonalTag ?: "Destaque",
                        color = onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                theme.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color(theme.textColorHex),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                theme.category.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = Color(theme.secondaryTextColorHex),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ThemeCatalogCard(
    theme: MyatoTheme,
    isActive: Boolean,
    isFavorite: Boolean,
    onOpenPreview: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDuplicate: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    val themePrimary = Color(theme.primaryColorHex)
    val themeOnPrimary = contrastContentColor(themePrimary)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPreview)
            .testTag("theme_card_${theme.id}"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (isActive) 2.2.dp else 1.dp,
            color = if (isActive) themePrimary else Color(theme.borderColorHex).copy(alpha = 0.32f),
        ),
        colors = CardDefaults.cardColors(containerColor = Color(theme.backgroundColorHex)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp),
            ) {
                ThemeThumbnailArtwork(
                    theme = theme,
                    enableParticles = false,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(32.dp),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remover favorito" else "Favoritar tema",
                        tint = if (isFavorite) themePrimary else Color(theme.textColorHex).copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(themePrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Tema ativo", tint = themeOnPrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = theme.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color(theme.textColorHex),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = theme.category.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = Color(theme.secondaryColorHex),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = theme.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(theme.secondaryTextColorHex),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )

            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = themePrimary.copy(alpha = 0.16f),
                    modifier = Modifier.clickable(onClick = onOpenPreview),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(Icons.Rounded.Visibility, contentDescription = null, tint = themePrimary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Prévia", style = MaterialTheme.typography.labelSmall, color = themePrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Editar", tint = Color(theme.secondaryTextColorHex), modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplicar", tint = Color(theme.secondaryTextColorHex), modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Share, contentDescription = "Compartilhar", tint = Color(theme.secondaryTextColorHex), modifier = Modifier.size(15.dp))
                    }
                    if (onRename != null) {
                        IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = "Renomear", tint = Color(theme.secondaryTextColorHex), modifier = Modifier.size(15.dp))
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreationModeOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.altBackground),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}
