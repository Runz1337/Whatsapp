package com.familyguardian.app.ui.screens.supervision

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.familyguardian.app.data.models.DeviceRole
import com.familyguardian.app.ui.theme.WhatsAppGreenDark

@Composable
fun SupervisionRoleScreen(
    displayName: String,
    transportReady: Boolean?,
    onDisplayNameChange: (String) -> Unit,
    onRoleChosen: (DeviceRole) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selected by remember { mutableStateOf<DeviceRole?>(null) }
    val canContinue = selected != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("supervision_role_screen")
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Family supervision",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Set up this device as a parent viewer or a supervised child device.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = { Text("Your name on this device") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("supervision_name_field")
        )

        Spacer(modifier = Modifier.height(22.dp))

        RoleOption(
            icon = Icons.Default.SupervisorAccount,
            title = "Parent device",
            subtitle = "View a child's chats and activity, read-only. Cannot send or delete anything.",
            selected = selected == DeviceRole.PARENT,
            onSelect = { selected = DeviceRole.PARENT }
        )
        Spacer(modifier = Modifier.height(10.dp))
        RoleOption(
            icon = Icons.Default.ChildCare,
            title = "Child device",
            subtitle = "Share chats and activity with a parent who confirms a pairing code.",
            selected = selected == DeviceRole.CHILD,
            onSelect = { selected = DeviceRole.CHILD }
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (transportReady == false) {
            Text(
                text = "Cloud linking is not configured. Add google-services.json to the app module to pair two real devices.",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = { selected?.let(onRoleChosen) },
            enabled = canContinue,
            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenDark),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("supervision_continue")
        ) {
            Text("Continue", modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel", color = WhatsAppGreenDark)
        }
    }
}

@Composable
private fun RoleOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .testTag("supervision_role_${title.lowercase().replace(' ', '_')}")
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = WhatsAppGreenDark)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
