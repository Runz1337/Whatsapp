package com.familyguardian.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familyguardian.app.data.models.DeviceRole
import com.familyguardian.app.data.models.MirroredChatEntity
import com.familyguardian.app.data.models.MirroredMessageEntity
import com.familyguardian.app.data.models.SupervisedEventEntity
import com.familyguardian.app.data.models.SupervisionEventType
import com.familyguardian.app.data.models.SupervisionLinkEntity
import com.familyguardian.app.data.repository.PairingOutcome
import com.familyguardian.app.data.repository.SupervisionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SupervisionScreen { ROLE, CHILD_HOME, PARENT_HOME, MIRROR_CHAT }

sealed interface PairingUiState {
    data object Idle : PairingUiState
    data object Working : PairingUiState
    data class CodeReady(val code: String) : PairingUiState
    data class Linked(val childName: String) : PairingUiState
    data class Error(val message: String) : PairingUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class SupervisionViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SupervisionRepository.getInstance(application)

    private val _screen = MutableStateFlow(SupervisionScreen.ROLE)
    val screen: StateFlow<SupervisionScreen> = _screen

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName

    private val _pairing = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val pairing: StateFlow<PairingUiState> = _pairing

    private val _selectedLinkId = MutableStateFlow<String?>(null)
    val selectedLinkId: StateFlow<String?> = _selectedLinkId

    private val _selectedMirrorChatId = MutableStateFlow<String?>(null)
    val selectedMirrorChatId: StateFlow<String?> = _selectedMirrorChatId

    val role: StateFlow<DeviceRole> = repository.role
    val transportReady: StateFlow<Boolean?> = repository.transportReady
    val supervisedBy: StateFlow<List<SupervisionLinkEntity>> = repository.supervisedBy
    val supervisedChildren: StateFlow<List<SupervisionLinkEntity>> = repository.supervisedChildren

    val mirroredChats: StateFlow<List<MirroredChatEntity>> = _selectedLinkId
        .flatMapLatest { linkId ->
            if (linkId == null) flowOf(emptyList()) else repository.mirroredChats(linkId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mirroredMessages: StateFlow<List<MirroredMessageEntity>> =
        combine(_selectedLinkId, _selectedMirrorChatId) { linkId, chatId -> linkId to chatId }
            .flatMapLatest { (linkId, chatId) ->
                if (linkId == null || chatId == null) flowOf(emptyList())
                else repository.mirroredMessages(linkId, chatId)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeEvents: StateFlow<List<SupervisedEventEntity>> = _selectedLinkId
        .flatMapLatest { linkId ->
            if (linkId == null) flowOf(emptyList()) else repository.mirroredEvents(linkId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ready.first { it }
            val current = repository.role.value
            _displayName.value = repository.identity.value?.displayName.orEmpty()
            _screen.value = when (current) {
                DeviceRole.PARENT -> SupervisionScreen.PARENT_HOME
                DeviceRole.CHILD -> SupervisionScreen.CHILD_HOME
                DeviceRole.UNSET -> SupervisionScreen.ROLE
            }
        }
    }

    fun setDisplayName(name: String) {
        _displayName.value = name
    }

    fun chooseRole(newRole: DeviceRole) {
        if (newRole == DeviceRole.UNSET) return
        viewModelScope.launch {
            repository.setRole(newRole, _displayName.value)
            _screen.value = if (newRole == DeviceRole.PARENT) {
                SupervisionScreen.PARENT_HOME
            } else {
                SupervisionScreen.CHILD_HOME
            }
        }
    }

    fun issuePairingCode() {
        _pairing.value = PairingUiState.Working
        viewModelScope.launch {
            repository.ready.first { it }
            _pairing.value = when (val outcome = repository.issueChildPairingCode()) {
                is PairingOutcome.ChildCodeIssued -> PairingUiState.CodeReady(outcome.code)
                PairingOutcome.RemoteUnavailable ->
                    PairingUiState.Error("Add google-services.json to connect devices. Same-network pairing is not enabled yet.")
                PairingOutcome.Failed -> PairingUiState.Error("Could not create a pairing code. Try again.")
                else -> PairingUiState.Error("Unexpected pairing state.")
            }
        }
    }

    fun claimPairingCode(code: String) {
        _pairing.value = PairingUiState.Working
        viewModelScope.launch {
            repository.ready.first { it }
            _pairing.value = when (val outcome = repository.claimChildCode(code)) {
                is PairingOutcome.Linked -> {
                    selectChild(outcome.linkId)
                    PairingUiState.Linked(outcome.childDisplayName.ifBlank { "Child device" })
                }

                PairingOutcome.CodeNotFound -> PairingUiState.Error("That code was not recognised.")
                PairingOutcome.CodeAlreadyUsed ->
                    PairingUiState.Error("That code has already been used. Ask for a new one.")
                PairingOutcome.RemoteUnavailable ->
                    PairingUiState.Error("Add google-services.json to connect devices.")
                else -> PairingUiState.Error("Linking failed. Try again.")
            }
        }
    }

    fun clearPairingState() {
        _pairing.value = PairingUiState.Idle
    }

    fun selectChild(linkId: String) {
        _selectedLinkId.value = linkId
        _selectedMirrorChatId.value = null
        repository.startParentIngest(linkId)
    }

    fun openMirrorChat(chatId: String) {
        _selectedMirrorChatId.value = chatId
        _screen.value = SupervisionScreen.MIRROR_CHAT
    }

    fun closeMirrorChat() {
        _selectedMirrorChatId.value = null
        _screen.value = SupervisionScreen.PARENT_HOME
    }

    fun backToRolePicker() {
        _screen.value = SupervisionScreen.ROLE
    }

    fun revokeLink(linkId: String) {
        viewModelScope.launch {
            repository.revokeLink(linkId)
            if (_selectedLinkId.value == linkId) {
                _selectedLinkId.value = null
                _selectedMirrorChatId.value = null
            }
        }
    }

    fun changeRole() {
        viewModelScope.launch {
            repository.endToEndRevoke()
            _screen.value = SupervisionScreen.ROLE
        }
    }

    fun describeEvent(type: String): String = when (SupervisionEventType.entries.firstOrNull { it.name == type }) {
        SupervisionEventType.MESSAGE_SENT -> "Message sent"
        SupervisionEventType.MESSAGE_RECEIVED -> "Message received"
        SupervisionEventType.MESSAGE_DELETED -> "Message deleted"
        SupervisionEventType.MESSAGE_REACTION -> "Reaction"
        SupervisionEventType.MESSAGE_STARRED -> "Starred"
        SupervisionEventType.CHAT_OPENED -> "Chat opened"
        SupervisionEventType.CHAT_DELETED -> "Chat deleted"
        SupervisionEventType.CALL_STARTED -> "Call started"
        SupervisionEventType.CALL_ENDED -> "Call ended"
        SupervisionEventType.STATUS_ADDED -> "Status added"
        SupervisionEventType.CONTACT_ADDED -> "Contact added"
        SupervisionEventType.CHANNEL_FOLLOW_TOGGLED -> "Channel updated"
        SupervisionEventType.PAIRING_ACTIVATED -> "Device linked"
        SupervisionEventType.PAIRING_REVOKED -> "Supervision ended"
        else -> "Activity"
    }
}
