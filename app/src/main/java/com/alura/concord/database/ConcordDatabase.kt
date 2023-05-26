package com.alura.concord.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.alura.concord.data.Chat
import com.alura.concord.data.DownloadableFileEntity
import com.alura.concord.data.MessageEntity
import com.alura.concord.database.migrations.Migration1To2

@Database(
    entities = [Chat::class, MessageEntity::class, DownloadableFileEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(
            from = 1, to = 2,
            Migration1To2::class
        ),
    ]
)

abstract class ConcordDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun downloadableFileDao(): DownloadableFileDao
}
