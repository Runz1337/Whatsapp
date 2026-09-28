package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.WhatsAppAvatar
import com.example.ui.theme.WhatsAppGreenDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentDarkMode: Boolean?,
    onSetDarkMode: (Boolean?) -> Unit,
    onBack: () -> Unit,
    onOpenAccessibility: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { /* search */ }) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Profile Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* profile */ }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("profile_card"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WhatsAppAvatar(
                        name = "Ayush",
                        colorHex = "#25D366",
                        size = 62.dp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ayush",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Hey there! I am using WhatsApp.",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { /* qr code */ }) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Code",
                            tint = WhatsAppGreenDark,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    thickness = 0.8.dp
                )
            }

            // Setting items
            item {
                SettingRow(
                    icon = Icons.Default.Key,
                    title = "Account",
                    subtitle = "Security notifications, change number",
                    onClick = { /* account */ }
                )
            }

            item {
                SettingRow(
                    icon = Icons.Default.Lock,
                    title = "Privacy",
                    subtitle = "Block contacts, disappearing messages",
                    onClick = { /* privacy */ }
                )
            }

            item {
                SettingRow(
                    icon = Icons.Default.Chat,
                    title = "Chats",
                    subtitle = "Theme, wallpapers, chat history",
                    onClick = { showThemeDialog = true },
                    tag = "setting_chats"
                )
            }

            item {
                SettingRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Message, group & call tones",
                    onClick = { /* notifications */ }
                )
            }

            item {
                SettingRow(
                    icon = Icons.Default.Accessibility,
                    title = "Accessibility",
                    subtitle = "Voice readout, speak messages in sequence, logs",
                    onClick = onOpenAccessibility,
                    tag = "setting_accessibility"
                )
            }

            item {
                SettingRow(
                    icon = Icons.Default.Storage,
                    title = "Storage and data",
                    subtitle = "Network usage, auto-download",
                    onClick = { /* storage */ }
                )
            }

            item {
                SettingRow(
                    icon = Icons.Default.Language,
                    title = "App language",
                    subtitle = "English (device's language)",
                    onClick = { /* language */ }
                )
            }

            item {
                SettingRow(
                    icon = Icons.Default.HelpOutline,
                    title = "Help",
                    subtitle = "Help centre, contact us, privacy policy",
                    onClick = { /* help */ }
                )
            }

            item {
                SettingRow(
                    icon = Icons.Default.People,
                    title = "Invite a friend",
                    subtitle = "",
                    onClick = { /* invite */ }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "from",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Meta",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Theme Dialog
        if (showThemeDialog) {
            val options = listOf("System default", "Light", "Dark")
            val currentSelected = when (currentDarkMode) {
                null -> "System default"
                false -> "Light"
                true -> "Dark"
            }

            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Choose theme") },
                text = {
                    Column {
                        options.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = option == currentSelected,
                                        onClick = {
                                            when (option) {
                                                "System default" -> onSetDarkMode(null)
                                                "Light" -> onSetDarkMode(false)
                                                "Dark" -> onSetDarkMode(true)
                                            }
                                            showThemeDialog = false
                                        }
                                    )
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = option == currentSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = WhatsAppGreenDark
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = option,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Cancel", color = WhatsAppGreenDark)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String = "setting_${title.lowercase()}"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(22.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
