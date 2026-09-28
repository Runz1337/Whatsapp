package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AccessibilitySettingsScreen
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.ContactInfoScreen
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NewChatScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatusViewerScreen
import com.example.ui.theme.WhatsAppTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.WhatsAppViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: WhatsAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkModePref by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val useDarkTheme = darkModePref ?: isSystemInDarkTheme()

            WhatsAppTheme(darkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WhatsAppApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun WhatsAppApp(viewModel: WhatsAppViewModel) {
    val activeScreen by viewModel.activeScreen.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val chats by viewModel.filteredChats.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val statuses by viewModel.allStatuses.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val calls by viewModel.allCalls.collectAsStateWithLifecycle()
    val contacts by viewModel.allContacts.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val selectedChatId by viewModel.selectedChatId.collectAsStateWithLifecycle()
    val activeStatus by viewModel.activeStatus.collectAsStateWithLifecycle()
    val activeCallState by viewModel.activeCallState.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    // Accessibility state collection
    val speakNewMessages by viewModel.speakNewMessagesEnabled.collectAsStateWithLifecycle()
    val queueInSequence by viewModel.queueInSequenceEnabled.collectAsStateWithLifecycle()
    val announceSender by viewModel.announceSenderEnabled.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()
    val accessibilityLogs by viewModel.allAccessibilityLogs.collectAsStateWithLifecycle()

    val currentChat = chats.firstOrNull { it.id == selectedChatId }

    when (activeScreen) {
        AppScreen.HOME -> {
            HomeScreen(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) },
                chats = chats,
                activeFilter = activeFilter,
                onFilterSelect = { viewModel.activeFilter.value = it },
                onChatClick = { viewModel.openChat(it) },
                onDeleteChat = { viewModel.deleteChat(it) },
                onNewChatClick = { viewModel.openNewChat() },
                statuses = statuses,
                channels = channels,
                onStatusClick = { viewModel.openStatusViewer(it) },
                onAddStatus = { text, bg -> viewModel.addStatusText(text, bg) },
                onToggleFollowChannel = { viewModel.toggleChannelFollow(it) },
                calls = calls,
                onStartCall = { name, isVideo -> viewModel.startCall(name, isVideo) },
                onNewCallClick = { viewModel.openNewChat() },
                onCommunityChatClick = { groupName ->
                    // Open or find chat for group
                    val match = chats.firstOrNull { it.title.contains(groupName, ignoreCase = true) }
                    if (match != null) {
                        viewModel.openChat(match.id)
                    } else {
                        viewModel.createNewChat(groupName, "+1 (555) 000-0000")
                    }
                },
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.searchQuery.value = it },
                onSearchToggle = { viewModel.isSearchActive.value = it },
                onCameraClick = {
                    viewModel.addStatusText("📷 Captured with WhatsApp camera", "#008069")
                },
                onSettingsClick = { viewModel.openSettings() },
                onAccessibilityClick = { viewModel.openAccessibilitySettings() }
            )
        }

        AppScreen.CONVERSATION -> {
            if (currentChat != null) {
                ConversationScreen(
                    chat = currentChat,
                    messages = messages,
                    onBack = { viewModel.closeChat() },
                    onSendMessage = { text, type, duration, replyTo ->
                        viewModel.sendMessage(text, type, duration, null, replyTo)
                    },
                    onReactMessage = { id, emoji -> viewModel.addMessageReaction(id, emoji) },
                    onDeleteMessage = { id -> viewModel.deleteMessage(id) },
                    onToggleStar = { id -> viewModel.toggleStarMessage(id) },
                    onStartCall = { name, isVideo -> viewModel.startCall(name, isVideo, currentChat.avatarColorHex) },
                    onOpenContactInfo = {
                        val contact = contacts.firstOrNull { it.id == currentChat.contactId }
                            ?: com.example.data.models.ContactEntity(
                                id = currentChat.id,
                                name = currentChat.title,
                                phoneNumber = "+1 (555) 234-5678",
                                avatarColorHex = currentChat.avatarColorHex
                            )
                        viewModel.openContactInfo(contact)
                    }
                )
            } else {
                viewModel.closeChat()
            }
        }

        AppScreen.STATUS_VIEWER -> {
            activeStatus?.let { status ->
                StatusViewerScreen(
                    status = status,
                    onClose = { viewModel.closeStatusViewer() }
                )
            } ?: viewModel.closeStatusViewer()
        }

        AppScreen.ACTIVE_CALL -> {
            activeCallState?.let { callState ->
                ActiveCallScreen(
                    callState = callState,
                    onEndCall = { viewModel.endCall() },
                    onToggleMute = { viewModel.toggleCallMute() },
                    onToggleSpeaker = { viewModel.toggleCallSpeaker() }
                )
            } ?: viewModel.endCall()
        }

        AppScreen.NEW_CHAT -> {
            NewChatScreen(
                contacts = contacts,
                onBack = { viewModel.closeNewChat() },
                onSelectContact = { contact ->
                    // Open existing chat or create new
                    val existing = chats.firstOrNull { it.contactId == contact.id || it.title == contact.name }
                    if (existing != null) {
                        viewModel.closeNewChat()
                        viewModel.openChat(existing.id)
                    } else {
                        viewModel.createNewChat(contact.name, contact.phoneNumber)
                    }
                },
                onCreateNewContact = { name, phone ->
                    viewModel.createNewChat(name, phone)
                }
            )
        }

        AppScreen.SETTINGS -> {
            SettingsScreen(
                currentDarkMode = currentDarkMode,
                onSetDarkMode = { viewModel.setDarkMode(it) },
                onBack = { viewModel.closeSettings() },
                onOpenAccessibility = { viewModel.openAccessibilitySettings() }
            )
        }

        AppScreen.ACCESSIBILITY_SETTINGS -> {
            AccessibilitySettingsScreen(
                speakNewMessages = speakNewMessages,
                queueInSequence = queueInSequence,
                announceSender = announceSender,
                speechRate = speechRate,
                logs = accessibilityLogs,
                onToggleSpeakNewMessages = { viewModel.setSpeakNewMessages(it) },
                onToggleQueueInSequence = { viewModel.setQueueInSequence(it) },
                onToggleAnnounceSender = { viewModel.setAnnounceSender(it) },
                onSetSpeechRate = { viewModel.setSpeechRate(it) },
                onTestSpeechSequence = { viewModel.testAccessibilitySpeech() },
                onStopSpeech = { viewModel.stopSpeaking() },
                onSpeakLog = { viewModel.speakLog(it) },
                onClearLogs = { viewModel.clearAccessibilityLogs() },
                onBack = { viewModel.closeAccessibilitySettings() }
            )
        }

        AppScreen.CONTACT_INFO -> {
            if (currentChat != null) {
                ContactInfoScreen(
                    chat = currentChat,
                    onBack = { viewModel.closeContactInfo() },
                    onStartCall = { name, isVideo ->
                        viewModel.startCall(name, isVideo, currentChat.avatarColorHex)
                    }
                )
            } else {
                viewModel.closeContactInfo()
            }
        }
    }
}
