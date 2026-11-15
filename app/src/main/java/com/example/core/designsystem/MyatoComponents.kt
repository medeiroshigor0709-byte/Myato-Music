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
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylists: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditTags: () -> Unit,
    onOpenLyrics: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.altBackground,
        contentColor = colors.textPrimary,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtwork(artUri = track.albumArtUri, size = 50.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${track.artist} • ${track.formatExtension}", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1)
                }
            }
            SheetActionRow(Icons.Rounded.PlaylistPlay, "Tocar em seguida") { onPlayNext(); onDismiss() }
            SheetActionRow(Icons.AutoMirrored.Rounded.QueueMusic, "Adicionar ao fim da fila") { onAddToQueue(); onDismiss() }
            SheetActionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, "Adicionar a playlists") { onAddToPlaylists(); onDismiss() }
            SheetActionRow(
                if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (track.isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
            ) { onToggleFavorite(); onDismiss() }
            SheetActionRow(Icons.Rounded.Edit, "Editar tags e metadados") { onEditTags(); onDismiss() }
            SheetActionRow(Icons.Rounded.MusicNote, "Ver / editar letras") { onOpenLyrics(); onDismiss() }
            SheetActionRow(Icons.Rounded.DeleteOutline, "Remover da biblioteca") { onRemoveFromLibrary(); onDismiss() }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
    }
}

@Composable
fun AddToPlaylistsDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onConfirm: (List<Long>) -> Unit,
) {
    val selected = remember { mutableStateListOf<Long>() }
    val colors = LocalMyatoColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = { Text("Adicionar às playlists") },
        text = {
            if (playlists.isEmpty()) {
                Text("Nenhuma playlist criada ainda. Crie uma abaixo ou na aba Playlists.", color = colors.textSecondary)
            } else {
                Column {
                    playlists.forEach { playlist ->
                        val checked = playlist.id in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selected.remove(playlist.id) else selected.add(playlist.id)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (it) selected.add(playlist.id) else selected.remove(playlist.id)
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(playlist.name, color = colors.textPrimary)
                                Text("${playlist.trackCount} músicas", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selected.isNotEmpty()) onConfirm(selected.toList())
                    onDismiss()
                },
                enabled = selected.isNotEmpty(),
            ) {
                Text("Adicionar", color = colors.primary)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onCreatePlaylist("Playlist ${playlists.size + 1}") }) {
                    Text("Nova Playlist", color = colors.secondary)
                }
                TextButton(onClick = onDismiss) {
                    Text("Fechar", color = colors.textSecondary)
                }
            }
        },
    )
}
