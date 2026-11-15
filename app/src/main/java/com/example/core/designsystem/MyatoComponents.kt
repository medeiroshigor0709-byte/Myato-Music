package com.example.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.core.common.TimeFormatter
import com.example.domain.models.AnimationMode
import com.example.domain.models.MusicTrack
import com.example.domain.models.Playlist

/**
 * Original geometric MYATO MUSIC brand emblem drawn via Compose Canvas.
 */
@Composable
fun MyatoLogoSymbol(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    accent: Color = Color.Black,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.11f

        // Stylized M-shaped soundwave
        val path = Path().apply {
            moveTo(w * 0.14f, h * 0.78f)
            lineTo(w * 0.14f, h * 0.30f)
            lineTo(w * 0.50f, h * 0.60f)
            lineTo(w * 0.86f, h * 0.30f)
            lineTo(w * 0.86f, h * 0.78f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )

        // Centered floating musical stem & dot
        drawLine(
            color = accent,
            start = Offset(w * 0.50f, h * 0.14f),
            end = Offset(w * 0.50f, h * 0.44f),
            strokeWidth = stroke * 0.72f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = accent,
            start = Offset(w * 0.50f, h * 0.14f),
            end = Offset(w * 0.68f, h * 0.22f),
            strokeWidth = stroke * 0.72f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = accent,
            radius = stroke * 0.65f,
            center = Offset(w * 0.45f, h * 0.44f),
        )
    }
}

@Composable
fun AlbumArtwork(
    artUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    small: Boolean = true,
) {
    val colors = LocalMyatoColors.current
    val shape = if (small) colors.resolveSmallCoverShape() else colors.resolveCoverShape()
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.primary.copy(alpha = 0.18f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (!artUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artUri)
                    .crossfade(colors.animationMode != AnimationMode.BATTERY_SAVER)
                    .error(R.drawable.img_default_album_art)
                    .placeholder(R.drawable.img_default_album_art)
                    .build(),
                contentDescription = stringResource(R.string.cd_album_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.img_default_album_art),
                contentDescription = stringResource(R.string.cd_album_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun FrequencyBarsVisualizer(
    bars: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyatoColors.current
    val useAnimatedFallback = isPlaying && bars.all { it <= 0.06f } && colors.animationMode != AnimationMode.BATTERY_SAVER
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bars")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "eq_phase",
    )

    Canvas(modifier = modifier) {
        val count = bars.size.coerceAtLeast(12)
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2.dp.toPx())
        for (i in 0 until count) {
            val amplitude = when {
                !isPlaying -> 0.12f
                useAnimatedFallback -> {
                    (0.28f + 0.55f * kotlin.math.abs(kotlin.math.sin(phase + i * 0.55f))).coerceIn(0.12f, 1f)
                }
                else -> bars.getOrElse(i) { 0.15f }.coerceIn(0.10f, 1f)
            }
            val barH = size.height * amplitude
            val x = i * (barWidth + gap)
            val y = size.height - barH
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(colors.secondary, colors.primary),
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalMyatoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactTrackRow(
    track: MusicTrack,
    isCurrent: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onMoreClick: () -> Unit,
    onQuickAddToPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyatoColors.current
    val bg = when {
        isSelected -> colors.primary.copy(alpha = 0.16f)
        isCurrent -> colors.primary.copy(alpha = 0.10f)
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (isSelected) colors.primary else colors.textSecondary,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp),
            )
        }

        Box(contentAlignment = Alignment.Center) {
            AlbumArtwork(artUri = track.albumArtUri, size = 48.dp, small = true)
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(colors.resolveSmallCoverShape())
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) colors.primary else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (track.isPlayable) colors.card else MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                ) {
                    Text(
                        text = if (track.isPlayable) track.formatExtension else "${track.formatExtension}!",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (track.isPlayable) colors.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = TimeFormatter.formatDuration(track.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
        )

        if (!isSelectionMode && onQuickAddToPlaylist != null) {
            IconButton(
                onClick = onQuickAddToPlaylist,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = "Adicionar à playlist",
                    tint = colors.primary,
                )
            }
        }

        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
                tint = colors.textSecondary,
            )
        }
    }
}

