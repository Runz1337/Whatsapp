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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familyguardian.app.data.models.SupervisionLinkEntity
import com.familyguardian.app.ui.theme.WhatsAppGreenDark
import com.familyguardian.app.ui.viewmodel.PairingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildSupervisionScreen(
    links: List<SupervisionLinkEntity>,
    pairing: PairingUiState,
    onIssueCode: () -> Unit,
    onRevoke: (String) -> Unit,
    onChangeRole: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val clipboard = LocalClipboardManager.current
    var pendingRevoke by remember { mutableStateOf<SupervisionLinkEntity?>(null) }

    Scaffold(
        modifier = Modifier.testTag("child_supervision_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Child device", fontSize = 19.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("child_supervision_back")) {
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
            item {
                SectionCard(title = "WHAT A PARENT CAN SEE") {
                    Text(
                        text = "While linked, the parent device can read your chats, messages, calls and status updates. " +
                            "It cannot send messages, edit or delete anything, and it cannot see your pairing code. " +
                            "You can end this at any time below.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SectionCard(title = if (links.isEmpty()) "NOT LINKED" else "LINKED DEVICES (${links.size})") {
                    if (links.isEmpty()) {
                        Text(
                            text = "No parent device is linked. Create a code and read it out to your parent.",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        links.forEach { link ->
                            LinkStatusRow(link = link, isChildSide = true)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { pendingRevoke = link },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("revoke_${link.linkId}")
                            ) {
                                Text("Remove ${link.parentDisplayName.ifBlank { "this parent" }}", color = WhatsAppGreenDark)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                SectionCard(title = "PAIR A PARENT DEVICE") {
                    when (val state = pairing) {
                        is PairingUiState.CodeReady -> {
                            Text(
                                text = "Give this code to your parent. It works once only.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = state.code,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppGreenDark,
                                modifier = Modifier.testTag("pairing_code")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = {
                        clipboard.setText(AnnotatedString(state.code))
                    },
                                    modifier = Modifier.testTag("copy_pairing_code")
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copy code", color = WhatsAppGreenDark)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                TextButton(onClick = onIssueCode) {
                                    Text("New code", color = WhatsAppGreenDark)
                                }
                            }
                        }

                        PairingUiState.Working -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = WhatsAppGreenDark
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Working…", fontSize = 13.5.sp)
                        }

                        is PairingUiState.Error -> {
                            Text(
                                text = state.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onIssueCode,
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenDark)
                            ) {
                                Text("Try again")
                            }
                        }

                        PairingUiState.Idle, is PairingUiState.Linked -> Button(
                            onClick = onIssueCode,
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("issue_pairing_code")
                        ) {
                            Text("Create pairing code")
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
                        .testTag("change_role")
                ) {
                    Text("Stop using this device as a child device", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    pendingRevoke?.let { link ->
        AlertDialog(
            onDismissRequest = { pendingRevoke = null },
            title = { Text("Remove ${link.parentDisplayName.ifBlank { "this parent" }}?") },
            text = {
                Text("They will lose access immediately and the mirrored copies on their device are deleted.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onRevoke(link.linkId)
                    pendingRevoke = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRevoke = null }) {
                    Text("Cancel", color = WhatsAppGreenDark)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisionVisibilityNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Visibility,
            contentDescription = null,
            tint = WhatsAppGreenDark,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Read-only view. Nothing you do here reaches the child device.",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
