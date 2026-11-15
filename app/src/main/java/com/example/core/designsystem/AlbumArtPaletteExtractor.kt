package com.example.core.designsystem

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.palette.graphics.Palette
import com.example.domain.models.MyatoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates an offline MyatoTheme palette directly from the current track's embedded or MediaStore album art.
 */
object AlbumArtPaletteExtractor {

    suspend fun extractThemeFromTrack(
        context: Context,
        artUri: String?,
        filePath: String?,
        baseTheme: MyatoTheme,
        customName: String = "Capa Atual",
    ): MyatoTheme = withContext(Dispatchers.IO) {
        val bitmap = loadCoverBitmap(context, artUri, filePath)
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

    private fun loadCoverBitmap(context: Context, artUri: String?, filePath: String?): Bitmap? {
        if (!artUri.isNullOrBlank()) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(artUri))?.use { stream ->
                    return BitmapFactory.decodeStream(stream)
                }
            }
        }
        if (!filePath.isNullOrBlank()) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(filePath)
                val bytes = retriever.embeddedPicture
                if (bytes != null) {
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }
        }
        return null
    }

    private fun darkenArgb(color: Int, factor: Float): Long {
        val r = ((color shr 16 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = ((color shr 8 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) * factor).toInt().coerceIn(0, 255)
        val argb = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        return argb.toLong() and 0xFFFFFFFFL
    }
}
