package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.ui.components.WhatsAppAvatar
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenDark

@Composable
fun CommunitiesScreen(
    onCommunityChatClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("communities_screen")
    ) {
        // "New community" row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* New community flow */ }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("new_community_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "New community",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "New community",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                thickness = 8.dp
            )
        }

        // Community 1: Campus Tech Club
        item {
            CommunitySection(
                name = "Campus Tech Club",
                avatarColor = "#008069",
                groups = listOf(
                    CommunityGroupItem("Announcements", "David: Hackathon registration is now open!", "10:30 AM", true),
                    CommunityGroupItem("General Discussion", "Alex: Who's attending the Kotlin meetup?", "Yesterday", false),
                    CommunityGroupItem("Mobile Devs Guild", "Sarah: Jetpack Compose 1.7 released! 🎉", "24/09/2026", false)
                ),
                onGroupClick = { title -> onCommunityChatClick(title) }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                thickness = 8.dp
            )
        }

        // Community 2: Green Hills Neighborhood
        item {
            CommunitySection(
                name = "Green Hills Neighborhood",
                avatarColor = "#128C7E",
                groups = listOf(
                    CommunityGroupItem("Announcements", "Notice: Street cleaning on Thursday", "Yesterday", true),
                    CommunityGroupItem("Neighbourhood Watch", "Everything quiet tonight. Stay safe!", "23/09/2026", false)
                ),
                onGroupClick = { title -> onCommunityChatClick(title) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

data class CommunityGroupItem(
    val name: String,
    val lastMessage: String,
    val time: String,
    val isAnnouncement: Boolean
)

@Composable
private fun CommunitySection(
    name: String,
    avatarColor: String,
    groups: List<CommunityGroupItem>,
    onGroupClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Community Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WhatsAppAvatar(
                name = name,
                colorHex = avatarColor,
                isGroup = true,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = name,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            thickness = 0.8.dp,
            modifier = Modifier.padding(start = 76.dp)
        )

        // Subgroups
        groups.forEach { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGroupClick(group.name) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (group.isAnnouncement) WhatsAppGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (group.isAnnouncement) Icons.Default.Campaign else Icons.Default.Group,
                        contentDescription = group.name,
                        tint = if (group.isAnnouncement) WhatsAppGreenDark else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = group.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = group.time,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = group.lastMessage,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        // "View all" row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* View all */ }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View all",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .padding(start = 10.dp)
            )
            Spacer(modifier = Modifier.width(26.dp))
            Text(
                text = "View all",
                fontSize = 14.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
