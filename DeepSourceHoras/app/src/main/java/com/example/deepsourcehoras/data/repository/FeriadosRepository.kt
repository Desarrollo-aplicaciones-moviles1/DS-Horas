package com.example.deepsourcehoras.data.repository

import com.example.deepsourcehoras.data.remote.FeriadosApi
import com.example.deepsourcehoras.data.remote.dto.aModelo
import com.example.deepsourcehoras.model.Feriado
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// Feriados de Chile desde la API externa Nager.Date.
// Se guardan solo en memoria: no se mezclan con la base local ni con los microservicios.
class FeriadosRepository(
    private val api: FeriadosApi,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    private val cache = mutableMapOf<Int, List<Feriado>>()
    private val candado = Mutex()

    // Feriados nacionales del año, ordenados por fecha (se piden una sola vez por año)
    suspend fun delAnio(anio: Int): Result<List<Feriado>> = withContext(io) {
        try {
            val lista = candado.withLock {
                cache.getOrPut(anio) {
                    api.feriados(anio)
                        .filter { it.global != false }
                        .map { it.aModelo() }
                        .distinctBy { it.fecha }
                        .sortedBy { it.fecha }
                }
            }
            Result.success(lista)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // El feriado de una fecha (yyyy-MM-dd) o null si es día hábil o no se pudo consultar
    suspend fun buscar(fecha: String): Feriado? {
        val anio = fecha.take(4).toIntOrNull() ?: return null
        return delAnio(anio).getOrNull()?.firstOrNull { it.fecha == fecha }
    }
}
