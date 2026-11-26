package com.example.deepsourcehoras.ui.screen

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deepsourcehoras.R
import com.example.deepsourcehoras.di.Fabricas
import com.example.deepsourcehoras.model.RegistroHorasUiState
import com.example.deepsourcehoras.model.ResumenRegistro
import com.example.deepsourcehoras.ui.components.CampoDesplegable
import com.example.deepsourcehoras.ui.components.CampoFecha
import com.example.deepsourcehoras.ui.components.CampoTexto
import com.example.deepsourcehoras.ui.components.FotoEvidencia
import com.example.deepsourcehoras.ui.components.MensajeError
import com.example.deepsourcehoras.ui.theme.DeepSourceHorasTheme
import com.example.deepsourcehoras.ui.viewmodel.RegistroHorasViewModel
import com.example.deepsourcehoras.util.formatearCronometro

@Composable
fun RegistroHorasScreen(
    onBack: () -> Unit,
    onRegistrado: (ResumenRegistro) -> Unit,
    finalizarAlAbrir: Boolean = false,
    viewModel: RegistroHorasViewModel = viewModel(factory = Fabricas.registro)
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Android 13 o superior: pide el permiso de notificaciones la primera vez
    val pedirPermisoNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (!concedido) viewModel.onPermisoNotificacionesDenegado()
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Si se llegó desde "Finalizar" del widget o de la notificación, se detiene al entrar
    LaunchedEffect(finalizarAlAbrir, state.corriendo) {
        if (finalizarAlAbrir) viewModel.finalizarSiCorre()
    }

    // Cámara: primero el permiso y después se abre la app de cámara del teléfono
    val tomarFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        viewModel.onFotoTomada(exito)
    }

    fun abrirCamara() {
        try {
            tomarFoto.launch(viewModel.prepararFoto())
        } catch (e: ActivityNotFoundException) {
            viewModel.onCamaraNoDisponible()
        }
    }

    // Galería: selector de fotos del sistema. No pide permisos porque el usuario elige
    // una sola imagen y la app solo recibe esa
    val elegirDeGaleria = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onImagenGaleria(uri)
    }

    val pedirPermisoCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) abrirCamara() else viewModel.onPermisoCamaraDenegado()
    }

    RegistroHorasScreen(
        state = state,
        onBack = onBack,
        onProyectoChange = viewModel::onProyectoChange,
        onDiaChange = viewModel::onDiaChange,
        onHorasChange = viewModel::onHorasChange,
        onMinutosChange = viewModel::onMinutosChange,
        onTareaChange = viewModel::onTareaChange,
        onIniciarClick = viewModel::onIniciarClick,
        onPausarClick = viewModel::onPausarClick,
        onReanudarClick = viewModel::onReanudarClick,
        onFinalizarClick = viewModel::onFinalizarClick,
        onTomarFoto = {
            val permitido = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (permitido) abrirCamara() else pedirPermisoCamara.launch(Manifest.permission.CAMERA)
        },
        onQuitarFoto = viewModel::onQuitarFoto,
        onElegirGaleria = {
            elegirDeGaleria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onMensajeMostrado = viewModel::onMensajeMostrado,
        onRegistrarClick = { viewModel.onRegistrarClick(onRegistrado) }
    )
}

