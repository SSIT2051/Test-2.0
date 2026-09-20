package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.LogEntry
import com.example.domain.model.LogLevel

@Entity(tableName = "server_logs")
data class ServerLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: String,
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String
) {
    fun toDomain(): LogEntry {
        return LogEntry(
            id = id,
            serverId = serverId,
            timestamp = timestamp,
            level = try { LogLevel.valueOf(level) } catch (e: Exception) { LogLevel.INFO },
            tag = tag,
            message = message
        )
    }

    companion object {
        fun fromDomain(log: LogEntry): ServerLogEntity {
            return ServerLogEntity(
                id = log.id,
                serverId = log.serverId,
                timestamp = log.timestamp,
                level = log.level.name,
                tag = log.tag,
                message = log.message
            )
        }
    }
}
