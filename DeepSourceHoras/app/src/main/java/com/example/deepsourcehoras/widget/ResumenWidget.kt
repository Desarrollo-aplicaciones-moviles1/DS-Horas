package com.example.deepsourcehoras.widget

import android.content.Context
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
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import com.example.deepsourcehoras.di.Dependencias
import com.example.deepsourcehoras.util.formatearDuracion
import com.example.deepsourcehoras.util.hoyBase
import com.example.deepsourcehoras.util.inicioSemanaBase

// Widget 3: horas de hoy, de la semana y el último registro.
// Ancho muestra un tercer bloque con el proyecto de más horas; alto agrega el último registro y el botón.
class ResumenWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repositorio = Dependencias.horasRepository(context)
        val hoy = hoyBase()
        val minutosHoy = repositorio.minutosEntre(hoy, hoy)
        val minutosSemana = repositorio.minutosEntre(inicioSemanaBase(), hoy)
        val top = repositorio.proyectosEnCurso().maxByOrNull { it.minutos }
        val ultimo = repositorio.ultimoRegistro()
        val textoUltimo = ultimo?.let { "${it.tarea} · ${formatearDuracion(it.minutos)}" } ?: "Sin registros todavía"

        provideContent {
            ContenidoResumen(
                hoy = formatearDuracion(minutosHoy),
                semana = formatearDuracion(minutosSemana),
                top = top?.let { it.nombre to formatearDuracion(it.minutos) },
                ultimo = textoUltimo
            )
        }
    }
}

class ResumenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ResumenWidget()
}

@Composable
private fun ContenidoResumen(hoy: String, semana: String, top: Pair<String, String>?, ultimo: String) {
    val context = LocalContext.current
    val tamano = LocalSize.current
    val ancho = tamano.width >= 300.dp
    val alto = tamano.height >= 140.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colorFijo(FondoWidget))
            .cornerRadius(20.dp)
            .padding(12.dp)
    ) {
        Text(text = "MI RESUMEN", style = TextStyle(color = colorFijo(TextoSecundario), fontSize = 10.sp, fontWeight = FontWeight.Bold))
        Spacer(modifier = GlanceModifier.height(6.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Bloque("Hoy", hoy, GlanceModifier.defaultWeight())
            Spacer(modifier = GlanceModifier.width(8.dp))
            Bloque("Esta semana", semana, GlanceModifier.defaultWeight())
            if (ancho && top != null) {
                Spacer(modifier = GlanceModifier.width(8.dp))
                Bloque(top.first, top.second, GlanceModifier.defaultWeight())
            }
        }
        if (alto) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = "Último: $ultimo",
                maxLines = 1,
                style = TextStyle(color = colorFijo(TextoPrincipal), fontSize = 11.sp)
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            Button(
                text = "Registrar horas",
                onClick = actionStartActivity(intentFormulario(context)),
                modifier = GlanceModifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorFijo(Morado),
                    contentColor = colorFijo(Color.White)
                )
            )
        }
    }
}

@Composable
private fun Bloque(titulo: String, valor: String, modifier: GlanceModifier) {
    Column(
        modifier = modifier
            .background(colorFijo(FondoInterno))
            .cornerRadius(12.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = titulo, maxLines = 1, style = TextStyle(color = colorFijo(TextoSecundario), fontSize = 10.sp))
        Text(
            text = valor,
            style = TextStyle(color = colorFijo(MoradoClaro), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        )
    }
}
