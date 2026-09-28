package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CallLogEntity
import com.example.ui.components.WhatsAppAvatar
import com.example.ui.theme.MissedCallRed
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenDark

@Composable
fun CallsScreen(
    calls: List<CallLogEntity>,
    onStartCall: (contactName: String, isVideo: Boolean) -> Unit,
    onNewCallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("calls_list")
        ) {
            // "Create call link" item
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Create call link */ }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("create_call_link_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Create call link",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create call link",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Share a link for your WhatsApp call",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // "Recent" Section header
            item {
                Text(
                    text = "Recent",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                )
            }

            // Call Logs
            items(calls, key = { it.id }) { call ->
                CallRowItem(
                    call = call,
                    onAudioCall = { onStartCall(call.contactName, false) },
                    onVideoCall = { onStartCall(call.contactName, true) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // New Call FAB
        FloatingActionButton(
            onClick = onNewCallClick,
            containerColor = WhatsAppGreen,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp)
                .testTag("fab_new_call")
        ) {
            Icon(
                imageVector = Icons.Default.AddIcCall,
                contentDescription = "New call",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CallRowItem(
    call: CallLogEntity,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = if (call.isVideo) onVideoCall else onAudioCall)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("call_item_${call.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WhatsAppAvatar(
            name = call.contactName,
            colorHex = call.contactAvatarColorHex,
            size = 50.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.contactName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (call.isMissed) MissedCallRed else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val arrowIcon = when {
                    call.isMissed -> Icons.AutoMirrored.Filled.CallMissed
                    call.isIncoming -> Icons.AutoMirrored.Filled.CallReceived
                    else -> Icons.AutoMirrored.Filled.CallMade
                }
                val arrowColor = if (call.isMissed) MissedCallRed else WhatsAppGreenDark

                Icon(
                    imageVector = arrowIcon,
                    contentDescription = "Call direction",
                    tint = arrowColor,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = call.timestampText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = if (call.isVideo) onVideoCall else onAudioCall,
            modifier = Modifier.testTag("call_action_${call.id}")
        ) {
            Icon(
                imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = if (call.isVideo) "Video call" else "Voice call",
                tint = WhatsAppGreenDark,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
