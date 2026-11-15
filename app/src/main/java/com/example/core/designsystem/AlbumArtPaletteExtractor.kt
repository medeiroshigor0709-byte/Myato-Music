package com.example.core.designsystem

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.example.domain.models.ExtractedArtPalette
import com.example.domain.models.MyatoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates an offline MyatoTheme palette or multi-swatch aurora palette directly from the current
 * track's embedded or MediaStore album art, using bounded downsampling and LRU caching for 60fps UI.
 */
object AlbumArtPaletteExtractor {

    private val auroraCache = LruCache<String, ExtractedArtPalette>(24)

    suspend fun extractAuroraPalette(
        context: Context,
        trackId: Long,
        artUri: String?,
        filePath: String?,
        fallbackColors: MyatoExtendedColors,
    ): ExtractedArtPalette {
        val cacheKey = "${trackId}_${artUri.orEmpty()}_${filePath.orEmpty()}"
        auroraCache.get(cacheKey)?.let { cached ->
            return if (cached.hasArtwork) cached else fallbackPalette(fallbackColors)
        }

        val extracted = withContext(Dispatchers.IO) {
            val bitmap = loadSampledCoverBitmap(context, artUri, filePath, targetMaxDim = 128)
                ?: return@withContext null

            val palette = runCatching {
                Palette.from(bitmap)
                    .maximumColorCount(12)
                    .resizeBitmapArea(128 * 128)
                    .generate()
            }.getOrNull()

            bitmap.recycle()
            if (palette == null) return@withContext null

            val primaryInt = palette.vibrantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
            val secondaryInt = palette.lightVibrantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb
            val tertiaryInt = palette.darkVibrantSwatch?.rgb
                ?: palette.lightMutedSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
            val darkInt = palette.darkMutedSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb

            val primary = primaryInt?.let { Color(it) } ?: fallbackColors.primary
            val secondary = secondaryInt?.let { Color(it) } ?: fallbackColors.secondary
            val tertiary = tertiaryInt?.let { Color(it) } ?: primary.copy(alpha = 0.85f)
            val darkBase = darkInt?.let { Color(darkenArgb(it, 0.28f)) } ?: fallbackColors.background

            ExtractedArtPalette(
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                darkBase = darkBase,
                hasArtwork = true,
            )
        }

        val result = extracted ?: fallbackPalette(fallbackColors)
        auroraCache.put(cacheKey, result)
        return result
    }

    private fun fallbackPalette(colors: MyatoExtendedColors): ExtractedArtPalette {
        return ExtractedArtPalette(
            primary = colors.primary,
            secondary = colors.secondary,
            tertiary = colors.primary.copy(alpha = 0.72f),
            darkBase = colors.background,
            hasArtwork = false,
        )
    }

    suspend fun extractThemeFromTrack(
        context: Context,
        artUri: String?,
        filePath: String?,
        baseTheme: MyatoTheme,
        customName: String = "Capa Atual",
    ): MyatoTheme = withContext(Dispatchers.IO) {
        val bitmap = loadSampledCoverBitmap(context, artUri, filePath, targetMaxDim = 160)
            ?: return@withContext baseTheme.copy(
                id = "custom_palette_${System.currentTimeMillis()}",
                name = customName,
                isBuiltIn = false,
            )

        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        val primary = palette.vibrantSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: 0xFFFF8500.toInt()
        val secondary = palette.lightVibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: 0xFFFF9D24.toInt()
        val darkMuted = palette.darkMutedSwatch?.rgb
            ?: palette.darkVibrantSwatch?.rgb
            ?: 0xFF101014.toInt()

        val bgHex = darkenArgb(darkMuted, 0.22f)
        val altBgHex = darkenArgb(darkMuted, 0.36f)
        val cardHex = darkenArgb(darkMuted, 0.54f)

        bitmap.recycle()

        baseTheme.copy(
            id = "palette_${System.currentTimeMillis()}",
            name = customName,
            isBuiltIn = false,
            isLight = false,
            primaryColorHex = primary.toLong() and 0xFFFFFFFFL,
            secondaryColorHex = secondary.toLong() and 0xFFFFFFFFL,
            backgroundColorHex = bgHex,
            altBackgroundColorHex = altBgHex,
            cardColorHex = cardHex,
            textColorHex = 0xFFFFFFFFL,
            secondaryTextColorHex = 0xFFB8BCC6L,
        )
    }

    private fun loadSampledCoverBitmap(
        context: Context,
        artUri: String?,
        filePath: String?,
        targetMaxDim: Int,
    ): Bitmap? {
        if (!artUri.isNullOrBlank()) {
            runCatching {
                val uri = Uri.parse(artUri)
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, targetMaxDim)
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream, null, decodeOpts)
                    if (bmp != null) return bmp
                }
            }
        }
        if (!filePath.isNullOrBlank()) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(filePath)
                val bytes = retriever.embeddedPicture
                if (bytes != null) {
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, targetMaxDim)
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                }
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }
        }
        return null
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        if (width > maxDim || height > maxDim) {
            var halfW = width / 2
            var halfH = height / 2
            while ((halfW / inSampleSize) >= maxDim && (halfH / inSampleSize) >= maxDim) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun darkenArgb(color: Int, factor: Float): Long {
        val r = ((color shr 16 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = ((color shr 8 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) * factor).toInt().coerceIn(0, 255)
        val argb = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        return argb.toLong() and 0xFFFFFFFFL
    }
}

