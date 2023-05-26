package com.alura.concord.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.alura.concord.data.DownloadableFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadableFileDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(downloadableFileEntity: DownloadableFileEntity)

    @Query("SELECT * FROM DownloadableFileEntity WHERE id = :id")
    fun getById(id: Long): Flow<DownloadableFileEntity?>
}