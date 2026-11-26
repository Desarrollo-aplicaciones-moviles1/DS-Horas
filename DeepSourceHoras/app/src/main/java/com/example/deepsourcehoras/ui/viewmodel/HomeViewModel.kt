package com.example.deepsourcehoras.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsourcehoras.data.preferences.SesionStore
import com.example.deepsourcehoras.data.remote.ErroresRed
import com.example.deepsourcehoras.data.repository.HorasRepository
import com.example.deepsourcehoras.model.HomeUiState
import com.example.deepsourcehoras.model.Proyecto
import com.example.deepsourcehoras.model.RegistroHoras
import com.example.deepsourcehoras.util.etiquetaDia
import com.example.deepsourcehoras.util.formatearDuracion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repositorio: HorasRepository,
    private val sesion: SesionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(userName = sesion.usuario() ?: "Usuario"))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Lee la copia local; se actualiza solo cada vez que cambia algo en el repositorio
        viewModelScope.launch {
            combine(
                repositorio.observarProyectos(),
                repositorio.observarRegistros(LIMITE_REGISTROS)
            ) { proyectos, registros -> proyectos to registros }
                .collect { (proyectos, registros) ->
                    _uiState.update { estado ->
                        estado.copy(
                            proyectos = proyectos
                                .filter { it.enCurso }
                                .map { Proyecto(it.nombre, it.minutos, it.progreso) },
                            registros = registros.map {
                                RegistroHoras(
                                    tarea = it.tarea,
                                    detalle = "${it.proyecto} · ${etiquetaDia(it.fecha)}",
                                    duracion = formatearDuracion(it.minutos),
                                    conFoto = it.foto != null
                                )
                            }
                        )
                    }
                }
        }
        sincronizar()
    }

    // Trae los datos del microservicio. Si falla, se siguen viendo los datos guardados
    fun sincronizar() {
        if (_uiState.value.sincronizando) return
        viewModelScope.launch {
            _uiState.update { it.copy(sincronizando = true) }
            val resultado = repositorio.sincronizar()
            _uiState.update {
                it.copy(
                    sincronizando = false,
                    errorConexion = resultado.exceptionOrNull()?.let(ErroresRed::mensaje)
                )
            }
        }
    }

    fun onCerrarSesion() {
        sesion.cerrar()
    }

    private companion object {
        const val LIMITE_REGISTROS = 5
    }
}
