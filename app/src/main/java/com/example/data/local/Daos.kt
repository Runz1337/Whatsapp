package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.models.AccessibilityLogEntity
import com.example.data.models.CallLogEntity
import com.example.data.models.ChannelEntity
import com.example.data.models.ChatEntity
import com.example.data.models.ContactEntity
import com.example.data.models.MessageEntity
import com.example.data.models.StatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, timestamp DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markChatAsRead(chatId: String)

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastMessageTime = :time, timestamp = :timestamp WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMessage: String, time: String, timestamp: Long)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET reaction = :reaction WHERE id = :messageId")
    suspend fun updateReaction(messageId: Long, reaction: String?)

    @Query("UPDATE messages SET isStarred = NOT isStarred WHERE id = :messageId")
    suspend fun toggleStar(messageId: Long)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearChatMessages(chatId: String)
}

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses ORDER BY isMyStatus DESC, timestamp DESC")
    fun getAllStatuses(): Flow<List<StatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatuses(statuses: List<StatusEntity>)

    @Query("UPDATE statuses SET isViewed = 1 WHERE id = :statusId")
    suspend fun markAsViewed(statusId: String)
}

@Dao
interface CallDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalls(calls: List<CallLogEntity>)

    @Query("DELETE FROM call_logs WHERE id = :callId")
    suspend fun deleteCall(callId: String)

    @Query("DELETE FROM call_logs")
    suspend fun clearAllCalls()
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFollowing = NOT isFollowing WHERE id = :channelId")
    suspend fun toggleFollow(channelId: String)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)
}

@Dao
interface AccessibilityLogDao {
    @Query("SELECT * FROM accessibility_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AccessibilityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AccessibilityLogEntity): Long

    @Query("DELETE FROM accessibility_logs")
    suspend fun clearLogs()
}
