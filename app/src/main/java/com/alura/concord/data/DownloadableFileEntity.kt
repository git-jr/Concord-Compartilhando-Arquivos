package com.alura.concord.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DownloadableFileEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    var name: String = "",
    var url: String = "",
    var size: Long = 0L,
)

fun DownloadableFileEntity.toDownloadableFile() = DownloadableFile(
    id = id,
    name = name,
    url = url,
    size = size,
    status = DownloadStatus.PENDING,
)