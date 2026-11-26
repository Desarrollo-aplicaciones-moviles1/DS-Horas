package com.example.deepsourcehoras.model

data class RegistroHorasUiState(
    val proyecto: String = "",
    val dia: String = "",
    val horas: String = "",
    val minutos: String = "",
    val tarea: String = "",
    val proyectos: List<String> = emptyList(),
    // true cuando se está editando un registro existente
    val editando: Boolean = false,
    // Cronómetro: corriendo = hay cronómetro (andando o en pausa)
    val corriendo: Boolean = false,
    val pausado: Boolean = false,
    val segundos: Int = 0,
    val horaInicio: String? = null,
    // Foto de evidencia (ruta del archivo en el teléfono)
    val fotoRuta: String? = null,
    val guardando: Boolean = false,
    // Aviso de la API de feriados para el día elegido
    val avisoFeriado: String? = null,
    // Mensaje corto que se muestra abajo (permisos, errores al guardar)
    val mensaje: String? = null,
    val errores: RegistroHorasErrores = RegistroHorasErrores()
)

// Lo que se guardó, para mostrarlo en la confirmación
data class ResumenRegistro(
    val proyecto: String,
    val horas: Int,
    val minutos: Int,
    val dia: String,
    val foto: String?,
    val editado: Boolean = false
)
