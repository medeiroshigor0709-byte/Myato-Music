package com.example.feature.themes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
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
import com.example.domain.models.AnimationMode
import com.example.domain.models.ButtonStyle
import com.example.domain.models.CoverShape
import com.example.domain.models.MyatoTheme
import com.example.domain.models.PlayerAppearance
import com.example.feature.library.SimpleHeader

/**
 * 20. Theme Center Screen — 12 mandatory curated themes + custom themes, live preview before applying.
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
) {
    val colors = LocalMyatoColors.current
    var previewTarget by remember { mutableStateOf<MyatoTheme?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("theme_center_screen"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Central de Temas", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                Text("${themes.size} temas disponíveis (pré-visualize antes de aplicar)", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
            Row {
                IconButton(onClick = { showImportDialog = true }) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = "Importar JSON", tint = colors.primary)
                }
                IconButton(onClick = { onOpenEditor(activeThemeId) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Editor de Temas", tint = colors.primary)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 164.dp),
            modifier = Modifier.testTag("theme_grid"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(themes, key = { it.id }) { theme ->
                val isActive = theme.id == activeThemeId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { previewTarget = theme }
                        .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) Color(theme.primaryColorHex) else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(20.dp),
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(theme.backgroundColorHex)),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Mini Theme Preview Swatch
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(theme.cardColorHex))
                                .padding(10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterStart)) {
                                Box(
                                    modifier = Modifier.size(30.dp).clip(CircleShape).background(Color(theme.primaryColorHex)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = if (theme.isLight) Color.White else Color.Black, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Box(modifier = Modifier.width(64.dp).height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color(theme.textColorHex)))
                                    Spacer(Modifier.height(4.dp))
                                    Box(modifier = Modifier.width(42.dp).height(5.dp).clip(RoundedCornerShape(4.dp)).background(Color(theme.secondaryColorHex)))
                                }
                            }
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(theme.primaryColorHex)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Ativo", tint = Color.Black, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = theme.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(theme.textColorHex),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (theme.isBuiltIn) "Oficial" else "Personalizado",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(theme.secondaryTextColorHex),
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = { onDuplicateTheme(theme, "${theme.name} Copy") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplicar", tint = Color(theme.secondaryTextColorHex), modifier = Modifier.size(16.dp))
                            }
                            if (!theme.isBuiltIn) {
                                IconButton(onClick = { onDeleteCustomTheme(theme.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Mandatory Live Preview Dialog before applying
    previewTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { previewTarget = null },
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
                    Text("MYATO PLAYER PREVIEW", style = MaterialTheme.typography.labelSmall, color = Color(target.primaryColorHex))
                    Spacer(Modifier.height(6.dp))
                    Text("Exemplo de Faixa Offline", style = MaterialTheme.typography.titleLarge, color = Color(target.textColorHex))
                    Text("Artista • Álbum", style = MaterialTheme.typography.bodyMedium, color = Color(target.secondaryTextColorHex))
                    Spacer(Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(target.secondaryTextColorHex).copy(alpha = 0.25f))) {
                        Box(modifier = Modifier.fillMaxWidth(0.62f).height(5.dp).background(Color(target.primaryColorHex)))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = CircleShape, color = Color(target.primaryColorHex)) {
                            Text("Principal", color = if (target.isLight) Color.White else Color.Black, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                        Surface(shape = CircleShape, color = Color(target.secondaryColorHex)) {
                            Text("Secundária", color = Color.Black, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSelectTheme(target.id)
                        previewTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(target.primaryColorHex), contentColor = Color.Black),
                ) {
                    Text("Aplicar Tema")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        previewTarget = null
                        onOpenEditor(target.id)
                    }) { Text("Editar", color = Color(target.secondaryColorHex)) }
                    TextButton(onClick = { previewTarget = null }) {
                        Text("Fechar", color = Color(target.secondaryTextColorHex))
                    }
                }
            },
        )
    }

    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = colors.card,
            title = { Text("Importar Tema (JSON)", color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Cole o JSON exportado do MYATO") },
                    minLines = 4,
                )
            },
            confirmButton = {
                TextButton(onClick = { onImportThemeJson(jsonInput); showImportDialog = false }) {
                    Text("Importar", color = colors.primary)
                }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancelar") } },
        )
    }
}

/**
 * 21. Theme Editor Screen — Custom colors, cover shape, button style, animations,
 * player appearance, export JSON, and album-art offline Palette generator.
 */
