package com.example.core.common

import android.media.MediaCodecList
import com.example.domain.models.AudioFormatCapability

/**
 * Inspects real Android MediaCodecList decoder capabilities for the 11 requested formats.
 * Never marks an audio format as supported unless a real decoder or native PCM path exists on device.
 */
object AudioFormatSupport {

    private data class FormatSpec(
        val extension: String,
        val mimeTypes: List<String>,
        val supportsTagEditing: Boolean,
        val rawPcmFallback: Boolean = false,
        val notes: String,
    )

    private val specs = listOf(
        FormatSpec(
            extension = "MP3",
            mimeTypes = listOf("audio/mpeg"),
            supportsTagEditing = true,
            notes = "Suporte nativo ID3v2 para leitura e edição de tags.",
        ),
        FormatSpec(
            extension = "M4A",
            mimeTypes = listOf("audio/mp4a-latm", "audio/mp4"),
            supportsTagEditing = false,
            notes = "Container MP4/AAC; edição de metadados via MediaStore quando permitido.",
        ),
        FormatSpec(
            extension = "WMA",
            mimeTypes = listOf("audio/x-ms-wma", "audio/wma"),
            supportsTagEditing = false,
            notes = "Proprietário Microsoft; depende de codec específico do fabricante.",
        ),
        FormatSpec(
            extension = "FLAC",
            mimeTypes = listOf("audio/flac"),
            supportsTagEditing = false,
            notes = "Áudio sem perdas (lossless); decodificação nativa no Android.",
        ),
        FormatSpec(
            extension = "OPUS",
            mimeTypes = listOf("audio/opus"),
            supportsTagEditing = false,
            notes = "Codec aberto de alta eficiência.",
        ),
        FormatSpec(
            extension = "AAC",
            mimeTypes = listOf("audio/mp4a-latm", "audio/aac"),
            supportsTagEditing = false,
            notes = "Advanced Audio Coding.",
        ),
        FormatSpec(
            extension = "ALAC",
            mimeTypes = listOf("audio/alac"),
            supportsTagEditing = false,
            notes = "Apple Lossless; depende de suporte do decodificador no dispositivo.",
        ),
        FormatSpec(
            extension = "APE",
            mimeTypes = listOf("audio/x-ape", "audio/ape"),
            supportsTagEditing = false,
            notes = "Monkey's Audio; requer codec específico no aparelho ou extensão FFmpeg.",
        ),
        FormatSpec(
            extension = "DSF",
            mimeTypes = listOf("audio/dsf", "audio/dsd"),
            supportsTagEditing = false,
            notes = "DSD Stream File; requer suporte dedicado de hardware/codec DSD.",
        ),
        FormatSpec(
            extension = "WAV",
            mimeTypes = listOf("audio/raw", "audio/wav", "audio/x-wav"),
            supportsTagEditing = false,
            rawPcmFallback = true,
            notes = "PCM linear sem compressão (suportado via AudioTrack/ExoPlayer).",
        ),
        FormatSpec(
            extension = "OGG",
            mimeTypes = listOf("audio/vorbis", "audio/ogg"),
            supportsTagEditing = false,
            notes = "Vorbis / Ogg container.",
        ),
    )

    fun detectDeviceCapabilities(): List<AudioFormatCapability> {
        val decoders = runCatching {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
                .filter { !it.isEncoder }
        }.getOrDefault(emptyList())

        return specs.map { spec ->
            val matchedCodec = decoders.firstOrNull { info ->
                info.supportedTypes.any { supported ->
                    spec.mimeTypes.any { target -> target.equals(supported, ignoreCase = true) }
                }
            }
            val supported = matchedCodec != null || spec.rawPcmFallback
            val decoderName = when {
                matchedCodec != null -> matchedCodec.name
                spec.rawPcmFallback -> "Android PCM AudioTrack"
                else -> "Indisponível neste dispositivo"
            }
            AudioFormatCapability(
                extension = spec.extension,
                mimeTypes = spec.mimeTypes,
                isSupportedOnDevice = supported,
                decoderName = decoderName,
                supportsTagEditing = spec.supportsTagEditing,
                notes = spec.notes,
            )
        }
    }

    fun isExtensionSupported(extension: String, capabilities: List<AudioFormatCapability> = detectDeviceCapabilities()): Boolean {
        val normalized = extension.trim().uppercase()
        return capabilities.firstOrNull { it.extension == normalized }?.isSupportedOnDevice ?: false
    }

    fun inferExtension(filePath: String, mimeType: String?): String {
        val ext = filePath.substringAfterLast('.', "").uppercase()
        if (ext.isNotBlank() && ext.length <= 5) return ext
        return when (mimeType?.lowercase()) {
            "audio/mpeg" -> "MP3"
            "audio/flac" -> "FLAC"
            "audio/opus" -> "OPUS"
            "audio/vorbis", "audio/ogg" -> "OGG"
            "audio/mp4", "audio/mp4a-latm" -> "M4A"
            "audio/aac" -> "AAC"
            "audio/x-wav", "audio/wav", "audio/raw" -> "WAV"
            "audio/alac" -> "ALAC"
            "audio/x-ms-wma" -> "WMA"
            "audio/x-ape", "audio/ape" -> "APE"
            "audio/dsf", "audio/dsd" -> "DSF"
            else -> "AUDIO"
        }
    }
}
