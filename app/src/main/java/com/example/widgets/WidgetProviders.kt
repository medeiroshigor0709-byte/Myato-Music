package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.core.audio.PlaybackControllerHolder

class SmallPlayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val state = PlaybackControllerHolder.getOrNull(context)?.playbackState?.value
        WidgetUpdateHelper.updateSmallWidgets(
            context,
            appWidgetManager,
            appWidgetIds,
            state?.currentTrack?.title ?: "MYATO MUSIC",
            state?.currentTrack?.albumArtUri,
            state?.isPlaying ?: false,
        )
    }
}

class MediumPlayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val state = PlaybackControllerHolder.getOrNull(context)?.playbackState?.value
        WidgetUpdateHelper.updateMediumWidgets(
            context,
            appWidgetManager,
            appWidgetIds,
            state?.currentTrack?.title ?: "MYATO MUSIC",
            state?.currentTrack?.artist ?: "Toque para abrir",
            state?.currentTrack?.albumArtUri,
            state?.isPlaying ?: false,
        )
    }
}

class LargePlayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val state = PlaybackControllerHolder.getOrNull(context)?.playbackState?.value
        val progress = if ((state?.durationMs ?: 0L) > 0L) {
            (((state?.positionMs ?: 0L) * 100L) / (state?.durationMs ?: 1L)).toInt().coerceIn(0, 100)
        } else 0
        WidgetUpdateHelper.updateLargeWidgets(
            context,
            appWidgetManager,
            appWidgetIds,
            state?.currentTrack?.title ?: "MYATO MUSIC",
            state?.currentTrack?.artist ?: "Sua música. Seu estilo.",
            state?.currentTrack?.album ?: "Offline Audio Engine",
            state?.currentTrack?.albumArtUri,
            state?.isPlaying ?: false,
            state?.currentTrack?.isFavorite ?: false,
            progress,
        )
    }
}

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val controller = PlaybackControllerHolder.get(context)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> controller.togglePlayPause()
            ACTION_NEXT -> controller.skipNext()
            ACTION_PREV -> controller.skipPrevious()
            ACTION_TOGGLE_FAV -> controller.toggleCurrentTrackFavorite()
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.myato.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.myato.ACTION_WIDGET_NEXT"
        const val ACTION_PREV = "com.example.myato.ACTION_WIDGET_PREV"
        const val ACTION_TOGGLE_FAV = "com.example.myato.ACTION_WIDGET_TOGGLE_FAV"
    }
}

object WidgetUpdateHelper {

    private val coverCache = android.util.LruCache<String, android.graphics.Bitmap>(2)

    fun updateAllWidgets(
        context: Context,
        title: String,
        artist: String,
        album: String,
        artUri: String?,
        isPlaying: Boolean,
        isFavorite: Boolean,
        progressPercent: Int,
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val smallIds = manager.getAppWidgetIds(ComponentName(context, SmallPlayerWidgetProvider::class.java))
        val mediumIds = manager.getAppWidgetIds(ComponentName(context, MediumPlayerWidgetProvider::class.java))
        val largeIds = manager.getAppWidgetIds(ComponentName(context, LargePlayerWidgetProvider::class.java))
        if (smallIds.isEmpty() && mediumIds.isEmpty() && largeIds.isEmpty()) return

        val sharedBitmap = resolveCoverBitmap(context, artUri)
        if (smallIds.isNotEmpty()) {
            updateSmallWidgets(context, manager, smallIds, title, artUri, isPlaying, sharedBitmap)
        }
        if (mediumIds.isNotEmpty()) {
            updateMediumWidgets(context, manager, mediumIds, title, artist, artUri, isPlaying, sharedBitmap)
        }
        if (largeIds.isNotEmpty()) {
            updateLargeWidgets(context, manager, largeIds, title, artist, album, artUri, isPlaying, isFavorite, progressPercent, sharedBitmap)
        }
    }

