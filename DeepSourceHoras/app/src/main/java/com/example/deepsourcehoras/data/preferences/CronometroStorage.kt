package com.example.deepsourcehoras.data.preferences

import android.content.Context
import com.example.deepsourcehoras.domain.cronometro.EstadoCronometro

// Lo que necesita el ViewModel del cronómetro (en las pruebas se usa uno en memoria)
interface CronometroStore {
    fun leer(): EstadoCronometro
    fun guardar(estado: EstadoCronometro)
    fun detener()
}

// Guarda el cronómetro en el teléfono (SharedPreferences) para que la app,
// la notificación y los widgets vean lo mismo, aunque se cierre la app.
object CronometroStorage {
    private const val ARCHIVO = "cronometro"
    private const val PROYECTO = "proyecto"
    private const val PRIMER_INICIO = "primer_inicio"
    private const val INICIO_TRAMO = "inicio_tramo"
    private const val ACUMULADO = "acumulado"
    private const val PAUSADO = "pausado"

    private fun prefs(context: Context) =
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun leer(context: Context): EstadoCronometro {
        val p = prefs(context)
        return EstadoCronometro(
            proyecto = p.getString(PROYECTO, "") ?: "",
            primerInicio = p.getLong(PRIMER_INICIO, 0L),
            inicioTramo = p.getLong(INICIO_TRAMO, 0L),
            acumulado = p.getLong(ACUMULADO, 0L),
            pausado = p.getBoolean(PAUSADO, false)
        )
    }

    fun guardar(context: Context, estado: EstadoCronometro) {
        prefs(context).edit()
            .putString(PROYECTO, estado.proyecto)
            .putLong(PRIMER_INICIO, estado.primerInicio)
            .putLong(INICIO_TRAMO, estado.inicioTramo)
            .putLong(ACUMULADO, estado.acumulado)
            .putBoolean(PAUSADO, estado.pausado)
            .apply()
    }

    fun detener(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

class CronometroPrefs(private val context: Context) : CronometroStore {
    override fun leer(): EstadoCronometro = CronometroStorage.leer(context)
    override fun guardar(estado: EstadoCronometro) = CronometroStorage.guardar(context, estado)
    override fun detener() = CronometroStorage.detener(context)
}