@Composable
fun MiniPlayerBar(
    track: MusicTrack?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (track == null) return
    val colors = LocalMyatoColors.current
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("mini_player_bar")
            .clickable(onClick = onOpenPlayer),
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtwork(artUri = track.albumArtUri, size = 44.dp, small = true)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mini_player_play_pause")
                        .clip(CircleShape)
                        .background(colors.primary),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause),
                        tint = if (colors.isLight) Color.White else Color.Black,
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_next"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.cd_next_track),
                        tint = colors.textPrimary,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = colors.primary,
                trackColor = colors.textSecondary.copy(alpha = 0.2f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackContextMenuSheet(
    track: MusicTrack,
    isInPlaylist: Boolean = false,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylists: () -> Unit,
    onCreatePlaylistWithTrack: () -> Unit,
    onPlayFromHere: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShareTrack: () -> Unit,
    onEditTags: () -> Unit,
    onViewDetails: () -> Unit,
    onOpenLocation: () -> Unit,
    onDeleteTrack: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
) {
    val colors = LocalMyatoColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.altBackground,
        contentColor = colors.textPrimary,
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtwork(artUri = track.albumArtUri, size = 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${track.artist} • ${track.album} • ${track.formatExtension}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }

            SheetActionRow(Icons.Rounded.PlayArrow, "Tocar agora") { onPlayNow(); onDismiss() }
            SheetActionRow(Icons.AutoMirrored.Rounded.PlaylistPlay, "Tocar a seguir") { onPlayNext(); onDismiss() }
            SheetActionRow(Icons.AutoMirrored.Rounded.QueueMusic, "Adicionar à fila") { onAddToQueue(); onDismiss() }
            SheetActionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, "Adicionar à playlist") { onAddToPlaylists(); onDismiss() }
            SheetActionRow(Icons.Rounded.Add, "Criar nova playlist com esta música") { onCreatePlaylistWithTrack(); onDismiss() }
            SheetActionRow(Icons.Rounded.PlayCircle, "Reproduzir a partir desta música") { onPlayFromHere(); onDismiss() }
            SheetActionRow(
                if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (track.isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
            ) { onToggleFavorite(); onDismiss() }
            SheetActionRow(Icons.Rounded.Share, "Compartilhar arquivo") { onShareTrack(); onDismiss() }
            SheetActionRow(Icons.Rounded.Edit, "Editar informações") { onEditTags(); onDismiss() }
            SheetActionRow(Icons.Rounded.Info, "Ver detalhes do arquivo") { onViewDetails(); onDismiss() }
            SheetActionRow(Icons.Rounded.Folder, "Abrir localização da música") { onOpenLocation(); onDismiss() }

            if (isInPlaylist && onRemoveFromPlaylist != null) {
                SheetActionRow(
                    Icons.Rounded.RemoveCircleOutline,
                    "Remover da playlist",
                    textColor = MaterialTheme.colorScheme.error,
                    iconTint = MaterialTheme.colorScheme.error,
                ) { onRemoveFromPlaylist(); onDismiss() }
            }

            SheetActionRow(
                Icons.Rounded.DeleteForever,
                "Excluir música do dispositivo",
                textColor = MaterialTheme.colorScheme.error,
                iconTint = MaterialTheme.colorScheme.error,
            ) { onDeleteTrack(); onDismiss() }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    textColor: Color? = null,
    iconTint: Color? = null,
    onClick: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint ?: colors.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor ?: colors.textPrimary,
        )
    }
}

@Composable
fun TrackDetailsDialog(
    track: MusicTrack,
    onDismiss: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = colors.primary)
                Spacer(Modifier.width(8.dp))
                Text("Detalhes do Arquivo", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DetailRow("Título", track.title)
                DetailRow("Artista", track.artist)
                DetailRow("Álbum", track.album)
                if (track.albumArtist.isNotBlank()) DetailRow("Artista do Álbum", track.albumArtist)
                if (track.genre.isNotBlank()) DetailRow("Gênero", track.genre)
                if (track.year > 0) DetailRow("Ano", track.year.toString())
                if (track.trackNumber > 0) DetailRow("Faixa / Disco", "${track.trackNumber} / ${track.discNumber.coerceAtLeast(1)}")
                DetailRow("Formato", "${track.formatExtension} (${track.mimeType})")
                DetailRow("Duração", TimeFormatter.formatDuration(track.durationMs))
                DetailRow("Tamanho do arquivo", TimeFormatter.formatFileSize(track.sizeBytes))
                if (track.bitrateKbps > 0) DetailRow("Taxa de bits", "${track.bitrateKbps} kbps")
                if (track.sampleRateHz > 0) DetailRow("Taxa de amostragem", "${track.sampleRateHz} Hz")
                DetailRow("Reproduções", "${track.playCount} vez(es)")
                DetailRow("Caminho", track.filePath)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = LocalMyatoColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
    }
}

@Composable
fun DeleteTracksConfirmDialog(
    tracks: List<MusicTrack>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp),
            )
        },
        title = {
            Text(
                text = if (tracks.size == 1) "Excluir Música do Dispositivo?" else "Excluir ${tracks.size} Músicas?",
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        },
        text = {
            Text(
                text = if (tracks.size == 1) {
                    "Tem certeza de que deseja excluir permanentemente \"${tracks.first().title}\" do armazenamento do aparelho?\n\nEsta ação apagará o arquivo físico e não pode ser desfeita."
                } else {
                    "Tem certeza de que deseja excluir permanentemente as ${tracks.size} músicas selecionadas do armazenamento do aparelho?\n\nOs arquivos de áudio serão apagados e não poderão ser recuperados."
                },
                color = colors.textSecondary,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            ) {
                Text("Excluir Permanentemente", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = colors.textPrimary)
            }
        },
    )
}

