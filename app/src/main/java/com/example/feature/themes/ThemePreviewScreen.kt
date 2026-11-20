package com.example.feature.themes

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.AppFontCatalog
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.designsystem.MyatoMusicTheme
import com.example.core.designsystem.ThemeParticleOverlay
import com.example.core.designsystem.ThemeWallpaperLayer
import com.example.domain.models.AppFontOption
import com.example.domain.models.MyatoTheme
import com.example.domain.models.ThemePreviewScreenTab

/**
 * Multi-Screen Interactive Theme Preview.
 * Renders all 6 main app sections (Tela inicial, Player, Playlists, Biblioteca, Pesquisa, Configurações)
 * inside an isolated MyatoMusicTheme(activeTheme = previewTheme) so browsing NEVER modifies the user's
 * active theme until they explicitly tap "Aplicar tema".
 */
@Composable
fun ThemePreviewScreen(
    previewTheme: MyatoTheme,
    isActiveTheme: Boolean,
    isFavorite: Boolean,
    activeGeneralFontId: String = AppFontCatalog.DEFAULT_FONT_ID,
    activeDecorativeFontId: String = AppFontCatalog.DEFAULT_FONT_ID,
    customFonts: List<AppFontOption> = emptyList(),
    onApplyTheme: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenInStudio: () -> Unit,
    onShareTheme: () -> Unit,
    onBack: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(ThemePreviewScreenTab.HOME) }

    MyatoMusicTheme(
        activeTheme = previewTheme,
        activeFontId = activeGeneralFontId,
        activeGeneralFontId = activeGeneralFontId,
        activeDecorativeFontId = activeDecorativeFontId,
        customFonts = customFonts,
    ) {
        val colors = LocalMyatoColors.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("theme_preview_screen"),
        ) {
            ThemeWallpaperLayer(colors = colors, overlayStrength = 1.15f)
            ThemeParticleOverlay(
                animationType = colors.animationType,
                primaryColor = colors.primary,
                secondaryColor = colors.secondary,
                accentColor = colors.accent,
                themeAnimationMode = colors.animationMode,
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.altSurfaceColor())
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("theme_preview_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar", tint = colors.textPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            previewTheme.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${previewTheme.category.displayName} • ${previewTheme.animationType.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.testTag("theme_preview_fav")) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favoritar",
                            tint = if (isFavorite) colors.primary else colors.textSecondary,
                        )
                    }
                    IconButton(onClick = onShareTheme, modifier = Modifier.testTag("theme_preview_share")) {
                        Icon(Icons.Rounded.Share, contentDescription = "Compartilhar tema", tint = colors.iconPrimary)
                    }
                }

                // 6-Screen Selector Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("theme_preview_tabs"),
                ) {
                    items(ThemePreviewScreenTab.entries) { tab ->
                        val selected = selectedTab == tab
                        FilterChip(
                            selected = selected,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = colors.onPrimary,
                            ),
                            modifier = Modifier.testTag("preview_tab_${tab.name}"),
                        )
                    }
                }

                // Simulated Target Screen Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        previewTheme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )

                    when (selectedTab) {
                        ThemePreviewScreenTab.HOME -> PreviewHomeMock()
                        ThemePreviewScreenTab.PLAYER -> PreviewPlayerMock()
                        ThemePreviewScreenTab.PLAYLISTS -> PreviewPlaylistsMock()
                        ThemePreviewScreenTab.LIBRARY -> PreviewLibraryMock()
                        ThemePreviewScreenTab.SEARCH -> PreviewSearchMock()
                        ThemePreviewScreenTab.SETTINGS -> PreviewSettingsMock()
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Bottom Confirmation Dock
                Surface(
                    color = colors.altSurfaceColor(),
                    tonalElevation = colors.elevationDp(4.dp),
                    border = colors.borderStroke(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onOpenInStudio,
                            shape = colors.resolveButtonShape(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_preview_edit_button"),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Studio", color = colors.textPrimary)
                        }
                        Button(
                            onClick = onApplyTheme,
                            shape = colors.resolveButtonShape(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.buttonContainer,
                                contentColor = colors.buttonContent,
                            ),
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("theme_preview_apply_button"),
                        ) {
                            Icon(
                                imageVector = if (isActiveTheme) Icons.Rounded.Check else Icons.Rounded.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isActiveTheme) "Tema Ativo" else "Aplicar tema",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewHomeMock() {
    val colors = LocalMyatoColors.current
    Card(
        shape = colors.resolveCardShape(),
        border = colors.borderStroke(),
        colors = CardDefaults.cardColors(containerColor = colors.cardSurfaceColor()),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Início • MYATO MUSIC", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(colors.resolveCoverShape())
                        .background(Brush.linearGradient(listOf(colors.primary, colors.secondary))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colors.onPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ouvidas recentemente", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    Text("Sinfonia Offline em Alta Definição", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Myato Studio • FLAC 24-bit", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = colors.resolveButtonShape(), color = colors.buttonContainer) {
                    Text("Tocar tudo", color = colors.buttonContent, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                Surface(shape = colors.resolveButtonShape(), color = colors.secondary.copy(alpha = 0.22f)) {
                    Text("Filas", color = colors.textPrimary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewPlayerMock() {
    val colors = LocalMyatoColors.current
    Card(
        shape = colors.resolveCardShape(),
        border = colors.borderStroke(highlighted = true),
        colors = CardDefaults.cardColors(containerColor = colors.cardSurfaceColor()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Tocando Agora (${colors.playerAppearance.name})", style = MaterialTheme.typography.labelMedium, color = colors.primary)
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .clip(colors.resolveCoverShape())
                    .background(Brush.radialGradient(listOf(colors.primary, colors.secondary, colors.altBackground)))
                    .border(2.dp, colors.borderColor.copy(alpha = 0.55f), colors.resolveCoverShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Album, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(58.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Horizonte Estelar", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            Text("Artista Exemplo • Álbum Demo", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.textSecondary.copy(alpha = 0.22f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.64f)
                        .height(6.dp)
                        .background(Brush.horizontalGradient(listOf(colors.primary, colors.secondary))),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = colors.iconPrimary, modifier = Modifier.size(30.dp))
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(colors.resolveButtonShape())
                        .background(colors.buttonContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Pause, contentDescription = null, tint = colors.buttonContent, modifier = Modifier.size(28.dp))
                }
                Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = colors.iconPrimary, modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun PreviewPlaylistsMock() {
    val colors = LocalMyatoColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Favoritas da Madrugada" to "28 faixas", "Treino & Foco" to "42 faixas", "Acústico Offline" to "15 faixas").forEach { (name, meta) ->
            Card(
                shape = colors.resolveCardShape(),
                border = colors.borderStroke(),
                colors = CardDefaults.cardColors(containerColor = colors.cardSurfaceColor()),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(colors.resolveSmallCoverShape())
                            .background(colors.primary.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null, tint = colors.iconPrimary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun PreviewLibraryMock() {
    val colors = LocalMyatoColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val hubs = listOf(
            Triple("Músicas", "320 faixas", Icons.Rounded.MusicNote),
            Triple("Álbuns", "34 álbuns", Icons.Rounded.Album),
            Triple("Artistas", "19 artistas", Icons.Rounded.Person),
            Triple("Pastas", "6 diretórios", Icons.Rounded.Folder),
        )
        hubs.forEach { (title, sub, icon) ->
            Card(
                shape = colors.resolveCardShape(),
                border = colors.borderStroke(),
                colors = CardDefaults.cardColors(containerColor = colors.cardSurfaceColor()),
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = colors.iconPrimary, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewSearchMock() {
    val colors = LocalMyatoColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = "Synthwave Neon",
            onValueChange = {},
            readOnly = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Card(
            shape = colors.resolveCardShape(),
            border = colors.borderStroke(),
            colors = CardDefaults.cardColors(containerColor = colors.cardSurfaceColor()),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = colors.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Resultado Instantâneo", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Faixa Local • Alta Qualidade", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun PreviewSettingsMock() {
    val colors = LocalMyatoColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            shape = colors.resolveCardShape(),
            border = colors.borderStroke(),
            colors = CardDefaults.cardColors(containerColor = colors.cardSurfaceColor()),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Settings, contentDescription = null, tint = colors.iconPrimary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reprodução Gapless", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Exemplo de cartão de ajustes", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                Switch(checked = true, onCheckedChange = {})
            }
        }
        Card(
            shape = colors.resolveCardShape(),
            border = colors.borderStroke(),
            colors = CardDefaults.cardColors(containerColor = colors.cardSurfaceColor()),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Palette, contentDescription = null, tint = colors.secondary)
                Spacer(Modifier.width(12.dp))
                Text("Personalização visual ativa", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
