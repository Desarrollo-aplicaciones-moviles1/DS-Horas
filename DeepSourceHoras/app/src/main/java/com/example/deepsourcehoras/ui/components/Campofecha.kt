package com.example.deepsourcehoras.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    valor: String,
    label: String,
    error: String?,
    onFechaSeleccionada: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrar by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val sacudida = desplazamientoError(error)

    // Abre el calendario al tocar cualquier parte del campo
    LaunchedEffect(interaction) {
        interaction.interactions.collect {
            if (it is PressInteraction.Release) mostrar = true
        }
    }

    OutlinedTextField(
        value = valor,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Elegir fecha") },
        isError = error != null,
        supportingText = {
            error?.let { MensajeError(it) }
        },
        singleLine = true,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = sacudida.dp.toPx() }
    )

    if (mostrar) {
        val anioActual = Calendar.getInstance().get(Calendar.YEAR)
        val limiteHoy = remember { hoyEnUtc() }
        // Solo días de este año y hasta hoy: no se registran horas a futuro
        val diasPermitidos = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= limiteHoy
                override fun isSelectableYear(year: Int): Boolean = year == anioActual
            }
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = aMillisUtc(valor),
            yearRange = anioActual..anioActual,
            selectableDates = diasPermitidos
        )
        DatePickerDialog(
            onDismissRequest = { mostrar = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onFechaSeleccionada(formatear(it)) }
                    mostrar = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrar = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// El DatePicker trabaja en UTC; si no se fija la zona, puede salir un día antes
private fun formatear(millis: Long): String {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-CL"))
    formato.timeZone = TimeZone.getTimeZone("UTC")
    return formato.format(Date(millis))
}

private fun aMillisUtc(dia: String): Long? {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-CL"))
    formato.timeZone = TimeZone.getTimeZone("UTC")
    formato.isLenient = false
    return try {
        formato.parse(dia)?.time
    } catch (e: ParseException) {
        null
    }
}

// Medianoche de hoy (fecha local) expresada en UTC, como la entrega el DatePicker
private fun hoyEnUtc(): Long {
    val local = Calendar.getInstance()
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.clear()
    utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    return utc.timeInMillis
}
