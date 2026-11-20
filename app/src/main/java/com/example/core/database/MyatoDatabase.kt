package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.designsystem.BuiltInThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MusicTrackEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        PlaybackQueueEntity::class,
        QueueTrackEntity::class,
        FavoriteEntity::class,
        BookmarkEntity::class,
        MusicNoteEntity::class,
        PlaybackHistoryEntity::class,
        PlaybackStatisticsEntity::class,
        ThemeEntity::class,
        AudioPresetEntity::class,
        AppSettingsEntity::class,
        BackupMetadataEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class MyatoDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun queueDao(): QueueDao
    abstract fun engagementDao(): EngagementDao
    abstract fun themeDao(): ThemeDao
    abstract fun audioPresetDao(): AudioPresetDao
    abstract fun settingsBackupDao(): SettingsBackupDao

    companion object {
        private const val DATABASE_NAME = "myato_music.db"

        @Volatile
        private var INSTANCE: MyatoDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN coverUri TEXT")
                db.execSQL("ALTER TABLE playlists ADD COLUMN lastUsedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playlist_tracks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        playlistId INTEGER NOT NULL,
                        trackId INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO playlist_tracks_new (playlistId, trackId, position, addedAt)
                    SELECT playlistId, trackId, position, addedAt FROM playlist_tracks
                    ORDER BY playlistId ASC, position ASC
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE playlist_tracks")
                db.execSQL("ALTER TABLE playlist_tracks_new RENAME TO playlist_tracks")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_playlistId ON playlist_tracks(playlistId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_trackId ON playlist_tracks(trackId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN artistFilterName TEXT")
                db.execSQL("ALTER TABLE playlists ADD COLUMN autoUpdateByArtist INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN includeCollaborations INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE themes ADD COLUMN extendedConfigJson TEXT NOT NULL DEFAULT '{}'")
            }
        }

        fun getInstance(context: Context): MyatoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { db ->
                    INSTANCE = db
                    seedInitialDataAsync(db)
                }
            }
        }

        fun createInMemoryForTest(context: Context): MyatoDatabase {
            return Room.inMemoryDatabaseBuilder(context, MyatoDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }

        private fun buildDatabase(appContext: Context): MyatoDatabase {
            return Room.databaseBuilder(
                appContext,
                MyatoDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
        }

        private fun seedInitialDataAsync(db: MyatoDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                val builtInEntities = BuiltInThemes.all.map { it.toEntity() }
                db.themeDao().upsertThemes(builtInEntities)
                db.themeDao().deleteObsoleteBuiltInThemes(builtInEntities.map { it.id })
                if (db.queueDao().getQueuesSync().isEmpty()) {
                    db.queueDao().insertQueue(
                        PlaybackQueueEntity(
                            name = "Fila Principal",
                            originType = "ALL_SONGS",
                            isActive = true,
                        )
                    )
                }
            }
        }
    }
}
