package com.example.deepsourcehoras.model

data class Proyecto(
    val nombre: String,
    val minutos: Int,
    val progreso: Float
)

data class RegistroHoras(
    val tarea: String,
    val detalle: String,
    val duracion: String,
    val conFoto: Boolean = false
)

data class HomeUiState(
    val userName: String = "",
    val proyectos: List<Proyecto> = emptyList(),
    val registros: List<RegistroHoras> = emptyList(),
    // Sincronización con el microservicio
    val sincronizando: Boolean = false,
    val errorConexion: String? = null
) {
    val totalMinutos: Int get() = proyectos.sumOf { it.minutos }
}
