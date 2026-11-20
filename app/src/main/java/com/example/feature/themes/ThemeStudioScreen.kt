package com.example.feature.themes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.BuiltInThemes
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.designsystem.ThemeParticleOverlay
import com.example.core.designsystem.ThemeWallpaperLayer
import com.example.core.designsystem.contrastContentColor
import com.example.core.designsystem.toExtendedColors
import com.example.domain.models.AnimationMode
import com.example.domain.models.ButtonStyle
import com.example.domain.models.CoverShape
import com.example.domain.models.MyatoTheme
import com.example.domain.models.PlayerAppearance
import com.example.domain.models.ThemeAnimationType
import com.example.domain.models.ThemeBorderStyle
import com.example.domain.models.ThemeCategory
import com.example.domain.models.ThemeCreationMode
import com.example.domain.models.ThemeEffectPreset
import com.example.domain.models.ThemeGradientStyle
import com.example.domain.models.ThemeIconStyle
import com.example.domain.models.WallpaperStyle
import com.example.feature.library.SimpleHeader

private enum class ColorTargetToken(val label: String) {
    PRIMARY("Principal"),
    SECONDARY("Secundária"),
    ACCENT("Destaque"),
    BACKGROUND("Fundo"),
    CARD("Cartões"),
    TEXT("Texto Principal"),
    BUTTON("Botões"),
    ICON("Ícones"),
    BORDER("Bordas"),
    GRADIENT_END("Fim do Gradiente"),
}

/**
 * Myato Theme Studio (ThemeEditorScreen):
 * Supports 3 creation modes (SCRATCH, FROM_THEME, FROM_IMAGE), zero-permission Photo Picker wallpaper
 * & palette extraction, full color/RGB/Hex picker, transparency/shadow/border/gradient/icon/shape/
 * player/animation editors, real-time animated preview, JSON export, and OS Share.
 */
