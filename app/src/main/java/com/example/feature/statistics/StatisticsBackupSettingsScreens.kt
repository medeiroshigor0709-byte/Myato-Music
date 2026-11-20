package com.example.feature.statistics

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.common.TimeFormatter
import com.example.core.database.AppUserPreferences
import com.example.core.database.BackupMetadataEntity
import com.example.core.designsystem.AlbumArtwork
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.designsystem.MyatoCompactBrandBadge
import com.example.core.designsystem.MyatoOfficialBrandLockup
import com.example.domain.models.AlbumItem
import com.example.domain.models.ArtistItem
import com.example.domain.models.AudioFormatCapability
import com.example.domain.models.BackupPreview
import com.example.domain.models.FolderItem
import com.example.domain.models.HeadsetButtonMapping
import com.example.domain.models.ListeningStatsSummary
import com.example.domain.models.PlayerVisualBackgroundStyle
import com.example.domain.models.Playlist
import com.example.feature.library.SimpleHeader
import com.example.feature.player.PlayerVisualStyleSelectorSection

/**
 * 19. Statistics Screen + CSV export of most played tracks
 */
@Composable
fun StatisticsScreen(
    summary: ListeningStatsSummary,
    onBack: () -> Unit,
    onExportCsv: (Uri) -> Unit,
) {
    val colors = LocalMyatoColors.current
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) onExportCsv(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("statistics_screen"),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                SimpleHeader(title = "Estatísticas Musicais", onBack = onBack)
            }
            OutlinedButton(onClick = { csvLauncher.launch("myato_top_tracks.csv") }) {
                Icon(Icons.Rounded.Download, contentDescription = null, tint = colors.primary)
                Spacer(Modifier.width(4.dp))
                Text("CSV", color = colors.textPrimary)
            }
        }

        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatBox("Tempo Total", TimeFormatter.formatHoursMinutes(summary.totalListenMs), Modifier.weight(1f))
                    StatBox("Reproduções", summary.totalPlays.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Text("Músicas Mais Reproduzidas", style = MaterialTheme.typography.titleLarge, color = colors.primary)
                Spacer(Modifier.height(6.dp))
            }

            if (summary.topTracks.isEmpty()) {
                item {
                    Text("Ouça algumas músicas para acumular estatísticas reais de reprodução.", color = colors.textSecondary)
                }
            } else {
                items(summary.topTracks, key = { it.trackId }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AlbumArtwork(artUri = item.artUri, size = 44.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.artist, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1)
                            }
                            Text("${item.playCount}x", style = MaterialTheme.typography.titleMedium, color = colors.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (summary.topArtists.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Text("Artistas Mais Ouvidos", style = MaterialTheme.typography.titleLarge, color = colors.primary)
                }
                items(summary.topArtists, key = { it.artist }) { artist ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(artist.artist, color = colors.textPrimary)
                        Text("${artist.playCount} plays • ${TimeFormatter.formatHoursMinutes(artist.totalListenMs)}", color = colors.textSecondary)
                    }
                }
            }

            if (summary.recentHistory.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Text("Histórico Recente", style = MaterialTheme.typography.titleLarge, color = colors.primary)
                }
                items(summary.recentHistory.take(20), key = { it.id }) { h ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(h.title, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(h.artist, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1)
                        }
                        Text(TimeFormatter.formatDuration(h.durationListenedMs), style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalMyatoColors.current
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = colors.primary, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * 22. Widgets & Home Shortcuts Screen
 */
@Composable
fun WidgetsShortcutsScreen(
    albums: List<AlbumItem>,
    artists: List<ArtistItem>,
    folders: List<FolderItem>,
    playlists: List<Playlist>,
    onBack: () -> Unit,
    onPinShortcut: (String, String, String) -> Unit,
) {
    val colors = LocalMyatoColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("widgets_screen"),
    ) {
        SimpleHeader(title = "Widgets e Atalhos", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Widgets, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("3 Tamanhos de Widgets Disponíveis", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("• MYATO Mini (3x1): Capa, título e Play/Pause.", color = colors.textSecondary)
                    Text("• MYATO Compacto (4x1): Capa, título, artista e controles Anterior/Play/Próxima.", color = colors.textSecondary)
                    Text("• MYATO Completo (4x2): Capa, barra de progresso, favoritos e controles completos.", color = colors.textSecondary)
                    Spacer(Modifier.height(6.dp))
                    Text("Adicione pela tela inicial do Android segurando em um espaço vazio > Widgets.", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Fixar Atalhos na Tela Inicial", style = MaterialTheme.typography.titleLarge, color = colors.primary)
            Spacer(Modifier.height(8.dp))

            playlists.take(4).forEach { pl ->
                ShortcutRow("Playlist: ${pl.name}") { onPinShortcut("playlist", pl.id.toString(), pl.name) }
            }
            albums.take(4).forEach { al ->
                ShortcutRow("Álbum: ${al.title}") { onPinShortcut("album", al.id.toString(), al.title) }
            }
            artists.take(4).forEach { ar ->
                ShortcutRow("Artista: ${ar.name}") { onPinShortcut("artist", ar.name, ar.name) }
            }
            folders.take(4).forEach { fl ->
                ShortcutRow("Pasta: ${fl.name}") { onPinShortcut("folder", fl.path, fl.name) }
            }
            Spacer(Modifier.height(140.dp))
        }
    }
}

@Composable
private fun ShortcutRow(label: String, onPin: () -> Unit) {
    val colors = LocalMyatoColors.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onPin),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Rounded.PushPin, contentDescription = "Fixar atalho", tint = colors.primary)
        }
    }
}

