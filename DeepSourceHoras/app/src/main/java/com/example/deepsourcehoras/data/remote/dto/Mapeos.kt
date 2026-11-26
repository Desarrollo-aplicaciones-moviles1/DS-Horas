package com.example.deepsourcehoras.data.remote.dto

import com.example.deepsourcehoras.data.local.ProyectoEntidad
import com.example.deepsourcehoras.data.local.ProyectoLocal
import com.example.deepsourcehoras.data.local.RegistroEntidad
import com.example.deepsourcehoras.data.local.RegistroLocal
import com.example.deepsourcehoras.model.Feriado

// Conversión de lo que llega del servidor a lo que se guarda en el teléfono y se muestra

fun ProyectoDto.aLocal() = ProyectoLocal(
    id = id,
    nombre = nombre,
    cliente = cliente,
    progreso = progreso.toFloat(),
    fijado = fijado,
    fechaCierre = fechaCierre
)

fun ProyectoDto.aEntidad() = ProyectoEntidad(
    id = id,
    nombre = nombre,
    cliente = cliente,
    progreso = progreso.toFloat(),
    fijado = fijado,
    fechaCierre = fechaCierre,
    minutos = minutos.toInt()
)

fun RegistroDto.aLocal() = RegistroLocal(
    id = id,
    proyectoId = proyectoId,
    tarea = tarea,
    fecha = fecha,
    minutos = minutos,
    foto = foto,
    creadoEn = creadoEn,
    usuarioId = usuarioId,
    usuario = usuarioNombre
)

fun RegistroDto.aEntidad() = RegistroEntidad(
    id = id,
    proyectoId = proyectoId,
    proyecto = proyecto,
    tarea = tarea,
    fecha = fecha,
    minutos = minutos,
    foto = foto,
    usuarioId = usuarioId,
    usuario = usuarioNombre
)

fun FeriadoDto.aModelo() = Feriado(fecha = date, nombre = localName)
