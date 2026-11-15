package com.example.feature.themes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SortByAlpha
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.common.TimeFormatter
import com.example.core.designsystem.AppFontCatalog
import com.example.core.designsystem.LocalMyatoColors
import com.example.domain.models.AppFontOption
import com.example.domain.models.FontApplyTarget
import com.example.domain.models.FontCategory
import com.example.domain.models.FontInstallFilter
import com.example.domain.models.FontSortOption
import com.example.feature.library.SimpleHeader

/**
 * DaFont-inspired offline Typography & Font Catalog ("Fontes e tipografia")
 * preserving MYATO MUSIC identity across all 12 themes.
 */
@Composable
fun FontSettingsScreen(
    activeGeneralFontId: String,
    activeDecorativeFontId: String,
    favoriteFontIds: Set<String>,
    recentFontIds: List<String>,
    customFonts: List<AppFontOption>,
    onBack: () -> Unit,
    onOpenPreview: (String) -> Unit,
    onApplyFont: (String, FontApplyTarget) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRestoreDefault: () -> Unit,
    onImportCustomFontUri: (Uri) -> Unit,
    onDeleteCustomFont: (AppFontOption) -> Unit,
) {
    val colors = LocalMyatoColors.current
    val onPrimary = if (colors.isLight) Color.White else Color.Black

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FontCategory?>(null) }
    var installFilter by remember { mutableStateOf(FontInstallFilter.ALL) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(FontSortOption.CATALOG) }

    var applyDialogFont by remember { mutableStateOf<AppFontOption?>(null) }
    var deleteCandidate by remember { mutableStateOf<AppFontOption?>(null) }

    val allFonts = remember(customFonts) { AppFontCatalog.allFonts(customFonts) }
    val activeGeneralOption = remember(activeGeneralFontId, customFonts) {
        AppFontCatalog.findFontOption(activeGeneralFontId, customFonts)
    }
    val activeDecorativeOption = remember(activeDecorativeFontId, customFonts) {
        AppFontCatalog.findFontOption(activeDecorativeFontId, customFonts)
    }

    val recentFonts = remember(recentFontIds, allFonts) {
        recentFontIds.mapNotNull { id -> allFonts.firstOrNull { it.id == id } }
    }

    val filteredFonts = remember(
        allFonts,
        searchQuery,
        selectedCategory,
        installFilter,
        favoritesOnly,
        favoriteFontIds,
        sortOption,
    ) {
        val q = searchQuery.trim().lowercase()
        val base = allFonts.filter { font ->
            val matchesQuery = q.isBlank() ||
                font.name.lowercase().contains(q) ||
                font.creator.lowercase().contains(q) ||
                font.category.lowercase().contains(q) ||
                font.description.lowercase().contains(q) ||
                font.styleReference.lowercase().contains(q)
            val matchesCategory = selectedCategory == null || font.fontCategory == selectedCategory
            val matchesInstall = when (installFilter) {
                FontInstallFilter.ALL -> true
                FontInstallFilter.BUILT_IN -> !font.isCustom
                FontInstallFilter.IMPORTED -> font.isCustom
            }
            val matchesFav = !favoritesOnly || font.id in favoriteFontIds
            matchesQuery && matchesCategory && matchesInstall && matchesFav
        }
        when (sortOption) {
            FontSortOption.CATALOG -> base
            FontSortOption.NAME_ASC -> base.sortedBy { it.name.lowercase() }
            FontSortOption.NAME_DESC -> base.sortedByDescending { it.name.lowercase() }
        }
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            onImportCustomFontUri(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("font_settings_screen"),
    ) {
        SimpleHeader(title = "Fontes e tipografia", onBack = onBack)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .testTag("font_settings_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Active Dual Typography & Smart Controls Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.28f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.TextFields,
                                        contentDescription = "Catálogo de fontes",
                                        tint = colors.primary,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Catálogo de Tipografias MYATO",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "${allFonts.size} fontes offline em ${FontCategory.entries.size} categorias",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary,
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Dual Active Fonts (Geral vs Decorativa)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ActiveFontRoleChip(
                                    roleLabel = "FONTE GERAL",
                                    fontOption = activeGeneralOption,
                                    customFonts = customFonts,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenPreview(activeGeneralOption.id) },
                                )
                                ActiveFontRoleChip(
                                    roleLabel = "FONTE DECORATIVA",
                                    fontOption = activeDecorativeOption,
                                    customFonts = customFonts,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenPreview(activeDecorativeOption.id) },
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Import + Restore Original Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = {
                                        fontPickerLauncher.launch(
                                            arrayOf(
                                                "font/ttf",
                                                "font/otf",
                                                "application/x-font-ttf",
                                                "application/x-font-opentype",
                                                "application/octet-stream",
                                                "*/*",
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("import_custom_font_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = onPrimary,
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Importar fonte", fontWeight = FontWeight.Bold, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = onRestoreDefault,
                                    enabled = activeGeneralFontId != AppFontCatalog.DEFAULT_FONT_ID ||
                                        activeDecorativeFontId != AppFontCatalog.DEFAULT_FONT_ID,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("restore_default_font_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.35f)),
                                ) {
                                    Icon(Icons.Rounded.Restore, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Restaurar fonte original",
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Pesquisar fonte por nome, estilo ou criador...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Limpar busca", tint = colors.textSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.28f),
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedContainerColor = colors.card,
                            unfocusedContainerColor = colors.card,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("font_search_input"),
                    )
                }

                // Category Chips (12 DaFont-inspired categories + Todas)
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("font_category_row"),
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text("Todas (${allFonts.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor = onPrimary,
                                ),
                                modifier = Modifier.testTag("font_category_all"),
                            )
                        }
                        items(FontCategory.entries, key = { it.name }) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = if (selectedCategory == category) null else category
                                },
                                label = { Text(category.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor = onPrimary,
                                ),
                                modifier = Modifier.testTag("font_category_${category.name.lowercase()}"),
                            )
                        }
                    }
                }

                // Filters: Installed, Favorites, Alphabetical Sort
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item {
                            FilterChip(
                                selected = favoritesOnly,
                                onClick = { favoritesOnly = !favoritesOnly },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (favoritesOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = { Text("Favoritas (${favoriteFontIds.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor = onPrimary,
                                    selectedLeadingIconColor = onPrimary,
                                ),
                                modifier = Modifier.testTag("font_filter_favorites"),
                            )
                        }
                        item {
                            FilterChip(
                                selected = installFilter != FontInstallFilter.ALL,
                                onClick = {
                                    installFilter = when (installFilter) {
                                        FontInstallFilter.ALL -> FontInstallFilter.IMPORTED
                                        FontInstallFilter.IMPORTED -> FontInstallFilter.BUILT_IN
                                        FontInstallFilter.BUILT_IN -> FontInstallFilter.ALL
                                    }
                                },
                                label = { Text("Instaladas: ${installFilter.label}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor = onPrimary,
                                ),
                                modifier = Modifier.testTag("font_filter_installed"),
                            )
                        }
                        item {
                            FilterChip(
                                selected = sortOption != FontSortOption.CATALOG,
                                onClick = {
                                    sortOption = when (sortOption) {
                                        FontSortOption.CATALOG -> FontSortOption.NAME_ASC
                                        FontSortOption.NAME_ASC -> FontSortOption.NAME_DESC
                                        FontSortOption.NAME_DESC -> FontSortOption.CATALOG
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.SortByAlpha, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Ordem: ${sortOption.label}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor = onPrimary,
                                    selectedLeadingIconColor = onPrimary,
                                ),
                                modifier = Modifier.testTag("font_sort_button"),
                            )
                        }
                    }
                }

                // Recently Used Fonts Strip
                if (recentFonts.isNotEmpty() && searchQuery.isBlank() && selectedCategory == null && !favoritesOnly) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.History, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Usadas recentemente",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(recentFonts, key = { "recent_${it.id}" }) { recent ->
                                    val fam = remember(recent.id, recent.customFilePath) {
                                        AppFontCatalog.resolveFontFamily(recent.id, customFonts)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = colors.card,
                                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .clickable { onOpenPreview(recent.id) }
                                            .testTag("recent_font_${recent.id}"),
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            Text(
                                                text = recent.name,
                                                fontFamily = fam,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                            )
                                            Text(
                                                text = recent.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Results Header
                item {
                    val subtitle = selectedCategory?.description ?: "Toque para visualizar em detalhes ou aplique direto."
                    Column {
                        Text(
                            text = if (selectedCategory != null) {
                                "${selectedCategory!!.label} (${filteredFonts.size})"
                            } else {
                                "Catálogo (${filteredFonts.size} fontes)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }

                if (filteredFonts.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.card),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Nenhuma fonte encontrada para este filtro.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                )
                                Spacer(Modifier.height(6.dp))
                                TextButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedCategory = null
                                        installFilter = FontInstallFilter.ALL
                                        favoritesOnly = false
                                    },
                                ) {
                                    Text("Limpar filtros", color = colors.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(filteredFonts, key = { it.id }) { fontOption ->
                        val family = remember(fontOption.id, fontOption.customFilePath) {
                            AppFontCatalog.resolveFontFamily(fontOption.id, customFonts)
                        }
                        val isGeneral = fontOption.id == activeGeneralFontId
                        val isDecorative = fontOption.id == activeDecorativeFontId
                        FontCatalogCard(
                            fontOption = fontOption,
                            fontFamily = family,
                            isActiveGeneral = isGeneral,
                            isActiveDecorative = isDecorative,
                            isFavorite = fontOption.id in favoriteFontIds,
                            onToggleFavorite = { onToggleFavorite(fontOption.id) },
                            onPreviewClick = { onOpenPreview(fontOption.id) },
                            onApplyClick = { applyDialogFont = fontOption },
                            onDeleteClick = if (fontOption.isCustom) {
                                { deleteCandidate = fontOption }
                            } else null,
                        )
                    }
                }
            }
        }
    }

    // Apply Target Dialog ("Geral", "Decorativa", "Geral + Decorativa")
    applyDialogFont?.let { targetFont ->
        AlertDialog(
            onDismissRequest = { applyDialogFont = null },
            containerColor = colors.card,
            title = {
                Text(
                    text = "Aplicar \"${targetFont.name}\"",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Escolha como deseja personalizar o MYATO MUSIC com esta fonte:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    ApplyOptionButton(
                        title = "Fonte Decorativa (Recomendado)",
                        subtitle = "Títulos, nomes das músicas e cabeçalhos; mantém menus e letras legíveis.",
                        onClick = {
                            onApplyFont(targetFont.id, FontApplyTarget.DECORATIVE)
                            applyDialogFont = null
                        },
                        testTag = "dialog_apply_decorative",
                    )
                    ApplyOptionButton(
                        title = "Fonte Geral",
                        subtitle = "Menus, configurações, letras de músicas, artistas e detalhes.",
                        onClick = {
                            onApplyFont(targetFont.id, FontApplyTarget.GENERAL)
                            applyDialogFont = null
                        },
                        testTag = "dialog_apply_general",
                    )
                    ApplyOptionButton(
                        title = "Aplicar em Ambas (Geral + Decorativa)",
                        subtitle = "Usa esta tipografia em toda a interface do aplicativo.",
                        onClick = {
                            onApplyFont(targetFont.id, FontApplyTarget.BOTH)
                            applyDialogFont = null
                        },
                        testTag = "dialog_apply_both",
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { applyDialogFont = null }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
        )
    }

    deleteCandidate?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            containerColor = colors.card,
            title = { Text("Remover fonte personalizada", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Deseja remover \"${target.name}\" do armazenamento privado do MYATO MUSIC?",
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustomFont(target)
                        deleteCandidate = null
                    },
                ) {
                    Text("Remover", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
        )
    }
}

@Composable
private fun ActiveFontRoleChip(
    roleLabel: String,
    fontOption: AppFontOption,
    customFonts: List<AppFontOption>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    val family = remember(fontOption.id, fontOption.customFilePath) {
        AppFontCatalog.resolveFontFamily(fontOption.id, customFonts)
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.altBackground,
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = roleLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = fontOption.name,
                fontFamily = family,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ApplyOptionButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    val colors = LocalMyatoColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.altBackground,
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.32f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}

@Composable
private fun FontCatalogCard(
    fontOption: AppFontOption,
    fontFamily: FontFamily,
    isActiveGeneral: Boolean,
    isActiveDecorative: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPreviewClick: () -> Unit,
    onApplyClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
) {
    val colors = LocalMyatoColors.current
    val onPrimary = if (colors.isLight) Color.White else Color.Black
    val isSelected = isActiveGeneral || isActiveDecorative

    val selectedBadge = when {
        isActiveGeneral && isActiveDecorative -> "Geral + Decorativa"
        isActiveDecorative -> "Decorativa"
        isActiveGeneral -> "Geral"
        else -> null
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colors.primary else colors.textSecondary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(20.dp),
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onPreviewClick)
            .testTag("font_option_${fontOption.id}"),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Header Row: Name + Creator + Category + Favorite/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = fontOption.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.altBackground,
                        ) {
                            Text(
                                text = fontOption.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    val meta = buildString {
                        append("Criador: ")
                        append(fontOption.creator)
                        append(" • ")
                        append(fontOption.license)
                        if (fontOption.isCustom && fontOption.sizeBytes > 0L) {
                            append(" • ")
                            append(TimeFormatter.formatFileSize(fontOption.sizeBytes))
                        }
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("fav_font_${fontOption.id}"),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) "Desfavoritar ${fontOption.name}" else "Favoritar ${fontOption.name}",
                        tint = if (isFavorite) colors.primary else colors.textSecondary,
                    )
                }

                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_custom_font_${fontOption.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remover fonte ${fontOption.name}",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // DaFont-style Real Font Specimen Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.altBackground.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = "MYATO MUSIC",
                        fontFamily = fontFamily,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ • abcdefghijklmnopqrstuvwxyz",
                        fontFamily = fontFamily,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Selected Badge + Visualizar & Aplicar Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectedBadge != null) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primary.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, colors.primary),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = selectedBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                OutlinedButton(
                    onClick = onPreviewClick,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("preview_btn_${fontOption.id}"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Visualizar", color = colors.textPrimary, maxLines = 1)
                }

                Button(
                    onClick = onApplyClick,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("apply_btn_${fontOption.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = onPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = if (isSelected) "Alterar" else "Aplicar",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
