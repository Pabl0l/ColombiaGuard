package com.colombiaguard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CallLogEntity::class, NumberListEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ColombiaGuardDatabase : RoomDatabase() {

    abstract fun callLogDao(): CallLogDao
    abstract fun numberListDao(): NumberListDao

    companion object {
        @Volatile
        private var INSTANCE: ColombiaGuardDatabase? = null

        fun getDatabase(context: Context): ColombiaGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ColombiaGuardDatabase::class.java,
                    "colombia_guard.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
