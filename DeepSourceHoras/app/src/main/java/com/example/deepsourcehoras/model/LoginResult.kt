package com.example.deepsourcehoras.model

sealed class LoginResult {
    data class Success(val token: String, val userName: String, val usuarioId: Long = 0) : LoginResult()
    data class Error(val mensaje: String) : LoginResult()
}
