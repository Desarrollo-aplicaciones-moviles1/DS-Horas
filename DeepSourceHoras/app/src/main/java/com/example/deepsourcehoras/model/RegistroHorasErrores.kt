package com.example.deepsourcehoras.model

data class RegistroHorasErrores(
    val proyecto: String? = null,
    val dia: String? = null,
    val horas: String? = null,
    val minutos: String? = null,
    val tarea: String? = null,
    val tiempo: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(proyecto, dia, horas, minutos, tarea, tiempo).any { it != null }
}
