package com.familyguardian.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.familyguardian.app.data.models.DeviceIdentityEntity
import com.familyguardian.app.data.models.MirroredChatEntity
import com.familyguardian.app.data.models.MirroredMessageEntity
import com.familyguardian.app.data.models.SupervisedEventEntity
import com.familyguardian.app.data.models.SupervisionLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceIdentityDao {
    @Query("SELECT * FROM device_identity WHERE id = 'local'")
    fun observe(): Flow<DeviceIdentityEntity?>

    @Query("SELECT * FROM device_identity WHERE id = 'local'")
    suspend fun get(): DeviceIdentityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(identity: DeviceIdentityEntity)
}

@Dao
interface SupervisionLinkDao {
    @Query("SELECT * FROM supervision_links WHERE status = 'ACTIVE' ORDER BY activatedAt DESC")
    fun observeActive(): Flow<List<SupervisionLinkEntity>>

    @Query("SELECT * FROM supervision_links ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SupervisionLinkEntity>>

    @Query("SELECT * FROM supervision_links WHERE linkId = :linkId")
    suspend fun getById(linkId: String): SupervisionLinkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: SupervisionLinkEntity)

    @Query("DELETE FROM supervision_links WHERE linkId = :linkId")
    suspend fun delete(linkId: String)
}

@Dao
interface SupervisedEventDao {
    @Query("SELECT * FROM supervised_events WHERE linkId = :linkId ORDER BY occurredAt DESC, eventId DESC LIMIT :limit")
    fun observeForLink(linkId: String, limit: Int = 500): Flow<List<SupervisedEventEntity>>

    @Query("SELECT * FROM supervised_events ORDER BY occurredAt DESC, eventId DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<SupervisedEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<SupervisedEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: SupervisedEventEntity)

    @Query("DELETE FROM supervised_events WHERE linkId = :linkId")
    suspend fun deleteForLink(linkId: String)
}

@Dao
interface MirroredChatDao {
    @Query("SELECT * FROM mirrored_chats WHERE linkId = :linkId ORDER BY timestamp DESC")
    fun observeForLink(linkId: String): Flow<List<MirroredChatEntity>>

    @Query("SELECT * FROM mirrored_chats WHERE linkId = :linkId AND chatId = :chatId")
    suspend fun get(linkId: String, chatId: String): MirroredChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(chats: List<MirroredChatEntity>)

    @Query("DELETE FROM mirrored_chats WHERE linkId = :linkId")
    suspend fun deleteForLink(linkId: String)

    @Query("DELETE FROM mirrored_chats")
    suspend fun clear()
}

@Dao
interface MirroredMessageDao {
    @Query("SELECT * FROM mirrored_messages WHERE linkId = :linkId AND chatId = :chatId ORDER BY timestamp ASC, sourceMessageId ASC")
    fun observeForChat(linkId: String, chatId: String): Flow<List<MirroredMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MirroredMessageEntity>)

    @Query("DELETE FROM mirrored_messages WHERE linkId = :linkId")
    suspend fun deleteForLink(linkId: String)

    @Query("DELETE FROM mirrored_messages")
    suspend fun clear()
}
