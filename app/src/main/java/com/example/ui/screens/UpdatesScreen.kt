package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
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
import com.example.data.models.ChannelEntity
import com.example.data.models.StatusEntity
import com.example.ui.components.WhatsAppAvatar
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenDark

@Composable
fun UpdatesScreen(
    statuses: List<StatusEntity>,
    channels: List<ChannelEntity>,
    onStatusClick: (StatusEntity) -> Unit,
    onAddStatus: (text: String, bgHex: String) -> Unit,
    onToggleFollowChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddStatusDialog by remember { mutableStateOf(false) }
    var newStatusText by remember { mutableStateOf("") }
    val statusColors = listOf("#075E54", "#128C7E", "#8E24AA", "#1E88E5", "#E91E63", "#FF6F00")
    var selectedColorIndex by remember { mutableStateOf(0) }

    val myStatus = statuses.firstOrNull { it.isMyStatus }
    val recentStatuses = statuses.filter { !it.isMyStatus && !it.isViewed }
    val viewedStatuses = statuses.filter { !it.isMyStatus && it.isViewed }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("updates_list")
        ) {
            // Section 1: Status Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { /* Status privacy options */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Status options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // My Status Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (myStatus != null) {
                                onStatusClick(myStatus)
                            } else {
                                showAddStatusDialog = true
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("my_status_item"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(54.dp)) {
                        WhatsAppAvatar(
                            name = "You",
                            colorHex = "#25D366",
                            size = 54.dp,
                            hasStatusRing = myStatus != null,
                            statusViewed = false
                        )
                        if (myStatus == null) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add status",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My status",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (myStatus != null) myStatus.timestampText else "Tap to add status update",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Recent Updates Header & Items
            if (recentStatuses.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent updates",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                items(recentStatuses, key = { it.id }) { status ->
                    StatusRowItem(status = status, onClick = { onStatusClick(status) })
                }
            }

            // Viewed Updates Header & Items
            if (viewedStatuses.isNotEmpty()) {
                item {
                    Text(
                        text = "Viewed updates",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                items(viewedStatuses, key = { it.id }) { status ->
                    StatusRowItem(status = status, onClick = { onStatusClick(status) })
                }
            }

            // Section 2: Channels
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Channels",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Stay updated on topics you care about",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Explore",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreenDark,
                        modifier = Modifier.clickable { /* Explore channels */ }
                    )
                }
            }

            // Channels List
            items(channels, key = { it.id }) { channel ->
                ChannelRowItem(
                    channel = channel,
                    onToggleFollow = { onToggleFollowChannel(channel.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }

        // Dual Floating Action Buttons: Pen (text status) & Camera (media status)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SmallFloatingActionButton(
                onClick = { showAddStatusDialog = true },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("fab_text_status")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Text status",
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            FloatingActionButton(
                onClick = {
                    onAddStatus("📷 Photo update from today! ✨", "#008069")
                },
                containerColor = WhatsAppGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_camera_status")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera status",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Add Status Dialog
        if (showAddStatusDialog) {
            AlertDialog(
                onDismissRequest = { showAddStatusDialog = false },
                title = { Text("New Status") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newStatusText,
                            onValueChange = { newStatusText = it },
                            placeholder = { Text("Type a status...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Background Color:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            statusColors.forEachIndexed { idx, hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColorIndex = idx }
                                        .border(
                                            width = if (selectedColorIndex == idx) 2.5.dp else 0.dp,
                                            color = if (selectedColorIndex == idx) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newStatusText.isNotBlank()) {
                                onAddStatus(newStatusText.trim(), statusColors[selectedColorIndex])
                                newStatusText = ""
                                showAddStatusDialog = false
                            }
                        }
                    ) {
                        Text("Post", color = WhatsAppGreenDark, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddStatusDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun StatusRowItem(
    status: StatusEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
            .testTag("status_item_${status.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WhatsAppAvatar(
            name = status.contactName,
            colorHex = status.contactAvatarColorHex,
            size = 54.dp,
            hasStatusRing = true,
            statusViewed = status.isViewed,
            statusSegments = 2
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = status.contactName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = status.timestampText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChannelRowItem(
    channel: ChannelEntity,
    onToggleFollow: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* open channel */ }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("channel_item_${channel.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WhatsAppAvatar(
            name = channel.name,
            colorHex = channel.avatarColorHex,
            size = 50.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channel.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = WhatsAppGreenDark,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = channel.lastUpdateText.ifEmpty { channel.followersText },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Follow / Following button
        if (channel.isFollowing) {
            OutlinedButton(
                onClick = onToggleFollow,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = WhatsAppGreenDark
                ),
                modifier = Modifier.testTag("channel_follow_${channel.id}")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Following",
                        tint = WhatsAppGreenDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Following", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            androidx.compose.material3.Button(
                onClick = onToggleFollow,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreenDark,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("channel_follow_${channel.id}")
            ) {
                Text("+ Follow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
