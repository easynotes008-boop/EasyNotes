package uk.ac.tees.mad.easynotes.presentation.screens.auth


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val isLoginMode by viewModel.isLoginMode.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onNavigateToHome()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "EasyNotes",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isLoginMode) "Welcome back!" else "Create account",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(48.dp))

                AuthForm(
                    email = email,
                    password = password,
                    isLoginMode = isLoginMode,
                    isLoading = uiState is AuthUiState.Loading,
                    onEmailChange = viewModel::updateEmail,
                    onPasswordChange = viewModel::updatePassword,
                    onSubmit = viewModel::authenticate,
                    onToggleMode = viewModel::toggleMode
                )
            }
        }
    }
}

@Composable
fun AuthForm(
    email: String,
    password: String,
    isLoginMode: Boolean,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Email") },
        leadingIcon = {
            Icon(Icons.Default.Email, contentDescription = null)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
        ),
        singleLine = true,
        enabled = !isLoading
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Password") },
        leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                onSubmit()
            }
        ),
        singleLine = true,
        enabled = !isLoading
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = if (isLoginMode) "Login" else "Register",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    TextButton(
        onClick = onToggleMode,
        enabled = !isLoading
    ) {
        Text(
            text = if (isLoginMode) "Don't have an account? Register" else "Already have an account? Login"
        )
    }
}

@Preview(showBackground = true, name = "AuthScreenPreview")
@Composable
fun AuthScreenPreview() {
    MaterialTheme {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoginMode by remember { mutableStateOf(true) }

        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "EasyNotes",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isLoginMode) "Welcome back!" else "Create account",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    AuthForm(
                        email = email,
                        password = password,
                        isLoginMode = isLoginMode,
                        isLoading = false,
                        onEmailChange = { email = it },
                        onPasswordChange = { password = it },
                        onSubmit = { },
                        onToggleMode = { isLoginMode = !isLoginMode }
                    )
                }
            }
        }
    }
}