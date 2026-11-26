package com.example.deepsourcehoras.data.repository

import com.example.deepsourcehoras.data.remote.ErroresRed
import com.example.deepsourcehoras.data.remote.UsuariosApi
import com.example.deepsourcehoras.data.remote.dto.LoginRequestDto
import com.example.deepsourcehoras.model.LoginResult
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

// Login contra el microservicio ms-usuarios
class AuthRepository(
    private val api: UsuariosApi,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun loginUser(email: String, password: String): LoginResult =
        withContext(io) {
            try {
                val respuesta = api.login(LoginRequestDto(email, password))
                LoginResult.Success(respuesta.token, respuesta.nombre, respuesta.id)
            } catch (e: HttpException) {
                if (e.code() == 401) LoginResult.Error("Correo o contraseña incorrectos")
                else LoginResult.Error(ErroresRed.mensaje(e))
            } catch (e: IOException) {
                LoginResult.Error(ErroresRed.mensaje(e))
            }
        }
}