@Composable
fun CreatePlaylistDialog(
    initialName: String = "",
    trackCount: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val colors = LocalMyatoColors.current
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = {
            Text(
                text = if (trackCount > 0) "Criar Playlist com $trackCount Música(s)" else "Nova Playlist",
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = name.trim().ifBlank { "Minha Playlist" }
                    onConfirm(clean)
                    onDismiss()
                },
            ) {
                Text("Criar Playlist", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = colors.textSecondary)
            }
        },
    )
}

enum class PlaylistSortOption(val label: String) {
    RECENTLY_USED("Recentes"),
    FAVORITES_FIRST("Favoritas"),
    NAME_ASC("Nome (A-Z)"),
    TRACK_COUNT_DESC("Mais músicas"),
}

@Composable
fun AddToPlaylistsDialog(
    playlists: List<Playlist>,
    targetTrackIds: List<Long>,
    tracks: List<MusicTrack>,
    playlistTrackIdsMap: Map<Long, List<Long>> = emptyMap(),
    onDismiss: () -> Unit,
    onCreatePlaylist: (String, List<Long>) -> Unit,
    onConfirm: (playlistIds: List<Long>, allowDuplicates: Boolean) -> Unit,
) {
    val colors = LocalMyatoColors.current
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(PlaylistSortOption.RECENTLY_USED) }
    var allowDuplicates by remember { mutableStateOf(false) }
    val selectedPlaylists = remember { mutableStateListOf<Long>() }
    var showInlineCreate by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val filteredPlaylists = remember(playlists, searchQuery, sortOption) {
        val q = searchQuery.trim().lowercase()
        val list = if (q.isBlank()) playlists else playlists.filter { it.name.lowercase().contains(q) }
        when (sortOption) {
            PlaylistSortOption.RECENTLY_USED -> list.sortedByDescending { it.lastUsedAt }
            PlaylistSortOption.FAVORITES_FIRST -> list.sortedWith(compareByDescending<Playlist> { it.isFavorite }.thenByDescending { it.lastUsedAt })
            PlaylistSortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            PlaylistSortOption.TRACK_COUNT_DESC -> list.sortedByDescending { it.trackCount }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Adicionar à Playlist",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "${targetTrackIds.size} música(s) selecionada(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primary,
                        )
                    }
                    IconButton(onClick = { showInlineCreate = !showInlineCreate }) {
                        Icon(
                            imageVector = if (showInlineCreate) Icons.Rounded.Close else Icons.Rounded.Add,
                            contentDescription = "Criar nova playlist",
                            tint = colors.primary,
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
            ) {
                if (showInlineCreate) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = { Text("Nome da nova playlist...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                val name = newPlaylistName.trim().ifBlank { "Nova Playlist" }
                                onCreatePlaylist(name, targetTrackIds)
                                onDismiss()
                            },
                        ) {
                            Text("Criar", color = colors.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar playlist...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                Spacer(Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(PlaylistSortOption.entries) { opt ->
                        FilterChip(
                            selected = sortOption == opt,
                            onClick = { sortOption = opt },
                            label = { Text(opt.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = Color.Black,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (filteredPlaylists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Nenhuma playlist criada ainda." else "Nenhuma playlist encontrada.",
                            color = colors.textSecondary,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(filteredPlaylists, key = { it.id }) { playlist ->
                            val isChecked = playlist.id in selectedPlaylists
                            val existingIds = playlistTrackIdsMap[playlist.id].orEmpty()
                            val duplicateHits = targetTrackIds.count { it in existingIds }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedPlaylists.remove(playlist.id)
                                        else selectedPlaylists.add(playlist.id)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isChecked) colors.primary.copy(alpha = 0.16f) else colors.altBackground,
                                border = if (isChecked) androidx.compose.foundation.BorderStroke(1.dp, colors.primary) else null,
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            if (it) selectedPlaylists.add(playlist.id)
                                            else selectedPlaylists.remove(playlist.id)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = colors.primary,
                                            checkmarkColor = Color.Black,
                                        ),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    AlbumArtwork(artUri = playlist.coverUri, size = 42.dp, small = true)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (isChecked) colors.primary else colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            if (playlist.isFavorite) {
                                                Spacer(Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Rounded.Favorite,
                                                    contentDescription = null,
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(14.dp),
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${playlist.trackCount} músicas",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                        )
                                        if (duplicateHits > 0) {
                                            Text(
                                                text = if (duplicateHits == 1) "Já contém 1 destas músicas" else "Já contém $duplicateHits destas músicas",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.secondary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { allowDuplicates = !allowDuplicates }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = allowDuplicates,
                        onCheckedChange = { allowDuplicates = it },
                        colors = CheckboxDefaults.colors(checkedColor = colors.primary, checkmarkColor = Color.Black),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Permitir adicionar músicas repetidas",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedPlaylists.isNotEmpty()) {
                        onConfirm(selectedPlaylists.toList(), allowDuplicates)
                        onDismiss()
                    }
                },
                enabled = selectedPlaylists.isNotEmpty(),
            ) {
                Text(
                    text = if (selectedPlaylists.isEmpty()) "Adicionar" else "Adicionar (${selectedPlaylists.size})",
                    color = if (selectedPlaylists.isNotEmpty()) colors.primary else colors.textSecondary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = colors.textSecondary)
            }
        },
    )
}
