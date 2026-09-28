package com.familyguardian.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familyguardian.app.R
import com.familyguardian.app.capture.NotificationAccess
import com.familyguardian.app.data.repository.CaptureRepository

@Composable
fun SettingsScreen(
    currentDarkMode: Boolean?,
    onSetDarkMode: (Boolean?) -> Unit,
    onOpenSupervision: () -> Unit,
    modifier: Modifier = Modifier,
    logViewModel: CaptureLogViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = CaptureLogViewModel.Factory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = stringResource(R.string.tab_settings),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            item { SectionHeader(stringResource(R.string.settings_section_privacy)) }

            item {
                SettingRow(
                    icon = Icons.Default.NotificationsActive,
                    title = stringResource(R.string.log_permission),
                    subtitle = stringResource(R.string.settings_notification_access_summary),
                    onClick = { context.startActivity(NotificationAccess.settingsIntent()) },
                    tag = "setting_notification_access"
                )
            }

            item {
                SettingRow(
                    icon = Icons.Default.FamilyRestroom,
                    title = stringResource(R.string.settings_supervision),
                    subtitle = stringResource(R.string.settings_supervision_summary),
                    onClick = onOpenSupervision,
                    tag = "setting_supervision"
                )
            }

            item { SectionHeader(stringResource(R.string.settings_section_appearance)) }

            item {
                SettingRow(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_theme),
                    subtitle = themeLabel(currentDarkMode),
                    onClick = { showThemeDialog = true },
                    tag = "setting_theme"
                )
            }

            item { SectionHeader(stringResource(R.string.settings_section_data)) }

            item {
                SettingRow(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.log_clear),
                    subtitle = stringResource(R.string.settings_clear_summary),
                    onClick = { showClearDialog = true },
                    tag = "setting_clear_log"
                )
            }

            item { SectionHeader(stringResource(R.string.settings_section_about)) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_about_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        val options = listOf(
            stringResource(R.string.theme_system) to null as Boolean?,
            stringResource(R.string.theme_light) to false,
            stringResource(R.string.theme_dark) to true
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    options.forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = currentDarkMode == value,
                                    onClick = {
                                        onSetDarkMode(value)
                                        showThemeDialog = false
                                    }
                                )
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentDarkMode == value, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.log_clear)) },
            text = { Text(stringResource(R.string.clear_log_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        logViewModel.clearLog()
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun themeLabel(currentDarkMode: Boolean?): String = when (currentDarkMode) {
    null -> stringResource(R.string.theme_system)
    false -> stringResource(R.string.theme_light)
    true -> stringResource(R.string.theme_dark)
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
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
    HorizontalDivider(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        thickness = 0.8.dp
    )
}
