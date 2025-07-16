package com.example.subtrack.ui.account

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class LoginViewModel : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var errorMessage by mutableStateOf<String?>(null)
    var isBiometricAvailable by mutableStateOf(false)
    var showBiometricButton by mutableStateOf(false)

    fun clearFields() {
        email = ""
        password = ""
    }

    fun clearError() {
        errorMessage = null
    }

    fun setBiometricAvailability(available: Boolean) {
        isBiometricAvailable = available
        showBiometricButton = available
    }
}
