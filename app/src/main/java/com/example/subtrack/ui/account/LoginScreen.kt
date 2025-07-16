package com.example.subtrack.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.platform.LocalContext
import com.example.subtrack.BiometricAuthHelper

@Composable
fun LoginScreen(
    onLogin: (String, String, (Boolean, Long?) -> Unit) -> Unit,
    onNavigateToCreateAccount: () -> Unit,
    onBiometricLogin: () -> Unit,
    viewModel: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val email = viewModel.email
    val password = viewModel.password
    val errorMessage = viewModel.errorMessage
    val showBiometricButton = viewModel.showBiometricButton
    val context = LocalContext.current
    
    // Check biometric availability when the screen is first displayed
    LaunchedEffect(Unit) {
        val biometricAuthHelper = BiometricAuthHelper(context)
        viewModel.setBiometricAvailability(biometricAuthHelper.isBiometricAvailable())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Log In", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                viewModel.email = it
                viewModel.clearError()
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                viewModel.password = it
                viewModel.clearError()
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Biometric Login Button
        if (showBiometricButton) {
            OutlinedButton(
                onClick = onBiometricLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Biometric Login",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Login with Biometric")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Or",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    viewModel.errorMessage = "Please enter both email and password"
                } else {
                    onLogin(email, password) { success, userId ->
                        if (success && userId != null) {
                            viewModel.clearFields()
                        } else {
                            viewModel.errorMessage = "Invalid email or password"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log In with Password")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onNavigateToCreateAccount,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Don't have an account? Sign Up")
        }
    }
}
