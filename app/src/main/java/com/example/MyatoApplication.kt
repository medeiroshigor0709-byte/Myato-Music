package com.example

import android.app.Application
import com.example.core.audio.PlaybackControllerHolder
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

class MyatoApplication : Application() {

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
        ThemeRepository(database, preferencesStore)
    }
    val statsRepository: StatsAndEngagementRepository by lazy {
        StatsAndEngagementRepository(database)
    }

    override fun onCreate() {
        super.onCreate()
        // Pre-warm database; PlaybackController connects when UI or widget invokes it
        database
    }
}
