package com.example.deepsourcehoras.domain.validation

import com.example.deepsourcehoras.model.ProyectoErrores

// Reglas del formulario "Nuevo proyecto". No usa clases de Android
object ProyectoValidator {

    const val MIN_NOMBRE = 3
    const val MAX_NOMBRE = 40
    const val MIN_CLIENTE = 2
    const val MAX_CLIENTE = 40

    // existentes: nombres de los proyectos que ya hay, para no repetir
    fun validar(nombre: String, cliente: String, existentes: List<String>): ProyectoErrores {
        val n = nombre.trim()
        val c = cliente.trim()
        val errorNombre = when {
            n.isEmpty() -> "Escribe el nombre del proyecto"
            n.length < MIN_NOMBRE -> "Mínimo $MIN_NOMBRE caracteres"
            n.length > MAX_NOMBRE -> "Máximo $MAX_NOMBRE caracteres"
            existentes.any { it.equals(n, ignoreCase = true) } -> "Ya existe un proyecto con ese nombre"
            else -> null
        }
        val errorCliente = when {
            c.isEmpty() -> "Escribe el cliente"
            c.length < MIN_CLIENTE -> "Mínimo $MIN_CLIENTE caracteres"
            c.length > MAX_CLIENTE -> "Máximo $MAX_CLIENTE caracteres"
            else -> null
        }
        return ProyectoErrores(nombre = errorNombre, cliente = errorCliente)
    }
}
