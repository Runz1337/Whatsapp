package com.familyguardian.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DeviceRole { UNSET, PARENT, CHILD }

enum class LinkStatus { PENDING, ACTIVE, REVOKED }

enum class SupervisionEventType {
    PAIRING_REQUESTED,
    PAIRING_ACTIVATED,
    PAIRING_REVOKED,
    CHAT_OPENED,
    MESSAGE_SENT,
    MESSAGE_RECEIVED,
    MESSAGE_DELETED,
    MESSAGE_REACTION,
    MESSAGE_STARRED,
    CALL_STARTED,
    CALL_ENDED,
    STATUS_ADDED,
    CHAT_DELETED,
    CONTACT_ADDED,
    CHANNEL_FOLLOW_TOGGLED
}

@Entity(tableName = "device_identity")
data class DeviceIdentityEntity(
    @PrimaryKey val id: String = "local",
    val role: String = DeviceRole.UNSET.name,
    val displayName: String = "",
    val deviceId: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "supervision_links")
data class SupervisionLinkEntity(
    @PrimaryKey val linkId: String,
    val status: String = LinkStatus.PENDING.name,
    val childDeviceId: String = "",
    val childDisplayName: String = "",
    val parentDeviceId: String = "",
    val parentDisplayName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val activatedAt: Long? = null,
    val revokedAt: Long? = null,
    val lastEventAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "supervised_events")
data class SupervisedEventEntity(
    @PrimaryKey val eventId: String,
    val linkId: String,
    val type: String,
    val chatId: String? = null,
    val chatTitle: String? = null,
    val summary: String = "",
    val detail: String? = null,
    val occurredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mirrored_chats")
data class MirroredChatEntity(
    @PrimaryKey val id: String,
    val linkId: String,
    val chatId: String,
    val title: String,
    val isGroup: Boolean = false,
    val groupMembersCount: Int = 0,
    val avatarColorHex: String = "#128C7E",
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val timestamp: Long = 0L,
    val unreadCount: Int = 0
)

@Entity(tableName = "mirrored_messages")
data class MirroredMessageEntity(
    @PrimaryKey val id: String,
    val linkId: String,
    val chatId: String,
    val sourceMessageId: Long,
    val senderName: String,
    val text: String,
    val timestamp: Long = 0L,
    val timeFormatted: String = "",
    val isOutgoing: Boolean = true,
    val status: String = "READ",
    val messageType: String = "TEXT",
    val mediaDuration: String? = null,
    val mediaCaption: String? = null,
    val reaction: String? = null
)
