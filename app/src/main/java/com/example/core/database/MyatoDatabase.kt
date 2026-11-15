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
    version = 1,
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

        /**
         * Explicit forward migration scaffold prepared for future database schema updates.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Prepared for future schema extensions without destructive fallback.
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
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        private fun seedInitialDataAsync(db: MyatoDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                db.themeDao().insertMissingBuiltInThemes(BuiltInThemes.all.map { it.toEntity() })
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
