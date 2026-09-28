package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ChatEntity
import com.example.ui.components.WhatsAppAvatar
import com.example.ui.theme.BlueCheckmark
import com.example.ui.theme.GrayCheckmark
import com.example.ui.theme.UnreadBadgeGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenDark

@Composable
fun ChatsScreen(
    chats: List<ChatEntity>,
    activeFilter: String,
    onFilterSelect: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var chatToDelete by remember { mutableStateOf<ChatEntity?>(null) }
    val filterOptions = listOf("All", "Unread", "Favourites", "Groups")

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter Pills Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { filter ->
                    val isSelected = activeFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSelect(filter) },
                        label = {
                            Text(
                                text = filter,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = if (MaterialTheme.colorScheme.background.red < 0.2f) Color(0xFF103629) else Color(0xFFD8FDD2),
                            selectedLabelColor = WhatsAppGreenDark
                        ),
                        border = null,
                        modifier = Modifier.testTag("filter_$filter")
                    )
                }
            }

            // Archived row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Archived folder */ }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("archived_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Archived",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = "Archived",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "2",
                    fontSize = 12.sp,
                    color = WhatsAppGreenDark,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Chats List
            if (chats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No chats found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chats_list")
                ) {
                    items(chats, key = { it.id }) { chat ->
                        ChatItemRow(
                            chat = chat,
                            onClick = { onChatClick(chat.id) },
                            onLongClick = { chatToDelete = chat }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Floating Action Button for New Chat
        FloatingActionButton(
            onClick = onNewChatClick,
            containerColor = WhatsAppGreen,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp)
                .testTag("fab_new_chat")
        ) {
            Icon(
                imageVector = Icons.Default.DoneAll, // Message with plus / compose
                contentDescription = "New chat",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Delete Chat Confirmation Dialog
        chatToDelete?.let { chat ->
            AlertDialog(
                onDismissRequest = { chatToDelete = null },
                title = { Text("Delete this chat?") },
                text = { Text("Messages from \"${chat.title}\" will be permanently deleted.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteChat(chat.id)
                            chatToDelete = null
                        }
                    ) {
                        Text("Delete", color = Color(0xFFF15C6D))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { chatToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatItemRow(
    chat: ChatEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .testTag("chat_item_${chat.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        WhatsAppAvatar(
            name = chat.title,
            colorHex = chat.avatarColorHex,
            isGroup = chat.isGroup,
            size = 52.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Center: Title + Last message
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = chat.lastMessageTime,
                    fontSize = 12.sp,
                    color = if (chat.unreadCount > 0) WhatsAppGreenDark else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Read tick marks if outgoing
                if (chat.unreadCount == 0 && !chat.isGroup) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Read",
                        tint = BlueCheckmark,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                }

                // Message snippet or audio/photo icon
                if (chat.lastMessage.startsWith("🎤")) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice note",
                        tint = WhatsAppGreenDark,
                        modifier = Modifier
                            .size(15.dp)
                            .padding(end = 2.dp)
                    )
                } else if (chat.lastMessage.startsWith("📷")) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Photo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(15.dp)
                            .padding(end = 2.dp)
                    )
                }

                Text(
                    text = chat.lastMessage,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Icons on right: Mute, Pin, Unread Badge
                if (chat.isMuted) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = "Muted",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(horizontal = 2.dp)
                    )
                }

                if (chat.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(horizontal = 2.dp)
                    )
                }

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(UnreadBadgeGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