// 2) Solo dibuja
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroHorasScreen(
    state: RegistroHorasUiState,
    onBack: () -> Unit,
    onProyectoChange: (String) -> Unit,
    onDiaChange: (String) -> Unit,
    onHorasChange: (String) -> Unit,
    onMinutosChange: (String) -> Unit,
    onTareaChange: (String) -> Unit,
    onIniciarClick: () -> Unit,
    onPausarClick: () -> Unit,
    onReanudarClick: () -> Unit,
    onFinalizarClick: () -> Unit,
    onTomarFoto: () -> Unit,
    onQuitarFoto: () -> Unit,
    onMensajeMostrado: () -> Unit,
    onElegirGaleria: () -> Unit = {},
    onRegistrarClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.mensaje) {
        state.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            onMensajeMostrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editando) "Editar registro" else "Registrar horas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BarraAcciones(
                state = state,
                onIniciarClick = onIniciarClick,
                onPausarClick = onPausarClick,
                onReanudarClick = onReanudarClick,
                onFinalizarClick = onFinalizarClick,
                onRegistrarClick = onRegistrarClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                // Con el teclado abierto el formulario se achica y se puede seguir bajando
                .imePadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedVisibility(visible = state.corriendo) {
                RelojCronometro(segundos = state.segundos, pausado = state.pausado)
            }

            CampoDesplegable(
                valor = state.proyecto,
                label = "Proyecto",
                error = state.errores.proyecto,
                opciones = state.proyectos,
                onSeleccion = onProyectoChange,
                icono = painterResource(R.drawable.ic_carpeta)
            )
            CampoFecha(
                valor = state.dia,
                label = "Día",
                error = state.errores.dia,
                onFechaSeleccionada = onDiaChange
            )

            // Aviso de la API externa de feriados (informativo, no bloquea)
            AnimatedVisibility(visible = state.avisoFeriado != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = state.avisoFeriado ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(
                    valor = state.horas,
                    label = "Horas",
                    error = state.errores.horas,
                    onChange = onHorasChange,
                    icono = painterResource(R.drawable.ic_reloj),
                    tipoTeclado = KeyboardType.Number,
                    enabled = !state.corriendo,
                    modifier = Modifier.weight(1f)
                )
                CampoTexto(
                    valor = state.minutos,
                    label = "Minutos",
                    error = state.errores.minutos,
                    onChange = onMinutosChange,
                    icono = painterResource(R.drawable.ic_reloj),
                    tipoTeclado = KeyboardType.Number,
                    enabled = !state.corriendo,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!state.corriendo) {
                state.horaInicio?.let {
                    Text(
                        text = "Calculado desde las $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            // Error de exceso de tiempo: aparece mientras se escribe
            state.errores.tiempo?.let {
                MensajeError(texto = it, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
            }

            CampoTexto(
                valor = state.tarea,
                label = "Tarea",
                error = state.errores.tarea,
                onChange = onTareaChange,
                icono = painterResource(R.drawable.ic_tarea)
            )

            Spacer(modifier = Modifier.height(4.dp))

            FotoEvidencia(
                ruta = state.fotoRuta,
                onTomarFoto = onTomarFoto,
                onQuitarFoto = onQuitarFoto,
                onElegirGaleria = onElegirGaleria,
                enabled = !state.guardando
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Botones de abajo según el momento:
//  - editando:          [Guardar cambios]
//  - sin cronómetro:    [Iniciar] [Registrar]
//  - con cronómetro:    [Pausar | Reanudar] [Finalizar]
@Composable
private fun BarraAcciones(
    state: RegistroHorasUiState,
    onIniciarClick: () -> Unit,
    onPausarClick: () -> Unit,
    onReanudarClick: () -> Unit,
    onFinalizarClick: () -> Unit,
    onRegistrarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            state.editando -> BotonGuardar("Guardar cambios", state.guardando, onRegistrarClick, Modifier.weight(1f))
            state.corriendo -> {
                OutlinedButton(
                    onClick = if (state.pausado) onReanudarClick else onPausarClick,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.pausado) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(if (state.pausado) "Reanudar" else "Pausar")
                }
                Button(onClick = onFinalizarClick, modifier = Modifier.weight(1f)) {
                    Text("Finalizar")
                }
            }
            else -> {
                OutlinedButton(
                    onClick = onIniciarClick,
                    enabled = !state.guardando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Iniciar")
                }
                BotonGuardar("Registrar", state.guardando, onRegistrarClick, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BotonGuardar(texto: String, guardando: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Button(onClick = onClick, enabled = !guardando, modifier = modifier) {
        if (guardando) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        else Text(texto)
    }
}

// Reloj grande del cronómetro. En pausa queda parpadeando suave para que se note
@Composable
private fun RelojCronometro(segundos: Int, pausado: Boolean) {
    val parpadeo = rememberInfiniteTransition(label = "pausa")
    val alfa by parpadeo.animateFloat(
        initialValue = 1f,
        targetValue = if (pausado) 0.35f else 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "alfa"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = formatearCronometro(segundos),
            style = MaterialTheme.typography.displayMedium,
            color = if (pausado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(alfa)
        )
        if (pausado) {
            Text(
                text = "EN PAUSA",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegistroHorasScreenPreview() {
    DeepSourceHorasTheme {
        RegistroHorasScreen(
            state = RegistroHorasUiState(
                dia = "29/10/2026",
                proyectos = listOf("Rediseño Web", "App Móvil"),
                corriendo = true,
                pausado = true,
                segundos = 3725
            ),
            onBack = {},
            onProyectoChange = {},
            onDiaChange = {},
            onHorasChange = {},
            onMinutosChange = {},
            onTareaChange = {},
            onIniciarClick = {},
            onPausarClick = {},
            onReanudarClick = {},
            onFinalizarClick = {},
            onTomarFoto = {},
            onQuitarFoto = {},
            onMensajeMostrado = {},
            onRegistrarClick = {}
        )
    }
}
