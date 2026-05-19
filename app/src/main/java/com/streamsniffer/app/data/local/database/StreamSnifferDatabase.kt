package com.streamsniffer.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.streamsniffer.app.data.local.dao.StreamDao
import com.streamsniffer.app.data.local.entity.StreamEntity

@Database(
    entities = [StreamEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StreamSnifferDatabase : RoomDatabase() {
    abstract fun streamDao(): StreamDao

    companion object {
        const val DATABASE_NAME = "stream_sniffer.db"
    }
}
