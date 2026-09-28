package com.familyguardian.app.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CapturedDirection { INCOMING, OUTGOING }

enum class CapturedKind {
    TEXT,
    IMAGE,
    VIDEO,
    VOICE,
    DOCUMENT,
    STICKER,
    CALL,
    CONTACT,
    LIVE_LOCATION,
    UNKNOWN
}

/**
 * A chat observed through WhatsApp notifications. Notifications identify a chat by
 * display name only, so [id] is derived from that name.
 */
@Entity(tableName = "captured_contacts")
data class CapturedContactEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val avatarColorHex: String = "#128C7E",
    val isGroup: Boolean = false,
    val isBusinessAccount: Boolean = false,
    val lastPreview: String = "",
    val lastTimeText: String = "",
    val lastOccurredAt: Long = 0L,
    val messageCount: Int = 0,
    val firstSeenAt: Long = 0L
)

@Entity(
    tableName = "captured_messages",
    indices = [
        Index("contactId"),
        Index(value = ["contactId", "occurredAt"])
    ]
)
data class CapturedMessageEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val contactName: String,
    val body: String,
    val occurredAt: Long,
    val senderName: String = contactName,
    val timeText: String = "",
    val direction: String = CapturedDirection.INCOMING.name,
    val kind: String = CapturedKind.TEXT.name,
    val isGroup: Boolean = false,
    val notificationKey: String = ""
)
