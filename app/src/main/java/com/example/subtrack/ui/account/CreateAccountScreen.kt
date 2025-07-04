package com.example.subtrack.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.subtrack.SubscriptionDatabase
import kotlinx.coroutines.launch

@Composable
fun CreateAccountScreen(
    db: SubscriptionDatabase,
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (password != confirmPassword) {
                    error = "Passwords do not match"
                } else if (email.isBlank() || password.isBlank()) {
                    error = "Please fill in all fields"
                } else {
                    error = null
                    scope.launch {
                        val existing = db.accountDao().getAccountByEmail(email)
                        if (existing != null) {
                            error = "Account already exists"
                        } else {
                            db.accountDao().insert(Account(email = email, password = password))
                            showConfirmation = true
                            email = ""
                            password = ""
                            confirmPassword = ""
                            onBackToLogin()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Account")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onBackToLogin,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Back to Login")
        }

        if (showConfirmation) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Account Created") },
                text = { Text("Your account has been created successfully!") },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmation = false
                    }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
