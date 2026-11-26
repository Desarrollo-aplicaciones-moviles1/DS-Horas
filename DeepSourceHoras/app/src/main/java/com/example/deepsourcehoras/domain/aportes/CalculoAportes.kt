package com.example.deepsourcehoras.domain.aportes

import com.example.deepsourcehoras.data.local.AporteEntidad
import com.example.deepsourcehoras.model.AporteUi
import com.example.deepsourcehoras.util.formatearDuracion

// Reparte el 100 % de un proyecto entre las personas que registraron horas en él.
// No usa clases de Android, así se prueba por separado.
object CalculoAportes {

    fun calcular(aportes: List<AporteEntidad>, usuarioActual: Long?): List<AporteUi> {
        val ordenados = aportes.filter { it.minutos > 0 }.sortedByDescending { it.minutos }
        val total = ordenados.sumOf { it.minutos }
        if (total == 0) return emptyList()

        // Método del mayor resto: se redondea hacia abajo y lo que falta para 100
        // se le da a quienes tenían el decimal más alto. Así la suma siempre es 100
        val exactos = ordenados.map { it.minutos * 100.0 / total }
        val porcentajes = exactos.map { it.toInt() }.toMutableList()
        val faltante = 100 - porcentajes.sum()
        exactos.indices
            .sortedByDescending { exactos[it] - porcentajes[it] }
            .take(faltante)
            .forEach { porcentajes[it]++ }

        return ordenados.mapIndexed { i, a ->
            val esTu = usuarioActual != null && a.usuarioId == usuarioActual
            AporteUi(
                nombre = a.usuario,
                duracion = formatearDuracion(a.minutos),
                minutos = a.minutos,
                porcentaje = porcentajes[i],
                esTu = esTu,
                color = i
            )
        }
    }

    // Agrupa por proyecto: id del proyecto -> aportes de su equipo
    fun porProyecto(aportes: List<AporteEntidad>, usuarioActual: Long?): Map<Long, List<AporteUi>> =
        aportes.groupBy { it.proyectoId }.mapValues { (_, lista) -> calcular(lista, usuarioActual) }
}
