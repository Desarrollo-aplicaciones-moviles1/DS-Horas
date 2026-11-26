package com.example.deepsourcehoras.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsourcehoras.data.preferences.CronometroStore
import com.example.deepsourcehoras.data.remote.ErroresRed
import com.example.deepsourcehoras.data.repository.FeriadosRepository
import com.example.deepsourcehoras.data.repository.HorasRepository
import com.example.deepsourcehoras.domain.cronometro.EstadoCronometro
import com.example.deepsourcehoras.domain.validation.DatosRegistro
import com.example.deepsourcehoras.domain.validation.RegistroHorasValidator
import com.example.deepsourcehoras.model.RegistroHorasUiState
import com.example.deepsourcehoras.model.ResumenRegistro
import com.example.deepsourcehoras.recursos.RecursosDispositivo
import com.example.deepsourcehoras.util.baseADia
import com.example.deepsourcehoras.util.diaABase
import com.example.deepsourcehoras.util.diaDe
import com.example.deepsourcehoras.util.hora
import com.example.deepsourcehoras.util.hoy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Formulario para registrar horas nuevas (registroId = null) o editar un registro existente
class RegistroHorasViewModel(
    private val registroId: Long?,
    private val repositorio: HorasRepository,
    private val feriados: FeriadosRepository,
    private val recursos: RecursosDispositivo,
    private val cronometro: CronometroStore,
    private val reloj: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroHorasUiState(dia = hoy(), editando = registroId != null))
    val uiState: StateFlow<RegistroHorasUiState> = _uiState.asStateFlow()

    private var cronometroJob: Job? = null
    private var estadoCronometro = EstadoCronometro()
    private var feriadoJob: Job? = null
    private var tiempoJob: Job? = null

    // Nombre del proyecto -> id en el microservicio
    private var idsProyectos: Map<String, Long> = emptyMap()

    // Foto que se está sacando, la que ya tenía el registro (al editar) y si se guardó
    private var fotoPendiente: String? = null
    private var fotoOriginal: String? = null
    private var guardado = false

    init {
        viewModelScope.launch {
            cargarProyectos()
            if (registroId != null) cargarRegistro(registroId) else restaurarCronometro()
            revisarFeriado(_uiState.value.dia)
        }
    }

    // Solo se puede registrar en proyectos que siguen en curso
    private suspend fun cargarProyectos() {
        val enCurso = repositorio.proyectosEnCurso()
        idsProyectos = enCurso.associate { it.nombre to it.id }
        _uiState.update { it.copy(proyectos = enCurso.map { p -> p.nombre }) }
    }

    // Al editar se rellena el formulario con lo que ya estaba guardado
    private suspend fun cargarRegistro(id: Long) {
        val registro = repositorio.obtenerRegistro(id)
        if (registro == null) {
            mostrarMensaje("No encontramos ese registro")
            return
        }
        // Si su proyecto ya se cerró, igual se deja como opción para no cambiarlo sin querer
        if (registro.proyecto !in idsProyectos) {
            idsProyectos = idsProyectos + (registro.proyecto to registro.proyectoId)
            _uiState.update { it.copy(proyectos = it.proyectos + registro.proyecto) }
        }
        fotoOriginal = registro.foto
        _uiState.update {
            it.copy(
                proyecto = registro.proyecto,
                dia = baseADia(registro.fecha),
                horas = (registro.minutos / 60).toString(),
                minutos = (registro.minutos % 60).toString(),
                tarea = registro.tarea,
                fotoRuta = registro.foto
            )
        }
    }

    private fun actualizarWidget() {
        viewModelScope.launch { recursos.actualizarWidget() }
    }

    fun onProyectoChange(valor: String) {
        // Si hay cronómetro, la notificación y el widget también muestran el cambio
        if (_uiState.value.corriendo) {
            estadoCronometro = estadoCronometro.copy(proyecto = valor)
            cronometro.guardar(estadoCronometro)
            mostrarNotificacionCronometro()
            actualizarWidget()
        }
        _uiState.update {
            it.copy(proyecto = valor, errores = it.errores.copy(proyecto = null))
        }
    }

    fun onDiaChange(valor: String) {
        _uiState.update {
            it.copy(dia = valor, avisoFeriado = null, errores = it.errores.copy(dia = null))
        }
        feriadoJob?.cancel()
        feriadoJob = viewModelScope.launch { revisarFeriado(valor) }
        // Cambia lo que ya hay registrado ese día, así que se vuelve a revisar el tiempo
        revisarTiempoEnVivo()
    }

    // API externa: avisa si el día elegido es feriado (no impide registrar)
    private suspend fun revisarFeriado(dia: String) {
        val fecha = diaABase(dia) ?: return
        val feriado = feriados.buscar(fecha)
        _uiState.update {
            if (it.dia != dia) it
            else it.copy(avisoFeriado = feriado?.let { f -> "Este día es feriado: ${f.nombre}" })
        }
    }

    fun onHorasChange(valor: String) {
        // Solo dígitos, máximo 2
        val limpio = valor.filter { it.isDigit() }.take(2)
        _uiState.update {
            it.copy(horas = limpio, horaInicio = null) // si edita a mano, ya no es el tiempo calculado
        }
        revisarTiempoEnVivo()
    }

    fun onMinutosChange(valor: String) {
        val limpio = valor.filter { it.isDigit() }.take(2)
        _uiState.update { it.copy(minutos = limpio, horaInicio = null) }
        revisarTiempoEnVivo()
    }

    fun onTareaChange(valor: String) {
        _uiState.update {
            it.copy(tarea = valor, errores = it.errores.copy(tarea = null))
        }
    }

    // Validación mientras se escribe: el error de exceso de tiempo aparece al tiro,
    // sin esperar a que se apriete "Registrar"
    private fun revisarTiempoEnVivo() {
        val estado = _uiState.value
        // Primero lo que se puede saber sin consultar la base (24 h / 59 min)
        aplicarErroresDeTiempo(RegistroHorasValidator.erroresDeTiempo(estado.horas, estado.minutos))

        // Después el tope del día, sumando lo que ya está registrado esa fecha
        val fecha = diaABase(estado.dia) ?: return
        tiempoJob?.cancel()
        tiempoJob = viewModelScope.launch {
            val yaRegistrado = repositorio.minutosDelDia(fecha, registroId)
            val actual = _uiState.value
            if (actual.horas != estado.horas || actual.minutos != estado.minutos || actual.dia != estado.dia) return@launch
            aplicarErroresDeTiempo(RegistroHorasValidator.erroresDeTiempo(actual.horas, actual.minutos, yaRegistrado))
        }
    }

    private fun aplicarErroresDeTiempo(tiempo: com.example.deepsourcehoras.model.RegistroHorasErrores) {
        val antes = _uiState.value.errores
        val habiaError = antes.horas != null || antes.minutos != null || antes.tiempo != null
        val hayError = tiempo.horas != null || tiempo.minutos != null || tiempo.tiempo != null
        _uiState.update {
            it.copy(errores = it.errores.copy(horas = tiempo.horas, minutos = tiempo.minutos, tiempo = tiempo.tiempo))
        }
        // Vibra solo cuando aparece el error, no en cada tecla
        if (hayError && !habiaError) recursos.vibrarError()
    }

    // ---------- Cronómetro ----------

    // Si la app se cerró con el cronómetro andando o en pausa, lo retoma donde iba
    private fun restaurarCronometro() {
        val guardadoEnTelefono = cronometro.leer()
        if (!guardadoEnTelefono.activo) return

        estadoCronometro = guardadoEnTelefono
        _uiState.update {
            it.copy(
                corriendo = true,
                pausado = guardadoEnTelefono.pausado,
                proyecto = guardadoEnTelefono.proyecto,
                horaInicio = hora(guardadoEnTelefono.primerInicio),
                segundos = segundosTranscurridos()
            )
        }
        if (!guardadoEnTelefono.pausado) empezarConteo()
    }

    private fun segundosTranscurridos(): Int =
        (estadoCronometro.transcurrido(reloj()) / 1000).toInt()

    private fun empezarConteo() {
        cronometroJob?.cancel()
        cronometroJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                // Se calcula desde los tiempos guardados para que no se desfase
                _uiState.update { it.copy(segundos = segundosTranscurridos()) }
            }
        }
    }

    private fun mostrarNotificacionCronometro() {
        recursos.mostrarCronometro(
            estadoCronometro.proyecto,
            estadoCronometro.transcurrido(reloj()),
            estadoCronometro.pausado
        )
    }

    fun onIniciarClick() {
        val estado = _uiState.value
        if (estado.corriendo || estado.editando) return

        // El cronómetro necesita un proyecto, porque el widget lo muestra
        if (estado.proyecto.isBlank()) {
            _uiState.update {
                it.copy(errores = it.errores.copy(proyecto = "Selecciona un proyecto para iniciar"))
            }
            recursos.vibrarError()
            return
        }

        estadoCronometro = EstadoCronometro.iniciar(estado.proyecto, reloj())
        cronometro.guardar(estadoCronometro)
        mostrarNotificacionCronometro()
        recursos.vibrarCorta()
        actualizarWidget()

        _uiState.update {
            it.copy(
                corriendo = true,
                pausado = false,
                segundos = 0,
                horaInicio = hora(estadoCronometro.primerInicio),
                horas = "",
                minutos = "",
                errores = it.errores.copy(horas = null, minutos = null, tiempo = null)
            )
        }
        empezarConteo()
    }

    // Pausa: el tiempo deja de correr (ej. si la persona sale un rato) sin perder lo acumulado
    fun onPausarClick() {
        if (!_uiState.value.corriendo || _uiState.value.pausado) return
        cronometroJob?.cancel()
        estadoCronometro = estadoCronometro.pausar(reloj())
        cronometro.guardar(estadoCronometro)
        mostrarNotificacionCronometro()
        recursos.vibrarCorta()
        actualizarWidget()
        _uiState.update { it.copy(pausado = true, segundos = segundosTranscurridos()) }
    }

    fun onReanudarClick() {
        if (!_uiState.value.corriendo || !_uiState.value.pausado) return
        estadoCronometro = estadoCronometro.reanudar(reloj())
        cronometro.guardar(estadoCronometro)
        mostrarNotificacionCronometro()
        recursos.vibrarCorta()
        actualizarWidget()
        _uiState.update { it.copy(pausado = false, segundos = segundosTranscurridos()) }
        empezarConteo()
    }

    fun onFinalizarClick() {
        if (!_uiState.value.corriendo) return
        cronometroJob?.cancel()
        val segundos = segundosTranscurridos()
        // Se redondea hacia arriba para que cualquier tiempo cuente al menos 1 minuto
        val totalMinutos = (segundos + 59) / 60
        val dia = diaDe(estadoCronometro.primerInicio)

        cronometro.detener()
        estadoCronometro = EstadoCronometro()
        recursos.ocultarCronometro()
        recursos.vibrarExito()
        actualizarWidget()

        _uiState.update {
            it.copy(
                corriendo = false,
                pausado = false,
                segundos = segundos,
                // El registro queda en el día en que partió el cronómetro
                dia = dia,
                horas = (totalMinutos / 60).toString(),
                minutos = (totalMinutos % 60).toString()
            )
        }
        feriadoJob?.cancel()
        feriadoJob = viewModelScope.launch { revisarFeriado(dia) }
        revisarTiempoEnVivo()
    }

    // Cuando se toca "Finalizar" en el widget o en la notificación
    fun finalizarSiCorre() {
        if (_uiState.value.corriendo) onFinalizarClick()
    }

    // ---------- Foto de evidencia ----------

    fun prepararFoto(): Uri {
        val foto = recursos.nuevaFoto()
        fotoPendiente = foto.ruta
        return foto.uri
    }

    fun onFotoTomada(exito: Boolean) {
        val ruta = fotoPendiente ?: return
        fotoPendiente = null

        if (exito && recursos.fotoValida(ruta)) {
            // Si ya había una foto nueva, se reemplaza
            descartarFotoActual()
            _uiState.update { it.copy(fotoRuta = ruta) }
            recursos.vibrarCorta()
        } else {
            recursos.borrarFoto(ruta)
        }
    }

    // Imagen elegida con el selector de fotos de Android (no necesita permisos).
    // Se copia a la carpeta privada de la app y desde ahí se trata igual que una foto de la cámara.
    fun onImagenGaleria(uri: Uri?) {
        if (uri == null) return // el usuario cerró el selector sin elegir
        viewModelScope.launch {
            val ruta = recursos.copiarDeGaleria(uri)
            if (ruta == null) {
                recursos.vibrarError()
                mostrarMensaje("No se pudo cargar la imagen. Prueba con otra")
                return@launch
            }
            // Si ya había una foto nueva, se reemplaza
            descartarFotoActual()
            _uiState.update { it.copy(fotoRuta = ruta) }
            recursos.vibrarCorta()
        }
    }

    fun onQuitarFoto() {
        descartarFotoActual()
        _uiState.update { it.copy(fotoRuta = null) }
    }

    // La foto que ya estaba guardada en el registro solo se borra si se guardan los cambios
    private fun descartarFotoActual() {
        val actual = _uiState.value.fotoRuta
        if (actual != null && actual != fotoOriginal) recursos.borrarFoto(actual)
    }

    fun onCamaraNoDisponible() {
        recursos.borrarFoto(fotoPendiente)
        fotoPendiente = null
        mostrarMensaje("No encontramos una app de cámara en el teléfono")
    }

    fun onPermisoCamaraDenegado() {
        mostrarMensaje("Sin permiso de cámara no se puede adjuntar la foto. Puedes activarlo en Ajustes")
    }

    fun onPermisoNotificacionesDenegado() {
        mostrarMensaje("Sin permiso de notificaciones no verás el aviso al registrar ni el cronómetro en la barra")
    }

    fun onMensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }

    private fun mostrarMensaje(texto: String) {
        _uiState.update { it.copy(mensaje = texto) }
    }

    // ---------- Registrar / guardar cambios ----------

    fun onRegistrarClick(onSuccess: (ResumenRegistro) -> Unit) {
        val estado = _uiState.value
        if (estado.guardando || estado.corriendo) return
        _uiState.update { it.copy(guardando = true) }

        viewModelScope.launch {
            val fecha = diaABase(estado.dia)
            // Al editar no se cuenta el mismo registro en el tope de 24 horas del día
            val yaRegistrado = if (fecha != null) repositorio.minutosDelDia(fecha, registroId) else 0

            // Las reglas están en RegistroHorasValidator, acá solo se usa el resultado
            val errores = RegistroHorasValidator.validar(
                DatosRegistro(estado.proyecto, estado.dia, estado.horas, estado.minutos, estado.tarea),
                hoy(),
                yaRegistrado
            )
            val proyectoId = idsProyectos[estado.proyecto]
            if (errores.hayErrores || fecha == null || proyectoId == null) {
                val conProyecto = if (proyectoId == null && errores.proyecto == null) {
                    errores.copy(proyecto = "Selecciona un proyecto de la lista")
                } else {
                    errores
                }
                _uiState.update { it.copy(errores = conProyecto, guardando = false) }
                recursos.vibrarError()
                return@launch
            }

            val horas = estado.horas.toIntOrNull() ?: 0
            val minutos = estado.minutos.toIntOrNull() ?: 0
            val total = horas * 60 + minutos
            val tarea = estado.tarea.trim()

            // Se envía al microservicio ms-horas (POST para nuevo, PUT para editar)
            val resultado = if (registroId == null) {
                repositorio.registrar(proyectoId, fecha, total, tarea, estado.fotoRuta)
            } else {
                repositorio.actualizarRegistro(registroId, proyectoId, fecha, total, tarea, estado.fotoRuta)
            }
            resultado.onFailure { e ->
                _uiState.update { it.copy(guardando = false, mensaje = ErroresRed.mensaje(e)) }
                recursos.vibrarError()
                return@launch
            }

            guardado = true
            if (registroId == null) {
                recursos.notificarHorasRegistradas(estado.proyecto, total)
            } else if (fotoOriginal != null && fotoOriginal != estado.fotoRuta) {
                // Se cambió o se quitó la foto: la anterior ya no sirve
                recursos.borrarFoto(fotoOriginal)
            }
            recursos.vibrarExito()
            // Se espera a que los widgets se actualicen antes de salir de la pantalla
            recursos.actualizarWidget()
            _uiState.update { it.copy(guardando = false) }
            onSuccess(
                ResumenRegistro(estado.proyecto, horas, minutos, estado.dia, estado.fotoRuta, editado = registroId != null)
            )
        }
    }

    override fun onCleared() {
        // Si se salió sin guardar, la foto nueva ya no sirve
        if (!guardado) descartarFotoActual()
        recursos.borrarFoto(fotoPendiente)
        super.onCleared()
    }
}
