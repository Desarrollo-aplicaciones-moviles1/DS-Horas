package com.example.deepsourcehoras.domain.validation

import com.example.deepsourcehoras.model.LoginErrores

// Reglas del login. No usa clases de Android, así se puede probar por separado
object LoginValidator {

    const val MIN_PASSWORD = 6

    private val formatoCorreo = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validar(email: String, password: String): LoginErrores {
        val correo = email.trim()

        val errorEmail = when {
            correo.isEmpty() -> "Ingresa tu correo"
            !formatoCorreo.matches(correo) -> "Correo no válido"
            else -> null
        }
        val errorPassword = when {
            password.isBlank() -> "Ingresa tu contraseña"
            password.length < MIN_PASSWORD -> "Mínimo $MIN_PASSWORD caracteres"
            else -> null
        }
        return LoginErrores(email = errorEmail, password = errorPassword)
    }
}
