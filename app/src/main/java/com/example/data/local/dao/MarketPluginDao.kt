package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CachedMarketPluginEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketPluginDao {
    @Query("SELECT * FROM cached_market_plugins ORDER BY downloads DESC")
    fun getAllPlugins(): Flow<List<CachedMarketPluginEntity>>

    @Query("SELECT * FROM cached_market_plugins WHERE category = :category ORDER BY downloads DESC")
    fun getPluginsByCategory(category: String): Flow<List<CachedMarketPluginEntity>>

    @Query("SELECT * FROM cached_market_plugins WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchPlugins(query: String): Flow<List<CachedMarketPluginEntity>>

    @Query("SELECT * FROM cached_market_plugins WHERE id = :id LIMIT 1")
    suspend fun getPluginById(id: String): CachedMarketPluginEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugins(plugins: List<CachedMarketPluginEntity>)

    @Query("SELECT COUNT(*) FROM cached_market_plugins")
    suspend fun getCount(): Int
}