/**
 * 23. Backup & Restore Screen — Manual JSON export/import, Preview before restore,
 * Preserve vs Replace confirmation, and WorkManager Auto-Backup scheduler.
 */
@Composable
fun BackupScreen(
    preferences: AppUserPreferences,
    backupHistory: List<BackupMetadataEntity>,
    previewedBackup: BackupPreview?,
    onBack: () -> Unit,
    onExportManual: (Uri) -> Unit,
    onSelectBackupToPreview: (Uri) -> Unit,
    onConfirmRestore: (Boolean) -> Unit,
    onDismissPreview: () -> Unit,
    onConfigureAutoBackup: (Int, String) -> Unit,
) {
    val colors = LocalMyatoColors.current
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportManual(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onSelectBackupToPreview(uri)
    }

    val dirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            onConfigureAutoBackup(preferences.autoBackupIntervalDays.coerceAtLeast(1), uri.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("backup_screen"),
    ) {
        SimpleHeader(title = "Backup e Restauração", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Button(
                onClick = { exportLauncher.launch("myato_backup_${System.currentTimeMillis()}.json") },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("export_backup_button"),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
            ) {
                Icon(Icons.Rounded.Backup, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Criar Backup Manual (JSON)", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("restore_backup_button"),
            ) {
                Icon(Icons.Rounded.Restore, contentDescription = null, tint = colors.primary)
                Spacer(Modifier.width(8.dp))
                Text("Selecionar Arquivo para Restaurar", color = colors.textPrimary)
            }

            Spacer(Modifier.height(18.dp))

            Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Backup Automático (WorkManager)", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Cria novos arquivos com timestamp na pasta escolhida para nunca sobrescrever backups antigos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Desativado", 1 to "Diário", 7 to "Semanal").forEach { (days, label) ->
                            FilterChip(
                                selected = preferences.autoBackupIntervalDays == days,
                                onClick = { onConfigureAutoBackup(days, preferences.autoBackupDirectoryUri) },
                                label = { Text(label) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { dirLauncher.launch(null) }) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (preferences.autoBackupDirectoryUri.isBlank()) "Escolher pasta de destino" else "Pasta configurada (Alterar)",
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            if (backupHistory.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Histórico de Backups", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                backupHistory.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(item.fileName, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${if (item.isAutomatic) "Automático" else "Manual"} • ${item.itemCount} itens • ${TimeFormatter.formatFileSize(item.sizeBytes)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(140.dp))
        }
    }

    previewedBackup?.let { preview ->
        var preserveExisting by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = onDismissPreview,
            containerColor = colors.card,
            title = { Text("Pré-visualizar Backup", color = colors.textPrimary) },
            text = {
                Column {
                    Text("Dispositivo: ${preview.deviceName}", color = colors.primary)
                    Text("Playlists: ${preview.playlistsCount}", color = colors.textPrimary)
                    Text("Filas: ${preview.queuesCount}", color = colors.textPrimary)
                    Text("Favoritos: ${preview.favoritesCount}", color = colors.textPrimary)
                    Text("Marcadores: ${preview.bookmarksCount} • Notas: ${preview.notesCount}", color = colors.textPrimary)
                    Text("Histórico: ${preview.historyCount} • Temas customizados: ${preview.customThemesCount}", color = colors.textPrimary)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = preserveExisting, onCheckedChange = { preserveExisting = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Preservar dados atuais (mesclar)", color = colors.textSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onConfirmRestore(preserveExisting) }) {
                    Text("Confirmar Restauração", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPreview) { Text("Cancelar") }
            },
        )
    }
}

/**
 * 24. Settings Screen — Headset multi-click mapping, Gapless, Animation saver,
 * Notification preferences, Audio Format real capability inspection, and link to About.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: AppUserPreferences,
    capabilities: List<AudioFormatCapability>,
    onSetGapless: (Boolean) -> Unit,
    onSetBatterySaverAnim: (Boolean) -> Unit,
    onSetNotificationPrefs: (Boolean, Boolean) -> Unit,
    onSetHeadsetMappings: (HeadsetButtonMapping, HeadsetButtonMapping, HeadsetButtonMapping, HeadsetButtonMapping) -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenAbout: () -> Unit,
    activeFontName: String = "MYATO Original",
    onOpenFontSettings: () -> Unit = {},
    onSetPlayerVisualStyle: (PlayerVisualBackgroundStyle) -> Unit = {},
    onSetEffectIntensity: (Float) -> Unit = {},
    onSetAnimationsDisabled: (Boolean) -> Unit = {},
    onSetReduceMotion: (Boolean) -> Unit = {},
    onSetReduceEffectsOnLowBattery: (Boolean) -> Unit = {},
    activeThemeName: String = "Myato Original",
    onOpenMyatoThemes: () -> Unit = {},
    onSelectAnimationIntensity: (com.example.domain.models.ThemeAnimationIntensity) -> Unit = {},
) {
    val colors = LocalMyatoColors.current
    var showFormatsDialog by remember { mutableStateOf(false) }
    var showVisualStyleSheet by remember { mutableStateOf(false) }
    val currentThemeIntensity = remember(preferences.animationsDisabled, preferences.visualizerAnimationIntensity) {
        com.example.domain.models.ThemeAnimationIntensity.fromFactor(
            factor = preferences.visualizerAnimationIntensity,
            disabled = preferences.animationsDisabled,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Configurações", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            MyatoCompactBrandBadge(symbolSize = 28.dp)
        }
        Spacer(Modifier.height(12.dp))

        // Seção Personalização (Configurações -> Personalização -> Myato Themes)
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.cardSurfaceColor()),
            border = colors.borderStroke(),
            shape = colors.resolveCardShape(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_personalization_section"),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Personalização", style = MaterialTheme.typography.titleMedium, color = colors.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                SettingActionRow(
                    title = "Myato Themes",
                    subtitle = "Ativo: $activeThemeName • 50+ temas, papéis de parede e Studio",
                    onClick = onOpenMyatoThemes,
                    modifier = Modifier.testTag("settings_myato_themes_row"),
                )
                SettingActionRow(
                    title = "Fontes e tipografia",
                    subtitle = "Catálogo de fontes • Ativa: $activeFontName",
                    onClick = onOpenFontSettings,
                    modifier = Modifier.testTag("settings_font_row"),
                )
                SettingActionRow(
                    title = "Estilo visual da reprodução",
                    subtitle = "${preferences.playerVisualStyle.title} • Transições e efeitos de movimento",
                    onClick = { showVisualStyleSheet = true },
                    modifier = Modifier.testTag("settings_visual_style_row"),
                )
                Spacer(Modifier.height(6.dp))
                Text("Animações dos Temas", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("settings_animation_intensity_row"),
                ) {
                    com.example.domain.models.ThemeAnimationIntensity.entries.forEach { level ->
                        FilterChip(
                            selected = currentThemeIntensity == level,
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

        Spacer(Modifier.height(12.dp))
        SettingToggleCard("Reprodução sem intervalos (Gapless)", "Transição contínua entre faixas da fila.", preferences.gaplessEnabled, onSetGapless)
        SettingToggleCard("Economia de animações", "Desativa rotações e efeitos pesados para poupar bateria.", preferences.batterySaverAnimations, onSetBatterySaverAnim)
        SettingToggleCard("Ações extras na notificação", "Exibe botão rápido de Adicionar/Remover Playlist na mídia.", preferences.notificationShowPlaylistButton) {
            onSetNotificationPrefs(preferences.notificationCompactActions, it)
        }

        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.cardSurfaceColor()),
            border = colors.borderStroke(),
            shape = colors.resolveCardShape(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Controles Físicos de Fone / Bluetooth", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                Spacer(Modifier.height(8.dp))
                HeadsetClickPicker("1 Clique", preferences.singleClickAction) {
                    onSetHeadsetMappings(it, preferences.doubleClickAction, preferences.tripleClickAction, preferences.quadClickAction)
                }
                HeadsetClickPicker("2 Cliques", preferences.doubleClickAction) {
                    onSetHeadsetMappings(preferences.singleClickAction, it, preferences.tripleClickAction, preferences.quadClickAction)
                }
                HeadsetClickPicker("3 Cliques", preferences.tripleClickAction) {
                    onSetHeadsetMappings(preferences.singleClickAction, preferences.doubleClickAction, it, preferences.quadClickAction)
                }
                HeadsetClickPicker("4+ Cliques", preferences.quadClickAction) {
                    onSetHeadsetMappings(preferences.singleClickAction, preferences.doubleClickAction, preferences.tripleClickAction, it)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SettingActionRow("Formatos de Áudio Compatíveis neste Aparelho", "Ver decodificadores reais (MP3, FLAC, OPUS, DSF...)", { showFormatsDialog = true })
        SettingActionRow("Permissões e Pastas SAF", "Gerenciar acesso ao armazenamento local", onOpenPermissions)
        SettingActionRow("Backup e Restauração", "Exportar/importar JSON e agendar backup", onOpenBackup)
        SettingActionRow("Widgets e Atalhos", "Configurar widgets e fixar atalhos", onOpenWidgets)
        SettingActionRow("Sobre o MYATO MUSIC", "Versão, privacidade offline e arquitetura", onOpenAbout)

        Spacer(Modifier.height(140.dp))
    }

    if (showVisualStyleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVisualStyleSheet = false },
            containerColor = colors.altBackground,
            contentColor = colors.textPrimary,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 28.dp),
            ) {
                PlayerVisualStyleSelectorSection(
                    selectedStyle = preferences.playerVisualStyle,
                    effectIntensity = preferences.visualizerAnimationIntensity,
                    animationsDisabled = preferences.animationsDisabled,
                    reduceMotion = preferences.reduceMotion,
                    reduceEffectsOnLowBattery = preferences.reduceEffectsOnLowBattery,
                    onSelectStyle = onSetPlayerVisualStyle,
                    onIntensityChange = onSetEffectIntensity,
                    onSetAnimationsDisabled = onSetAnimationsDisabled,
                    onSetReduceMotion = onSetReduceMotion,
                    onSetReduceEffectsOnLowBattery = onSetReduceEffectsOnLowBattery,
                )
            }
        }
    }

    if (showFormatsDialog) {
        AlertDialog(
            onDismissRequest = { showFormatsDialog = false },
            containerColor = colors.card,
            title = { Text("Compatibilidade Real de Formatos", color = colors.textPrimary) },
            text = {
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    items(capabilities, key = { it.extension }) { cap ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (cap.isSupportedOnDevice) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = if (cap.isSupportedOnDevice) Color(0xFF2ECC71) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${cap.extension} — ${if (cap.isSupportedOnDevice) "Suportado" else "Não suportado neste aparelho"}",
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text("${cap.decoderName} • ${cap.notes}", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormatsDialog = false }) { Text("Fechar", color = colors.primary) }
            },
        )
    }
}

@Composable
private fun HeadsetClickPicker(
    label: String,
    current: HeadsetButtonMapping,
    onSelect: (HeadsetButtonMapping) -> Unit,
) {
    val colors = LocalMyatoColors.current
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.textPrimary)
        Box {
            Text(current.label, color = colors.primary, fontWeight = FontWeight.SemiBold)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                HeadsetButtonMapping.entries.forEach { mapping ->
                    DropdownMenuItem(
                        text = { Text(mapping.label) },
                        onClick = { onSelect(mapping); expanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleCard(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalMyatoColors.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = colors.resolveCardShape(),
        border = colors.borderStroke(),
        colors = CardDefaults.cardColors(containerColor = colors.cardSurfaceColor()),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyatoColors.current
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        shape = colors.resolveCardShape(),
        border = colors.borderStroke(),
        colors = CardDefaults.cardColors(containerColor = colors.cardSurfaceColor()),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
            Icon(Icons.Rounded.Settings, contentDescription = null, tint = colors.iconPrimary)
        }
    }
}

/**
 * 25. About Screen (Sobre o aplicativo)
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = LocalMyatoColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("about_screen"),
    ) {
        SimpleHeader(title = "Sobre o MYATO MUSIC", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MyatoOfficialBrandLockup(
                symbolSize = 96.dp,
                animateBars = true,
                showGlow = true,
                useContrastCardOnLightTheme = true,
            )
            Spacer(Modifier.height(14.dp))
            Text("Sua música. Seu estilo. Sua experiência.", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Versão 1.0.0 • Build Offline", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            Spacer(Modifier.height(20.dp))

            Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Compromisso de Privacidade Offline", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Sem permissão INTERNET no manifesto.\n" +
                            "• Sem streaming, rastreamento ou anúncios.\n" +
                            "• Motor Media3 / ExoPlayer com reprodução gapless e MediaLibraryService.\n" +
                            "• Banco Room com 16 entidades e backups locais JSON.\n" +
                            "• Suporte a Android Auto, Widgets em 3 tamanhos, Equalizador real e 50+ Myato Themes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}
