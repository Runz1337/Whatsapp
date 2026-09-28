package com.familyguardian.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel(context: Context) : ViewModel() {

    private val prefs = context.applicationContext
        .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(
        when (prefs.getString(KEY_THEME, null)) {
            "light" -> false
            "dark" -> true
            else -> null
        }
    )
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode

    fun setDarkMode(value: Boolean?) {
        _isDarkMode.value = value
        viewModelScope.launch {
            prefs.edit().putString(KEY_THEME, value?.let { if (it) "dark" else "light" }).apply()
        }
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(appContext) as T
    }

    private companion object {
        const val KEY_THEME = "theme"
    }
}
