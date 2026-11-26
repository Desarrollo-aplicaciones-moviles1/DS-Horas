package com.example.deepsourcehoras.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val localeChile: Locale = Locale.forLanguageTag("es-CL")

// Formato que ve el usuario y formato con el que se guarda en la base y viaja a los microservicios
const val FORMATO_DIA = "dd/MM/yyyy"
const val FORMATO_BASE = "yyyy-MM-dd"

// 135 -> "2h 15min", 120 -> "2h", 45 -> "45min"
fun formatearDuracion(totalMinutos: Int): String {
    val horas = totalMinutos / 60
    val minutos = totalMinutos % 60
    return when {
        horas == 0 -> "${minutos}min"
        minutos == 0 -> "${horas}h"
        else -> "${horas}h ${minutos}min"
    }
}

// 3725 -> "01:02:05"
fun formatearCronometro(segundos: Int): String =
    String.format(Locale.US, "%02d:%02d:%02d", segundos / 3600, (segundos % 3600) / 60, segundos % 60)

fun hoy(): String = SimpleDateFormat(FORMATO_DIA, localeChile).format(Date())

// Hoy en formato yyyy-MM-dd
fun hoyBase(): String = SimpleDateFormat(FORMATO_BASE, Locale.US).format(Date())

// Lunes de esta semana en formato yyyy-MM-dd
fun inicioSemanaBase(): String {
    val calendario = Calendar.getInstance()
    calendario.firstDayOfWeek = Calendar.MONDAY
    calendario.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    // Si hoy es domingo, set() puede saltar al lunes siguiente
    if (calendario.time.after(Date())) calendario.add(Calendar.WEEK_OF_YEAR, -1)
    return SimpleDateFormat(FORMATO_BASE, Locale.US).format(calendario.time)
}

// Día (dd/MM/yyyy) en que se tomó un instante
fun diaDe(millis: Long): String = SimpleDateFormat(FORMATO_DIA, localeChile).format(Date(millis))

fun hora(millis: Long): String = SimpleDateFormat("HH:mm", localeChile).format(Date(millis))

// "11/11/2026" -> "2026-11-11". Devuelve null si la fecha no existe
fun diaABase(dia: String): String? = convertir(dia, FORMATO_DIA, FORMATO_BASE)

// "2026-11-11" -> "11/11/2026"
fun baseADia(fecha: String): String = convertir(fecha, FORMATO_BASE, FORMATO_DIA) ?: fecha

// Para la lista de registros: "Hoy", "Ayer" o "06/10"
fun etiquetaDia(fechaBase: String): String {
    val formato = SimpleDateFormat(FORMATO_BASE, localeChile)
    val calendario = Calendar.getInstance()
    val fechaHoy = formato.format(calendario.time)
    calendario.add(Calendar.DAY_OF_YEAR, -1)
    val fechaAyer = formato.format(calendario.time)
    return when (fechaBase) {
        fechaHoy -> "Hoy"
        fechaAyer -> "Ayer"
        else -> convertir(fechaBase, FORMATO_BASE, "dd/MM") ?: fechaBase
    }
}

private fun convertir(valor: String, desde: String, hacia: String): String? {
    // Se exige el largo exacto: SimpleDateFormat acepta "1/1/26" aunque no sea el formato
    if (valor.length != desde.length) return null
    val entrada = SimpleDateFormat(desde, localeChile)
    entrada.isLenient = false
    return try {
        val fecha = entrada.parse(valor) ?: return null
        SimpleDateFormat(hacia, localeChile).format(fecha)
    } catch (e: ParseException) {
        null
    }
}
