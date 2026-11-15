package com.example.feature.themes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.common.TimeFormatter
import com.example.core.designsystem.AppFontCatalog
import com.example.core.designsystem.LocalMyatoColors
import com.example.domain.models.AppFontOption
import com.example.domain.models.FontApplyTarget
import com.example.domain.models.FontCategory
import com.example.feature.library.SimpleHeader

/**
 * Full-screen Font Preview ("Pré-visualização da Fonte") allowing users to evaluate
 * how a font renders across MYATO MUSIC branding, library, now-playing, albums/playlists,
 * buttons, and settings before applying as General, Decorative, or Both.
 */
@Composable
fun FontPreviewScreen(
    fontOption: AppFontOption,
    customFonts: List<AppFontOption>,
    activeGeneralFontId: String,
    activeDecorativeFontId: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onApplyFont: (FontApplyTarget) -> Unit,
    onDeleteCustomFont: (() -> Unit)?,
    onBack: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    val onPrimary = if (colors.isLight) Color.White else Color.Black

    val defaultTarget = remember(fontOption.fontCategory) {
        when (fontOption.fontCategory) {
            FontCategory.MINIMALISTAS -> FontApplyTarget.GENERAL
            else -> FontApplyTarget.DECORATIVE
        }
    }
    var chosenTarget by remember(fontOption.id) { mutableStateOf(defaultTarget) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val previewFamily = remember(fontOption.id, fontOption.customFilePath) {
        AppFontCatalog.resolveFontFamily(fontOption.id, customFonts)
    }
    val previewTypo = remember(fontOption.id, fontOption.customFilePath) {
        AppFontCatalog.resolveAppTypography(fontOption.id, customFonts)
    }

    val isActiveGeneral = activeGeneralFontId == fontOption.id
    val isActiveDecorative = activeDecorativeFontId == fontOption.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("font_preview_screen"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SimpleHeader(title = "Visualizar Fonte", onBack = onBack)
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("preview_favorite_button"),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remover dos favoritos" else "Favoritar fonte",
                    tint = if (isFavorite) colors.primary else colors.textSecondary,
                )
            }
            if (onDeleteCustomFont != null) {
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("preview_delete_custom_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remover fonte personalizada",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .testTag("font_preview_scroll"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Metadata & Specimen Banner
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.32f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fontOption.name,
                                        style = previewTypo.headlineMedium.copy(fontFamily = previewFamily),
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Criador: ${fontOption.creator} • ${fontOption.license}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary,
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = colors.primary.copy(alpha = 0.16f),
                                ) {
                                    Text(
                                        text = fontOption.category,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }

                            if (fontOption.styleReference.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = fontOption.styleReference,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.primary,
                                )
                            }

                            if (fontOption.isCustom && fontOption.sizeBytes > 0L) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Formato ${fontOption.fileFormat} • ${TimeFormatter.formatFileSize(fontOption.sizeBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                )
                            }

                            if (isActiveGeneral || isActiveDecorative) {
                                Spacer(Modifier.height(8.dp))
                                val activeLabel = when {
                                    isActiveGeneral && isActiveDecorative -> "Ativa como Geral + Decorativa"
                                    isActiveDecorative -> "Ativa como Fonte Decorativa"
                                    else -> "Ativa como Fonte Geral"
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.primary,
                                ) {
                                    Text(
                                        text = activeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.14f))
                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "MYATO MUSIC",
                                style = previewTypo.displaySmall.copy(fontFamily = previewFamily),
                                color = colors.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                                style = previewTypo.titleSmall.copy(fontFamily = previewFamily),
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "abcdefghijklmnopqrstuvwxyz • 0123456789",
                                style = previewTypo.bodyMedium.copy(fontFamily = previewFamily),
                                color = colors.textSecondary,
                            )
                        }
                    }
                }

                // Sample 1: Reproduzindo agora + Song & Artist
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.altBackground),
                        border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.16f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Reproduzindo agora",
                                style = previewTypo.labelLarge.copy(fontFamily = previewFamily),
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.primary.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "The Sound of Music",
                                        style = previewTypo.titleLarge.copy(fontFamily = previewFamily),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "Myato Artist",
                                        style = previewTypo.bodyLarge.copy(fontFamily = previewFamily),
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = onPrimary)
                                }
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = colors.textPrimary)
                            }
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { 0.64f },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                color = colors.primary,
                                trackColor = colors.textSecondary.copy(alpha = 0.22f),
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("02:14", style = previewTypo.labelMedium.copy(fontFamily = previewFamily), color = colors.textSecondary)
                                Text("03:45 • FLAC", style = previewTypo.labelMedium.copy(fontFamily = previewFamily), color = colors.primary)
                            }
                        }
                    }
                }

                // Sample 2: Minha biblioteca + Álbuns e playlists
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = colors.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Minha biblioteca",
                                    style = previewTypo.titleLarge.copy(fontFamily = previewFamily),
                                    color = colors.textPrimary,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Álbuns e playlists",
                                style = previewTypo.titleMedium.copy(fontFamily = previewFamily),
                                color = colors.primary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.altBackground,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Rounded.Album, contentDescription = null, tint = colors.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Acústico Offline",
                                                style = previewTypo.titleSmall.copy(fontFamily = previewFamily),
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                            )
                                            Text(
                                                text = "Myato Artist • 14 faixas",
                                                style = previewTypo.bodySmall.copy(fontFamily = previewFamily),
                                                color = colors.textSecondary,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.altBackground,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = colors.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Favoritas Rock",
                                                style = previewTypo.titleSmall.copy(fontFamily = previewFamily),
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                            )
                                            Text(
                                                text = "Playlist • 28 faixas",
                                                style = previewTypo.bodySmall.copy(fontFamily = previewFamily),
                                                color = colors.textSecondary,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sample 3: Botões e informações das configurações
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Settings, contentDescription = null, tint = colors.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Botões e informações das configurações",
                                    style = previewTypo.titleMedium.copy(fontFamily = previewFamily),
                                    color = colors.textPrimary,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Reprodução sem intervalos (Gapless)",
                                        style = previewTypo.bodyLarge.copy(fontFamily = previewFamily),
                                        color = colors.textPrimary,
                                    )
                                    Text(
                                        text = "Sua música. Seu estilo. Sua experiência.",
                                        style = previewTypo.bodyMedium.copy(fontFamily = previewFamily),
                                        color = colors.textSecondary,
                                    )
                                }
                                Switch(checked = true, onCheckedChange = {})
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {},
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = onPrimary),
                                ) {
                                    Text(
                                        text = "Botão Principal",
                                        style = previewTypo.labelLarge.copy(fontFamily = previewFamily),
                                    )
                                }
                                OutlinedButton(onClick = {}) {
                                    Text(
                                        text = "Botão Secundário",
                                        style = previewTypo.labelLarge.copy(fontFamily = previewFamily),
                                        color = colors.textPrimary,
                                    )
                                }
                            }
                        }
                    }
                }

                // Smart Application Target Selector (Fonte Geral vs Decorativa vs Ambas)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.altBackground),
                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.28f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Personalização Inteligente — Onde aplicar?",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Use Fonte Decorativa em títulos e nomes de músicas para manter menus e configurações fáceis de ler, ou aplique como Fonte Geral.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(
                                    FontApplyTarget.DECORATIVE to "Decorativa",
                                    FontApplyTarget.GENERAL to "Geral",
                                    FontApplyTarget.BOTH to "Ambas",
                                ).forEach { (target, label) ->
                                    FilterChip(
                                        selected = chosenTarget == target,
                                        onClick = { chosenTarget = target },
                                        label = { Text(label, maxLines = 1) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.primary,
                                            selectedLabelColor = onPrimary,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Persistent Bottom Action Bar ("Aplicar fonte" & "Voltar")
        Surface(
            color = colors.altBackground,
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(0.42f)
                        .height(48.dp)
                        .testTag("preview_back_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Voltar", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { onApplyFont(chosenTarget) },
                    modifier = Modifier
                        .weight(0.58f)
                        .height(48.dp)
                        .testTag("preview_apply_font_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = onPrimary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Aplicar fonte", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteConfirm && onDeleteCustomFont != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = colors.card,
            title = { Text("Remover fonte personalizada", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Deseja remover \"${fontOption.name}\" do armazenamento interno do MYATO MUSIC?",
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteCustomFont()
                        onBack()
                    },
                ) {
                    Text("Remover", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
        )
    }
}
