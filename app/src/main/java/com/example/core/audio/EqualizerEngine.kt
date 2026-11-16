package com.example.core.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import androidx.core.content.ContextCompat
import com.example.core.database.AudioPresetEntity
import com.example.core.database.MyatoDatabase
import com.example.domain.models.AudioEqualizerState
import com.example.domain.models.AudioOutputDeviceType
import com.example.domain.models.EqualizerBand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Real Android AudioEffect Equalizer, BassBoost, Virtualizer, LoudnessEnhancer, and Visualizer engine.
 * Maintains independent presets per output device (Speaker, Wired Headset, Bluetooth, Other).
 * Never fakes equalization if hardware/OS reports effect unavailability.
 */
class EqualizerEngine(
    private val context: Context,
    private val database: MyatoDatabase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var visualizer: Visualizer? = null
    private var attachedSessionId: Int = 0
    private var isForegroundActive: Boolean = true
    private var isVisualizerUserEnabled: Boolean = true
    private val reusableFftMagnitudes = FloatArray(16)

    private val _state = MutableStateFlow(AudioEqualizerState())
    val state: StateFlow<AudioEqualizerState> = _state.asStateFlow()

    private val _spectrumBars = MutableStateFlow(List(16) { 0f })
    val spectrumBars: StateFlow<List<Float>> = _spectrumBars.asStateFlow()

    private val _hasRealFftSignal = MutableStateFlow(false)
    val hasRealFftSignal: StateFlow<Boolean> = _hasRealFftSignal.asStateFlow()

    private val _savedPresets = MutableStateFlow<List<AudioPresetEntity>>(emptyList())
    val savedPresets: StateFlow<List<AudioPresetEntity>> = _savedPresets.asStateFlow()

    val builtInPresets: Map<String, List<Int>> = mapOf(
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Rock" to listOf(400, 200, -100, 300, 500),
        "Pop" to listOf(-100, 250, 400, 200, -100),
        "Jazz" to listOf(300, 150, -150, 200, 400),
        "Clássico" to listOf(450, 250, 0, 250, 450),
        "Graves Profundos" to listOf(700, 450, 100, 0, 100),
        "Vocal" to listOf(-250, 100, 500, 350, 0),
        "Eletrônica" to listOf(550, 300, 0, 200, 500),
    )

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshOutputDeviceProfile()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshOutputDeviceProfile()
        }
    }

    init {
        runCatching { audioManager.registerAudioDeviceCallback(deviceCallback, null) }
        refreshOutputDeviceProfile()
    }

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == attachedSessionId) return
        releaseEffects()
        attachedSessionId = audioSessionId

        val eq = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
        if (eq == null) {
            _state.value = _state.value.copy(
                isAvailable = false,
                availabilityReason = "O hardware/sistema de áudio deste dispositivo não expôs o módulo Equalizer para esta sessão.",
                bands = emptyList(),
            )
            return
        }

        equalizer = eq
        bassBoost = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
        virtualizer = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()
        loudnessEnhancer = runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()

        val numBands = eq.numberOfBands.toInt()
        val range = eq.bandLevelRange
        val minMb = range.getOrNull(0)?.toInt() ?: -1500
        val maxMb = range.getOrNull(1)?.toInt() ?: 1500

        val bands = (0 until numBands).map { idx ->
            val freqHz = eq.getCenterFreq(idx.toShort()) / 1000
            val level = eq.getBandLevel(idx.toShort()).toInt()
            EqualizerBand(
                index = idx,
                centerFreqHz = freqHz,
                levelMillibels = level,
                minMillibels = minMb,
                maxMillibels = maxMb,
            )
        }

        _state.value = _state.value.copy(
            isAvailable = true,
            availabilityReason = "",
            bands = bands,
        )

        scope.launch {
            applySavedProfileForDevice(_state.value.outputDevice)
        }
    }

    fun startVisualizerIfPermitted() {
        isVisualizerUserEnabled = true
        if (!isForegroundActive || attachedSessionId <= 0) return
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPerm) {
            _hasRealFftSignal.value = false
            return
        }
        if (visualizer != null) {
            runCatching { visualizer?.enabled = true }
            return
        }
        runCatching {
            val viz = Visualizer(attachedSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(256)
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            if (fft == null || fft.size < 4 || !isForegroundActive || !isVisualizerUserEnabled) return
                            val step = ((fft.size / 2) / 16).coerceAtLeast(1)
                            var hasSignal = false
                            for (i in 0 until 16) {
                                val idx = (i * step * 2).coerceIn(0, fft.size - 2)
                                val re = fft[idx].toFloat()
                                val im = fft[idx + 1].toFloat()
                                val mag = (kotlin.math.hypot(re, im) / 96f).coerceIn(0f, 1f)
                                if (mag > 0.04f) hasSignal = true
                                reusableFftMagnitudes[i] = mag.coerceAtLeast(0.04f)
                            }
                            if (!hasSignal && !_hasRealFftSignal.value) return
                            _hasRealFftSignal.value = hasSignal
                            _spectrumBars.value = reusableFftMagnitudes.toList()
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true,
                )
                enabled = true
            }
            visualizer = viz
        }.onFailure {
            _hasRealFftSignal.value = false
        }
    }

    fun setVisualizerAppForeground(inForeground: Boolean) {
        if (isForegroundActive == inForeground) return
        isForegroundActive = inForeground
        if (!inForeground) {
            runCatching { visualizer?.enabled = false }
            _hasRealFftSignal.value = false
            _spectrumBars.value = List(16) { 0f }
        } else if (isVisualizerUserEnabled) {
            startVisualizerIfPermitted()
        }
    }

    fun stopVisualizer() {
        isVisualizerUserEnabled = false
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
        visualizer = null
        _hasRealFftSignal.value = false
        _spectrumBars.value = List(16) { 0f }
    }

    fun detectCurrentOutputDevice(): AudioOutputDeviceType {
        val devices = runCatching { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }.getOrNull()
            ?: return AudioOutputDeviceType.SPEAKER

        return when {
            devices.any {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
            } -> AudioOutputDeviceType.BLUETOOTH
            devices.any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            } -> AudioOutputDeviceType.WIRED_HEADSET
            devices.any {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            } -> AudioOutputDeviceType.SPEAKER
            else -> AudioOutputDeviceType.OTHER
        }
    }

    fun refreshOutputDeviceProfile() {
        val device = detectCurrentOutputDevice()
        _state.value = _state.value.copy(outputDevice = device)
        scope.launch {
            loadSavedPresetsForDevice(device)
            applySavedProfileForDevice(device)
        }
    }

    fun selectOutputDeviceProfile(device: AudioOutputDeviceType) {
        _state.value = _state.value.copy(outputDevice = device)
        scope.launch {
            loadSavedPresetsForDevice(device)
            applySavedProfileForDevice(device)
        }
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled && _state.value.bassBoostStrength > 0
        virtualizer?.enabled = enabled && _state.value.virtualizerStrength > 0
        loudnessEnhancer?.enabled = enabled && _state.value.loudnessGainMb > 0
        _state.value = _state.value.copy(isEnabled = enabled)
        persistActiveDeviceProfile()
    }

    fun setBandLevel(bandIndex: Int, levelMillibels: Int) {
        val eq = equalizer ?: return
        val bands = _state.value.bands.toMutableList()
        val band = bands.getOrNull(bandIndex) ?: return
        val clamped = levelMillibels.coerceIn(band.minMillibels, band.maxMillibels)
        runCatching { eq.setBandLevel(bandIndex.toShort(), clamped.toShort()) }
        bands[bandIndex] = band.copy(levelMillibels = clamped)
        _state.value = _state.value.copy(bands = bands, presetName = "Personalizado")
        persistActiveDeviceProfile()
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    bb.setStrength(clamped.toShort())
                    bb.enabled = _state.value.isEnabled && clamped > 0
                }
            }
        }
        _state.value = _state.value.copy(bassBoostStrength = clamped)
        persistActiveDeviceProfile()
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            virtualizer?.let { vz ->
                if (vz.strengthSupported) {
                    vz.setStrength(clamped.toShort())
                    vz.enabled = _state.value.isEnabled && clamped > 0
                }
            }
        }
        _state.value = _state.value.copy(virtualizerStrength = clamped)
        persistActiveDeviceProfile()
    }

    fun setLoudnessGain(gainMb: Int) {
        val clamped = gainMb.coerceIn(0, 1500)
        runCatching {
            loudnessEnhancer?.let { le ->
                le.setTargetGain(clamped)
                le.enabled = _state.value.isEnabled && clamped > 0
            }
        }
        _state.value = _state.value.copy(loudnessGainMb = clamped)
        persistActiveDeviceProfile()
    }

    fun setTrebleBoost(db: Int) {
        val clamped = db.coerceIn(-6, 6)
        val bands = _state.value.bands
        if (bands.isNotEmpty()) {
            val highIdx = bands.lastIndex
            val targetMb = (clamped * 100).coerceIn(bands[highIdx].minMillibels, bands[highIdx].maxMillibels)
            setBandLevel(highIdx, targetMb)
        }
        _state.value = _state.value.copy(trebleBoostDb = clamped)
        persistActiveDeviceProfile()
    }

    fun applyBuiltInPreset(name: String) {
        val template = builtInPresets[name] ?: return
        val currentBands = _state.value.bands
        val updated = currentBands.mapIndexed { idx, band ->
            val raw = template.getOrElse(idx) { template.last() }
            val clamped = raw.coerceIn(band.minMillibels, band.maxMillibels)
            runCatching { equalizer?.setBandLevel(idx.toShort(), clamped.toShort()) }
            band.copy(levelMillibels = clamped)
        }
        _state.value = _state.value.copy(bands = updated, presetName = name)
        persistActiveDeviceProfile()
    }

    fun saveCustomPreset(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val current = _state.value
        val entity = AudioPresetEntity(
            id = "custom_${current.outputDevice.name}_${System.currentTimeMillis()}",
            name = clean,
            outputDeviceType = current.outputDevice.name,
            isEnabled = current.isEnabled,
            bandLevelsJson = Json.encodeToString(current.bands.map { it.levelMillibels }),
            bassBoostStrength = current.bassBoostStrength,
            virtualizerStrength = current.virtualizerStrength,
            loudnessGainMb = current.loudnessGainMb,
            trebleBoostDb = current.trebleBoostDb,
            isCustom = true,
        )
        scope.launch {
            database.audioPresetDao().upsertPreset(entity)
            _state.value = _state.value.copy(presetName = clean)
            loadSavedPresetsForDevice(current.outputDevice)
        }
    }

    fun applySavedPreset(preset: AudioPresetEntity) {
        val levels = runCatching { Json.decodeFromString<List<Int>>(preset.bandLevelsJson) }.getOrDefault(emptyList())
        val updatedBands = _state.value.bands.mapIndexed { idx, band ->
            val target = levels.getOrNull(idx)?.coerceIn(band.minMillibels, band.maxMillibels) ?: band.levelMillibels
            runCatching { equalizer?.setBandLevel(idx.toShort(), target.toShort()) }
            band.copy(levelMillibels = target)
        }
        _state.value = _state.value.copy(
            isEnabled = preset.isEnabled,
            presetName = preset.name,
            bands = updatedBands,
            bassBoostStrength = preset.bassBoostStrength,
            virtualizerStrength = preset.virtualizerStrength,
            loudnessGainMb = preset.loudnessGainMb,
            trebleBoostDb = preset.trebleBoostDb,
        )
        setEnabled(preset.isEnabled)
        setBassBoost(preset.bassBoostStrength)
        setVirtualizer(preset.virtualizerStrength)
        setLoudnessGain(preset.loudnessGainMb)
    }

    fun resetDefaults() {
        applyBuiltInPreset("Flat")
        setBassBoost(0)
        setVirtualizer(0)
        setLoudnessGain(0)
        _state.value = _state.value.copy(trebleBoostDb = 0, presetName = "Flat")
        persistActiveDeviceProfile()
    }

    private suspend fun loadSavedPresetsForDevice(device: AudioOutputDeviceType) {
        val all = database.audioPresetDao().getAllPresetsSync()
            .filter { it.outputDeviceType == device.name && it.isCustom }
        _savedPresets.value = all
    }

    private suspend fun applySavedProfileForDevice(device: AudioOutputDeviceType) {
        val activeId = "active_${device.name}"
        val saved = database.audioPresetDao().getPresetById(activeId) ?: return
        applySavedPreset(saved)
    }

    private fun persistActiveDeviceProfile() {
        val current = _state.value
        val entity = AudioPresetEntity(
            id = "active_${current.outputDevice.name}",
            name = current.presetName,
            outputDeviceType = current.outputDevice.name,
            isEnabled = current.isEnabled,
            bandLevelsJson = Json.encodeToString(current.bands.map { it.levelMillibels }),
            bassBoostStrength = current.bassBoostStrength,
            virtualizerStrength = current.virtualizerStrength,
            loudnessGainMb = current.loudnessGainMb,
            trebleBoostDb = current.trebleBoostDb,
            isCustom = false,
        )
        scope.launch {
            database.audioPresetDao().upsertPreset(entity)
        }
    }

    fun releaseEffects() {
        stopVisualizer()
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        attachedSessionId = 0
    }
}
