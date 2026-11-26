package com.example.deepsourcehoras.data.repository

import com.example.deepsourcehoras.data.local.AporteEntidad
import com.example.deepsourcehoras.data.local.HorasLocal
import com.example.deepsourcehoras.data.local.ProyectoEntidad
import com.example.deepsourcehoras.data.local.RegistroEntidad
import com.example.deepsourcehoras.data.remote.HorasApi
import com.example.deepsourcehoras.data.remote.dto.ProyectoRequestDto
import com.example.deepsourcehoras.data.remote.dto.RegistroRequestDto
import com.example.deepsourcehoras.data.remote.dto.aEntidad
import com.example.deepsourcehoras.data.remote.dto.aLocal
import com.example.deepsourcehoras.model.Autor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import retrofit2.HttpException

// Único punto de acceso a proyectos y registros. Lo usan los ViewModel y el widget.
//  - Escribe siempre en el microservicio ms-horas (fuente de verdad).
//  - Con la respuesta del servidor actualiza la copia local en SQLite.
//  - Las pantallas leen la copia local, así siguen mostrando datos aunque no haya conexión.
class HorasRepository(
    private val api: HorasApi,
    private val local: HorasLocal,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    // Quién está usando la app: queda como autor de cada registro nuevo
    private val autor: () -> Autor? = { null }
) {

    // Sube cada vez que cambia algo, así las pantallas que observan vuelven a leer
    private val cambios = MutableStateFlow(0)

    fun observarProyectos(): Flow<List<ProyectoEntidad>> =
        cambios.map { local.proyectos() }.flowOn(io)

    fun observarRegistros(limite: Int? = null): Flow<List<RegistroEntidad>> =
        cambios.map { local.registros(limite) }.flowOn(io)

    // Minutos de cada persona por proyecto (para "Aporte del equipo")
    fun observarAportes(): Flow<List<AporteEntidad>> =
        cambios.map { local.aportes() }.flowOn(io)

    // Trae todo del servidor y reemplaza la copia local
    suspend fun sincronizar(): Result<Unit> = ejecutar {
        val proyectos = api.proyectos()
        val registros = api.registros()
        local.reemplazarTodo(proyectos.map { it.aLocal() }, registros.map { it.aLocal() })
    }

    suspend fun proyectosEnCurso(): List<ProyectoEntidad> = withContext(io) {
        local.proyectos().filter { it.enCurso }
    }

    suspend fun minutosDelDia(fecha: String, excluirId: Long? = null): Int = withContext(io) {
        local.minutosDelDia(fecha, excluirId)
    }

    suspend fun minutosEntre(desde: String, hasta: String): Int = withContext(io) {
        local.minutosEntre(desde, hasta)
    }

    suspend fun ultimoRegistro(): RegistroEntidad? = withContext(io) {
        local.registros(1).firstOrNull()
    }

    suspend fun obtenerRegistro(id: Long): RegistroEntidad? = withContext(io) {
        local.registro(id)
    }

    // fecha en formato yyyy-MM-dd
    suspend fun registrar(
        proyectoId: Long,
        fecha: String,
        minutos: Int,
        tarea: String,
        foto: String?
    ): Result<RegistroEntidad> = ejecutar {
        val quien = autor()
        val creado = api.crearRegistro(
            RegistroRequestDto(proyectoId, tarea, fecha, minutos, foto, quien?.id, quien?.nombre)
        )
        local.guardarRegistro(creado.aLocal())
        creado.aEntidad()
    }

    suspend fun actualizarRegistro(
        id: Long,
        proyectoId: Long,
        fecha: String,
        minutos: Int,
        tarea: String,
        foto: String?
    ): Result<RegistroEntidad> = ejecutar {
        val editado = api.actualizarRegistro(id, RegistroRequestDto(proyectoId, tarea, fecha, minutos, foto))
        local.guardarRegistro(editado.aLocal())
        editado.aEntidad()
    }

    suspend fun eliminarRegistro(id: Long): Result<Unit> = ejecutar {
        val respuesta = api.eliminarRegistro(id)
        // Si ya no existía en el servidor, igual se borra la copia local
        if (!respuesta.isSuccessful && respuesta.code() != 404) throw HttpException(respuesta)
        local.eliminarRegistro(id)
    }

    suspend fun crearProyecto(nombre: String, cliente: String): Result<ProyectoEntidad> = ejecutar {
        val creado = api.crearProyecto(ProyectoRequestDto(nombre = nombre, cliente = cliente))
        local.guardarProyecto(creado.aLocal())
        creado.aEntidad()
    }

    suspend fun finalizarProyecto(id: Long): Result<ProyectoEntidad> = ejecutar {
        val cerrado = api.finalizarProyecto(id)
        local.guardarProyecto(cerrado.aLocal())
        cerrado.aEntidad()
    }

    // Corre en segundo plano, convierte las excepciones en Result y avisa a las pantallas si salió bien
    private suspend fun <T> ejecutar(bloque: suspend () -> T): Result<T> {
        val resultado = withContext(io) {
            try {
                Result.success(bloque())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        if (resultado.isSuccess) cambios.update { it + 1 }
        return resultado
    }
}
