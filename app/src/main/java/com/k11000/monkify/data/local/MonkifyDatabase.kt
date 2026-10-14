package com.k11000.monkify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.k11000.monkify.data.local.dao.TrackDao
import com.k11000.monkify.data.local.entity.TrackEntity

@Database(entities = [TrackEntity::class], version = 1, exportSchema = false)
abstract class MonkifyDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao

    companion object {
        const val DATABASE_NAME = "monkify_local.db"
    }
}