@Composable
fun ThemeEditorScreen(
    sourceTheme: MyatoTheme,
    onBack: () -> Unit,
    onSaveTheme: (MyatoTheme) -> Unit,
    onGenerateFromCover: ((MyatoTheme) -> Unit) -> Unit,
    onExportThemeJson: (MyatoTheme) -> String,
    onNotify: (String) -> Unit,
) {
    val colors = LocalMyatoColors.current
    val context = LocalContext.current

    var draft by remember(sourceTheme.id) {
        mutableStateOf(
            if (sourceTheme.isBuiltIn) {
                sourceTheme.copy(
                    id = "custom_${System.currentTimeMillis()}",
                    name = "${sourceTheme.name} Studio",
                    isBuiltIn = false,
                )
            } else sourceTheme
        )
    }

    val colorSwatches = listOf(
        0xFFFF8500L, 0xFFFF9D24L, 0xFF00E5FFL, 0xFFFF007FL,
        0xFFD4AF37L, 0xFF2ECC71L, 0xFFFF3B5CL, 0xFFB388FFL,
        0xFF29B6F6L, 0xFFFFFFFFL, 0xFF000000L, 0xFF111111L,
        0xFF1C1C1CL, 0xFF151B38L, 0xFFA5A5A5L,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("theme_editor_screen"),
    ) {
        SimpleHeader(title = "Editor de Temas", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Live Mini Preview
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(draft.backgroundColorHex)),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(draft.primaryColorHex), RoundedCornerShape(20.dp)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Prévia em Tempo Real", style = MaterialTheme.typography.labelSmall, color = Color(draft.primaryColorHex))
                    Spacer(Modifier.height(6.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(draft.cardColorHex)), shape = RoundedCornerShape(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(draft.primaryColorHex)))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(draft.name, color = Color(draft.textColorHex), fontWeight = FontWeight.Bold)
                                Text("Texto secundário de exemplo", color = Color(draft.secondaryTextColorHex), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    onGenerateFromCover { generated -> draft = generated }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.secondary, contentColor = Color.Black),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Gerar Paleta pela Capa da Música Atual (Offline)", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = { Text("Nome do Tema") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
            ColorRowPicker("Cor Principal", draft.primaryColorHex, colorSwatches) { draft = draft.copy(primaryColorHex = it) }
            ColorRowPicker("Cor Secundária", draft.secondaryColorHex, colorSwatches) { draft = draft.copy(secondaryColorHex = it) }
            ColorRowPicker("Fundo", draft.backgroundColorHex, colorSwatches) { draft = draft.copy(backgroundColorHex = it, altBackgroundColorHex = it) }
            ColorRowPicker("Cartões", draft.cardColorHex, colorSwatches) { draft = draft.copy(cardColorHex = it) }
            ColorRowPicker("Texto Principal", draft.textColorHex, colorSwatches) { draft = draft.copy(textColorHex = it) }

            Spacer(Modifier.height(10.dp))
            Text("Formato da Capa", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CoverShape.entries) { shape ->
                    FilterChip(
                        selected = draft.coverShape == shape,
                        onClick = { draft = draft.copy(coverShape = shape) },
                        label = { Text(shape.name) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primary, selectedLabelColor = Color.Black),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Estilo dos Botões", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ButtonStyle.entries) { style ->
                    FilterChip(
                        selected = draft.buttonStyle == style,
                        onClick = { draft = draft.copy(buttonStyle = style) },
                        label = { Text(style.name) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primary, selectedLabelColor = Color.Black),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Aparência do Player", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PlayerAppearance.entries) { app ->
                    FilterChip(
                        selected = draft.playerAppearance == app,
                        onClick = { draft = draft.copy(playerAppearance = app) },
                        label = { Text(app.name) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primary, selectedLabelColor = Color.Black),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Animações", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AnimationMode.entries) { anim ->
                    FilterChip(
                        selected = draft.animationMode == anim,
                        onClick = { draft = draft.copy(animationMode = anim) },
                        label = { Text(anim.name) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primary, selectedLabelColor = Color.Black),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val json = onExportThemeJson(draft)
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("MYATO Theme", json))
                        onNotify("JSON do tema copiado para a área de transferência!")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Exportar JSON", color = colors.textPrimary)
                }
                Button(
                    onClick = { onSaveTheme(draft); onBack() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                ) {
                    Icon(Icons.Rounded.Palette, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Salvar e Aplicar", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(140.dp))
        }
    }
}

@Composable
private fun ColorRowPicker(
    label: String,
    selectedHex: Long,
    swatches: List<Long>,
    onPick: (Long) -> Unit,
) {
    val colors = LocalMyatoColors.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(swatches) { hex ->
                val active = hex == selectedHex
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(if (active) 3.dp else 1.dp, if (active) Color.White else Color.Gray, CircleShape)
                        .clickable { onPick(hex) },
                )
            }
        }
    }
}
