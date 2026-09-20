package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ServerLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerLogDao {
    @Query("SELECT * FROM server_logs WHERE serverId = :serverId ORDER BY id ASC")
    fun getLogsForServer(serverId: String): Flow<List<ServerLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ServerLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<ServerLogEntity>)

    @Query("DELETE FROM server_logs WHERE serverId = :serverId")
    suspend fun clearLogsForServer(serverId: String)
}
