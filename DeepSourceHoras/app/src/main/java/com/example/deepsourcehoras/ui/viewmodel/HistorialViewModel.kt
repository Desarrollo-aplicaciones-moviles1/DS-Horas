package com.example.deepsourcehoras.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsourcehoras.data.remote.ErroresRed
import com.example.deepsourcehoras.data.repository.HorasRepository
import com.example.deepsourcehoras.model.HistorialUiState
import com.example.deepsourcehoras.model.RegistroItemUi
import com.example.deepsourcehoras.recursos.RecursosDispositivo
import com.example.deepsourcehoras.util.etiquetaDia
import com.example.deepsourcehoras.util.formatearDuracion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Pantalla "Mis registros": lista completa con editar y eliminar
class HistorialViewModel(
    private val repositorio: HorasRepository,
    private val recursos: RecursosDispositivo
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repositorio.observarRegistros().collect { registros ->
                _uiState.update { estado ->
                    estado.copy(
                        cargando = false,
                        registros = registros.map {
                            RegistroItemUi(
                                id = it.id,
                                tarea = it.tarea,
                                detalle = "${it.proyecto} · ${etiquetaDia(it.fecha)}" +
                                    (it.usuario?.let { quien -> " · $quien" } ?: ""),
                                duracion = formatearDuracion(it.minutos),
                                foto = it.foto
                            )
                        }
                    )
                }
            }
        }
        actualizar()
    }

    fun actualizar() {
        viewModelScope.launch {
            repositorio.sincronizar().onFailure { e ->
                _uiState.update { it.copy(mensaje = ErroresRed.mensaje(e)) }
            }
        }
    }

    fun onEliminarClick(registro: RegistroItemUi) {
        _uiState.update { it.copy(porEliminar = registro) }
    }

    fun onCancelarEliminar() {
        _uiState.update { it.copy(porEliminar = null) }
    }

    fun onConfirmarEliminar() {
        val registro = _uiState.value.porEliminar ?: return
        if (_uiState.value.eliminando) return
        _uiState.update { it.copy(eliminando = true) }
        viewModelScope.launch {
            repositorio.eliminarRegistro(registro.id)
                .onSuccess {
                    recursos.borrarFoto(registro.foto)
                    recursos.vibrarExito()
                    recursos.actualizarWidget()
                    _uiState.update {
                        it.copy(eliminando = false, porEliminar = null, mensaje = "Registro eliminado")
                    }
                }
                .onFailure { e ->
                    recursos.vibrarError()
                    _uiState.update {
                        it.copy(eliminando = false, porEliminar = null, mensaje = ErroresRed.mensaje(e))
                    }
                }
        }
    }

    fun onMensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }
}
