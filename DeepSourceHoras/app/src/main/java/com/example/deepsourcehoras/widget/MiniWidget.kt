package com.example.deepsourcehoras.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.deepsourcehoras.R
import com.example.deepsourcehoras.data.preferences.CronometroStorage
import com.example.deepsourcehoras.domain.cronometro.EstadoCronometro

// Widget 4 (el más chico, 1x1): solo el tiempo.
//  - Sin cronómetro: tocarlo abre el formulario para iniciar.
//  - Con cronómetro: tocarlo pausa o reanuda. Si se agranda, muestra también el proyecto.
class MiniWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val estado = CronometroStorage.leer(context)
        val reloj = if (estado.activo) relojNativo(context, estado, R.layout.widget_cronometro_centro) else null
        provideContent { ContenidoMini(estado, reloj) }
    }
}

class MiniWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MiniWidget()
}

@Composable
private fun ContenidoMini(estado: EstadoCronometro, reloj: RemoteViews?) {
    val context = LocalContext.current
    val ancho = LocalSize.current.width
    val accion = if (estado.activo) actionRunCallback<PausarReanudarCallback>()
    else actionStartActivity(intentFormulario(context))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colorFijo(if (estado.pausado) MoradoOscuro else FondoWidget))
            .cornerRadius(18.dp)
            .padding(6.dp)
            .clickable(accion),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (reloj == null) {
            Text(
                text = "▶",
                style = TextStyle(color = colorFijo(MoradoClaro), fontSize = 22.sp, textAlign = TextAlign.Center)
            )
            Text(
                text = if (ancho >= 110.dp) "Iniciar cronómetro" else "DS",
                style = TextStyle(color = colorFijo(TextoSecundario), fontSize = 10.sp, textAlign = TextAlign.Center)
            )
        } else {
            Text(
                text = if (estado.pausado) "❚❚ PAUSA" else "● EN CURSO",
                style = TextStyle(
                    color = colorFijo(if (estado.pausado) AmarilloPausa else VerdeActivo),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            AndroidRemoteViews(
                remoteViews = reloj,
                modifier = GlanceModifier.fillMaxWidth().height(26.dp)
            )
            if (ancho >= 110.dp) {
                Text(
                    text = estado.proyecto,
                    maxLines = 1,
                    style = TextStyle(color = colorFijo(MoradoClaro), fontSize = 10.sp, textAlign = TextAlign.Center)
                )
            }
        }
    }
}
