package com.example.deepsourcehoras.recursos

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.deepsourcehoras.MainActivity
import com.example.deepsourcehoras.R
import com.example.deepsourcehoras.util.formatearCronometro
import com.example.deepsourcehoras.util.formatearDuracion

// Recurso nativo 1: notificaciones de Android
object Notificaciones {
    private const val CANAL_REGISTROS = "horas_registradas"
    private const val CANAL_CRONOMETRO = "cronometro"
    private const val ID_REGISTRO = 1
    private const val ID_CRONOMETRO = 2

    // Aviso cuando se guardan horas. Al tocarlo abre la app
    @SuppressLint("MissingPermission") // se revisa con areNotificationsEnabled()
    fun mostrarHorasRegistradas(context: Context, proyecto: String, minutos: Int) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        crearCanal(manager, CANAL_REGISTROS, "Horas registradas", NotificationManagerCompat.IMPORTANCE_DEFAULT)

        val notificacion = NotificationCompat.Builder(context, CANAL_REGISTROS)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle("Horas registradas")
            .setContentText("${formatearDuracion(minutos)} en $proyecto")
            .setContentIntent(abrirApp(context, abrirRegistro = false))
            .setAutoCancel(true)
            .build()

        manager.notify(ID_REGISTRO, notificacion)
    }

    // Queda fija mientras hay cronómetro. Corriendo, el tiempo lo va contando Android;
    // en pausa se muestra detenido.
    @SuppressLint("MissingPermission")
    fun mostrarCronometro(context: Context, proyecto: String, transcurridoMillis: Long, pausado: Boolean) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        crearCanal(manager, CANAL_CRONOMETRO, "Cronómetro en curso", NotificationManagerCompat.IMPORTANCE_LOW)

        val constructor = NotificationCompat.Builder(context, CANAL_CRONOMETRO)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(abrirApp(context, abrirRegistro = true))

        if (pausado) {
            constructor
                .setContentTitle("Cronómetro en pausa")
                .setContentText("$proyecto · ${formatearCronometro((transcurridoMillis / 1000).toInt())}")
                .setShowWhen(false)
                .setUsesChronometer(false)
        } else {
            constructor
                .setContentTitle("Cronómetro en curso")
                .setContentText(proyecto)
                // "when" en el pasado hace que el contador parta del tiempo ya acumulado
                .setWhen(System.currentTimeMillis() - transcurridoMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
        }

        manager.notify(ID_CRONOMETRO, constructor.build())
    }

    fun ocultarCronometro(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_CRONOMETRO)
    }

    // Desde Android 8 toda notificación necesita un canal
    private fun crearCanal(manager: NotificationManagerCompat, id: String, nombre: String, importancia: Int) {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(id, importancia)
                .setName(nombre)
                .build()
        )
    }

    private fun abrirApp(context: Context, abrirRegistro: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_ABRIR_REGISTRO, abrirRegistro)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            if (abrirRegistro) 1 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
