package com.familyguardian.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familyguardian.app.ui.components.MonitorBottomNavBar
import com.familyguardian.app.ui.components.MonitorTab
import com.familyguardian.app.ui.screens.CaptureLogScreen
import com.familyguardian.app.ui.screens.CapturedConversationScreen
import com.familyguardian.app.ui.screens.SettingsScreen
import com.familyguardian.app.ui.screens.supervision.ChildSupervisionScreen
import com.familyguardian.app.ui.screens.supervision.SupervisedBanner
import com.familyguardian.app.ui.screens.supervision.ParentSupervisionScreen
import com.familyguardian.app.ui.screens.supervision.SupervisionRoleScreen
import com.familyguardian.app.ui.screens.supervision.SupervisedChatViewerScreen
import com.familyguardian.app.ui.theme.MonitorTheme
import com.familyguardian.app.ui.viewmodel.AppViewModel
import com.familyguardian.app.ui.viewmodel.SupervisionScreen
import com.familyguardian.app.ui.viewmodel.SupervisionViewModel

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels {
        AppViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkModePref by appViewModel.isDarkMode.collectAsStateWithLifecycle()
            MonitorTheme(darkTheme = darkModePref ?: isSystemInDarkTheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MonitorApp(appViewModel = appViewModel)
                }
            }
        }
    }
}

private sealed interface MonitorDestination {
    data object Tabs : MonitorDestination
    data class CapturedChat(val contactId: String) : MonitorDestination
    data object Supervision : MonitorDestination
}

@Composable
private fun MonitorApp(appViewModel: AppViewModel) {
    val supervisionViewModel: SupervisionViewModel = viewModel()
    var destination by remember { mutableStateOf<MonitorDestination>(MonitorDestination.Tabs) }

    when (val current = destination) {
        is MonitorDestination.CapturedChat -> {
            BackHandler { destination = MonitorDestination.Tabs }
            CapturedConversationScreen(
                contactId = current.contactId,
                onBack = { destination = MonitorDestination.Tabs }
            )
        }

        is MonitorDestination.Supervision -> {
            BackHandler { destination = MonitorDestination.Tabs }
            SupervisionHost(
                supervisionViewModel = supervisionViewModel,
                onBack = { destination = MonitorDestination.Tabs }
            )
        }

        MonitorDestination.Tabs -> {
            var selectedTab by remember { mutableStateOf(MonitorTab.LOG) }
            Scaffold(
                bottomBar = {
                    MonitorBottomNavBar(
                        selected = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    val supervisedBy by supervisionViewModel.supervisedBy.collectAsStateWithLifecycle()
                    val darkMode by appViewModel.isDarkMode.collectAsStateWithLifecycle()
                    val supervisorName = supervisedBy.firstOrNull()?.parentDisplayName
                    if (!supervisorName.isNullOrBlank()) {
                        SupervisedBanner(
                            parentName = supervisorName,
                            onClick = { destination = MonitorDestination.Supervision }
                        )
                    }

                    when (selectedTab) {
                        MonitorTab.LOG -> CaptureLogScreen(
                            onOpenContact = { contactId ->
                                destination = MonitorDestination.CapturedChat(contactId)
                            }
                        )

                        MonitorTab.SUPERVISION -> SupervisionHost(
                            supervisionViewModel = supervisionViewModel,
                            onBack = { selectedTab = MonitorTab.LOG }
                        )

                        MonitorTab.SETTINGS -> SettingsScreen(
                            currentDarkMode = darkMode,
                            onSetDarkMode = appViewModel::setDarkMode,
                            onOpenSupervision = { destination = MonitorDestination.Supervision }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SupervisionHost(
    supervisionViewModel: SupervisionViewModel,
    onBack: () -> Unit
) {
    val supervisionScreen by supervisionViewModel.screen.collectAsStateWithLifecycle()
    val supervisionName by supervisionViewModel.displayName.collectAsStateWithLifecycle()
    val transportReady by supervisionViewModel.transportReady.collectAsStateWithLifecycle()
    val pairingState by supervisionViewModel.pairing.collectAsStateWithLifecycle()
    val supervisedBy by supervisionViewModel.supervisedBy.collectAsStateWithLifecycle()
    val supervisedChildren by supervisionViewModel.supervisedChildren.collectAsStateWithLifecycle()
    val selectedLinkId by supervisionViewModel.selectedLinkId.collectAsStateWithLifecycle()
    val selectedMirrorChatId by supervisionViewModel.selectedMirrorChatId.collectAsStateWithLifecycle()
    val mirroredChats by supervisionViewModel.mirroredChats.collectAsStateWithLifecycle()
    val mirroredMessages by supervisionViewModel.mirroredMessages.collectAsStateWithLifecycle()
    val supervisionEvents by supervisionViewModel.activeEvents.collectAsStateWithLifecycle()

    when (supervisionScreen) {
        SupervisionScreen.ROLE -> SupervisionRoleScreen(
            displayName = supervisionName,
            transportReady = transportReady,
            onDisplayNameChange = supervisionViewModel::setDisplayName,
            onRoleChosen = supervisionViewModel::chooseRole,
            onBack = onBack
        )

        SupervisionScreen.CHILD_HOME -> ChildSupervisionScreen(
            links = supervisedBy,
            pairing = pairingState,
            onIssueCode = supervisionViewModel::issuePairingCode,
            onRevoke = supervisionViewModel::revokeLink,
            onChangeRole = supervisionViewModel::changeRole,
            onBack = onBack
        )

        SupervisionScreen.PARENT_HOME -> ParentSupervisionScreen(
            children = supervisedChildren,
            selectedLinkId = selectedLinkId,
            chats = mirroredChats,
            events = supervisionEvents,
            pairing = pairingState,
            eventLabel = supervisionViewModel::describeEvent,
            onClaimCode = supervisionViewModel::claimPairingCode,
            onSelectChild = supervisionViewModel::selectChild,
            onOpenChat = supervisionViewModel::openMirrorChat,
            onRevoke = supervisionViewModel::revokeLink,
            onChangeRole = supervisionViewModel::changeRole,
            onBack = onBack
        )

        SupervisionScreen.MIRROR_CHAT -> SupervisedChatViewerScreen(
            chat = mirroredChats.firstOrNull { it.chatId == selectedMirrorChatId },
            messages = mirroredMessages,
            onBack = supervisionViewModel::closeMirrorChat
        )
    }
}
