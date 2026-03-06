package uk.ac.tees.mad.easynotes.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoginMode = MutableStateFlow(true)
    val isLoginMode: StateFlow<Boolean> = _isLoginMode.asStateFlow()

    fun updateEmail(value: String) {
        _email.value = value
    }

    fun updatePassword(value: String) {
        _password.value = value
    }

    fun toggleMode() {
        _isLoginMode.value = !_isLoginMode.value
        _uiState.value = AuthUiState.Idle
    }

    fun authenticate() {
        if (!validateInput()) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            try {
                if (_isLoginMode.value) {
                    auth.signInWithEmailAndPassword(_email.value, _password.value).await()
                } else {
                    auth.createUserWithEmailAndPassword(_email.value, _password.value).await()
                }
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    e.message ?: "Authentication failed"
                )
            }
        }
    }

    private fun validateInput(): Boolean {
        return when {
            _email.value.isBlank() -> {
                _uiState.value = AuthUiState.Error("Email is required")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(_email.value).matches() -> {
                _uiState.value = AuthUiState.Error("Invalid email format")
                false
            }
            _password.value.length < 6 -> {
                _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
                false
            }
            else -> true
        }
    }
}