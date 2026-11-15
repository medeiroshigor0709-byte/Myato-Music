package com.example.feature.equalizer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.audio.EqualizerEngine
import com.example.core.common.TimeFormatter
import com.example.core.database.AudioPresetEntity
import com.example.core.designsystem.LocalMyatoColors
import com.example.domain.models.AudioEqualizerState
import com.example.domain.models.AudioOutputDeviceType
import com.example.domain.models.SleepTimerMode
import com.example.domain.models.SleepTimerState
import com.example.feature.library.SimpleHeader

/**
 * 17. Equalizer Screen — Real AudioEffect bands, BassBoost, Virtualizer, Loudness, Presets,
 * Save/Reset, and independent profiles for Speaker, Wired Headset, Bluetooth, and Other devices.
 */
@Composable
fun EqualizerScreen(
    eqState: AudioEqualizerState,
    savedPresets: List<AudioPresetEntity>,
    builtInPresetNames: List<String>,
    visualizerEnabled: Boolean,
    onBack: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectOutputDevice: (AudioOutputDeviceType) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Int) -> Unit,
    onTrebleChange: (Int) -> Unit,
    onApplyBuiltInPreset: (String) -> Unit,
    onApplySavedPreset: (AudioPresetEntity) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onResetDefaults: () -> Unit,
    onToggleVisualizer: (Boolean) -> Unit,
) {
    val colors = LocalMyatoColors.current
    var showSaveDialog by remember { mutableStateOf(false) }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onToggleVisualizer(granted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("equalizer_screen"),
    ) {
        SimpleHeader(title = "Equalizador de Áudio", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (!eqState.isAvailable) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.primary.copy(alpha = 0.16f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Status do Hardware de Áudio", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            eqState.availabilityReason.ifBlank {
                                "Inicie uma música para anexar o Equalizador real do Android ao fluxo de áudio ativo."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            // Master EQ Toggle
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Equalizador Ativo", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text("Perfil: ${eqState.outputDevice.displayName} • ${eqState.presetName}", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    Switch(
                        checked = eqState.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        enabled = eqState.isAvailable,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Perfil Independente por Saída", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AudioOutputDeviceType.entries) { device ->
                    FilterChip(
                        selected = eqState.outputDevice == device,
                        onClick = { onSelectOutputDevice(device) },
                        label = { Text(device.displayName) },
                        leadingIcon = {
                            Icon(
                                imageVector = when (device) {
                                    AudioOutputDeviceType.SPEAKER -> Icons.Rounded.Speaker
                                    AudioOutputDeviceType.WIRED_HEADSET -> Icons.Rounded.Headphones
                                    AudioOutputDeviceType.BLUETOOTH -> Icons.Rounded.BluetoothAudio
                                    AudioOutputDeviceType.OTHER -> Icons.Rounded.GraphicEq
                                },
                                contentDescription = null,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = Color.Black,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Presets", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(builtInPresetNames) { name ->
                    FilterChip(
                        selected = eqState.presetName == name,
                        onClick = { onApplyBuiltInPreset(name) },
                        label = { Text(name) },
                    )
                }
                items(savedPresets, key = { it.id }) { preset ->
                    FilterChip(
                        selected = eqState.presetName == preset.name,
                        onClick = { onApplySavedPreset(preset) },
                        label = { Text("★ ${preset.name}") },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Bandas de Frequência", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                    Spacer(Modifier.height(8.dp))
                    if (eqState.bands.isEmpty()) {
                        Text("As bandas reais do aparelho aparecerão assim que uma faixa estiver em reprodução.", color = colors.textSecondary)
                    } else {
                        eqState.bands.forEach { band ->
                            val freqLabel = if (band.centerFreqHz >= 1000) "${band.centerFreqHz / 1000} kHz" else "${band.centerFreqHz} Hz"
                            val dbLabel = String.format("%+.1f dB", band.levelMillibels / 100f)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(freqLabel, style = MaterialTheme.typography.labelMedium, color = colors.textPrimary, modifier = Modifier.width(64.dp))
                                Slider(
                                    value = band.levelMillibels.toFloat(),
                                    onValueChange = { onBandChange(band.index, it.toInt()) },
                                    valueRange = band.minMillibels.toFloat()..band.maxMillibels.toFloat(),
                                    enabled = eqState.isAvailable && eqState.isEnabled,
                                    colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(dbLabel, style = MaterialTheme.typography.labelSmall, color = colors.primary, modifier = Modifier.width(56.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EffectSlider("Graves (Bass Boost)", eqState.bassBoostStrength.toFloat(), 0f..1000f, "${eqState.bassBoostStrength / 10}%", eqState.isAvailable && eqState.isEnabled) {
                        onBassBoostChange(it.toInt())
                    }
                    EffectSlider("Agudos (Treble)", eqState.trebleBoostDb.toFloat(), -6f..6f, "${eqState.trebleBoostDb} dB", eqState.isAvailable && eqState.isEnabled) {
                        onTrebleChange(it.toInt())
                    }
                    EffectSlider("Virtualizer 3D", eqState.virtualizerStrength.toFloat(), 0f..1000f, "${eqState.virtualizerStrength / 10}%", eqState.isAvailable && eqState.isEnabled) {
                        onVirtualizerChange(it.toInt())
                    }
                    EffectSlider("Loudness", eqState.loudnessGainMb.toFloat(), 0f..1500f, "${eqState.loudnessGainMb} mB", eqState.isAvailable && eqState.isEnabled) {
                        onLoudnessChange(it.toInt())
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Visualizador de Espectro Real", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                        Text("Usa API Visualizer do Android (requer permissão RECORD_AUDIO).", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = visualizerEnabled,
                        onCheckedChange = { enable ->
                            if (enable) recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            else onToggleVisualizer(false)
                        },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onResetDefaults,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Restaurar", color = colors.textPrimary)
                }
                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Salvar Preset", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(140.dp))
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = colors.card,
            title = { Text("Salvar Preset (${eqState.outputDevice.displayName})", color = colors.textPrimary) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do preset") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { onSaveCustomPreset(name); showSaveDialog = false }) { Text("Salvar", color = colors.primary) }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun EffectSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    label: String,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    val colors = LocalMyatoColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.width(120.dp))
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = colors.secondary, activeTrackColor = colors.secondary),
            modifier = Modifier.weight(1f),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.primary, modifier = Modifier.width(52.dp))
    }
}

/**
 * 18. Sleep Timer Screen — Mode A (Duration/Clock Time) & Mode B (Track Count), Pause/Resume/Cancel.
 */
@Composable
fun SleepTimerScreen(
    timerState: SleepTimerState,
    onBack: () -> Unit,
    onStartMinutes: (Int) -> Unit,
    onStartClockTime: (Int, Int) -> Unit,
    onStartTrackCount: (Int) -> Unit,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    var customMinutes by remember { mutableIntStateOf(30) }
    var targetHour by remember { mutableIntStateOf(23) }
    var targetMinute by remember { mutableIntStateOf(0) }
    var trackCount by remember { mutableIntStateOf(5) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("sleep_timer_screen"),
    ) {
        SimpleHeader(title = "Temporizador de Reprodução", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (timerState.mode != SleepTimerMode.NONE) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.primary.copy(alpha = 0.16f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when (timerState.mode) {
                                SleepTimerMode.DURATION_OR_TIME -> TimeFormatter.formatDuration(timerState.remainingMs)
                                SleepTimerMode.TRACK_COUNT -> "${timerState.remainingTracks} música(s) restante(s)"
                                SleepTimerMode.NONE -> ""
                            },
                            style = MaterialTheme.typography.displayLarge,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        if (timerState.targetClockLabel.isNotBlank()) {
                            Text("Encerra às ${timerState.targetClockLabel}", color = colors.textSecondary)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onPauseResume,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                            ) {
                                Icon(if (timerState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (timerState.isPaused) "Retomar" else "Pausar")
                            }
                            OutlinedButton(onClick = onCancel) {
                                Text("Cancelar", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Mode A: By Duration or Clock Time
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Modalidade A — Por Horário ou Duração", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(15, 30, 45, 60, 90)) { mins ->
                            FilterChip(
                                selected = customMinutes == mins,
                                onClick = { customMinutes = mins; onStartMinutes(mins) },
                                label = { Text("${mins} min") },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Ou definir horário exato (HH:MM):", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetHour.toString().padStart(2, '0'),
                            onValueChange = { targetHour = (it.toIntOrNull() ?: 0).coerceIn(0, 23) },
                            label = { Text("Hora") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = targetMinute.toString().padStart(2, '0'),
                            onValueChange = { targetMinute = (it.toIntOrNull() ?: 0).coerceIn(0, 59) },
                            label = { Text("Min") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Button(
                            onClick = { onStartClockTime(targetHour, targetMinute) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                        ) {
                            Text("Agendar")
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Mode B: By Track Count
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Modalidade B — Por Quantidade de Músicas", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Encerrar após $trackCount música(s)", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    Slider(
                        value = trackCount.toFloat(),
                        onValueChange = { trackCount = it.toInt().coerceIn(1, 30) },
                        valueRange = 1f..30f,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary),
                    )
                    Button(
                        onClick = { onStartTrackCount(trackCount) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                    ) {
                        Text("Ativar após $trackCount música(s)", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(140.dp))
        }
    }
}
