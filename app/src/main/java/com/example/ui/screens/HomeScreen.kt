package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.models.CallLogEntity
import com.example.data.models.ChannelEntity
import com.example.data.models.ChatEntity
import com.example.data.models.StatusEntity
import com.example.ui.components.WhatsAppBottomNavBar
import com.example.ui.components.WhatsAppTab
import com.example.ui.components.WhatsAppTopAppBar

@Composable
fun HomeScreen(
    currentTab: WhatsAppTab,
    onTabSelected: (WhatsAppTab) -> Unit,
    chats: List<ChatEntity>,
    activeFilter: String,
    onFilterSelect: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onNewChatClick: () -> Unit,
    statuses: List<StatusEntity>,
    channels: List<ChannelEntity>,
    onStatusClick: (StatusEntity) -> Unit,
    onAddStatus: (text: String, bgHex: String) -> Unit,
    onToggleFollowChannel: (String) -> Unit,
    calls: List<CallLogEntity>,
    onStartCall: (contactName: String, isVideo: Boolean) -> Unit,
    onNewCallClick: () -> Unit,
    onCommunityChatClick: (String) -> Unit,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onCameraClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAccessibilityClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalUnreadChats = chats.sumOf { it.unreadCount }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        topBar = {
            WhatsAppTopAppBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearchToggle = onSearchToggle,
                onCameraClick = onCameraClick,
                onSettingsClick = onSettingsClick,
                onNewGroupClick = onNewChatClick,
                onAccessibilityClick = onAccessibilityClick
            )
        },
        bottomBar = {
            WhatsAppBottomNavBar(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
                totalUnreadChats = totalUnreadChats,
                hasStatusUpdates = statuses.any { !it.isViewed && !it.isMyStatus }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                WhatsAppTab.CHATS -> {
                    ChatsScreen(
                        chats = chats,
                        activeFilter = activeFilter,
                        onFilterSelect = onFilterSelect,
                        onChatClick = onChatClick,
                        onDeleteChat = onDeleteChat,
                        onNewChatClick = onNewChatClick
                    )
                }

                WhatsAppTab.UPDATES -> {
                    UpdatesScreen(
                        statuses = statuses,
                        channels = channels,
                        onStatusClick = onStatusClick,
                        onAddStatus = onAddStatus,
                        onToggleFollowChannel = onToggleFollowChannel
                    )
                }

                WhatsAppTab.COMMUNITIES -> {
                    CommunitiesScreen(
                        onCommunityChatClick = onCommunityChatClick
                    )
                }

                WhatsAppTab.CALLS -> {
                    CallsScreen(
                        calls = calls,
                        onStartCall = onStartCall,
                        onNewCallClick = onNewCallClick
                    )
                }
            }
        }
    }
}
