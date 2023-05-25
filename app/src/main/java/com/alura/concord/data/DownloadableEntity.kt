package com.alura.concord.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DownloadableEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    var name: String = "",
    var url: String = "",
    var size: Long = 0L,
)

data class DownloadableFile(
    val content: DownloadableEntity,
    var status: DownloadStatus = DownloadStatus.PENDING,
)


enum class DownloadStatus {
    PENDING, DOWNLOADING, ERROR
}
