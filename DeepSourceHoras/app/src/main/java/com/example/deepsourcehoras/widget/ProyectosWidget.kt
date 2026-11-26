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
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
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
import com.example.deepsourcehoras.data.preferences.CronometroStorage
import com.example.deepsourcehoras.di.Dependencias
import com.example.deepsourcehoras.domain.cronometro.EstadoCronometro
import com.example.deepsourcehoras.util.formatearDuracion

// Widget 5: lista de proyectos en curso, cada uno con ▶ para iniciar su cronómetro
// sin abrir la app. El que está corriendo se marca y muestra su tiempo.
// Mientras más alto se deja, más proyectos muestra.
class ProyectosWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val estado = CronometroStorage.leer(context)
        val proyectos = Dependencias.horasRepository(context)
            .proyectosEnCurso()
            .map { it.nombre to formatearDuracion(it.minutos) }
        val reloj = if (estado.activo) relojNativo(context, estado) else null
        provideContent { ContenidoProyectos(proyectos, estado, reloj) }
    }
}

class ProyectosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProyectosWidget()
}

@Composable
private fun ContenidoProyectos(
    proyectos: List<Pair<String, String>>,
    estado: EstadoCronometro,
    reloj: RemoteViews?
) {
    val context = LocalContext.current
    val alto = LocalSize.current.height
    // Cada fila ocupa unos 40dp; el título y el margen, unos 40dp
    val cuantos = ((alto.value - 40f) / 40f).toInt().coerceIn(1, 8)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colorFijo(FondoWidget))
            .cornerRadius(20.dp)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity(intentApp(context)))
        ) {
            Text(
                text = "MIS PROYECTOS",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = colorFijo(TextoSecundario), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
            Text(text = "toca ▶ para iniciar", style = TextStyle(color = colorFijo(TextoSecundario), fontSize = 9.sp))
        }
        Spacer(modifier = GlanceModifier.height(6.dp))

        if (proyectos.isEmpty()) {
            Text(
                text = "Abre la app para sincronizar tus proyectos",
                style = TextStyle(color = colorFijo(TextoSecundario), fontSize = 12.sp)
            )
        }

        // El proyecto que está corriendo siempre va primero
        val ordenados = proyectos.sortedByDescending { it.first == estado.proyecto && estado.activo }
        ordenados.take(cuantos).forEach { (nombre, horas) ->
            val esteCorre = estado.activo && estado.proyecto == nombre
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .background(colorFijo(if (esteCorre) MoradoOscuro else FondoInterno))
                    .cornerRadius(10.dp)
                    .padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = nombre,
                        maxLines = 1,
                        style = TextStyle(color = colorFijo(TextoPrincipal), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = if (esteCorre) (if (estado.pausado) "En pausa" else "En curso") else horas,
                        style = TextStyle(
                            color = colorFijo(if (esteCorre) (if (estado.pausado) AmarilloPausa else VerdeActivo) else MoradoClaro),
                            fontSize = 10.sp
                        )
                    )
                }
                if (esteCorre && reloj != null) {
                    AndroidRemoteViews(remoteViews = reloj, modifier = GlanceModifier.width(70.dp).height(22.dp))
                    Spacer(modifier = GlanceModifier.width(4.dp))
                }
                // Con otro proyecto corriendo, este no se puede iniciar (no se mezclan horas)
                val bloqueado = estado.activo && !esteCorre
                Button(
                    text = when {
                        esteCorre && !estado.pausado -> "❚❚"
                        else -> "▶"
                    },
                    onClick = actionRunCallback<IniciarProyectoCallback>(actionParametersOf(Widgets.PROYECTO to nombre)),
                    enabled = !bloqueado,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorFijo(if (bloqueado) FondoWidget else Morado),
                        contentColor = colorFijo(if (bloqueado) TextoSecundario else Color.White)
                    )
                )
            }
        }

        if (estado.activo) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Button(
                text = "Finalizar y registrar",
                onClick = actionStartActivity(intentFormulario(context, finalizar = true)),
                modifier = GlanceModifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorFijo(MoradoOscuro),
                    contentColor = colorFijo(Color.White)
                )
            )
        }
    }
}
