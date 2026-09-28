package com.familyguardian.app.data.remote

import com.familyguardian.app.data.models.LinkStatus
import com.familyguardian.app.data.models.MirroredChatEntity
import com.familyguardian.app.data.models.MirroredMessageEntity
import com.familyguardian.app.data.models.SupervisionEventType

data class LinkDto(
    val linkId: String = "",
    val status: String = LinkStatus.PENDING.name,
    val childDeviceId: String = "",
    val childDisplayName: String = "",
    val parentDeviceId: String = "",
    val parentDisplayName: String = "",
    val createdAt: Long = 0L,
    val activatedAt: Long? = null,
    val revokedAt: Long? = null,
    val lastEventAt: Long = 0L
)

data class EventDto(
    val eventId: String = "",
    val type: String = SupervisionEventType.PAIRING_REQUESTED.name,
    val chatId: String? = null,
    val chatTitle: String? = null,
    val summary: String = "",
    val detail: String? = null,
    val occurredAt: Long = 0L
)

data class MirroredChatDto(
    val chatId: String = "",
    val title: String = "",
    val isGroup: Boolean = false,
    val groupMembersCount: Int = 0,
    val avatarColorHex: String = "#128C7E",
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val timestamp: Long = 0L,
    val unreadCount: Int = 0
)

data class MirroredMessageDto(
    val sourceMessageId: Long = 0L,
    val chatId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val timeFormatted: String = "",
    val isOutgoing: Boolean = true,
    val status: String = "READ",
    val messageType: String = "TEXT",
    val mediaDuration: String? = null,
    val mediaCaption: String? = null,
    val reaction: String? = null
)

fun LinkDto.toEntity() = com.familyguardian.app.data.models.SupervisionLinkEntity(
    linkId = linkId,
    status = status,
    childDeviceId = childDeviceId,
    childDisplayName = childDisplayName,
    parentDeviceId = parentDeviceId,
    parentDisplayName = parentDisplayName,
    createdAt = createdAt,
    activatedAt = activatedAt,
    revokedAt = revokedAt,
    lastEventAt = lastEventAt
)

fun EventDto.toEntity(linkId: String) = com.familyguardian.app.data.models.SupervisedEventEntity(
    eventId = eventId,
    linkId = linkId,
    type = type,
    chatId = chatId,
    chatTitle = chatTitle,
    summary = summary,
    detail = detail,
    occurredAt = occurredAt
)

fun MirroredChatDto.toEntity(linkId: String) = MirroredChatEntity(
    id = mirrorKey(linkId, chatId),
    linkId = linkId,
    chatId = chatId,
    title = title,
    isGroup = isGroup,
    groupMembersCount = groupMembersCount,
    avatarColorHex = avatarColorHex,
    lastMessage = lastMessage,
    lastMessageTime = lastMessageTime,
    timestamp = timestamp,
    unreadCount = unreadCount
)

fun MirroredMessageDto.toEntity(linkId: String) = MirroredMessageEntity(
    id = mirrorKey(linkId, chatId, sourceMessageId.toString()),
    linkId = linkId,
    chatId = chatId,
    sourceMessageId = sourceMessageId,
    senderName = senderName,
    text = text,
    timestamp = timestamp,
    timeFormatted = timeFormatted,
    isOutgoing = isOutgoing,
    status = status,
    messageType = messageType,
    mediaDuration = mediaDuration,
    mediaCaption = mediaCaption,
    reaction = reaction
)

internal fun mirrorKey(vararg parts: String) = parts.joinToString("::")
