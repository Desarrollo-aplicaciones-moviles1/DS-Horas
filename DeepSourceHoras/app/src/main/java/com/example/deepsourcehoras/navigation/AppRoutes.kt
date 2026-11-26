package com.example.deepsourcehoras.navigation

import android.net.Uri

object AppRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val REGISTRO = "registro?finalizar={finalizar}&id={id}"
    const val MIS_PROYECTOS = "mis_proyectos"
    const val HISTORIAL = "historial"
    const val CONFIRMACION = "confirmacion/{proyecto}/{horas}/{minutos}/{dia}?foto={foto}"

    // finalizar = true cuando se llega desde el botón "Finalizar" del widget
    fun registro(finalizar: Boolean = false) = "registro?finalizar=$finalizar"

    // Mismo formulario, pero con los datos de un registro existente para editarlo
    fun editarRegistro(id: Long) = "registro?id=$id"

    fun confirmacion(proyecto: String, horas: Int, minutos: Int, dia: String, foto: String?): String {
        val ruta = "confirmacion/${Uri.encode(proyecto)}/$horas/$minutos/${Uri.encode(dia)}"
        return if (foto == null) ruta else "$ruta?foto=${Uri.encode(foto)}"
    }
}
