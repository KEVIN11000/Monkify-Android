package com.k11000.monkify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.k11000.monkify.data.local.dao.TrackDao
import com.k11000.monkify.data.local.entity.TrackEntity

@Database(entities = [TrackEntity::class], version = 3, exportSchema = false)
abstract class MonkifyDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao

    companion object {
        const val DATABASE_NAME = "monkify_local.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorite_tracks ADD COLUMN playCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE favorite_tracks ADD COLUMN lastPlayedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_tracks_isFavorite` ON `favorite_tracks` (`isFavorite`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_tracks_isDownloaded` ON `favorite_tracks` (`isDownloaded`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_tracks_addedAt` ON `favorite_tracks` (`addedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_tracks_lastPlayedAt` ON `favorite_tracks` (`lastPlayedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_tracks_playCount` ON `favorite_tracks` (`playCount`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_tracks_album` ON `favorite_tracks` (`album`)")
            }
        }
    }
}
