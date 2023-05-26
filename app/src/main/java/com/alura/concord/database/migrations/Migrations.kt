package com.alura.concord.database.migrations

import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec

@RenameTable(
    fromTableName = "Message",
    toTableName = "MessageEntity"
)
class Migration1To2 : AutoMigrationSpec