package com.snipware.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Local persistence layer, replacing IndexedDB (SnipwareDB) from db.js. */
@Database(entities = [SnippetEntity::class], version = 1, exportSchema = false)
abstract class SnipwareDatabase : RoomDatabase() {

    abstract fun snippetDao(): SnippetDao

    companion object {
        @Volatile
        private var instance: SnipwareDatabase? = null

        fun getInstance(context: Context): SnipwareDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SnipwareDatabase::class.java,
                    "snipware.db"
                ).build().also { instance = it }
            }
    }
}
