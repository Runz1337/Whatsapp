package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NavIndicatorDark
import com.example.ui.theme.NavIndicatorLight
import com.example.ui.theme.UnreadBadgeGreen
import com.example.ui.theme.WhatsAppGreenDark

enum class WhatsAppTab(val title: String) {
    CHATS("Chats"),
    UPDATES("Updates"),
    COMMUNITIES("Communities"),
    CALLS("Calls")
}

@Composable
fun WhatsAppBottomNavBar(
    currentTab: WhatsAppTab,
    onTabSelected: (WhatsAppTab) -> Unit,
    totalUnreadChats: Int = 0,
    hasStatusUpdates: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val indicatorColor = if (isDark) NavIndicatorDark else NavIndicatorLight

    NavigationBar(
        modifier = modifier
            .testTag("whatsapp_bottom_navigation")
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        // Tab 1: Chats
        NavigationBarItem(
            selected = currentTab == WhatsAppTab.CHATS,
            onClick = { onTabSelected(WhatsAppTab.CHATS) },
            modifier = Modifier.testTag("nav_tab_chats"),
            icon = {
                BadgedBox(
                    badge = {
                        if (totalUnreadChats > 0) {
                            Badge(
                                containerColor = UnreadBadgeGreen,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = if (totalUnreadChats > 99) "99+" else totalUnreadChats.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (currentTab == WhatsAppTab.CHATS) Icons.Filled.Chat else Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Chats"
                    )
                }
            },
            label = {
                Text(
                    text = "Chats",
                    fontWeight = if (currentTab == WhatsAppTab.CHATS) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = indicatorColor,
                selectedIconColor = WhatsAppGreenDark,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // Tab 2: Updates
        NavigationBarItem(
            selected = currentTab == WhatsAppTab.UPDATES,
            onClick = { onTabSelected(WhatsAppTab.UPDATES) },
            modifier = Modifier.testTag("nav_tab_updates"),
            icon = {
                BadgedBox(
                    badge = {
                        if (hasStatusUpdates) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(UnreadBadgeGreen)
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (currentTab == WhatsAppTab.UPDATES) Icons.Filled.Update else Icons.Outlined.Update,
                        contentDescription = "Updates"
                    )
                }
            },
            label = {
                Text(
                    text = "Updates",
                    fontWeight = if (currentTab == WhatsAppTab.UPDATES) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = indicatorColor,
                selectedIconColor = WhatsAppGreenDark,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // Tab 3: Communities
        NavigationBarItem(
            selected = currentTab == WhatsAppTab.COMMUNITIES,
            onClick = { onTabSelected(WhatsAppTab.COMMUNITIES) },
            modifier = Modifier.testTag("nav_tab_communities"),
            icon = {
                Icon(
                    imageVector = if (currentTab == WhatsAppTab.COMMUNITIES) Icons.Filled.Groups else Icons.Outlined.Groups,
                    contentDescription = "Communities"
                )
            },
            label = {
                Text(
                    text = "Communities",
                    fontWeight = if (currentTab == WhatsAppTab.COMMUNITIES) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = indicatorColor,
                selectedIconColor = WhatsAppGreenDark,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // Tab 4: Calls
        NavigationBarItem(
            selected = currentTab == WhatsAppTab.CALLS,
            onClick = { onTabSelected(WhatsAppTab.CALLS) },
            modifier = Modifier.testTag("nav_tab_calls"),
            icon = {
                Icon(
                    imageVector = if (currentTab == WhatsAppTab.CALLS) Icons.Filled.Call else Icons.Outlined.Call,
                    contentDescription = "Calls"
                )
            },
            label = {
                Text(
                    text = "Calls",
                    fontWeight = if (currentTab == WhatsAppTab.CALLS) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = indicatorColor,
                selectedIconColor = WhatsAppGreenDark,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
