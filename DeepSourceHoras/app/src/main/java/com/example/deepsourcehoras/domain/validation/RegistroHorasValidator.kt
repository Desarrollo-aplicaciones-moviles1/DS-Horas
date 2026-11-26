package com.example.deepsourcehoras.domain.validation

import com.example.deepsourcehoras.model.RegistroHorasErrores
import com.example.deepsourcehoras.util.diaABase
import com.example.deepsourcehoras.util.formatearDuracion

// Lo que viene escrito en el formulario
data class DatosRegistro(
    val proyecto: String,
    val dia: String,
    val horas: String,
    val minutos: String,
    val tarea: String
)

// Reglas del formulario de registro. No usa clases de Android
object RegistroHorasValidator {

    const val MAX_HORAS = 24
    const val MAX_MINUTOS = 59
    const val MAX_MINUTOS_DIA = 24 * 60
    const val MIN_TAREA = 3
    const val MAX_TAREA = 60

    // Errores de tiempo que se muestran MIENTRAS se escribe (no espera a "Registrar"):
    // más de 24 horas, más de 59 minutos o pasarse del tope del día.
    // No reclama por el tiempo vacío, porque el usuario todavía está escribiendo.
    fun erroresDeTiempo(horasTexto: String, minutosTexto: String, minutosDelDia: Int = 0): RegistroHorasErrores {
        val horas = horasTexto.toIntOrNull() ?: 0
        val minutos = minutosTexto.toIntOrNull() ?: 0
        val total = horas * 60 + minutos

        val errorHoras = if (horas > MAX_HORAS) "Máximo $MAX_HORAS horas" else null
        val errorMinutos = if (minutos > MAX_MINUTOS) "Máximo $MAX_MINUTOS minutos" else null

        // El total solo se revisa si horas y minutos están en rango
        val errorTiempo = when {
            errorHoras != null || errorMinutos != null -> null
            total > MAX_MINUTOS_DIA -> "Un registro no puede pasar de 24 horas"
            total > 0 && minutosDelDia + total > MAX_MINUTOS_DIA -> {
                val disponible = (MAX_MINUTOS_DIA - minutosDelDia).coerceAtLeast(0)
                "Ese día ya tienes ${formatearDuracion(minutosDelDia)} registradas. " +
                    "Te quedan ${formatearDuracion(disponible)}"
            }
            else -> null
        }
        return RegistroHorasErrores(horas = errorHoras, minutos = errorMinutos, tiempo = errorTiempo)
    }

    // Validación completa al apretar "Registrar"
    // hoy: fecha de hoy en dd/MM/yyyy
    // minutosDelDia: lo que ya está registrado ese día
    fun validar(datos: DatosRegistro, hoy: String, minutosDelDia: Int = 0): RegistroHorasErrores {
        val tiempo = erroresDeTiempo(datos.horas, datos.minutos, minutosDelDia)
        val total = (datos.horas.toIntOrNull() ?: 0) * 60 + (datos.minutos.toIntOrNull() ?: 0)
        val tarea = datos.tarea.trim()
        val fecha = diaABase(datos.dia)
        val fechaHoy = diaABase(hoy)

        val errorDia = when {
            datos.dia.isBlank() -> "Selecciona un día"
            fecha == null -> "Fecha no válida"
            fechaHoy != null && fecha > fechaHoy -> "No puedes registrar días futuros"
            else -> null
        }
        val errorTiempo = when {
            tiempo.horas != null || tiempo.minutos != null -> null
            tiempo.tiempo != null -> tiempo.tiempo
            total == 0 -> "Ingresa un tiempo mayor a cero"
            else -> null
        }
        val errorTarea = when {
            tarea.isEmpty() -> "Escribe el nombre de la tarea"
            tarea.length < MIN_TAREA -> "Mínimo $MIN_TAREA caracteres"
            tarea.length > MAX_TAREA -> "Máximo $MAX_TAREA caracteres"
            else -> null
        }

        return RegistroHorasErrores(
            proyecto = if (datos.proyecto.isBlank()) "Selecciona un proyecto" else null,
            dia = errorDia,
            horas = tiempo.horas,
            minutos = tiempo.minutos,
            tarea = errorTarea,
            tiempo = errorTiempo
        )
    }
}
