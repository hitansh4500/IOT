package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.SecurityHistoryDao
import com.example.data.local.entity.IntrusionAlertEntity
import com.example.data.local.entity.ScanResultEntity

@Database(
    entities = [ScanResultEntity::class, IntrusionAlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HomeSecurityDatabase : RoomDatabase() {

    abstract fun securityHistoryDao(): SecurityHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: HomeSecurityDatabase? = null

        fun getInstance(context: Context): HomeSecurityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HomeSecurityDatabase::class.java,
                    "iot_shield_security.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
