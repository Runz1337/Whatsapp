package com.example.data.repository

import android.content.Context
import com.example.data.local.WhatsAppDatabase
import com.example.data.models.AccessibilityLogEntity
import com.example.data.models.CallLogEntity
import com.example.data.models.ChannelEntity
import com.example.data.models.ChatEntity
import com.example.data.models.ContactEntity
import com.example.data.models.MessageEntity
import com.example.data.models.StatusEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WhatsAppRepository(context: Context) {
    private val db = WhatsAppDatabase.getInstance(context)
    private val chatDao = db.chatDao()
    private val messageDao = db.messageDao()
    private val statusDao = db.statusDao()
    private val callDao = db.callDao()
    private val channelDao = db.channelDao()
    private val contactDao = db.contactDao()
    private val accessibilityLogDao = db.accessibilityLogDao()

    private val _incomingMessageEvents = MutableSharedFlow<MessageEntity>(extraBufferCapacity = 64)
    val incomingMessageEvents = _incomingMessageEvents.asSharedFlow()

    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChats()
    val allStatuses: Flow<List<StatusEntity>> = statusDao.getAllStatuses()
    val allCalls: Flow<List<CallLogEntity>> = callDao.getAllCalls()
    val allChannels: Flow<List<ChannelEntity>> = channelDao.getAllChannels()
    val allContacts: Flow<List<ContactEntity>> = contactDao.getAllContacts()
    val allAccessibilityLogs: Flow<List<AccessibilityLogEntity>> = accessibilityLogDao.getAllLogs()

    suspend fun addAccessibilityLog(title: String, content: String, wasSpoken: Boolean = true) {
        val now = System.currentTimeMillis()
        val timeFmt = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(now))
        val log = AccessibilityLogEntity(
            title = title,
            content = content,
            timestamp = now,
            timeFormatted = timeFmt,
            wasSpoken = wasSpoken
        )
        accessibilityLogDao.insertLog(log)
    }

    suspend fun clearAccessibilityLogs() {
        accessibilityLogDao.clearLogs()
    }

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForChat(chatId)

    suspend fun getChat(chatId: String): ChatEntity? =
        chatDao.getChatById(chatId)

    suspend fun markChatAsRead(chatId: String) {
        chatDao.markChatAsRead(chatId)
    }

    suspend fun markStatusAsViewed(statusId: String) {
        statusDao.markAsViewed(statusId)
    }

    suspend fun toggleChannelFollow(channelId: String) {
        channelDao.toggleFollow(channelId)
    }

    suspend fun updateMessageReaction(messageId: Long, reaction: String?) {
        messageDao.updateReaction(messageId, reaction)
    }

    suspend fun toggleMessageStar(messageId: Long) {
        messageDao.toggleStar(messageId)
    }

    suspend fun deleteMessage(messageId: Long) {
        messageDao.deleteMessage(messageId)
    }

    suspend fun deleteChat(chatId: String) {
        chatDao.deleteChat(chatId)
        messageDao.clearChatMessages(chatId)
    }

    suspend fun addStatus(text: String, bgHex: String) {
        val newStatus = StatusEntity(
            id = "my_status_${System.currentTimeMillis()}",
            contactName = "My status",
            contactAvatarColorHex = "#25D366",
            timestampText = "Just now",
            timestamp = System.currentTimeMillis(),
            isViewed = false,
            isMyStatus = true,
            textContent = text,
            backgroundHex = bgHex,
            mediaType = "TEXT"
        )
        statusDao.insertStatus(newStatus)
    }

    suspend fun addCallLog(contactName: String, isVideo: Boolean, isOutgoing: Boolean) {
        val call = CallLogEntity(
            id = "call_${System.currentTimeMillis()}",
            contactName = contactName,
            contactAvatarColorHex = "#075E54",
            timestampText = "Just now",
            timestamp = System.currentTimeMillis(),
            isVideo = isVideo,
            isIncoming = !isOutgoing,
            isMissed = false,
            durationText = "Ongoing"
        )
        callDao.insertCall(call)
    }

    suspend fun sendMessage(
        chatId: String,
        text: String,
        messageType: String = "TEXT",
        mediaDuration: String? = null,
        mediaCaption: String? = null,
        replyTo: String? = null
    ) {
        val now = System.currentTimeMillis()
        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now))
        val message = MessageEntity(
            chatId = chatId,
            senderName = "You",
            text = text,
            timestamp = now,
            timeFormatted = timeFmt,
            isOutgoing = true,
            status = "READ",
            messageType = messageType,
            mediaDuration = mediaDuration,
            mediaCaption = mediaCaption,
            replyToText = replyTo
        )
        messageDao.insertMessage(message)
        val snippet = when (messageType) {
            "VOICE" -> "🎤 Voice message ($mediaDuration)"
            "IMAGE" -> "📷 Photo"
            "DOC" -> "📄 Document"
            else -> text
        }
        chatDao.updateLastMessage(chatId, snippet, timeFmt, now)

        // Trigger realistic reply after a realistic delay
        CoroutineScope(Dispatchers.IO).launch {
            delay(1600)
            val chat = chatDao.getChatById(chatId)
            val replies = listOf(
                "Awesome! Got your message 👌",
                "Sounds like a plan! Let me know when you're free.",
                "Sure thing, let's catch up later today!",
                "Great! Thanks for sending this over.",
                "Haha definitely! Talk soon 🙌",
                "Got it! Looking forward to it."
            )
            val autoReply = replies.random()
            val replyNow = System.currentTimeMillis()
            val replyTimeFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(replyNow))
            val incomingMessage = MessageEntity(
                chatId = chatId,
                senderName = chat?.title ?: "Contact",
                text = autoReply,
                timestamp = replyNow,
                timeFormatted = replyTimeFmt,
                isOutgoing = false,
                status = "READ",
                messageType = "TEXT"
            )
            messageDao.insertMessage(incomingMessage)
            chatDao.updateLastMessage(chatId, autoReply, replyTimeFmt, replyNow)
            _incomingMessageEvents.emit(incomingMessage)
        }
    }

    suspend fun createNewChat(name: String, phone: String, initialMessage: String = "Hey there!") {
        val chatId = "chat_${System.currentTimeMillis()}"
        val colors = listOf("#008069", "#128C7E", "#25D366", "#075E54", "#1EBEA5", "#0288D1")
        val color = colors.random()
        val now = System.currentTimeMillis()
        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now))

        val newChat = ChatEntity(
            id = chatId,
            title = name,
            isGroup = false,
            avatarColorHex = color,
            lastMessage = initialMessage,
            lastMessageTime = timeFmt,
            timestamp = now,
            unreadCount = 0,
            filterCategory = "All"
        )
        chatDao.insertChat(newChat)

        val msg = MessageEntity(
            chatId = chatId,
            senderName = "You",
            text = initialMessage,
            timestamp = now,
            timeFormatted = timeFmt,
            isOutgoing = true,
            status = "READ"
        )
        messageDao.insertMessage(msg)
    }

    suspend fun seedInitialDataIfEmpty() {
        val existingChats = chatDao.getAllChats().first()
        if (existingChats.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val contacts = listOf(
            ContactEntity("c1", "Sarah Jenkins", "+1 (555) 234-5678", "Busy coding ✨", "#008069", true, "online"),
            ContactEntity("c2", "Alex Rivera", "+1 (555) 876-5432", "At the gym 🏋️", "#128C7E", false, "today at 11:15 AM"),
            ContactEntity("c3", "Android Devs Group", "+1 (555) 000-1122", "Group for Jetpack Compose enthusiasts", "#00A884", false, "today at 9:00 AM"),
            ContactEntity("c4", "Mom ❤️", "+1 (555) 432-1098", "Always available for family", "#E91E63", true, "online"),
            ContactEntity("c5", "David Chen", "+1 (555) 987-6543", "Living in the moment", "#3F51B5", false, "yesterday at 8:45 PM"),
            ContactEntity("c6", "Tech Club HQ", "+1 (555) 345-6789", "Campus tech events & workshops", "#FF9800", false, "23/09/2026"),
            ContactEntity("c7", "Emily Watson", "+1 (555) 654-3210", "Coffee first ☕", "#9C27B0", true, "online")
        )
        contactDao.insertContacts(contacts)

        val chats = listOf(
            ChatEntity(
                id = "chat_1",
                title = "Sarah Jenkins",
                isGroup = false,
                avatarColorHex = "#008069",
                lastMessage = "See you at the coffee shop at 4! ☕",
                lastMessageTime = "11:42 AM",
                timestamp = now - 60000 * 18,
                unreadCount = 2,
                isPinned = true,
                filterCategory = "Unread",
                contactId = "c1"
            ),
            ChatEntity(
                id = "chat_2",
                title = "Android Devs Group",
                isGroup = true,
                groupMembersCount = 48,
                avatarColorHex = "#00A884",
                lastMessage = "Alex: Check out the new Jetpack Compose update!",
                lastMessageTime = "10:15 AM",
                timestamp = now - 60000 * 105,
                unreadCount = 5,
                isPinned = true,
                filterCategory = "Groups",
                contactId = "c3"
            ),
            ChatEntity(
                id = "chat_3",
                title = "Mom ❤️",
                isGroup = false,
                avatarColorHex = "#E91E63",
                lastMessage = "Don't forget to eat lunch sweetie ❤️",
                lastMessageTime = "9:30 AM",
                timestamp = now - 60000 * 150,
                unreadCount = 0,
                isPinned = false,
                filterCategory = "Favourites",
                contactId = "c4"
            ),
            ChatEntity(
                id = "chat_4",
                title = "Alex Rivera",
                isGroup = false,
                avatarColorHex = "#128C7E",
                lastMessage = "🎤 Voice message (0:24)",
                lastMessageTime = "Yesterday",
                timestamp = now - 86400000L,
                unreadCount = 0,
                isPinned = false,
                filterCategory = "All",
                contactId = "c2"
            ),
            ChatEntity(
                id = "chat_5",
                title = "Tech Club HQ",
                isGroup = true,
                groupMembersCount = 120,
                avatarColorHex = "#FF9800",
                lastMessage = "Meeting link posted for tomorrow's hackathon 🚀",
                lastMessageTime = "Yesterday",
                timestamp = now - 86400000L * 2,
                unreadCount = 0,
                isPinned = false,
                filterCategory = "Groups",
                contactId = "c6"
            ),
            ChatEntity(
                id = "chat_6",
                title = "Emily Watson",
                isGroup = false,
                avatarColorHex = "#9C27B0",
                lastMessage = "Loved the photos from the trip!",
                lastMessageTime = "24/09/2026",
                timestamp = now - 86400000L * 3,
                unreadCount = 0,
                isPinned = false,
                filterCategory = "Favourites",
                contactId = "c7"
            )
        )
        chatDao.insertChats(chats)

        // Seed messages for chat 1 (Sarah)
        val sarahMessages = listOf(
            MessageEntity(
                chatId = "chat_1",
                senderName = "Sarah Jenkins",
                text = "Hey! Are we still on for the project review today?",
                timestamp = now - 60000 * 30,
                timeFormatted = "11:30 AM",
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                chatId = "chat_1",
                senderName = "You",
                text = "Yes, absolutely! I finished the UI implementation in Compose.",
                timestamp = now - 60000 * 25,
                timeFormatted = "11:35 AM",
                isOutgoing = true,
                status = "READ",
                reaction = "🔥"
            ),
            MessageEntity(
                chatId = "chat_1",
                senderName = "Sarah Jenkins",
                text = "That's awesome! Does it match the latest WhatsApp design?",
                timestamp = now - 60000 * 20,
                timeFormatted = "11:40 AM",
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                chatId = "chat_1",
                senderName = "You",
                text = "100%! Bottom tabs, chat bubbles, status stories, call logs, channels, and full persistence.",
                timestamp = now - 60000 * 19,
                timeFormatted = "11:41 AM",
                isOutgoing = true,
                status = "READ"
            ),
            MessageEntity(
                chatId = "chat_1",
                senderName = "Sarah Jenkins",
                text = "See you at the coffee shop at 4! ☕",
                timestamp = now - 60000 * 18,
                timeFormatted = "11:42 AM",
                isOutgoing = false,
                status = "READ",
                reaction = "❤️"
            )
        )
        messageDao.insertMessages(sarahMessages)

        // Seed messages for chat 4 (Alex with voice note)
        val alexMessages = listOf(
            MessageEntity(
                chatId = "chat_4",
                senderName = "Alex Rivera",
                text = "Hey man, check this quick update:",
                timestamp = now - 86400000L - 10000,
                timeFormatted = "5:12 PM",
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                chatId = "chat_4",
                senderName = "Alex Rivera",
                text = "Voice message",
                timestamp = now - 86400000L,
                timeFormatted = "5:13 PM",
                isOutgoing = false,
                status = "READ",
                messageType = "VOICE",
                mediaDuration = "0:24"
            ),
            MessageEntity(
                chatId = "chat_4",
                senderName = "You",
                text = "Sounds solid! Let's ship it.",
                timestamp = now - 86400000L + 50000,
                timeFormatted = "5:15 PM",
                isOutgoing = true,
                status = "READ"
            )
        )
        messageDao.insertMessages(alexMessages)

        // Seed status updates
        val statuses = listOf(
            StatusEntity(
                id = "status_1",
                contactName = "Sarah Jenkins",
                contactAvatarColorHex = "#008069",
                timestampText = "35 minutes ago",
                timestamp = now - 60000 * 35,
                isViewed = false,
                textContent = "Sunny day in the city! ☀️ Enjoying iced matcha latte.",
                backgroundHex = "#075E54"
            ),
            StatusEntity(
                id = "status_2",
                contactName = "Alex Rivera",
                contactAvatarColorHex = "#128C7E",
                timestampText = "2 hours ago",
                timestamp = now - 60000 * 120,
                isViewed = false,
                textContent = "New personal record at the gym! 💪 120kg bench press.",
                backgroundHex = "#8E24AA"
            ),
            StatusEntity(
                id = "status_3",
                contactName = "Emily Watson",
                contactAvatarColorHex = "#9C27B0",
                timestampText = "5 hours ago",
                timestamp = now - 60000 * 300,
                isViewed = true,
                textContent = "Reading 'Clean Architecture' 📚 Highly recommend it!",
                backgroundHex = "#1E88E5"
            )
        )
        statusDao.insertStatuses(statuses)

        // Seed Call Logs
        val calls = listOf(
            CallLogEntity(
                id = "call_1",
                contactName = "Mom ❤️",
                contactAvatarColorHex = "#E91E63",
                timestampText = "Today, 9:15 AM",
                timestamp = now - 60000 * 165,
                isVideo = false,
                isIncoming = true,
                isMissed = false,
                durationText = "12 mins"
            ),
            CallLogEntity(
                id = "call_2",
                contactName = "Sarah Jenkins",
                contactAvatarColorHex = "#008069",
                timestampText = "Yesterday, 8:40 PM",
                timestamp = now - 86400000L - 60000 * 60,
                isVideo = true,
                isIncoming = false,
                isMissed = false,
                durationText = "4 mins"
            ),
            CallLogEntity(
                id = "call_3",
                contactName = "David Chen",
                contactAvatarColorHex = "#3F51B5",
                timestampText = "September 25, 4:20 PM",
                timestamp = now - 86400000L * 2,
                isVideo = false,
                isIncoming = true,
                isMissed = true,
                durationText = "Missed"
            )
        )
        callDao.insertCalls(calls)

        // Seed Channels
        val channels = listOf(
            ChannelEntity(
                id = "ch_1",
                name = "WhatsApp",
                avatarColorHex = "#25D366",
                description = "The official WhatsApp channel. Stay informed on product updates and features.",
                followersText = "168M followers",
                isVerified = true,
                isFollowing = true,
                lastUpdateText = "Introducing custom chat themes and pinned messages in groups! 🎉",
                lastUpdateTime = "10:00 AM"
            ),
            ChannelEntity(
                id = "ch_2",
                name = "Real Madrid C.F.",
                avatarColorHex = "#1E88E5",
                description = "Official Real Madrid WhatsApp Channel ⚽ ¡Hala Madrid!",
                followersText = "52M followers",
                isVerified = true,
                isFollowing = false,
                lastUpdateText = "Matchday training ahead of the big derby! 🔥",
                lastUpdateTime = "Yesterday"
            ),
            ChannelEntity(
                id = "ch_3",
                name = "TechCrunch",
                avatarColorHex = "#00A884",
                description = "Breaking technology news and startup analysis.",
                followersText = "14M followers",
                isVerified = true,
                isFollowing = true,
                lastUpdateText = "AI breakthroughs: what the latest generation means for mobile dev.",
                lastUpdateTime = "2 days ago"
            ),
            ChannelEntity(
                id = "ch_4",
                name = "National Geographic",
                avatarColorHex = "#FFB300",
                description = "Inspiring people to care about the planet since 1888.",
                followersText = "31M followers",
                isVerified = true,
                isFollowing = false,
                lastUpdateText = "Astonishing photos from the deep ocean floor 🌊",
                lastUpdateTime = "3 days ago"
            )
        )
        channelDao.insertChannels(channels)
    }
}
