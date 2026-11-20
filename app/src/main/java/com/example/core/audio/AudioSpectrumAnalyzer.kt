package com.example.core.audio

import androidx.compose.runtime.Immutable
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class VisualizerCaptureStatus {
    ACTIVE_SYNCED,
    PAUSED,
    STOPPED,
    NO_AUDIO_SESSION,
    PERMISSION_REQUIRED,
    UNAVAILABLE,
    DISABLED,
}

@Immutable
data class AudioVisualizerFrame(
    val bars: List<Float> = List(AudioSpectrumAnalyzer.BAR_COUNT) { 0f },
    val peakHolds: List<Float> = List(AudioSpectrumAnalyzer.BAR_COUNT) { 0f },
    val bassLevel: Float = 0f,
    val midLevel: Float = 0f,
    val trebleLevel: Float = 0f,
    val overallEnergy: Float = 0f,
    val beatPulse: Float = 0f,
    val waveformRms: Float = 0f,
    val hasRealSignal: Boolean = false,
    val status: VisualizerCaptureStatus = VisualizerCaptureStatus.STOPPED,
    val sampleRateHz: Int = 44_100,
) {
    companion object {
        val Empty = AudioVisualizerFrame()
    }
}

/**
 * Pure-Kotlin real-time DSP analyzer converting Android Visualizer FFT & waveform bytes
 * into calibrated logarithmic spectrum bands:
 * - Bars 0..5: GRAVES (20 Hz - 250 Hz)
 * - Bars 6..12: MÉDIOS (250 Hz - 4,000 Hz)
 * - Bars 13..17: AGUDOS (4,000 Hz - 20,000 Hz, bounded by Nyquist Fs/2)
 *
 * Zero fake sine-wave generators: silence strictly yields 0f across all bands.
 */
class AudioSpectrumAnalyzer {

    companion object {
        const val BAR_COUNT = 18
        const val BASS_END_INDEX = 5       // Bars 0..5  -> 20..250 Hz
        const val MID_END_INDEX = 12       // Bars 6..12 -> 250..4000 Hz
        // Bars 13..17 -> 4000..20000 Hz

        private val LOG_BAND_EDGES_HZ = floatArrayOf(
            20f, 34f, 56f, 90f, 140f, 190f, 250f,             // 6 Bass bands
            380f, 580f, 880f, 1350f, 2000f, 2900f, 4000f,     // 7 Mid bands
            5600f, 7800f, 10800f, 14800f, 20000f              // 5 Treble bands
        )

        private const val NOISE_FLOOR_MAG = 1.15f
        private const val LOG_REF_MAG = 128f
        private const val ATTACK_ALPHA = 0.72f
        private const val DECAY_ALPHA = 0.28f
        private const val PEAK_DECAY_STEP = 0.028f

        fun normalizeSampleRateHz(rawSamplingRate: Int): Float {
            if (rawSamplingRate <= 0) return 44_100f
            // Android Visualizer reports samplingRate in milliHertz (e.g., 44100000)
            return if (rawSamplingRate >= 100_000) {
                (rawSamplingRate / 1000f).coerceIn(8_000f, 192_000f)
            } else {
                rawSamplingRate.toFloat().coerceIn(8_000f, 192_000f)
            }
        }
    }

    private val smoothedBars = FloatArray(BAR_COUNT)
    private val peakHolds = FloatArray(BAR_COUNT)
    private val instantBands = FloatArray(BAR_COUNT)
    private var rollingBassBaseline = 0f
    private var currentBeatPulse = 0f
    private var latestWaveformRms = 0f
    private var lastFrameTimestampMs = 0L

    @Synchronized
    fun reset(status: VisualizerCaptureStatus = VisualizerCaptureStatus.PAUSED): AudioVisualizerFrame {
        smoothedBars.fill(0f)
        peakHolds.fill(0f)
        instantBands.fill(0f)
        rollingBassBaseline = 0f
        currentBeatPulse = 0f
        latestWaveformRms = 0f
        lastFrameTimestampMs = 0L
        return AudioVisualizerFrame(
            bars = List(BAR_COUNT) { 0f },
            peakHolds = List(BAR_COUNT) { 0f },
            bassLevel = 0f,
            midLevel = 0f,
            trebleLevel = 0f,
            overallEnergy = 0f,
            beatPulse = 0f,
            waveformRms = 0f,
            hasRealSignal = false,
            status = status,
        )
    }

