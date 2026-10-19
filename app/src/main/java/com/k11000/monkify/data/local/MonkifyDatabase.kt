package com.k11000.monkify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.k11000.monkify.data.local.dao.TrackDao
import com.k11000.monkify.data.local.entity.TrackEntity

@Database(entities = [TrackEntity::class], version = 2, exportSchema = false)
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
    }
}
