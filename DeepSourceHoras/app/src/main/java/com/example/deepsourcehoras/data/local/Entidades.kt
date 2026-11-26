package com.example.deepsourcehoras.data.local

// Filas de la base ya leídas. Las fechas van en formato yyyy-MM-dd

data class ProyectoEntidad(
    val id: Long,
    val nombre: String,
    val cliente: String,
    val progreso: Float,
    val fijado: Boolean,
    val fechaCierre: String?,
    val minutos: Int // suma de los registros del proyecto
) {
    val enCurso: Boolean get() = fechaCierre == null
}

data class RegistroEntidad(
    val id: Long,
    val proyectoId: Long,
    val proyecto: String,
    val tarea: String,
    val fecha: String,
    val minutos: Int,
    val foto: String?,
    val usuarioId: Long? = null,
    val usuario: String? = null // nombre de quien lo registró
)

// Minutos que lleva una persona en un proyecto (suma de sus registros)
data class AporteEntidad(
    val proyectoId: Long,
    val usuarioId: Long?,
    val usuario: String,
    val minutos: Int
)

// Lo que se escribe en la base (copia local de lo que hay en el microservicio)

data class ProyectoLocal(
    val id: Long,
    val nombre: String,
    val cliente: String,
    val progreso: Float,
    val fijado: Boolean,
    val fechaCierre: String?
)

data class RegistroLocal(
    val id: Long,
    val proyectoId: Long,
    val tarea: String,
    val fecha: String,
    val minutos: Int,
    val foto: String?,
    val creadoEn: Long,
    val usuarioId: Long? = null,
    val usuario: String? = null
)
