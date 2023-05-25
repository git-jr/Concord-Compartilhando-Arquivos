package com.alura.concord.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.alura.concord.data.DownloadableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadableContentDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(downloadableEntity: DownloadableEntity)

    @Query("SELECT * FROM DownloadableEntity WHERE id = :id")
    fun getById(id: Long): Flow<DownloadableEntity?>
}