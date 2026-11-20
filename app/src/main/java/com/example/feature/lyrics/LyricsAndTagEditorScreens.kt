package com.example.feature.lyrics

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Preview
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.common.TimeFormatter
import com.example.core.designsystem.AlbumArtwork
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.metadata.LrcLyricsEngine
import com.example.core.metadata.TagEditPayload
import com.example.domain.models.LyricLine
import com.example.domain.models.MusicTrack
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.feature.library.SimpleHeader
import kotlinx.coroutines.flow.StateFlow

/**
 * 14. Offline Lyrics & LRC Sync/Editor Screen
 */
@Composable
fun LyricsScreen(
    track: MusicTrack?,
    currentPositionMs: Long,
    onBack: () -> Unit,
    onSeekTo: (Long) -> Unit,
    loadLyrics: suspend (MusicTrack) -> String,
    onSaveLyrics: (MusicTrack, String) -> Unit,
    progressPositionFlow: StateFlow<Long>? = null,
) {
    val colors = LocalMyatoColors.current
    val livePositionMs = if (progressPositionFlow != null) {
        progressPositionFlow.collectAsStateWithLifecycle().value
    } else {
        currentPositionMs
    }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Visualizar, 1 = Editar / Sincronizar LRC
    var rawText by remember { mutableStateOf("") }
    val parsedLines = remember(rawText) { LrcLyricsEngine.parseLyrics(rawText) }
    val syncLines = remember { mutableStateListOf<LyricLine>() }
    var syncCursor by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    LaunchedEffect(track?.id) {
        if (track != null) {
            rawText = loadLyrics(track)
            syncLines.clear()
            syncLines.addAll(LrcLyricsEngine.parseLyrics(rawText))
            syncCursor = 0
        }
    }

    val activeIdx = remember(parsedLines, livePositionMs) {
        LrcLyricsEngine.findActiveLineIndex(parsedLines, livePositionMs)
    }

    LaunchedEffect(activeIdx) {
        if (selectedTab == 0 && activeIdx >= 0) {
            runCatching { listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0)) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lyrics_screen"),
    ) {
        SimpleHeader(title = track?.title ?: "Letras", onBack = onBack)

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.altBackground,
            contentColor = colors.primary,
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Exibir Letra") })
            Tab(selected = selectedTab == 1, onClick = {
                syncLines.clear()
                syncLines.addAll(LrcLyricsEngine.parseLyrics(rawText))
                selectedTab = 1
            }, text = { Text("Editor & Criador LRC") })
        }

        if (track == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Selecione uma música para visualizar ou editar letras offline.", color = colors.textSecondary)
            }
            return
        }

        if (selectedTab == 0) {
            if (parsedLines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sem letras salvas ou incorporadas para esta faixa.", color = colors.textSecondary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { selectedTab = 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.buttonContainer, contentColor = colors.buttonContent),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Criar / Colar Letra LRC")
                        }
                    }
                }
            } else {
                // Step forward/back between synced lines
                val syncedOnly = parsedLines.filter { it.timestampMs != null }
                if (syncedOnly.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(onClick = {
                            val prev = parsedLines.take(activeIdx.coerceAtLeast(0)).lastOrNull { it.timestampMs != null }
                            prev?.timestampMs?.let(onSeekTo)
                        }) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = colors.primary)
                            Text("Linha anterior", color = colors.textPrimary)
                        }
                        Text(TimeFormatter.formatDuration(livePositionMs), style = MaterialTheme.typography.labelMedium, color = colors.primary)
                        OutlinedButton(onClick = {
                            val next = parsedLines.drop((activeIdx + 1).coerceAtLeast(0)).firstOrNull { it.timestampMs != null }
                            next?.timestampMs?.let(onSeekTo)
                        }) {
                            Text("Próxima linha", color = colors.textPrimary)
                            Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = colors.primary)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    itemsIndexed(parsedLines) { idx, line ->
                        val isActive = idx == activeIdx
                        Text(
                            text = line.text.ifBlank { "♪" },
                            style = if (isActive) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                            color = if (isActive) colors.primary else colors.textSecondary,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = line.timestampMs != null) {
                                    line.timestampMs?.let(onSeekTo)
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            }
        } else {
            // Editor & Manual LRC Sync Tool
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(
                    "Cole a letra ou edite tags [mm:ss.cc]. Use o botão Sincronizar Linha para marcar o tempo atual da música.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val shifted = LrcLyricsEngine.shiftTimestamps(LrcLyricsEngine.parseLyrics(rawText), -500L)
                        rawText = LrcLyricsEngine.serializeToLrc(shifted, track.title, track.artist)
                    }) {
                        Text("-0.5s", color = colors.primary)
                    }
                    OutlinedButton(onClick = {
                        val shifted = LrcLyricsEngine.shiftTimestamps(LrcLyricsEngine.parseLyrics(rawText), +500L)
                        rawText = LrcLyricsEngine.serializeToLrc(shifted, track.title, track.artist)
                    }) {
                        Text("+0.5s", color = colors.primary)
                    }
                    Button(
                        onClick = {
                            val lines = LrcLyricsEngine.parseLyrics(rawText).toMutableList()
                            if (syncCursor in lines.indices) {
                                lines[syncCursor] = lines[syncCursor].copy(timestampMs = livePositionMs)
                                rawText = LrcLyricsEngine.serializeToLrc(lines, track.title, track.artist)
                                syncCursor = (syncCursor + 1).coerceAtMost(lines.lastIndex)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.buttonContainer, contentColor = colors.buttonContent),
                    ) {
                        Icon(Icons.Rounded.Timer, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Marcar Linha #${syncCursor + 1}")
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Conteúdo LRC / Letra Offline") },
                    minLines = 12,
                    modifier = Modifier.fillMaxWidth().testTag("lyrics_editor_input"),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onSaveLyrics(track, rawText) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_lyrics_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.buttonContainer, contentColor = colors.buttonContent),
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Salvar Letra / Arquivo LRC", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 15. Professional Metadata & Tag Editor Screen (Single + Batch + Cover Picker + Preview)
 */
@Composable
fun TagEditorScreen(
    targetTracks: List<MusicTrack>,
    onBack: () -> Unit,
    inspectTags: suspend (MusicTrack) -> TagEditPayload,
    onSaveTags: (List<MusicTrack>, TagEditPayload, Boolean) -> Unit,
) {
    val colors = LocalMyatoColors.current
    val context = LocalContext.current
    val isBatch = targetTracks.size > 1
    val primary = targetTracks.firstOrNull()

    var title by remember { mutableStateOf(primary?.title.orEmpty()) }
    var artist by remember { mutableStateOf(primary?.artist.orEmpty()) }
    var album by remember { mutableStateOf(primary?.album.orEmpty()) }
    var albumArtist by remember { mutableStateOf(primary?.albumArtist.orEmpty()) }
    var genre by remember { mutableStateOf(primary?.genre.orEmpty()) }
    var year by remember { mutableStateOf(primary?.year?.takeIf { it > 0 }?.toString().orEmpty()) }
    var trackNum by remember { mutableStateOf(primary?.trackNumber?.takeIf { it > 0 }?.toString().orEmpty()) }
    var discNum by remember { mutableStateOf(primary?.discNumber?.takeIf { it > 0 }?.toString().orEmpty()) }
    var composer by remember { mutableStateOf(primary?.composer.orEmpty()) }
    var comments by remember { mutableStateOf(primary?.comments.orEmpty()) }
    var embeddedLyrics by remember { mutableStateOf(primary?.embeddedLyrics.orEmpty()) }
    var newCoverBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(primary?.id) {
        if (primary != null && !isBatch) {
            val inspected = inspectTags(primary)
            title = inspected.title
            artist = inspected.artist
            album = inspected.album
            albumArtist = inspected.albumArtist
            genre = inspected.genre
            year = if (inspected.year > 0) inspected.year.toString() else ""
            trackNum = if (inspected.trackNumber > 0) inspected.trackNumber.toString() else ""
            discNum = if (inspected.discNumber > 0) inspected.discNumber.toString() else ""
            composer = inspected.composer
            comments = inspected.comments
            embeddedLyrics = inspected.embeddedLyrics
        }
    }

    // Compliant Zero-Permission Android Photo Picker
    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            newCoverBytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tag_editor_screen"),
    ) {
        SimpleHeader(
            title = if (isBatch) "Edição em Lote (${targetTracks.size} faixas)" else "Editor de Tags",
            onBack = onBack,
        )

        if (primary == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhuma faixa selecionada.", color = colors.textSecondary)
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            val nonMp3Count = targetTracks.count { !it.formatExtension.equals("MP3", ignoreCase = true) }
            if (nonMp3Count > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.primary.copy(alpha = 0.14f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                ) {
                    Text(
                        text = "Aviso: $nonMp3Count arquivo(s) não são MP3. Para formatos sem escrita ID3v2 nativa (${primary.formatExtension}), as alterações serão salvas no catálogo MYATO e MediaStore quando permitido.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AlbumArtwork(artUri = primary.albumArtUri, size = 72.dp, small = false)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = {
                        coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Rounded.Image, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(if (newCoverBytes != null) "Nova capa selecionada" else "Alterar Capa (Photo Picker)", color = colors.textPrimary)
                    }
                    Text("Arquivo: ${primary.formatExtension} • ${TimeFormatter.formatFileSize(primary.sizeBytes)}", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))
            if (!isBatch) {
                TagField("Título", title) { title = it }
            }
            TagField("Artista", artist) { artist = it }
            TagField("Álbum", album) { album = it }
            TagField("Artista do Álbum", albumArtist) { albumArtist = it }
            TagField("Gênero", genre) { genre = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) { TagField("Ano", year) { year = it.filter(Char::isDigit) } }
                if (!isBatch) {
                    Box(modifier = Modifier.weight(1f)) { TagField("Faixa #", trackNum) { trackNum = it.filter(Char::isDigit) } }
                }
                Box(modifier = Modifier.weight(1f)) { TagField("Disco #", discNum) { discNum = it.filter(Char::isDigit) } }
            }
            TagField("Compositor", composer) { composer = it }
            TagField("Comentários", comments) { comments = it }
            if (!isBatch) {
                OutlinedTextField(
                    value = embeddedLyrics,
                    onValueChange = { embeddedLyrics = it },
                    label = { Text("Letras incorporadas (USLT / LRC)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { showPreviewDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("preview_tags_button"),
                colors = ButtonDefaults.buttonColors(containerColor = colors.buttonContainer, contentColor = colors.buttonContent),
            ) {
                Icon(Icons.Rounded.Preview, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pré-visualizar & Salvar Alterações", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(90.dp))
        }
    }

    if (showPreviewDialog) {
        val payload = TagEditPayload(
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            genre = genre,
            year = year.toIntOrNull() ?: 0,
            trackNumber = trackNum.toIntOrNull() ?: 0,
            discNumber = discNum.toIntOrNull() ?: 0,
            composer = composer,
            comments = comments,
            embeddedLyrics = embeddedLyrics,
            newCoverBytes = newCoverBytes,
        )
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            containerColor = colors.card,
            title = { Text("Confirmar Alterações de Tags", color = colors.textPrimary) },
            text = {
                Column {
                    Text("Faixas afetadas: ${targetTracks.size}", color = colors.primary)
                    if (!isBatch) Text("Título: ${payload.title}", color = colors.textPrimary)
                    Text("Artista: ${payload.artist}", color = colors.textPrimary)
                    Text("Álbum: ${payload.album}", color = colors.textPrimary)
                    Text("Gênero: ${payload.genre.ifBlank { "—" }} • Ano: ${payload.year}", color = colors.textSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveTags(targetTracks, payload, isBatch)
                    showPreviewDialog = false
                    onBack()
                }) {
                    Text("Confirmar e Gravar", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreviewDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun TagField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}
