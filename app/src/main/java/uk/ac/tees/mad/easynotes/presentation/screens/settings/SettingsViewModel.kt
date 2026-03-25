package uk.ac.tees.mad.easynotes.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val userEmail: String = "",
    val isLoading: Boolean = false
)

class SettingsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val user = auth.currentUser
            _uiState.update {
                it.copy(userEmail = user?.email ?: "")
            }
        }
    }

    fun updateTheme(theme: ThemePreference) {
        viewModelScope.launch {
            _uiState.update { it.copy(themePreference = theme) }
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