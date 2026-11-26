package com.example.deepsourcehoras.recursos

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// Recurso nativo 2: vibración como respuesta a lo que hace el usuario
object Vibracion {

    // Toque corto: iniciar el cronómetro, tomar una foto
    fun corta(context: Context) = vibrar(context, longArrayOf(0, 40))

    // Dos pulsos: registro guardado, cronómetro finalizado
    fun exito(context: Context) = vibrar(context, longArrayOf(0, 60, 90, 60))

    // Uno más largo: el formulario tiene errores
    fun error(context: Context) = vibrar(context, longArrayOf(0, 180))

    private fun vibrar(context: Context, patron: LongArray) {
        val vibrador = obtenerVibrador(context) ?: return
        if (!vibrador.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrador.vibrate(VibrationEffect.createWaveform(patron, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrador.vibrate(patron, -1)
        }
    }

    private fun obtenerVibrador(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
