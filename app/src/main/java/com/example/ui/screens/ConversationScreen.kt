package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ChatEntity
import com.example.data.models.MessageEntity
import com.example.ui.components.AttachmentBottomSheet
import com.example.ui.components.EmojiReactionRow
import com.example.ui.components.WhatsAppAvatar
import com.example.ui.components.WhatsAppDoodleBackground
import com.example.ui.theme.AudioWaveformGreen
import com.example.ui.theme.AudioWaveformTrack
import com.example.ui.theme.BlueCheckmark
import com.example.ui.theme.BubbleReceivedDark
import com.example.ui.theme.BubbleReceivedLight
import com.example.ui.theme.BubbleSentDark
import com.example.ui.theme.BubbleSentLight
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    chat: ChatEntity,
    messages: List<MessageEntity>,
    onBack: () -> Unit,
    onSendMessage: (text: String, type: String, duration: String?, replyTo: String?) -> Unit,
    onReactMessage: (messageId: Long, emoji: String) -> Unit,
    onDeleteMessage: (messageId: Long) -> Unit,
    onToggleStar: (messageId: Long) -> Unit,
    onStartCall: (contactName: String, isVideo: Boolean) -> Unit,
    onOpenContactInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var inputText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var selectedMessageForReaction by remember { mutableStateOf<MessageEntity?>(null) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var audioRecordSeconds by remember { mutableStateOf(0) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var topMenuExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val attachmentSheetState = rememberModalBottomSheetState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Audio recording timer simulation
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            audioRecordSeconds = 0
            while (isRecordingAudio) {
                delay(1000)
                audioRecordSeconds += 1
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("conversation_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Top App Bar
            TopAppBar(
                modifier = Modifier.testTag("chat_top_bar"),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onBack)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        WhatsAppAvatar(
                            name = chat.title,
                            colorHex = chat.avatarColorHex,
                            isGroup = chat.isGroup,
                            size = 38.dp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                },
                title = {
                    Column(
                        modifier = Modifier
                            .clickable(onClick = onOpenContactInfo)
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text = chat.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (chat.isGroup) "${chat.groupMembersCount} members" else "online",
                            fontSize = 12.sp,
                            color = WhatsAppGreenDark
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onStartCall(chat.title, true) },
                        modifier = Modifier.testTag("chat_video_call")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video call",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { onStartCall(chat.title, false) },
                        modifier = Modifier.testTag("chat_voice_call")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Voice call",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box {
                        IconButton(onClick = { topMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = topMenuExpanded,
                            onDismissRequest = { topMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View contact") },
                                onClick = {
                                    topMenuExpanded = false
                                    onOpenContactInfo()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Media, links, and docs") },
                                onClick = { topMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Search") },
                                onClick = { topMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Mute notifications") },
                                onClick = { topMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Disappearing messages") },
                                onClick = { topMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear chat") },
                                onClick = { topMenuExpanded = false }
                            )
                        }
                    }
                }
            )

            // Chat Wallpaper + Message List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WhatsAppDoodleBackground {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                            .testTag("messages_list")
                    ) {
                        // End-to-end encryption notice pill
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = if (MaterialTheme.colorScheme.background.red < 0.2f) Color(0xFF182229) else Color(0xFFFCF4CB),
                                    shape = RoundedCornerShape(8.dp),
                                    shadowElevation = 0.5.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Encrypted",
                                            tint = if (MaterialTheme.colorScheme.background.red < 0.2f) Color(0xFFF7C85C) else Color(0xFF5E5434),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Messages and calls are end-to-end encrypted. No one outside of this chat can read or listen to them.",
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp,
                                            color = if (MaterialTheme.colorScheme.background.red < 0.2f) Color(0xFFF7C85C) else Color(0xFF5E5434)
                                        )
                                    }
                                }
                            }
                        }

                        // Date badge "TODAY"
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = if (MaterialTheme.colorScheme.background.red < 0.2f) Color(0xFF182229) else Color(0xFFE1F2FB),
                                    shape = RoundedCornerShape(8.dp),
                                    shadowElevation = 1.dp
                                ) {
                                    Text(
                                        text = "TODAY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Message bubbles
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                isGroup = chat.isGroup,
                                onLongPress = { selectedMessageForReaction = message },
                                onReplyClick = { replyingToMessage = message }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }

                // Reaction floating bar if message selected
                selectedMessageForReaction?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .pointerInput(Unit) {
                                detectTapGestures { selectedMessageForReaction = null }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            EmojiReactionRow(
                                onSelectEmoji = { emoji ->
                                    onReactMessage(msg.id, emoji)
                                    selectedMessageForReaction = null
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // Actions bar: Reply, Star, Delete
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    IconButton(onClick = {
                                        replyingToMessage = msg
                                        selectedMessageForReaction = null
                                    }) {
                                        Icon(Icons.Default.Reply, contentDescription = "Reply")
                                    }
                                    IconButton(onClick = {
                                        onToggleStar(msg.id)
                                        selectedMessageForReaction = null
                                    }) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "Star",
                                            tint = if (msg.isStarred) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(onClick = {
                                        onDeleteMessage(msg.id)
                                        selectedMessageForReaction = null
                                    }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFF15C6D)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Replying banner preview
            replyingToMessage?.let { replyMsg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(36.dp)
                            .background(WhatsAppGreenDark, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = replyMsg.senderName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WhatsAppGreenDark
                        )
                        Text(
                            text = replyMsg.text,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { replyingToMessage = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel reply",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRecordingAudio) {
                    // Recording Audio UI Bar
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Recording",
                            tint = Color(0xFFF15C6D),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "0:${if (audioRecordSeconds < 10) "0$audioRecordSeconds" else "$audioRecordSeconds"}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Slide to cancel",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { isRecordingAudio = false }
                        )
                    }
                } else {
                    // Normal Input Pill
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { inputText += "😊" },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mood,
                                contentDescription = "Emoji",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(WhatsAppGreenDark),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (inputText.isEmpty()) {
                                        Text(
                                            text = "Message",
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .testTag("message_input_field")
                        )

                        IconButton(
                            onClick = { showAttachmentSheet = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (inputText.isEmpty()) {
                            IconButton(
                                onClick = {
                                    // Quick camera picture simulation
                                    onSendMessage("📷 Photo", "IMAGE", null, null)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Green Round Action Button (Send or Mic)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(WhatsAppGreen)
                        .clickable {
                            if (isRecordingAudio) {
                                // Finish audio record and send
                                isRecordingAudio = false
                                val dur = "0:${if (audioRecordSeconds < 10) "0$audioRecordSeconds" else "$audioRecordSeconds"}"
                                onSendMessage("Voice message", "VOICE", dur, replyingToMessage?.text)
                                replyingToMessage = null
                            } else if (inputText.isNotBlank()) {
                                onSendMessage(inputText.trim(), "TEXT", null, replyingToMessage?.text)
                                inputText = ""
                                replyingToMessage = null
                            } else {
                                // Tap mic: simulate starting voice recording
                                isRecordingAudio = true
                            }
                        }
                        .testTag("action_send_or_mic"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecordingAudio || inputText.isNotBlank()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice note",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Attachment Sheet
        if (showAttachmentSheet) {
            AttachmentBottomSheet(
                sheetState = attachmentSheetState,
                onDismiss = { showAttachmentSheet = false },
                onSelectOption = { option ->
                    showAttachmentSheet = false
                    when (option) {
                        "DOC" -> onSendMessage("Project_Report_Final.pdf (2.4 MB)", "DOC", null, null)
                        "IMAGE" -> onSendMessage("Sunset at the coast", "IMAGE", null, null)
                        "AUDIO" -> onSendMessage("Voice recording", "VOICE", "0:45", null)
                        "LOCATION" -> onSendMessage("📍 Current location: Central Park West", "TEXT", null, null)
                        "CONTACT" -> onSendMessage("👤 Contact: Michael Scott (+1 555-0199)", "TEXT", null, null)
                        "POLL" -> onSendMessage("📊 Poll: Where should we meet for lunch? \n1. Café \n2. Pizzeria", "TEXT", null, null)
                    }
                }
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    isGroup: Boolean,
    onLongPress: () -> Unit,
    onReplyClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val bubbleColor = if (message.isOutgoing) {
        if (isDark) BubbleSentDark else BubbleSentLight
    } else {
        if (isDark) BubbleReceivedDark else BubbleReceivedLight
    }

    var isPlayingAudio by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlayingAudio) {
        if (isPlayingAudio) {
            audioProgress = 0f
            while (audioProgress < 1f && isPlayingAudio) {
                delay(100)
                audioProgress += 0.05f
            }
            isPlayingAudio = false
            audioProgress = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        contentAlignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Surface(
                color = bubbleColor,
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (message.isOutgoing) 14.dp else 2.dp,
                    bottomEnd = if (message.isOutgoing) 2.dp else 14.dp
                ),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .pointerInput(message.id) {
                        detectTapGestures(
                            onLongPress = { onLongPress() },
                            onDoubleTap = { onReplyClick() }
                        )
                    }
                    .testTag("bubble_${message.id}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) {
                    // Group sender name if incoming
                    if (isGroup && !message.isOutgoing) {
                        Text(
                            text = message.senderName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhatsAppGreenDark,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // Replied-to header quote
                    if (!message.replyToText.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(28.dp)
                                        .background(WhatsAppGreenDark, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = message.replyToText,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Message Content by Type
                    when (message.messageType) {
                        "VOICE" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = { isPlayingAudio = !isPlayingAudio },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play voice note",
                                        tint = AudioWaveformGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    LinearProgressIndicator(
                                        progress = { audioProgress },
                                        color = AudioWaveformGreen,
                                        trackColor = AudioWaveformTrack,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = message.mediaDuration ?: "0:15",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(AudioWaveformGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Mic",
                                        tint = AudioWaveformGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        "IMAGE" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF263238)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Photo",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Photo",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (message.text.isNotBlank() && message.text != "📷 Photo") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.text,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        "DOC" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF5F66CD)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message.text,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        else -> {
                            Text(
                                text = message.text,
                                fontSize = 14.5.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Timestamp + Ticks row
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.isStarred) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Starred",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier
                                    .size(12.dp)
                                    .padding(end = 3.dp)
                            )
                        }
                        Text(
                            text = message.timeFormatted,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (message.isOutgoing) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read",
                                tint = if (message.status == "READ") BlueCheckmark else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Attached reaction emoji badge
            if (!message.reaction.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .padding(top = 2.dp, start = 4.dp, end = 4.dp)
                        .testTag("reaction_badge_${message.id}")
                ) {
                    Text(
                        text = message.reaction,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
