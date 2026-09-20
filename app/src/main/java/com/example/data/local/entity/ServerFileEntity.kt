package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ServerFileItem

@Entity(tableName = "server_files")
data class ServerFileEntity(
    @PrimaryKey
    val compositeId: String, // serverId + ":" + path
    val serverId: String,
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val content: String
) {
    fun toDomain(): ServerFileItem {
        return ServerFileItem(
            path = path,
            name = name,
            isDirectory = isDirectory,
            sizeBytes = sizeBytes,
            lastModified = lastModified,
            content = content
        )
    }

    companion object {
        fun fromDomain(serverId: String, file: ServerFileItem): ServerFileEntity {
            return ServerFileEntity(
                compositeId = "$serverId:${file.path}",
                serverId = serverId,
                path = file.path,
                name = file.name,
                isDirectory = file.isDirectory,
                sizeBytes = file.sizeBytes,
                lastModified = file.lastModified,
                content = file.content
            )
        }
    }
}
