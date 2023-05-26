package com.alura.concord.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.alura.concord.data.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(messageEntity: MessageEntity)

    @Query("SELECT * FROM MessageEntity")
    fun getAll(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM MessageEntity WHERE chatId = :chatId")
    fun getByChatId(chatId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM MessageEntity WHERE id = :id")
    fun getById(id: Long): Flow<MessageEntity?>

    @Query("DELETE FROM MessageEntity WHERE id = :id")
    suspend fun delete(id: Long)


}