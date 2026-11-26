package com.example.deepsourcehoras.domain.cronometro

// Estado del cronómetro con pausa. No usa clases de Android, así se prueba por separado.
//  primerInicio: cuándo se apretó "Iniciar" (0 = no hay cronómetro)
//  inicioTramo:  cuándo partió el tramo actual (0 mientras está en pausa)
//  acumulado:    milisegundos de los tramos anteriores a la última pausa
data class EstadoCronometro(
    val proyecto: String = "",
    val primerInicio: Long = 0L,
    val inicioTramo: Long = 0L,
    val acumulado: Long = 0L,
    val pausado: Boolean = false
) {
    val activo: Boolean get() = primerInicio > 0L

    fun transcurrido(ahora: Long): Long =
        acumulado + if (!pausado && inicioTramo > 0L) (ahora - inicioTramo).coerceAtLeast(0L) else 0L

    fun pausar(ahora: Long): EstadoCronometro =
        if (!activo || pausado) this
        else copy(acumulado = transcurrido(ahora), inicioTramo = 0L, pausado = true)

    fun reanudar(ahora: Long): EstadoCronometro =
        if (!activo || !pausado) this
        else copy(inicioTramo = ahora, pausado = false)

    // Pausa si corre, reanuda si está en pausa
    fun alternar(ahora: Long): EstadoCronometro = if (pausado) reanudar(ahora) else pausar(ahora)

    companion object {
        fun iniciar(proyecto: String, ahora: Long) =
            EstadoCronometro(proyecto = proyecto, primerInicio = ahora, inicioTramo = ahora)
    }
}
