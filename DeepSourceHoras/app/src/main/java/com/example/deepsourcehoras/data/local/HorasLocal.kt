package com.example.deepsourcehoras.data.local

// Almacenamiento local de proyectos y registros. El repositorio solo conoce esta interfaz,
// así en las pruebas se reemplaza por un simulado sin necesitar el teléfono.
interface HorasLocal {
    fun proyectos(): List<ProyectoEntidad>
    fun registros(limite: Int? = null): List<RegistroEntidad>
    fun registro(id: Long): RegistroEntidad?
    fun minutosDelDia(fecha: String, excluirId: Long? = null): Int
    fun minutosEntre(desde: String, hasta: String): Int
    // Minutos por persona y proyecto, para mostrar cuánto aportó cada integrante
    fun aportes(): List<AporteEntidad>
    fun reemplazarTodo(proyectos: List<ProyectoLocal>, registros: List<RegistroLocal>)
    fun guardarProyecto(proyecto: ProyectoLocal)
    fun guardarRegistro(registro: RegistroLocal)
    fun eliminarRegistro(id: Long)
}
