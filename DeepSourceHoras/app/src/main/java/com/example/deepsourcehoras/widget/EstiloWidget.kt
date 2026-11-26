package com.example.deepsourcehoras.widget

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import com.example.deepsourcehoras.MainActivity
import com.example.deepsourcehoras.R
import com.example.deepsourcehoras.domain.cronometro.EstadoCronometro

// Colores del prototipo, compartidos por todos los widgets
internal val FondoWidget = Color(0xFF252338)
internal val FondoInterno = Color(0xFF1A1828)
internal val TextoPrincipal = Color(0xFFC9C6DA)
internal val TextoSecundario = Color(0xFF8A87A0)
internal val MoradoClaro = Color(0xFF9B8AD4)
internal val Morado = Color(0xFF6B5AAD)
internal val MoradoOscuro = Color(0xFF3A3560)
internal val VerdeActivo = Color(0xFF6FCF97)
internal val AmarilloPausa = Color(0xFFE2B85C)

// Mismo color de día y de noche (la versión simple de ColorProvider no es pública)
internal fun colorFijo(color: Color) = ColorProvider(day = color, night = color)

// Abre la app directo en el formulario de registro
internal fun intentFormulario(context: Context, finalizar: Boolean = false): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_ABRIR_REGISTRO, true)
        .putExtra(MainActivity.EXTRA_FINALIZAR, finalizar)
        // CLEAR_TASK: aunque la app esté abierta atrás, parte de nuevo y llega al formulario
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

// Abre la app en el Home
internal fun intentApp(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

// Reloj nativo de Android: corriendo avanza solo; en pausa queda detenido en el tiempo acumulado
internal fun relojNativo(
    context: Context,
    estado: EstadoCronometro,
    layout: Int = R.layout.widget_cronometro
): RemoteViews =
    RemoteViews(context.packageName, layout).apply {
        val transcurrido = estado.transcurrido(System.currentTimeMillis())
        val base = SystemClock.elapsedRealtime() - transcurrido
        setChronometer(R.id.cronometro, base, null, !estado.pausado)
    }
