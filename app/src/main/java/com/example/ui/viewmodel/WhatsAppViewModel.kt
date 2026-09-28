package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.AccessibilityLogEntity
import com.example.data.models.CallLogEntity
import com.example.data.models.ChannelEntity
import com.example.data.models.ChatEntity
import com.example.data.models.ContactEntity
import com.example.data.models.MessageEntity
import com.example.data.models.StatusEntity
import com.example.data.repository.WhatsAppRepository
import com.example.ui.components.WhatsAppTab
import com.example.util.AccessibilitySpeechManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    CONVERSATION,
    STATUS_VIEWER,
    ACTIVE_CALL,
    NEW_CHAT,
    SETTINGS,
    CONTACT_INFO,
    ACCESSIBILITY_SETTINGS
}

data class ActiveCallState(
    val contactName: String,
    val avatarColorHex: String = "#075E54",
    val isVideo: Boolean = false,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeaker: Boolean = false,
    val isConnected: Boolean = true
)

class WhatsAppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WhatsAppRepository(application)

    // Navigation and state
    val currentTab = MutableStateFlow(WhatsAppTab.CHATS)
    val activeScreen = MutableStateFlow(AppScreen.HOME)
    val selectedChatId = MutableStateFlow<String?>(null)
    val selectedContact = MutableStateFlow<ContactEntity?>(null)
    val activeStatus = MutableStateFlow<StatusEntity?>(null)
    val activeCallState = MutableStateFlow<ActiveCallState?>(null)

    // Dark mode: null = system default, true = force dark, false = force light
    val isDarkMode = MutableStateFlow<Boolean?>(null)

    // Filters and search
    val isSearchActive = MutableStateFlow(false)
    val searchQuery = MutableStateFlow("")
    val activeFilter = MutableStateFlow("All") // "All", "Unread", "Favourites", "Groups"

    // Raw flows from Room
    private val rawChats = repository.allChats
    val allStatuses = repository.allStatuses.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allCalls = repository.allCalls.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allChannels = repository.allChannels.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allContacts = repository.allContacts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allAccessibilityLogs = repository.allAccessibilityLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Accessibility Configuration for Visually Impaired / Partially Blind Users
    val accessibilitySpeechManager = AccessibilitySpeechManager(application)
    val speakNewMessagesEnabled = MutableStateFlow(true)
    val queueInSequenceEnabled = MutableStateFlow(true)
    val announceSenderEnabled = MutableStateFlow(true)
    val speechRate = MutableStateFlow(1.0f)

    // Filtered chats flow based on search and selected filter tab
    val filteredChats: StateFlow<List<ChatEntity>> = combine(
        rawChats,
        searchQuery,
        activeFilter
    ) { chats, query, filter ->
        chats.filter { chat ->
            val matchesFilter = when (filter) {
                "Unread" -> chat.unreadCount > 0
                "Favourites" -> chat.filterCategory == "Favourites"
                "Groups" -> chat.isGroup
                else -> true
            }
            val matchesQuery = query.isBlank() ||
                chat.title.contains(query, ignoreCase = true) ||
                chat.lastMessage.contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active conversation messages
    private val _activeMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val activeMessages: StateFlow<List<MessageEntity>> = _activeMessages

    private var messagesJob: Job? = null
    private var callTimerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }

        // Sequential message readout for visually impaired / partially blind users
        viewModelScope.launch {
            repository.incomingMessageEvents.collect { msg ->
                val shouldSpeak = speakNewMessagesEnabled.value
                val announcement = if (announceSenderEnabled.value) {
                    "New message from ${msg.senderName}: ${msg.text}"
                } else {
                    msg.text
                }

                if (shouldSpeak) {
                    accessibilitySpeechManager.speak(
                        announcement,
                        queueInSequence = queueInSequenceEnabled.value
                    )
                }

                repository.addAccessibilityLog(
                    title = "Message from ${msg.senderName}",
                    content = announcement,
                    wasSpoken = shouldSpeak
                )
            }
        }
    }

    fun setTab(tab: WhatsAppTab) {
        currentTab.value = tab
    }

    fun openChat(chatId: String) {
        selectedChatId.value = chatId
        activeScreen.value = AppScreen.CONVERSATION
        viewModelScope.launch {
            repository.markChatAsRead(chatId)
        }
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessagesForChat(chatId).collect { msgs ->
                _activeMessages.value = msgs
            }
        }
    }

    fun closeChat() {
        selectedChatId.value = null
        activeScreen.value = AppScreen.HOME
        messagesJob?.cancel()
    }

    fun sendMessage(
        text: String,
        type: String = "TEXT",
        mediaDuration: String? = null,
        mediaCaption: String? = null,
        replyTo: String? = null
    ) {
        val chatId = selectedChatId.value ?: return
        if (text.isBlank() && type == "TEXT") return

        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = text,
                messageType = type,
                mediaDuration = mediaDuration,
                mediaCaption = mediaCaption,
                replyTo = replyTo
            )
        }
    }

    fun addMessageReaction(messageId: Long, emoji: String) {
        viewModelScope.launch {
            repository.updateMessageReaction(messageId, emoji)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun toggleStarMessage(messageId: Long) {
        viewModelScope.launch {
            repository.toggleMessageStar(messageId)
        }
    }

    fun startCall(contactName: String, isVideo: Boolean, avatarColorHex: String = "#075E54") {
        activeCallState.value = ActiveCallState(
            contactName = contactName,
            avatarColorHex = avatarColorHex,
            isVideo = isVideo,
            durationSeconds = 0
        )
        activeScreen.value = AppScreen.ACTIVE_CALL

        viewModelScope.launch {
            repository.addCallLog(contactName, isVideo, isOutgoing = true)
        }

        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (activeCallState.value != null) {
                delay(1000)
                activeCallState.value = activeCallState.value?.let { current ->
                    current.copy(durationSeconds = current.durationSeconds + 1)
                }
            }
        }
    }

    fun endCall() {
        callTimerJob?.cancel()
        activeCallState.value = null
        if (selectedChatId.value != null) {
            activeScreen.value = AppScreen.CONVERSATION
        } else {
            activeScreen.value = AppScreen.HOME
        }
    }

    fun toggleCallMute() {
        activeCallState.value = activeCallState.value?.let { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleCallSpeaker() {
        activeCallState.value = activeCallState.value?.let { it.copy(isSpeaker = !it.isSpeaker) }
    }

    fun openStatusViewer(status: StatusEntity) {
        activeStatus.value = status
        activeScreen.value = AppScreen.STATUS_VIEWER
        viewModelScope.launch {
            repository.markStatusAsViewed(status.id)
        }
    }

    fun closeStatusViewer() {
        activeStatus.value = null
        activeScreen.value = AppScreen.HOME
    }

    fun addStatusText(text: String, bgHex: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addStatus(text, bgHex)
        }
    }

    fun toggleChannelFollow(channelId: String) {
        viewModelScope.launch {
            repository.toggleChannelFollow(channelId)
        }
    }

    fun openSettings() {
        activeScreen.value = AppScreen.SETTINGS
    }

    fun closeSettings() {
        activeScreen.value = AppScreen.HOME
    }

    fun openNewChat() {
        activeScreen.value = AppScreen.NEW_CHAT
    }

    fun closeNewChat() {
        activeScreen.value = AppScreen.HOME
    }

    fun createNewChat(name: String, phone: String) {
        viewModelScope.launch {
            repository.createNewChat(name, phone)
            closeNewChat()
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            if (selectedChatId.value == chatId) {
                closeChat()
            }
        }
    }

    fun openContactInfo(contact: ContactEntity) {
        selectedContact.value = contact
        activeScreen.value = AppScreen.CONTACT_INFO
    }

    fun closeContactInfo() {
        selectedContact.value = null
        if (selectedChatId.value != null) {
            activeScreen.value = AppScreen.CONVERSATION
        } else {
            activeScreen.value = AppScreen.HOME
        }
    }

    fun setDarkMode(dark: Boolean?) {
        isDarkMode.value = dark
    }

    // Accessibility Controls
    fun openAccessibilitySettings() {
        activeScreen.value = AppScreen.ACCESSIBILITY_SETTINGS
    }

    fun closeAccessibilitySettings() {
        activeScreen.value = AppScreen.SETTINGS
    }

    fun setSpeakNewMessages(enabled: Boolean) {
        speakNewMessagesEnabled.value = enabled
        if (!enabled) {
            accessibilitySpeechManager.stop()
        }
    }

    fun setQueueInSequence(enabled: Boolean) {
        queueInSequenceEnabled.value = enabled
    }

    fun setAnnounceSender(enabled: Boolean) {
        announceSenderEnabled.value = enabled
    }

    fun setSpeechRate(rate: Float) {
        speechRate.value = rate
        accessibilitySpeechManager.setSpeechRate(rate)
    }

    fun speakText(text: String, queueInSequence: Boolean = queueInSequenceEnabled.value) {
        accessibilitySpeechManager.speak(text, queueInSequence = queueInSequence)
    }

    fun speakLog(log: AccessibilityLogEntity) {
        val messageToSpeak = log.content.ifBlank { log.title }
        accessibilitySpeechManager.speak(messageToSpeak, queueInSequence = queueInSequenceEnabled.value)
    }

    fun stopSpeaking() {
        accessibilitySpeechManager.stop()
    }

    fun clearAccessibilityLogs() {
        viewModelScope.launch {
            repository.clearAccessibilityLogs()
        }
    }

    fun testAccessibilitySpeech() {
        val test1 = "Sequential message 1: Hey, are you free this afternoon?"
        val test2 = "Sequential message 2: Don't forget our meeting at 3 PM!"
        val test3 = "Sequential message 3: See you soon!"

        speakText(test1, queueInSequence = queueInSequenceEnabled.value)
        speakText(test2, queueInSequence = queueInSequenceEnabled.value)
        speakText(test3, queueInSequence = queueInSequenceEnabled.value)

        viewModelScope.launch {
            repository.addAccessibilityLog(
                title = "Accessibility Test Msg 1",
                content = test1,
                wasSpoken = true
            )
            repository.addAccessibilityLog(
                title = "Accessibility Test Msg 2",
                content = test2,
                wasSpoken = true
            )
            repository.addAccessibilityLog(
                title = "Accessibility Test Msg 3",
                content = test3,
                wasSpoken = true
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        accessibilitySpeechManager.shutdown()
        messagesJob?.cancel()
        callTimerJob?.cancel()
    }
}