    @Synchronized
    fun processWaveformCapture(waveform: ByteArray?): Float {
        if (waveform == null || waveform.isEmpty()) {
            latestWaveformRms = 0f
            return 0f
        }
        var sumSq = 0.0
        for (i in waveform.indices) {
            val centered = ((waveform[i].toInt() and 0xFF) - 128) / 128.0
            sumSq += centered * centered
        }
        val rms = sqrt(sumSq / waveform.size.toDouble()).toFloat().coerceIn(0f, 1f)
        val silenced = if (rms < 0.008f) 0f else rms
        latestWaveformRms = latestWaveformRms + (silenced - latestWaveformRms) * 0.55f
        return latestWaveformRms
    }

    @Synchronized
    fun processFftCapture(
        fft: ByteArray?,
        samplingRateRaw: Int,
        timestampMs: Long = System.currentTimeMillis(),
        status: VisualizerCaptureStatus = VisualizerCaptureStatus.ACTIVE_SYNCED,
    ): AudioVisualizerFrame {
        if (fft == null || fft.size < 4) {
            return AudioVisualizerFrame.Empty.copy(status = status)
        }

        val captureSize = fft.size
        val numBins = captureSize / 2
        val sampleRateHz = normalizeSampleRateHz(samplingRateRaw)
        val nyquistHz = sampleRateHz * 0.5f
        val binResolutionHz = sampleRateHz / captureSize.toFloat()

        var anyRealEnergy = false

        // Note: fft[0] is DC (0 Hz) and fft[1] is Nyquist/2 real;
        // complex bin pairs for k = 1 .. numBins-1 live at fft[2*k], fft[2*k + 1].
        for (barIdx in 0 until BAR_COUNT) {
            val fLow = LOG_BAND_EDGES_HZ[barIdx].coerceAtMost(nyquistHz * 0.98f)
            val fHigh = LOG_BAND_EDGES_HZ[barIdx + 1].coerceAtMost(nyquistHz)
            if (fHigh <= fLow) {
                instantBands[barIdx] = 0f
                continue
            }

            val exactLow = fLow / binResolutionHz
            val exactHigh = fHigh / binResolutionHz
            val startBin = floor(exactLow.toDouble()).toInt().coerceIn(1, numBins - 1)
            val endBin = ceil(exactHigh.toDouble()).toInt().coerceIn(startBin, numBins - 1)

            var peakMag = 0f
            var sumMag = 0f
            var count = 0
            for (k in startBin..endBin) {
                val re = fft[2 * k].toFloat()
                val im = fft[2 * k + 1].toFloat()
                val mag = hypot(re, im)
                if (mag > peakMag) peakMag = mag
                sumMag += mag
                count++
            }

            // If bin spacing is wider than this low band (e.g., 43 Hz bin at 20-34 Hz), interpolate center bin
            val centerFreq = sqrt((fLow * fHigh).toDouble()).toFloat()
            val centerBinFloat = (centerFreq / binResolutionHz).coerceIn(1f, (numBins - 1).toFloat())
            val b0 = floor(centerBinFloat.toDouble()).toInt().coerceIn(1, numBins - 1)
            val b1 = (b0 + 1).coerceAtMost(numBins - 1)
            val frac = centerBinFloat - b0
            val m0 = hypot(fft[2 * b0].toFloat(), fft[2 * b0 + 1].toFloat())
            val m1 = hypot(fft[2 * b1].toFloat(), fft[2 * b1 + 1].toFloat())
            val interpMag = m0 + (m1 - m0) * frac

            val effectiveMag = if (count <= 1) {
                maxOf(peakMag, interpMag)
            } else {
                // Blend peak & mean so transient hits snap while wide upper bands stay dense
                (peakMag * 0.65f) + ((sumMag / count) * 0.35f)
            }

            if (effectiveMag <= NOISE_FLOOR_MAG) {
                instantBands[barIdx] = 0f
            } else {
                anyRealEnergy = true
                val bandTiltGain = when {
                    barIdx <= BASS_END_INDEX -> 1.0f
                    barIdx <= MID_END_INDEX -> 1.06f
                    else -> 1.15f
                }
                val logScaled = (ln(1f + (effectiveMag - NOISE_FLOOR_MAG) * bandTiltGain) / ln(1f + LOG_REF_MAG))
                    .coerceIn(0f, 1f)
                val shaped = logScaled.toDouble().pow(1.32).toFloat().coerceIn(0f, 1f)
                instantBands[barIdx] = shaped
            }
        }

        // Compute adaptive attack/decay per bar
        val dtMs = if (lastFrameTimestampMs > 0L) (timestampMs - lastFrameTimestampMs).coerceIn(8L, 250L) else 45L
        lastFrameTimestampMs = timestampMs
        val peakDrop = PEAK_DECAY_STEP * (dtMs / 45f)

        for (i in 0 until BAR_COUNT) {
            val target = instantBands[i]
            val prev = smoothedBars[i]
            val alpha = if (target > prev) ATTACK_ALPHA else DECAY_ALPHA
            val next = prev + (target - prev) * alpha
            smoothedBars[i] = if (next < 0.005f && target == 0f) 0f else next.coerceIn(0f, 1f)

            if (smoothedBars[i] >= peakHolds[i]) {
                peakHolds[i] = smoothedBars[i]
            } else {
                peakHolds[i] = (peakHolds[i] - peakDrop).coerceAtLeast(smoothedBars[i])
            }
        }

        var bassSum = 0f
        for (i in 0..BASS_END_INDEX) bassSum += smoothedBars[i]
        val bassLevel = (bassSum / (BASS_END_INDEX + 1)).coerceIn(0f, 1f)

        var midSum = 0f
        for (i in (BASS_END_INDEX + 1)..MID_END_INDEX) midSum += smoothedBars[i]
        val midLevel = (midSum / (MID_END_INDEX - BASS_END_INDEX)).coerceIn(0f, 1f)

        var trebleSum = 0f
        for (i in (MID_END_INDEX + 1) until BAR_COUNT) trebleSum += smoothedBars[i]
        val trebleLevel = (trebleSum / (BAR_COUNT - 1 - MID_END_INDEX)).coerceIn(0f, 1f)

        val overall = ((bassLevel * 0.42f) + (midLevel * 0.36f) + (trebleLevel * 0.22f)).coerceIn(0f, 1f)

        // Beat onset detection driven by low-frequency transient flux + waveform support
        val kickInstant = ((instantBands[1] + instantBands[2] + instantBands[3]) / 3f).coerceIn(0f, 1f)
        val delta = kickInstant - rollingBassBaseline
        rollingBassBaseline = rollingBassBaseline * 0.85f + kickInstant * 0.15f
        val beatDecay = 0.72f.toDouble().pow(dtMs / 45.0).toFloat()
        currentBeatPulse = if (kickInstant > 0.16f && delta > 0.07f) {
            maxOf(currentBeatPulse * beatDecay, (delta * 3.4f).coerceIn(0.32f, 1f))
        } else {
            val decayed = currentBeatPulse * beatDecay
            if (decayed < 0.01f) 0f else decayed
        }

        val hasActiveSignal = anyRealEnergy || latestWaveformRms > 0.01f || smoothedBars.any { it > 0.01f }

        return AudioVisualizerFrame(
            bars = smoothedBars.toList(),
            peakHolds = peakHolds.toList(),
            bassLevel = bassLevel,
            midLevel = midLevel,
            trebleLevel = trebleLevel,
            overallEnergy = overall,
            beatPulse = currentBeatPulse,
            waveformRms = latestWaveformRms,
            hasRealSignal = hasActiveSignal,
            status = status,
            sampleRateHz = sampleRateHz.roundToInt(),
        )
    }
}
