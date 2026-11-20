package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.core.database.AppPreferencesStore
import com.example.core.database.MyatoDatabase
import com.example.core.metadata.TagEditorEngine
import com.example.core.storage.BackupManager
import com.example.core.storage.CustomFontManager
import com.example.core.storage.FileManagerRepository
import com.example.core.storage.MediaStoreScanner
import com.example.data.repository.LibraryRepository
import com.example.data.repository.PlaylistRepository
import com.example.data.repository.QueueRepository
import com.example.data.repository.StatsAndEngagementRepository
import com.example.data.repository.ThemeRepository
import java.io.File

class MyatoApplication : Application(), ImageLoaderFactory {

    val database: MyatoDatabase by lazy { MyatoDatabase.getInstance(this) }
    val preferencesStore: AppPreferencesStore by lazy { AppPreferencesStore(this) }
    val mediaStoreScanner: MediaStoreScanner by lazy { MediaStoreScanner(this, database) }
    val fileManagerRepository: FileManagerRepository by lazy { FileManagerRepository(this, database) }
    val tagEditorEngine: TagEditorEngine by lazy { TagEditorEngine(this, database) }
    val backupManager: BackupManager by lazy { BackupManager(this, database, preferencesStore) }
    val customFontManager: CustomFontManager by lazy { CustomFontManager(this) }

    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(database, mediaStoreScanner, preferencesStore)
    }
    val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepository(this, database)
    }
    val queueRepository: QueueRepository by lazy {
        QueueRepository(database)
    }
    val themeRepository: ThemeRepository by lazy {
        ThemeRepository(this, database, preferencesStore)
    }
    val statsRepository: StatsAndEngagementRepository by lazy {
        StatsAndEngagementRepository(database)
    }

    override fun onCreate() {
        super.onCreate()
        // Pre-warm database lazily; PlaybackController connects when UI or widget invokes it
        database
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.18)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "myato_art_cache"))
                    .maxSizeBytes(40L * 1024L * 1024L)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}

