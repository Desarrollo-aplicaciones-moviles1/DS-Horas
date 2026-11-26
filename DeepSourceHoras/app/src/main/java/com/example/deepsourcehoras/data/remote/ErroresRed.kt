package com.example.deepsourcehoras.data.remote

import com.example.deepsourcehoras.data.remote.dto.ErrorDto
import com.google.gson.Gson
import java.io.IOException
import retrofit2.HttpException

// Convierte cualquier error de red en un texto que se le puede mostrar al usuario
object ErroresRed {

    const val SIN_CONEXION =
        "No se pudo conectar con el servidor. Revisa que los microservicios estén encendidos"

    fun mensaje(error: Throwable): String = when (error) {
        is HttpException -> leerMensaje(error) ?: "El servidor respondió con un error (${error.code()})"
        is IOException -> SIN_CONEXION
        else -> error.message ?: "Ocurrió un error inesperado"
    }

    // Los microservicios responden {"mensaje": "..."} cuando algo no se puede hacer
    private fun leerMensaje(error: HttpException): String? =
        try {
            val cuerpo = error.response()?.errorBody()?.string()
            if (cuerpo.isNullOrBlank()) null
            else Gson().fromJson(cuerpo, ErrorDto::class.java)?.mensaje?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
}
