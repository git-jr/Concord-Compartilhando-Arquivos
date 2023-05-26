package com.alura.concord.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.alura.concord.data.Chat
import com.alura.concord.data.ChatWithLastMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(chat: Chat)

    @Query("SELECT * FROM Chat")
    fun getAll(): Flow<List<Chat>>

    @Transaction
    @Query(
        """
    SELECT chat.*, messageEntity.content AS lastMessage, messageEntity.date AS dateLastMessage 
    FROM Chat LEFT JOIN MessageEntity ON chat.id = messageEntity.chatId 
    WHERE messageEntity.id = ( SELECT MAX(id) FROM MessageEntity
    WHERE chatId = chat.id )
    """
    )
    fun getAllWithLastMessage(): Flow<List<ChatWithLastMessage>>

    @Query("SELECT * FROM Chat WHERE id = :id")
    fun getById(id: Long): Flow<Chat?>

    @Query("DELETE FROM Chat WHERE id = :id")
    suspend fun delete(id: Long)
}