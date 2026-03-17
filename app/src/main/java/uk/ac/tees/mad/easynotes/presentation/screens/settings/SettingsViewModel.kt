package uk.ac.tees.mad.easynotes.presentation.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uk.ac.tees.mad.easynotes.data.preferences.UserPreferencesManager

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val userEmail: String = "",
    val isLoading: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val preferencesManager = UserPreferencesManager(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUser()
        loadThemePreference()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val user = auth.currentUser
            _uiState.update {
                it.copy(userEmail = user?.email ?: "")
            }
        }
    }

    private fun loadThemePreference() {
        viewModelScope.launch {
            preferencesManager.themePreferenceFlow.collect { theme ->
                _uiState.update { it.copy(themePreference = theme) }
            }
        }
    }

    fun updateTheme(theme: ThemePreference) {
        viewModelScope.launch {
            preferencesManager.updateThemePreference(theme)
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            auth.signOut()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}