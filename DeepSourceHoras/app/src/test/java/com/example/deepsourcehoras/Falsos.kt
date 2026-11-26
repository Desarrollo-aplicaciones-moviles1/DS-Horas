package com.example.deepsourcehoras

import com.example.deepsourcehoras.data.local.ProyectoEntidad
import com.example.deepsourcehoras.data.local.RegistroEntidad
import com.example.deepsourcehoras.data.preferences.CronometroStore
import com.example.deepsourcehoras.data.preferences.SesionStore
import com.example.deepsourcehoras.domain.cronometro.EstadoCronometro

// Implementaciones en memoria para las pruebas

class CronometroFalso(var estado: EstadoCronometro = EstadoCronometro()) : CronometroStore {
    override fun leer() = estado
    override fun guardar(estado: EstadoCronometro) { this.estado = estado }
    override fun detener() { estado = EstadoCronometro() }
}

class SesionFalsa(var token: String? = null, var nombre: String? = null, var id: Long? = null) : SesionStore {
    override fun guardar(token: String, usuario: String, usuarioId: Long) { this.token = token; nombre = usuario; id = usuarioId }
    override fun usuarioId() = id
    override fun haySesion() = token != null
    override fun usuario() = nombre
    override fun cerrar() { token = null; nombre = null; id = null }
}

fun proyecto(id: Long, nombre: String, minutos: Int = 60, cierre: String? = null, fijado: Boolean = false) =
    ProyectoEntidad(id, nombre, "AFC", 0.5f, fijado, cierre, minutos)

fun registro(
    id: Long,
    proyecto: String = "Web",
    minutos: Int = 90,
    fecha: String = "2026-11-19",
    foto: String? = null,
    usuario: String? = null
) = RegistroEntidad(id, 1, proyecto, "Tarea $id", fecha, minutos, foto, usuario = usuario)

// Error HTTP como el que lanza Retrofit cuando el servidor responde con un código de error
fun errorHttp(codigo: Int, cuerpo: String = "") =
    retrofit2.HttpException(
        retrofit2.Response.error<Any>(
            codigo,
            okhttp3.ResponseBody.Companion.run { cuerpo.toResponseBody(okhttp3.MediaType.Companion.run { "application/json".toMediaType() }) }
        )
    )
