package com.familyguardian.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familyguardian.app.capture.NotificationAccess
import com.familyguardian.app.data.models.CapturedContactEntity
import com.familyguardian.app.data.models.CapturedMessageEntity
import com.familyguardian.app.data.repository.CaptureRepository
import com.familyguardian.app.data.repository.SupervisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CaptureLogViewModel(
    private val capture: CaptureRepository,
    private val supervision: SupervisionRepository,
    private val appContext: Context
) : ViewModel() {

    val contacts: StateFlow<List<CapturedContactEntity>> = capture.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val listenerConnected: StateFlow<Boolean> = capture.listenerConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted

    init {
        refreshPermission()
    }

    fun refreshPermission() {
        _permissionGranted.value = NotificationAccess.isGranted(appContext)
    }

    fun clearLog() {
        viewModelScope.launch { capture.clearLog() }
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val capture = CaptureRepository.getInstance(appContext)
            val supervision = SupervisionRepository.getInstance(appContext)
            return CaptureLogViewModel(capture, supervision, appContext) as T
        }
    }
}
