package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ServerFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerFileDao {
    @Query("SELECT * FROM server_files WHERE serverId = :serverId ORDER BY isDirectory DESC, name ASC")
    fun getFilesForServer(serverId: String): Flow<List<ServerFileEntity>>

    @Query("SELECT * FROM server_files WHERE compositeId = :compositeId LIMIT 1")
    suspend fun getFileByCompositeId(compositeId: String): ServerFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ServerFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<ServerFileEntity>)

    @Update
    suspend fun updateFile(file: ServerFileEntity)

    @Query("DELETE FROM server_files WHERE compositeId = :compositeId")
    suspend fun deleteFile(compositeId: String)

    @Query("DELETE FROM server_files WHERE serverId = :serverId")
    suspend fun deleteAllForServer(serverId: String)
}
