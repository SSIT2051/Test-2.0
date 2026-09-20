package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.MarketPlugin

@Entity(tableName = "cached_market_plugins")
data class CachedMarketPluginEntity(
    @PrimaryKey
    val id: String,
    val slug: String,
    val name: String,
    val description: String,
    val category: String,
    val author: String,
    val version: String,
    val mcVersion: String,
    val rating: Float,
    val downloads: Int,
    val iconUrl: String,
    val isMod: Boolean,
    val sizeText: String,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(isInstalled: Boolean = false, isEnabled: Boolean = true): MarketPlugin {
        return MarketPlugin(
            id = id,
            slug = slug,
            name = name,
            description = description,
            category = category,
            author = author,
            version = version,
            mcVersion = mcVersion,
            rating = rating,
            downloads = downloads,
            iconUrl = iconUrl,
            isInstalled = isInstalled,
            isEnabled = isEnabled,
            isMod = isMod,
            sizeText = sizeText
        )
    }

    companion object {
        fun fromDomain(plugin: MarketPlugin): CachedMarketPluginEntity {
            return CachedMarketPluginEntity(
                id = plugin.id,
                slug = plugin.slug,
                name = plugin.name,
                description = plugin.description,
                category = plugin.category,
                author = plugin.author,
                version = plugin.version,
                mcVersion = plugin.mcVersion,
                rating = plugin.rating,
                downloads = plugin.downloads,
                iconUrl = plugin.iconUrl,
                isMod = plugin.isMod,
                sizeText = plugin.sizeText
            )
        }
    }
}
