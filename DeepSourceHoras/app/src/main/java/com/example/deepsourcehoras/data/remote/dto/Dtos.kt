package com.example.deepsourcehoras.data.remote.dto

// Objetos que viajan en JSON. Los nombres de los campos son los mismos que usan los microservicios

// ---- ms-usuarios ----
data class LoginRequestDto(val email: String, val password: String)

data class LoginResponseDto(val token: String, val id: Long, val nombre: String, val email: String)

// ---- ms-horas ----
data class ProyectoDto(
    val id: Long,
    val nombre: String,
    val cliente: String,
    val progreso: Double,
    val fijado: Boolean,
    val fechaCierre: String?, // yyyy-MM-dd, null = en curso
    val minutos: Long
)

data class ProyectoRequestDto(
    val nombre: String,
    val cliente: String,
    val progreso: Double? = null,
    val fijado: Boolean? = null
)

data class RegistroDto(
    val id: Long,
    val proyectoId: Long,
    val proyecto: String,
    val tarea: String,
    val fecha: String, // yyyy-MM-dd
    val minutos: Int,
    val foto: String?,
    val creadoEn: Long,
    // Quién hizo el registro
    val usuarioId: Long? = null,
    val usuarioNombre: String? = null
)

data class RegistroRequestDto(
    val proyectoId: Long,
    val tarea: String,
    val fecha: String,
    val minutos: Int,
    val foto: String?,
    val usuarioId: Long? = null,
    val usuarioNombre: String? = null
)

// Respuesta de error de los microservicios: {"mensaje": "..."}
data class ErrorDto(val mensaje: String?)

// ---- API externa Nager.Date ----
data class FeriadoDto(
    val date: String,      // yyyy-MM-dd
    val localName: String, // nombre en español
    val name: String,
    val global: Boolean?   // false = feriado solo regional
)
