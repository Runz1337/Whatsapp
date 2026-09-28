package com.familyguardian.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.familyguardian.app.data.models.CapturedContactEntity
import com.familyguardian.app.data.models.CapturedMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CapturedContactDao {
    @Query("SELECT * FROM captured_contacts ORDER BY lastOccurredAt DESC")
    fun observeAll(): Flow<List<CapturedContactEntity>>

    @Query("SELECT * FROM captured_contacts WHERE id = :contactId")
    suspend fun get(contactId: String): CapturedContactEntity?

    @Query("SELECT COUNT(*) FROM captured_contacts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: CapturedContactEntity)

    @Query("DELETE FROM captured_contacts")
    suspend fun clear()
}

@Dao
interface CapturedMessageDao {
    @Query("SELECT * FROM captured_messages WHERE contactId = :contactId ORDER BY occurredAt DESC LIMIT :limit")
    fun observeForContact(contactId: String, limit: Int = 500): Flow<List<CapturedMessageEntity>>

    @Query("SELECT * FROM captured_messages ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<CapturedMessageEntity>>

    @Query("SELECT * FROM captured_messages WHERE notificationKey = :key ORDER BY occurredAt DESC LIMIT 1")
    suspend fun latestForNotification(key: String): CapturedMessageEntity?

    @Query("SELECT * FROM captured_messages WHERE id = :id LIMIT 1")
    suspend fun get(id: String): CapturedMessageEntity?

    @Query("SELECT COUNT(*) FROM captured_messages")
    suspend fun count(): Int

    @Query("SELECT id FROM captured_messages ORDER BY occurredAt ASC LIMIT :skip OFFSET 0")
    suspend fun oldestIds(skip: Int): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: CapturedMessageEntity)

    @Query("DELETE FROM captured_messages WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM captured_messages")
    suspend fun clear()
}
