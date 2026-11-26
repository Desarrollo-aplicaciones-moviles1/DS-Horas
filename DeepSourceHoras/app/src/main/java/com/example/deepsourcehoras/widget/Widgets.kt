package com.example.deepsourcehoras.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.example.deepsourcehoras.data.preferences.CronometroStorage
import com.example.deepsourcehoras.domain.cronometro.EstadoCronometro
import com.example.deepsourcehoras.recursos.Notificaciones
import com.example.deepsourcehoras.recursos.Vibracion

object Widgets {
    // Nombre del proyecto que se pasa al botón ▶ del widget de proyectos
    val PROYECTO = ActionParameters.Key<String>("proyecto")

    // Redibuja los cinco widgets (si no hay ninguno puesto, no pasa nada)
    suspend fun actualizarTodos(context: Context) {
        listOf(DsHorasWidget(), CronometroWidget(), ResumenWidget(), MiniWidget(), ProyectosWidget())
            .forEach {
                try {
                    it.updateAll(context)
                } catch (e: Exception) {
                    // Un widget que no se pudo dibujar no debe romper la app ni a los otros
                }
            }
    }
}

// Botón "Pausar / Reanudar" de los widgets: funciona sin abrir la app
class PausarReanudarCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val estado = CronometroStorage.leer(context)
        if (!estado.activo) return
        val ahora = System.currentTimeMillis()
        val nuevo = estado.alternar(ahora)
        CronometroStorage.guardar(context, nuevo)
        Notificaciones.mostrarCronometro(context, nuevo.proyecto, nuevo.transcurrido(ahora), nuevo.pausado)
        Vibracion.corta(context)
        Widgets.actualizarTodos(context)
    }
}

// Botón ▶ de un proyecto: inicia el cronómetro de ese proyecto sin abrir la app.
// Si ya corre el de ese mismo proyecto, lo pausa o reanuda. Si corre otro, no hace nada
// (para no mezclar las horas de dos proyectos).
class IniciarProyectoCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val proyecto = parameters[Widgets.PROYECTO] ?: return
        val estado = CronometroStorage.leer(context)
        val ahora = System.currentTimeMillis()
        val nuevo = when {
            !estado.activo -> EstadoCronometro.iniciar(proyecto, ahora)
            estado.proyecto == proyecto -> estado.alternar(ahora)
            else -> return
        }
        CronometroStorage.guardar(context, nuevo)
        Notificaciones.mostrarCronometro(context, nuevo.proyecto, nuevo.transcurrido(ahora), nuevo.pausado)
        Vibracion.corta(context)
        Widgets.actualizarTodos(context)
    }
}
