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
import com.example.deepsourcehoras.R
import com.example.deepsourcehoras.data.preferences.CronometroStorage
import com.example.deepsourcehoras.domain.cronometro.EstadoCronometro
import com.example.deepsourcehoras.util.hora

// Widget 2 (mediano): cronómetro del proyecto en curso, con Pausar/Reanudar sin abrir la app.
// Se adapta al tamaño: bajito muestra todo en una fila; más alto agrega la hora de inicio.
class CronometroWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val estado = CronometroStorage.leer(context)
        val reloj = if (estado.activo) relojNativo(context, estado, R.layout.widget_cronometro_grande) else null
        provideContent { ContenidoCronometro(estado, reloj) }
    }
}

class CronometroWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CronometroWidget()
}

@Composable
private fun ContenidoCronometro(estado: EstadoCronometro, reloj: RemoteViews?) {
    val context = LocalContext.current
    val alto = LocalSize.current.height
    val bajito = alto < 100.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colorFijo(FondoWidget))
            .cornerRadius(20.dp)
            .padding(horizontal = 12.dp, vertical = if (bajito) 6.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = when {
                    !estado.activo -> "●  CRONÓMETRO"
                    estado.pausado -> "❚❚  EN PAUSA"
                    else -> "●  EN CURSO"
                },
                style = TextStyle(
                    color = colorFijo(
                        when {
                            !estado.activo -> TextoSecundario
                            estado.pausado -> AmarilloPausa
                            else -> VerdeActivo
                        }
                    ),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (estado.activo) {
                Text(
                    text = estado.proyecto,
                    maxLines = 1,
                    style = TextStyle(color = colorFijo(MoradoClaro), fontSize = 11.sp)
                )
            }
        }

        if (!bajito) Spacer(modifier = GlanceModifier.height(4.dp))

        if (reloj == null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = "00:00",
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(color = colorFijo(TextoPrincipal), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                )
                if (bajito) BotonWidget("Iniciar", actionStartActivity(intentFormulario(context)), Morado)
            }
            if (!bajito) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                Button(
                    text = "Iniciar",
                    onClick = actionStartActivity(intentFormulario(context)),
                    modifier = GlanceModifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorFijo(Morado),
                        contentColor = colorFijo(Color.White)
                    )
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                AndroidRemoteViews(
                    remoteViews = reloj,
                    modifier = GlanceModifier.defaultWeight().height(34.dp)
                )
                if (bajito) {
                    BotonWidget(if (estado.pausado) "▶" else "❚❚", actionRunCallback<PausarReanudarCallback>(), Morado)
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    BotonWidget("■", actionStartActivity(intentFormulario(context, finalizar = true)), MoradoOscuro)
                }
            }
            if (!bajito) {
                Text(
                    text = "Desde las ${hora(estado.primerInicio)}",
                    style = TextStyle(color = colorFijo(TextoSecundario), fontSize = 10.sp)
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
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
                    Spacer(modifier = GlanceModifier.width(6.dp))
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
}

@Composable
internal fun BotonWidget(texto: String, accion: androidx.glance.action.Action, fondo: Color) {
    Button(
        text = texto,
        onClick = accion,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorFijo(fondo),
            contentColor = colorFijo(Color.White)
        )
    )
}
