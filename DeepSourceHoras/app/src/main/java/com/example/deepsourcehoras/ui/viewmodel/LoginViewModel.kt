package com.example.deepsourcehoras.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsourcehoras.data.preferences.SesionStore
import com.example.deepsourcehoras.data.repository.AuthRepository
import com.example.deepsourcehoras.domain.validation.LoginValidator
import com.example.deepsourcehoras.model.LoginResult
import com.example.deepsourcehoras.model.LoginUiState
import com.example.deepsourcehoras.recursos.RecursosDispositivo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: AuthRepository,
    private val sesion: SesionStore,
    private val recursos: RecursosDispositivo
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(valor: String) {
        _uiState.update {
            it.copy(
                email = valor,
                errores = it.errores.copy(email = null),
                mensajeError = null
            )
        }
    }

    fun onPasswordChange(valor: String) {
        _uiState.update {
            it.copy(
                password = valor,
                errores = it.errores.copy(password = null),
                mensajeError = null
            )
        }
    }

    fun onLoginClick(onSuccess: () -> Unit) {
        val estado = _uiState.value
        if (estado.isLoading) return

        // Las reglas están en LoginValidator, acá solo se usa el resultado
        val errores = LoginValidator.validar(estado.email, estado.password)
        _uiState.update { it.copy(errores = errores, aviso = null) }
        if (errores.hayErrores) {
            recursos.vibrarError()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, mensajeError = null) }

            // Se valida contra el microservicio ms-usuarios
            when (val result = repository.loginUser(estado.email.trim(), estado.password)) {
                is LoginResult.Success -> {
                    sesion.guardar(result.token, result.userName, result.usuarioId)
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is LoginResult.Error -> {
                    recursos.vibrarError()
                    _uiState.update { it.copy(isLoading = false, mensajeError = result.mensaje) }
                }
            }
        }
    }

    // El ingreso con Google necesita el servidor OAuth de Deepsource, que todavía no existe
    fun onGoogleClick() {
        _uiState.update {
            it.copy(aviso = "El ingreso con Google estará disponible cuando Deepsource habilite su cuenta de Google. Por ahora entra con tu correo.")
        }
    }
}
