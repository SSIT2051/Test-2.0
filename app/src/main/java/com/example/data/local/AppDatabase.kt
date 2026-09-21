package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.MarketPluginDao
import com.example.data.local.dao.ServerDao
import com.example.data.local.dao.ServerFileDao
import com.example.data.local.dao.ServerLogDao
import com.example.data.local.entity.CachedMarketPluginEntity
import com.example.data.local.entity.ServerEntity
import com.example.data.local.entity.ServerFileEntity
import com.example.data.local.entity.ServerLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ServerEntity::class,
        ServerFileEntity::class,
        CachedMarketPluginEntity::class,
        ServerLogEntity::class,
        com.example.data.local.entity.TunnelEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun serverFileDao(): ServerFileDao
    abstract fun marketPluginDao(): MarketPluginDao
    abstract fun serverLogDao(): ServerLogDao
    abstract fun tunnelDao(): com.example.data.local.dao.TunnelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pumpkinmc_host.db"
                ).fallbackToDestructiveMigration()
                 .addCallback(DatabaseCallback())
                 .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            // No preset servers created automatically - clean empty server list on fresh start
            // Cache available market plugins
            database.marketPluginDao().insertPlugins(com.example.data.remote.PumpkinMarketCatalog.fullCatalog)
        }
    }
}