@Composable
fun ThemeEditorScreen(
    sourceTheme: MyatoTheme,
    creationMode: ThemeCreationMode = ThemeCreationMode.FROM_THEME,
    onBack: () -> Unit,
    onSaveTheme: (MyatoTheme) -> Unit,
    onGenerateFromCover: ((MyatoTheme) -> Unit) -> Unit,
    onExportThemeJson: (MyatoTheme) -> String,
    onNotify: (String) -> Unit,
    onCreateFromGalleryImage: ((Uri, Boolean, String, Boolean, MyatoTheme, (MyatoTheme) -> Unit) -> Unit)? = null,
    onPersistWallpaperUri: ((Uri, (String?) -> Unit) -> Unit)? = null,
    onShareTheme: (MyatoTheme) -> Unit = {},
) {
    val colors = LocalMyatoColors.current
    val context = LocalContext.current

    var draft by remember(sourceTheme.id, creationMode) {
        val base = when {
            creationMode == ThemeCreationMode.SCRATCH -> BuiltInThemes.MonoBlack.copy(
                id = "custom_${System.currentTimeMillis()}",
                name = "Meu Tema Myato",
                isBuiltIn = false,
                category = ThemeCategory.CUSTOM,
                description = "Tema autoral criado no Myato Theme Studio.",
                primaryColorHex = 0xFFFF8500L,
                secondaryColorHex = 0xFF7C4DFFL,
                accentColorHex = 0xFFFFB74DL,
                buttonColorHex = 0xFFFF8500L,
                iconColorHex = 0xFFFF8500L,
                borderColorHex = 0xFFFF8500L,
                wallpaperStyle = WallpaperStyle.GRADIENT,
                animationType = ThemeAnimationType.SOUND_WAVES,
            )
            sourceTheme.isBuiltIn -> sourceTheme.copy(
                id = "custom_${System.currentTimeMillis()}",
                name = "${sourceTheme.name} Studio",
                isBuiltIn = false,
                category = ThemeCategory.CUSTOM,
            )
            else -> sourceTheme
        }
        mutableStateOf(base)
    }

    var selectedColorTarget by remember { mutableStateOf(ColorTargetToken.PRIMARY) }
    var hexInput by remember(selectedColorTarget, draft) {
        val currentColor = draft.readTokenHex(selectedColorTarget)
        mutableStateOf(String.format("%06X", (currentColor and 0xFFFFFFL).toInt()))
    }

    val galleryPaletteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && onCreateFromGalleryImage != null) {
            onCreateFromGalleryImage(uri, true, draft.name, draft.isLight, draft) { extracted ->
                draft = extracted
                onNotify("Paleta e papel de parede aplicados da imagem!")
            }
        }
    }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && onPersistWallpaperUri != null) {
            onPersistWallpaperUri(uri) { savedPath ->
                if (savedPath != null) {
                    draft = draft.copy(
                        wallpaperStyle = WallpaperStyle.CUSTOM_IMAGE,
                        wallpaperImagePath = savedPath,
                    )
                    onNotify("Imagem definida como papel de parede do tema.")
                }
            }
        }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val payload = onExportThemeJson(draft)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess { onNotify("Arquivo JSON do tema salvo!") }
        }
    }

    val colorSwatches = listOf(
        0xFFFF8500L, 0xFFFF5C93L, 0xFF00E5FFL, 0xFFB388FFL,
        0xFFD4AF37L, 0xFF2ECC71L, 0xFFFF3B5CL, 0xFF38BDF8L,
        0xFFFFD54FL, 0xFFFFFFFFL, 0xFFF5F7FAL, 0xFF121216L,
        0xFF070B16L, 0xFF1F2433L, 0xFF9CA3AFL,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("theme_editor_screen"),
    ) {
        SimpleHeader(title = "Myato Theme Studio", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Mode selector pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ThemeCreationMode.entries) { mode ->
                    FilterChip(
                        selected = creationMode == mode,
                        onClick = {
                            when (mode) {
                                ThemeCreationMode.SCRATCH -> draft = BuiltInThemes.MonoBlack.copy(
                                    id = "custom_${System.currentTimeMillis()}",
                                    name = "Novo Tema Myato",
                                    isBuiltIn = false,
                                    category = ThemeCategory.CUSTOM,
                                )
                                ThemeCreationMode.FROM_IMAGE -> galleryPaletteLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                                ThemeCreationMode.FROM_THEME -> Unit
                            }
                        },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.onPrimary,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Live Interactive Preview Card
            val draftExt = remember(draft) { draft.toExtendedColors() }
            Card(
                shape = draftExt.resolveCardShape(),
                border = draftExt.borderStroke(highlighted = true),
                colors = CardDefaults.cardColors(containerColor = draftExt.background),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .testTag("studio_live_preview_card"),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ThemeWallpaperLayer(colors = draftExt, overlayStrength = 1.1f)
                    ThemeParticleOverlay(
                        animationType = draftExt.animationType,
                        primaryColor = draftExt.primary,
                        secondaryColor = draftExt.secondary,
                        accentColor = draftExt.accent,
                        themeAnimationMode = draftExt.animationMode,
                        intensityOverride = 0.85f,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("PRÉVIA AO VIVO", style = MaterialTheme.typography.labelSmall, color = draftExt.primary, fontWeight = FontWeight.Bold)
                            Text(draftExt.animationType.label, style = MaterialTheme.typography.labelSmall, color = draftExt.textSecondary)
                        }
                        Card(
                            shape = draftExt.resolveCardShape(),
                            border = draftExt.borderStroke(),
                            colors = CardDefaults.cardColors(containerColor = draftExt.cardSurfaceColor()),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(draftExt.resolveCoverShape())
                                        .background(draftExt.primary),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(draft.name.ifBlank { "Meu Tema" }, color = draftExt.textPrimary, fontWeight = FontWeight.Bold)
                                    Text(draft.description, style = MaterialTheme.typography.bodySmall, color = draftExt.textSecondary, maxLines = 1)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(draftExt.resolveButtonShape())
                                        .background(draftExt.buttonContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = draftExt.buttonContent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Image / Cover Extraction Actions (Zero-permission Photo Picker)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onGenerateFromCover { generated -> draft = generated } },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.secondary, contentColor = colors.onSecondary),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Capa Atual", maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        galleryPaletteLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("De Imagem", color = colors.textPrimary, maxLines = 1)
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = { Text("Nome do Tema") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_name_input"),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.description,
                onValueChange = { draft = draft.copy(description = it) },
                label = { Text("Descrição") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Modo Claro (Superfícies Claras)", color = colors.textPrimary, fontWeight = FontWeight.Medium)
                Switch(
                    checked = draft.isLight,
                    onCheckedChange = { light ->
                        draft = draft.copy(
                            isLight = light,
                            textColorHex = if (light) 0xFF141722L else 0xFFFFFFFFL,
                            secondaryTextColorHex = if (light) 0xFF566075L else 0xFFB8BCC6L,
                        )
                    },
                )
            }

            // Color Token Picker + Hex + RGB
            Text("Editor Completo de Cores", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 6.dp),
            ) {
                items(ColorTargetToken.entries) { token ->
                    val swatch = Color(draft.readTokenHex(token))
                    FilterChip(
                        selected = selectedColorTarget == token,
                        onClick = { selectedColorTarget = token },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(1.dp, colors.textSecondary, CircleShape),
                            )
                        },
                        label = { Text(token.label) },
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(colorSwatches) { hex ->
                    val active = hex == draft.readTokenHex(selectedColorTarget)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(if (active) 3.dp else 1.dp, if (active) colors.primary else Color.Gray, CircleShape)
                            .clickable { draft = draft.withTokenHex(selectedColorTarget, hex) },
                    )
                }
            }

            val currentHex = draft.readTokenHex(selectedColorTarget)
            val rVal = ((currentHex shr 16) and 0xFF).toFloat()
            val gVal = ((currentHex shr 8) and 0xFF).toFloat()
            val bVal = (currentHex and 0xFF).toFloat()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val cleaned = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
                        hexInput = cleaned
                        if (cleaned.length == 6) {
                            val parsed = (0xFF000000L or cleaned.toLong(16))
                            draft = draft.withTokenHex(selectedColorTarget, parsed)
                        }
                    },
                    prefix = { Text("#") },
                    label = { Text("HEX") },
                    singleLine = true,
                    modifier = Modifier.width(136.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("R:${rVal.toInt()} G:${gVal.toInt()} B:${bVal.toInt()}", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Slider(
                        value = rVal,
                        onValueChange = { draft = draft.withTokenHex(selectedColorTarget, packRgb(it.toInt(), gVal.toInt(), bVal.toInt())) },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red),
                    )
                    Slider(
                        value = gVal,
                        onValueChange = { draft = draft.withTokenHex(selectedColorTarget, packRgb(rVal.toInt(), it.toInt(), bVal.toInt())) },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF2ECC71), activeTrackColor = Color(0xFF2ECC71)),
                    )
                    Slider(
                        value = bVal,
                        onValueChange = { draft = draft.withTokenHex(selectedColorTarget, packRgb(rVal.toInt(), gVal.toInt(), it.toInt())) },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8)),
                    )
                }
            }

            // Wallpaper & Surface Sliders
            Spacer(Modifier.height(8.dp))
            Text("Papel de Parede e Superfícies", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                OutlinedButton(
                    onClick = { wallpaperPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Escolher Foto de Fundo", color = colors.textPrimary, maxLines = 1)
                }
                if (!draft.wallpaperImagePath.isNullOrBlank()) {
                    IconButton(onClick = { draft = draft.copy(wallpaperImagePath = null, wallpaperStyle = WallpaperStyle.GRADIENT) }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Remover foto", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WallpaperStyle.entries.filter { it != WallpaperStyle.CUSTOM_IMAGE || !draft.wallpaperImagePath.isNullOrBlank() }) { wp ->
                    FilterChip(
                        selected = draft.wallpaperStyle == wp,
                        onClick = { draft = draft.copy(wallpaperStyle = wp) },
                        label = { Text(wp.label) },
                    )
                }
            }

            LabeledSlider("Transparência dos Cartões (${(draft.surfaceAlpha * 100).toInt()}%)", draft.surfaceAlpha, 0.45f..1.0f) {
                draft = draft.copy(surfaceAlpha = it)
            }
            LabeledSlider("Sombra e Elevação (${draft.shadowElevationDp.toInt()} dp)", draft.shadowElevationDp, 0f..18f) {
                draft = draft.copy(shadowElevationDp = it)
            }
            LabeledSlider("Escurecer Wallpaper (${(draft.wallpaperDimAlpha * 100).toInt()}%)", draft.wallpaperDimAlpha, 0f..0.8f) {
                draft = draft.copy(wallpaperDimAlpha = it)
            }

            // Shapes, Borders, Gradients, Icons, Effects, Animations
            ChipEnumRow("Gradiente", ThemeGradientStyle.entries, draft.gradientStyle, { it.label }) {
                draft = draft.copy(gradientStyle = it)
            }
            ChipEnumRow("Bordas", ThemeBorderStyle.entries, draft.borderStyle, { it.label }) {
                draft = draft.copy(borderStyle = it)
            }
            ChipEnumRow("Formato da Capa", CoverShape.entries, draft.coverShape, { it.name }) {
                draft = draft.copy(coverShape = it)
            }
            ChipEnumRow("Estilo dos Botões", ButtonStyle.entries, draft.buttonStyle, { it.name }) {
                draft = draft.copy(buttonStyle = it)
            }
            ChipEnumRow("Estilo dos Ícones", ThemeIconStyle.entries, draft.iconStyle, { it.label }) {
                draft = draft.copy(iconStyle = it)
            }
            ChipEnumRow("Efeito Visual", ThemeEffectPreset.entries, draft.effectPreset, { it.label }) {
                draft = draft.copy(effectPreset = it)
            }
            ChipEnumRow("Aparência do Player", PlayerAppearance.entries, draft.playerAppearance, { it.name }) {
                draft = draft.copy(playerAppearance = it)
            }
            ChipEnumRow("Modo de Movimento", AnimationMode.entries, draft.animationMode, { it.name }) {
                draft = draft.copy(animationMode = it)
            }
            ChipEnumRow("Partículas do Tema", ThemeAnimationType.entries, draft.animationType, { it.label }) {
                draft = draft.copy(animationType = it)
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val json = onExportThemeJson(draft)
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("MYATO Theme", json))
                        onNotify("JSON copiado! Salvando arquivo...")
                        exportFileLauncher.launch("${draft.name.replace(" ", "_")}.json")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Exportar", color = colors.textPrimary)
                }
                OutlinedButton(
                    onClick = { onShareTheme(draft) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Enviar", color = colors.textPrimary)
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onSaveTheme(draft); onBack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("studio_save_apply_button"),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = contrastContentColor(colors.primary)),
            ) {
                Icon(Icons.Rounded.Palette, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Salvar e Aplicar Tema", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(140.dp))
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    val colors = LocalMyatoColors.current
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun <T> ChipEnumRow(
    title: String,
    entries: List<T>,
    selected: T,
    labelProvider: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val colors = LocalMyatoColors.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { entry ->
                FilterChip(
                    selected = selected == entry,
                    onClick = { onSelect(entry) },
                    label = { Text(labelProvider(entry)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primary,
                        selectedLabelColor = colors.onPrimary,
                    ),
                )
            }
        }
    }
}

private fun packRgb(r: Int, g: Int, b: Int): Long {
    val argb = (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    return argb.toLong() and 0xFFFFFFFFL
}

private fun MyatoTheme.readTokenHex(token: ColorTargetToken): Long = when (token) {
    ColorTargetToken.PRIMARY -> primaryColorHex
    ColorTargetToken.SECONDARY -> secondaryColorHex
    ColorTargetToken.ACCENT -> accentColorHex
    ColorTargetToken.BACKGROUND -> backgroundColorHex
    ColorTargetToken.CARD -> cardColorHex
    ColorTargetToken.TEXT -> textColorHex
    ColorTargetToken.BUTTON -> buttonColorHex
    ColorTargetToken.ICON -> iconColorHex
    ColorTargetToken.BORDER -> borderColorHex
    ColorTargetToken.GRADIENT_END -> gradientEndColorHex
}

private fun MyatoTheme.withTokenHex(token: ColorTargetToken, hex: Long): MyatoTheme = when (token) {
    ColorTargetToken.PRIMARY -> copy(primaryColorHex = hex)
    ColorTargetToken.SECONDARY -> copy(secondaryColorHex = hex)
    ColorTargetToken.ACCENT -> copy(accentColorHex = hex)
    ColorTargetToken.BACKGROUND -> copy(backgroundColorHex = hex, altBackgroundColorHex = hex)
    ColorTargetToken.CARD -> copy(cardColorHex = hex)
    ColorTargetToken.TEXT -> copy(textColorHex = hex)
    ColorTargetToken.BUTTON -> copy(buttonColorHex = hex)
    ColorTargetToken.ICON -> copy(iconColorHex = hex)
    ColorTargetToken.BORDER -> copy(borderColorHex = hex)
    ColorTargetToken.GRADIENT_END -> copy(gradientEndColorHex = hex)
}
