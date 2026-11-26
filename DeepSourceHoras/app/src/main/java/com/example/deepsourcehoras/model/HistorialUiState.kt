package com.example.deepsourcehoras.model

data class RegistroItemUi(
    val id: Long,
    val tarea: String,
    val detalle: String,
    val duracion: String,
    val foto: String? = null
)

data class HistorialUiState(
    val registros: List<RegistroItemUi> = emptyList(),
    val cargando: Boolean = true,
    // Registro que se pidió eliminar (se muestra el diálogo de confirmación)
    val porEliminar: RegistroItemUi? = null,
    val eliminando: Boolean = false,
    val mensaje: String? = null
) {
    val totalMinutosTexto: String get() = "${registros.size} registros"
}
