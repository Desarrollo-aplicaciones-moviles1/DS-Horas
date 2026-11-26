package com.example.deepsourcehoras.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.deepsourcehoras.R
import com.example.deepsourcehoras.data.preferences.CronometroStorage
import com.example.deepsourcehoras.di.Dependencias
import com.example.deepsourcehoras.domain.cronometro.EstadoCronometro
import com.example.deepsourcehoras.util.formatearDuracion

// Widget 1 (grande): horas por proyecto + registrar / cronómetro
class DsHorasWidget : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val estado = CronometroStorage.leer(context)

        // Los proyectos salen de la base local, los mismos que muestra el Home
        val proyectos = Dependencias.horasRepository(context)
            .proyectosEnCurso()
            .take(MAX_PROYECTOS)
            .map { it.nombre to formatearDuracion(it.minutos) }

        val reloj: RemoteViews? = if (estado.activo) relojNativo(context, estado) else null

        provideContent {
            ContenidoWidget(proyectos = proyectos, estado = estado, reloj = reloj)
        }
    }

    private companion object {
        const val MAX_PROYECTOS = 8
    }
}

@Composable
private fun ContenidoWidget(
    proyectos: List<Pair<String, String>>,
    estado: EstadoCronometro,
    reloj: RemoteViews?
) {
    val context = LocalContext.current
    // Mientras más alto el widget, más proyectos se muestran (unos 24dp por fila)
    val alto = androidx.glance.LocalSize.current.height.value
    val cuantos = ((alto - (if (reloj == null) 120f else 170f)) / 24f).toInt().coerceIn(1, 8)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colorFijo(FondoWidget))
            .cornerRadius(20.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(R.drawable.logo_deepsource),
                contentDescription = "Deepsource",
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.width(48.dp).height(16.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "DS HORAS",
                style = TextStyle(color = colorFijo(TextoSecundario), fontSize = 10.sp)
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (proyectos.isEmpty()) {
            Text(
                text = "Abre la app para sincronizar tus proyectos",
                style = TextStyle(color = colorFijo(TextoSecundario), fontSize = 12.sp)
            )
        }

        proyectos.take(cuantos).forEach { (nombre, horas) ->
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nombre,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(color = colorFijo(TextoPrincipal), fontSize = 13.sp)
                )
                Text(
                    text = horas,
                    style = TextStyle(
                        color = colorFijo(MoradoClaro),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        if (reloj == null) {
            // En reposo
            Button(
                text = "Registrar horas",
                onClick = actionStartActivity(intentFormulario(context)),
                modifier = GlanceModifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorFijo(Morado),
                    contentColor = colorFijo(Color.White)
                )
            )
        } else {
            // Con cronómetro: proyecto, tiempo y botones Pausar/Reanudar y Finalizar
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(colorFijo(FondoInterno))
                    .cornerRadius(12.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (estado.pausado) "PAUSA" else "EN CURSO",
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(color = colorFijo(TextoSecundario), fontSize = 11.sp)
                )
                Text(
                    text = estado.proyecto.ifBlank { "Sin proyecto" },
                    style = TextStyle(color = colorFijo(MoradoClaro), fontSize = 12.sp)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                AndroidRemoteViews(
                    remoteViews = reloj,
                    modifier = GlanceModifier.width(72.dp).height(24.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Button(
                    text = if (estado.pausado) "Reanudar" else "Pausar",
                    onClick = actionRunCallback<PausarReanudarCallback>(),
                    modifier = GlanceModifier.defaultWeight(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorFijo(Morado),
                        contentColor = colorFijo(Color.White)
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Button(
                    text = "Finalizar",
                    onClick = actionStartActivity(intentFormulario(context, finalizar = true)),
                    modifier = GlanceModifier.defaultWeight(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorFijo(MoradoOscuro),
                        contentColor = colorFijo(Color.White)
                    )
                )
            }
        }
    }
}
