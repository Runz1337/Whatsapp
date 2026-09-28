package com.familyguardian.app.ui.screens.supervision

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familyguardian.app.data.models.MirroredChatEntity
import com.familyguardian.app.data.models.SupervisedEventEntity
import com.familyguardian.app.data.models.SupervisionLinkEntity
import com.familyguardian.app.ui.theme.WhatsAppGreenDark
import com.familyguardian.app.ui.viewmodel.PairingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSupervisionScreen(
    children: List<SupervisionLinkEntity>,
    selectedLinkId: String?,
    chats: List<MirroredChatEntity>,
    events: List<SupervisedEventEntity>,
    pairing: PairingUiState,
    eventLabel: (String) -> String,
    onClaimCode: (String) -> Unit,
    onSelectChild: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onRevoke: (String) -> Unit,
    onChangeRole: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var codeInput by remember { mutableStateOf("") }
    var showClaim by remember { mutableStateOf(false) }
    val selectedChild = children.firstOrNull { it.linkId == selectedLinkId }

    Scaffold(
        modifier = Modifier.testTag("parent_supervision_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Parent device", fontSize = 19.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("parent_supervision_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SupervisionVisibilityNotice() }

            item {
                SectionCard(title = if (children.isEmpty()) "NO DEVICES LINKED" else "SUPERVISED DEVICES (${children.size})") {
                    if (children.isEmpty()) {
                        Text(
                            text = "Nothing linked yet. Enter the pairing code your child read out to you.",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        children.forEach { link ->
                            LinkStatusRow(link = link, isChildSide = false)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onSelectChild(link.linkId) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreenDark),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_child_${link.linkId}")
                            ) {
                                Text("View chats and activity")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { onRevoke(link.linkId) }) {
                                Text("Stop supervising", color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showClaim = true },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_claim_code")
                    ) {
                        Text("Link a child device")
                    }
                }
            }

            selectedChild?.let { link ->
                item {
                    SectionCard(title = "CHATS · ${link.childDisplayName.ifBlank { "Child" }}") {
                        if (chats.isEmpty()) {
                            Text(
                                text = "No chats shared yet.",
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            chats.forEach { chat ->
                                MirroredChatRow(
                                    chat = chat,
                                    onClick = { onOpenChat(chat.chatId) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }

                item {
                    SectionCard(title = "ACTIVITY") {
                        if (events.isEmpty()) {
                            Text(
                                text = "No activity recorded yet.",
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            events.take(EVENT_PREVIEW_LIMIT).forEach { event ->
                                EventRow(event = event, label = eventLabel(event.type))
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }

            item {
                TextButton(
                    onClick = onChangeRole,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parent_change_role")
                ) {
                    Text("Stop using this device as a parent device", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showClaim) {
        AlertDialog(
            onDismissRequest = { showClaim = false },
            title = { Text("Enter pairing code") },
            text = {
                Column {
                    Text(
                        text = "Ask the child device to create a code, then type it here.",
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.uppercase().take(24) },
                        singleLine = true,
                        label = { Text("Pairing code") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("claim_code_field")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    when (val state = pairing) {
                        PairingUiState.Working -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = WhatsAppGreenDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checking code…", fontSize = 13.sp)
                        }

                        is PairingUiState.Error -> Text(
                            text = state.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )

                        else -> Unit
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClaimCode(codeInput)
                        codeInput = ""
                    },
                    enabled = codeInput.isNotBlank() && pairing !is PairingUiState.Working,
                    modifier = Modifier.testTag("submit_claim_code")
                ) {
                    Text("Link", color = WhatsAppGreenDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClaim = false }) {
                    Text("Cancel", color = WhatsAppGreenDark)
                }
            }
        )
    }
}

private const val EVENT_PREVIEW_LIMIT = 40
