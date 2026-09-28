package com.familyguardian.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familyguardian.app.data.models.CapturedContactEntity
import com.familyguardian.app.data.models.CapturedMessageEntity
import com.familyguardian.app.data.repository.CaptureRepository
import com.familyguardian.app.data.repository.SupervisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CapturedConversationViewModel(
    private val capture: CaptureRepository,
    private val supervision: SupervisionRepository,
    private val contactId: String
) : ViewModel() {

    val messages: StateFlow<List<CapturedMessageEntity>> = capture.messagesFor(contactId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _contact = MutableStateFlow<CapturedContactEntity?>(null)
    val contact: StateFlow<CapturedContactEntity?> = _contact

    init {
        viewModelScope.launch {
            _contact.value = capture.contacts.value.firstOrNull { it.id == contactId }
        }
        viewModelScope.launch {
            val name = _contact.value?.displayName ?: contactId
            supervision.recordChatOpened(name)
        }
    }

    class Factory(private val context: Context, private val contactId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val capture = CaptureRepository.getInstance(context.applicationContext)
            val supervision = SupervisionRepository.getInstance(context.applicationContext)
            return CapturedConversationViewModel(capture, supervision, contactId) as T
        }
    }
}
