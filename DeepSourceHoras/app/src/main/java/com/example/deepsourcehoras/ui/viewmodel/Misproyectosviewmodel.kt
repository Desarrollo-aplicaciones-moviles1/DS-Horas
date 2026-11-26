package com.example.deepsourcehoras.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsourcehoras.data.remote.ErroresRed
import com.example.deepsourcehoras.data.repository.HorasRepository
import com.example.deepsourcehoras.domain.aportes.CalculoAportes
import com.example.deepsourcehoras.domain.validation.ProyectoValidator
import com.example.deepsourcehoras.model.FiltroProyectos
import com.example.deepsourcehoras.model.MisProyectosUiState
import com.example.deepsourcehoras.model.ProyectoDetalle
import com.example.deepsourcehoras.model.ProyectoErrores
import com.example.deepsourcehoras.recursos.RecursosDispositivo
import com.example.deepsourcehoras.util.baseADia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MisProyectosViewModel(
    private val repositorio: HorasRepository,
    private val recursos: RecursosDispositivo,
    // id del usuario con sesión, para marcar su aporte como "Tú"
    private val usuarioActual: () -> Long? = { null }
) : ViewModel() {

    private val _uiState = MutableStateFlow(MisProyectosUiState())
    val uiState: StateFlow<MisProyectosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Proyectos y aporte del equipo se leen juntos: al registrar horas se actualizan los dos
            combine(repositorio.observarProyectos(), repositorio.observarAportes()) { proyectos, aportes ->
                proyectos to CalculoAportes.porProyecto(aportes, usuarioActual())
            }.collect { (proyectos, aportesPorProyecto) ->
                _uiState.update { estado ->
                    estado.copy(
                        proyectos = proyectos.map {
                            ProyectoDetalle(
                                id = it.id,
                                nombre = it.nombre,
                                cliente = it.cliente,
                                minutos = it.minutos,
                                progreso = it.progreso,
                                fijado = it.fijado,
                                fechaCierre = it.fechaCierre?.let(::baseADia),
                                aportes = aportesPorProyecto[it.id].orEmpty()
                            )
                        }
                    )
                }
            }
        }
    }

    // Abre o cierra el detalle "Aporte del equipo" de un proyecto
    fun onAlternarEquipo(proyectoId: Long) {
        _uiState.update {
            val abiertos = if (proyectoId in it.expandidos) it.expandidos - proyectoId else it.expandidos + proyectoId
            it.copy(expandidos = abiertos)
        }
    }

    fun onFiltroChange(filtro: FiltroProyectos) {
        _uiState.update { it.copy(filtro = filtro) }
    }

    // ---------- Nuevo proyecto (POST /api/proyectos) ----------

    fun onNuevoClick() {
        _uiState.update {
            it.copy(dialogoNuevo = true, nombreNuevo = "", clienteNuevo = "", erroresNuevo = ProyectoErrores())
        }
    }

    fun onCerrarDialogo() {
        if (_uiState.value.guardando) return
        _uiState.update { it.copy(dialogoNuevo = false) }
    }

    fun onNombreChange(valor: String) {
        _uiState.update { it.copy(nombreNuevo = valor, erroresNuevo = it.erroresNuevo.copy(nombre = null)) }
    }

    fun onClienteChange(valor: String) {
        _uiState.update { it.copy(clienteNuevo = valor, erroresNuevo = it.erroresNuevo.copy(cliente = null)) }
    }

    fun onGuardarProyecto() {
        val estado = _uiState.value
        if (estado.guardando) return
        val errores = ProyectoValidator.validar(
            estado.nombreNuevo,
            estado.clienteNuevo,
            estado.proyectos.map { it.nombre }
        )
        if (errores.hayErrores) {
            _uiState.update { it.copy(erroresNuevo = errores) }
            recursos.vibrarError()
            return
        }
        _uiState.update { it.copy(guardando = true) }
        viewModelScope.launch {
            repositorio.crearProyecto(estado.nombreNuevo.trim(), estado.clienteNuevo.trim())
                .onSuccess { creado ->
                    recursos.vibrarExito()
                    recursos.actualizarWidget()
                    _uiState.update {
                        it.copy(
                            guardando = false,
                            dialogoNuevo = false,
                            filtro = FiltroProyectos.EN_CURSO,
                            mensaje = "Proyecto ${creado.nombre} creado"
                        )
                    }
                }
                .onFailure { e ->
                    recursos.vibrarError()
                    _uiState.update { it.copy(guardando = false, mensaje = ErroresRed.mensaje(e)) }
                }
        }
    }

    // ---------- Finalizar proyecto (PATCH /api/proyectos/{id}/finalizar) ----------

    fun onFinalizarClick(proyecto: ProyectoDetalle) {
        _uiState.update { it.copy(porFinalizar = proyecto) }
    }

    fun onCancelarFinalizar() {
        _uiState.update { it.copy(porFinalizar = null) }
    }

    fun onConfirmarFinalizar() {
        val proyecto = _uiState.value.porFinalizar ?: return
        _uiState.update { it.copy(porFinalizar = null) }
        viewModelScope.launch {
            repositorio.finalizarProyecto(proyecto.id)
                .onSuccess {
                    recursos.vibrarExito()
                    recursos.actualizarWidget()
                    _uiState.update { it.copy(mensaje = "${proyecto.nombre} quedó finalizado") }
                }
                .onFailure { e ->
                    recursos.vibrarError()
                    _uiState.update { it.copy(mensaje = ErroresRed.mensaje(e)) }
                }
        }
    }

    fun onMensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }
}
