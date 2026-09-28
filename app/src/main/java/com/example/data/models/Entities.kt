package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val about: String = "Hey there! I am using WhatsApp.",
    val avatarColorHex: String = "#008069",
    val isOnline: Boolean = false,
    val lastSeenText: String = "today at 10:42 AM"
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isGroup: Boolean = false,
    val groupMembersCount: Int = 0,
    val avatarColorHex: String = "#128C7E",
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val filterCategory: String = "All", // "All", "Unread", "Favourites", "Groups"
    val contactId: String? = null
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = "12:00 PM",
    val isOutgoing: Boolean = true,
    val status: String = "READ", // "SENT", "DELIVERED", "READ"
    val messageType: String = "TEXT", // "TEXT", "IMAGE", "VOICE", "DOC"
    val mediaDuration: String? = null,
    val mediaCaption: String? = null,
    val reaction: String? = null,
    val isStarred: Boolean = false,
    val replyToText: String? = null
)

@Entity(tableName = "statuses")
data class StatusEntity(
    @PrimaryKey val id: String,
    val contactName: String,
    val contactAvatarColorHex: String = "#25D366",
    val timestampText: String = "Just now",
    val timestamp: Long = System.currentTimeMillis(),
    val isViewed: Boolean = false,
    val isMyStatus: Boolean = false,
    val textContent: String = "",
    val backgroundHex: String = "#128C7E",
    val mediaType: String = "TEXT" // "TEXT", "PHOTO"
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val contactName: String,
    val contactAvatarColorHex: String = "#075E54",
    val timestampText: String = "Today, 10:30 AM",
    val timestamp: Long = System.currentTimeMillis(),
    val isVideo: Boolean = false,
    val isIncoming: Boolean = true,
    val isMissed: Boolean = false,
    val durationText: String = "2 mins"
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarColorHex: String = "#00A884",
    val description: String = "",
    val followersText: String = "1.2M followers",
    val isVerified: Boolean = true,
    val isFollowing: Boolean = false,
    val lastUpdateText: String = "",
    val lastUpdateTime: String = ""
)

@Entity(tableName = "accessibility_logs")
data class AccessibilityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = "",
    val wasSpoken: Boolean = true
)