    fun updateSmallWidgets(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        title: String,
        artUri: String?,
        isPlaying: Boolean,
        preloadedCover: android.graphics.Bitmap? = null,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_small)
        views.setTextViewText(R.id.widget_title, title)
        views.setImageViewResource(
            R.id.widget_btn_play_pause,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
        )
        bindCover(context, views, artUri, preloadedCover)
        views.setOnClickPendingIntent(R.id.widget_root_small, openAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, actionIntent(context, WidgetActionReceiver.ACTION_PLAY_PAUSE, 1))
        manager.updateAppWidget(ids, views)
    }

    fun updateMediumWidgets(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        title: String,
        artist: String,
        artUri: String?,
        isPlaying: Boolean,
        preloadedCover: android.graphics.Bitmap? = null,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_medium)
        views.setTextViewText(R.id.widget_title, title)
        views.setTextViewText(R.id.widget_artist, artist)
        views.setImageViewResource(
            R.id.widget_btn_play_pause,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
        )
        bindCover(context, views, artUri, preloadedCover)
        views.setOnClickPendingIntent(R.id.widget_root_medium, openAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_btn_prev, actionIntent(context, WidgetActionReceiver.ACTION_PREV, 2))
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, actionIntent(context, WidgetActionReceiver.ACTION_PLAY_PAUSE, 3))
        views.setOnClickPendingIntent(R.id.widget_btn_next, actionIntent(context, WidgetActionReceiver.ACTION_NEXT, 4))
        manager.updateAppWidget(ids, views)
    }

    fun updateLargeWidgets(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        title: String,
        artist: String,
        album: String,
        artUri: String?,
        isPlaying: Boolean,
        isFavorite: Boolean,
        progressPercent: Int,
        preloadedCover: android.graphics.Bitmap? = null,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_large)
        views.setTextViewText(R.id.widget_title, title)
        views.setTextViewText(R.id.widget_artist, artist)
        views.setTextViewText(R.id.widget_album, album)
        views.setProgressBar(R.id.widget_progress, 100, progressPercent, false)
        views.setImageViewResource(
            R.id.widget_btn_play_pause,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
        )
        views.setImageViewResource(
            R.id.widget_btn_favorite,
            if (isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off,
        )
        bindCover(context, views, artUri, preloadedCover)
        views.setOnClickPendingIntent(R.id.widget_root_large, openAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_btn_prev, actionIntent(context, WidgetActionReceiver.ACTION_PREV, 5))
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, actionIntent(context, WidgetActionReceiver.ACTION_PLAY_PAUSE, 6))
        views.setOnClickPendingIntent(R.id.widget_btn_next, actionIntent(context, WidgetActionReceiver.ACTION_NEXT, 7))
        views.setOnClickPendingIntent(R.id.widget_btn_favorite, actionIntent(context, WidgetActionReceiver.ACTION_TOGGLE_FAV, 8))
        manager.updateAppWidget(ids, views)
    }

    fun pinHomeShortcut(
        context: Context,
        shortcutId: String,
        label: String,
        shortcutType: String,
        shortcutKey: String,
    ): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("extra_shortcut_type", shortcutType)
            putExtra("extra_shortcut_key", shortcutKey)
        }
        val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(label.take(24))
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(launchIntent)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    private fun resolveCoverBitmap(context: Context, artUri: String?): android.graphics.Bitmap? {
        if (artUri.isNullOrBlank()) return null
        coverCache.get(artUri)?.takeIf { !it.isRecycled }?.let { return it }
        val decoded = runCatching {
            val uri = Uri.parse(artUri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            var sampleSize = 1
            val targetMax = 192
            if (bounds.outWidth > targetMax || bounds.outHeight > targetMax) {
                var halfW = bounds.outWidth / 2
                var halfH = bounds.outHeight / 2
                while ((halfW / sampleSize) >= targetMax && (halfH / sampleSize) >= targetMax) {
                    sampleSize *= 2
                }
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize.coerceAtLeast(1)
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
        }.getOrNull()
        if (decoded != null) {
            coverCache.put(artUri, decoded)
        }
        return decoded
    }

    private fun bindCover(
        context: Context,
        views: RemoteViews,
        artUri: String?,
        preloadedCover: android.graphics.Bitmap? = null,
    ) {
        if (artUri.isNullOrBlank()) {
            views.setImageViewResource(R.id.widget_cover, R.drawable.img_default_album_art)
            return
        }
        val bmp = preloadedCover ?: resolveCoverBitmap(context, artUri)
        if (bmp != null) {
            views.setImageViewBitmap(R.id.widget_cover, bmp)
        } else {
            views.setImageViewResource(R.id.widget_cover, R.drawable.img_default_album_art)
        }
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("extra_open_player", true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(context, 100, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun actionIntent(context: Context, action: String, reqCode: Int): PendingIntent {
        val intent = Intent(context, WidgetActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(context, reqCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
