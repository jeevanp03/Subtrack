package com.example.subtrack.ui.account

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class LoginViewModel : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var errorMessage by mutableStateOf<String?>(null)

    fun onLoginClick(onSuccess: (String) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please enter both email and password"
        } else {
            errorMessage = null
            onSuccess(email)
            clearFields()
        }
    }
    fun clearFields() {
        email = ""
        password = ""
    }

    fun clearError() {
        errorMessage = null
    }
}
