package com.example.deepsourcehoras.model

enum class FiltroProyectos { EN_CURSO, FINALIZADOS }

data class ProyectoDetalle(
    val nombre: String,
    val cliente: String,
    val minutos: Int,
    val progreso: Float = 0f,
    val fijado: Boolean = false,
    val fechaCierre: String? = null, // null = en curso
    val id: Long = 0,
    // Cuánto aportó cada integrante (de mayor a menor)
    val aportes: List<AporteUi> = emptyList()
)

// Aporte de una persona a un proyecto
data class AporteUi(
    val nombre: String,
    val duracion: String,
    val minutos: Int,
    val porcentaje: Int,
    val esTu: Boolean = false,
    val color: Int = 0 // posición, para elegir el color de su barra
)

data class ProyectoErrores(
    val nombre: String? = null,
    val cliente: String? = null
) {
    val hayErrores: Boolean get() = nombre != null || cliente != null
}

data class MisProyectosUiState(
    val filtro: FiltroProyectos = FiltroProyectos.EN_CURSO,
    val proyectos: List<ProyectoDetalle> = emptyList(),
    // Formulario "Nuevo proyecto"
    val dialogoNuevo: Boolean = false,
    val nombreNuevo: String = "",
    val clienteNuevo: String = "",
    val erroresNuevo: ProyectoErrores = ProyectoErrores(),
    val guardando: Boolean = false,
    // Proyecto que se pidió finalizar (diálogo de confirmación)
    val porFinalizar: ProyectoDetalle? = null,
    val mensaje: String? = null,
    // Proyectos con el detalle del equipo abierto
    val expandidos: Set<Long> = emptySet()
) {
    val visibles: List<ProyectoDetalle>
        get() = proyectos.filter {
            (it.fechaCierre == null) == (filtro == FiltroProyectos.EN_CURSO)
        }
}
