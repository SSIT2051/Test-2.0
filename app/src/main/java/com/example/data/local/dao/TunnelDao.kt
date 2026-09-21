package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TunnelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TunnelDao {
    @Query("SELECT * FROM server_tunnels WHERE serverId = :serverId LIMIT 1")
    suspend fun getTunnelForServer(serverId: String): TunnelEntity?

    @Query("SELECT * FROM server_tunnels WHERE serverId = :serverId LIMIT 1")
    fun observeTunnelForServer(serverId: String): Flow<TunnelEntity?>

    @Query("SELECT * FROM server_tunnels")
    fun observeAllTunnels(): Flow<List<TunnelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTunnel(tunnel: TunnelEntity)

    @Update
    suspend fun updateTunnel(tunnel: TunnelEntity)

    @Query("UPDATE server_tunnels SET status = :status, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE serverId = :serverId")
    suspend fun updateTunnelStatus(serverId: String, status: String, errorMessage: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM server_tunnels WHERE serverId = :serverId")
    suspend fun deleteTunnelForServer(serverId: String)

    @Query("DELETE FROM server_tunnels WHERE providerTunnelId = :providerTunnelId")
    suspend fun deleteTunnelByProviderId(providerTunnelId: String)
}
