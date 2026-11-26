package com.example.deepsourcehoras.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.example.deepsourcehoras.model.AporteUi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deepsourcehoras.R
import com.example.deepsourcehoras.di.Fabricas
import com.example.deepsourcehoras.model.FiltroProyectos
import com.example.deepsourcehoras.model.MisProyectosUiState
import com.example.deepsourcehoras.model.ProyectoDetalle
import com.example.deepsourcehoras.ui.components.BarraProgreso
import com.example.deepsourcehoras.ui.components.CampoTexto
import com.example.deepsourcehoras.ui.theme.DeepSourceHorasTheme
import com.example.deepsourcehoras.ui.viewmodel.MisProyectosViewModel
import com.example.deepsourcehoras.util.formatearDuracion

@Composable
fun MisProyectosScreen(
    onBack: () -> Unit,
    viewModel: MisProyectosViewModel = viewModel(factory = Fabricas.misProyectos)
) {
    val state by viewModel.uiState.collectAsState()

    MisProyectosScreen(
        state = state,
        onBack = onBack,
        onFiltroChange = viewModel::onFiltroChange,
        onNuevoClick = viewModel::onNuevoClick,
        onCerrarDialogo = viewModel::onCerrarDialogo,
        onNombreChange = viewModel::onNombreChange,
        onClienteChange = viewModel::onClienteChange,
        onGuardarProyecto = viewModel::onGuardarProyecto,
        onFinalizar = viewModel::onFinalizarClick,
        onAlternarEquipo = viewModel::onAlternarEquipo,
        onConfirmarFinalizar = viewModel::onConfirmarFinalizar,
        onCancelarFinalizar = viewModel::onCancelarFinalizar,
        onMensajeMostrado = viewModel::onMensajeMostrado
    )
}

// 2) Solo dibuja
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisProyectosScreen(
    state: MisProyectosUiState,
    onBack: () -> Unit,
    onFiltroChange: (FiltroProyectos) -> Unit,
    onNuevoClick: () -> Unit = {},
    onCerrarDialogo: () -> Unit = {},
    onNombreChange: (String) -> Unit = {},
    onClienteChange: (String) -> Unit = {},
    onGuardarProyecto: () -> Unit = {},
    onFinalizar: (ProyectoDetalle) -> Unit = {},
    onAlternarEquipo: (Long) -> Unit = {},
    onConfirmarFinalizar: () -> Unit = {},
    onCancelarFinalizar: () -> Unit = {},
    onMensajeMostrado: () -> Unit = {}
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
                title = { Text("Mis proyectos") },
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevoClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo proyecto") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.filtro == FiltroProyectos.EN_CURSO,
                    onClick = { onFiltroChange(FiltroProyectos.EN_CURSO) },
                    label = { Text("En curso") }
                )
                FilterChip(
                    selected = state.filtro == FiltroProyectos.FINALIZADOS,
                    onClick = { onFiltroChange(FiltroProyectos.FINALIZADOS) },
                    label = { Text("Finalizados") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.visibles.isEmpty()) {
                Text(
                    text = if (state.filtro == FiltroProyectos.EN_CURSO) "No hay proyectos en curso" else "No hay proyectos finalizados",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Espacio abajo para que el botón flotante no tape la última tarjeta
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(state.visibles, key = { it.id }) { proyecto ->
                    ProyectoCard(
                        proyecto = proyecto,
                        equipoAbierto = proyecto.id in state.expandidos,
                        onAlternarEquipo = { onAlternarEquipo(proyecto.id) },
                        onFinalizar = { onFinalizar(proyecto) }
                    )
                }
            }
        }
    }

    if (state.dialogoNuevo) {
        AlertDialog(
            onDismissRequest = onCerrarDialogo,
            title = { Text("Nuevo proyecto") },
            text = {
                Column {
                    CampoTexto(
                        valor = state.nombreNuevo,
                        label = "Nombre del proyecto",
                        error = state.erroresNuevo.nombre,
                        onChange = onNombreChange,
                        icono = painterResource(R.drawable.ic_carpeta),
                        enabled = !state.guardando
                    )
                    CampoTexto(
                        valor = state.clienteNuevo,
                        label = "Cliente",
                        error = state.erroresNuevo.cliente,
                        onChange = onClienteChange,
                        enabled = !state.guardando
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onGuardarProyecto, enabled = !state.guardando) {
                    if (state.guardando) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = onCerrarDialogo, enabled = !state.guardando) { Text("Cancelar") }
            }
        )
    }

    state.porFinalizar?.let { proyecto ->
        AlertDialog(
            onDismissRequest = onCancelarFinalizar,
            title = { Text("Finalizar proyecto") },
            text = { Text("${proyecto.nombre} pasará a Finalizados y ya no se podrán registrar horas en él.") },
            confirmButton = { TextButton(onClick = onConfirmarFinalizar) { Text("Finalizar") } },
            dismissButton = { TextButton(onClick = onCancelarFinalizar) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ProyectoCard(
    proyecto: ProyectoDetalle,
    equipoAbierto: Boolean,
    onAlternarEquipo: () -> Unit,
    onFinalizar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(proyecto.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatearDuracion(proyecto.minutos),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (proyecto.fijado) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = "Fijado",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(16.dp)
                        )
                    }
                }
            }
            Text("Cliente: ${proyecto.cliente}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (proyecto.fechaCierre == null) {
                // En curso: barra de progreso y opción de cerrarlo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BarraProgreso(
                        progreso = proyecto.progreso,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(proyecto.progreso * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                AporteEquipo(proyecto.aportes, equipoAbierto, onAlternarEquipo) {
                    TextButton(onClick = onFinalizar) {
                        Text("Finalizar proyecto")
                    }
                }
            } else {
                // Finalizado: aporte del equipo, estado y fecha de cierre
                AporteEquipo(proyecto.aportes, equipoAbierto, onAlternarEquipo)
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF1E3A2B)
                    ) {
                        Text(
                            text = "Finalizado",
                            color = Color(0xFF6FCF97),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "Cerrado ${proyecto.fechaCierre}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// Colores para distinguir a cada integrante en la barra
private val ColoresEquipo = listOf(
    Color(0xFF9B8AD4), Color(0xFF6FCF97), Color(0xFFE2B85C), Color(0xFF56CCF2), Color(0xFFEB8A90), Color(0xFFBB86FC)
)

private fun colorDe(indice: Int) = ColoresEquipo[indice % ColoresEquipo.size]

// Barra dividida por persona (el ancho de cada tramo es su porcentaje) y,
// al tocar "Ver aporte del equipo", el detalle con horas y porcentaje de cada uno
// "accion" va a la derecha en la misma fila del botón (ej. "Finalizar proyecto"), para no dejar espacios vacíos
@Composable
private fun ColumnScope.AporteEquipo(
    aportes: List<AporteUi>,
    abierto: Boolean,
    onAlternar: () -> Unit,
    accion: (@Composable () -> Unit)? = null
) {
    if (aportes.isEmpty()) {
        // Sin horas registradas todavía: solo la acción, a la derecha
        accion?.let { Box(modifier = Modifier.align(Alignment.End)) { it() } }
        return
    }
    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
    ) {
        aportes.forEach { a ->
            Box(
                modifier = Modifier
                    .weight(a.minutos.toFloat().coerceAtLeast(1f))
                    .fillMaxHeight()
                    .background(colorDe(a.color))
            )
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onAlternar, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Text(
                text = if (abierto) "Ocultar equipo" else "Ver aporte del equipo (${aportes.size})",
                style = MaterialTheme.typography.labelMedium
            )
            Icon(
                imageVector = if (abierto) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        accion?.invoke()
    }

    AnimatedVisibility(visible = abierto) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
            aportes.forEach { a -> FilaAporte(a) }
        }
    }
}

@Composable
private fun FilaAporte(aporte: AporteUi) {
    val avance by animateFloatAsState(targetValue = aporte.porcentaje / 100f, animationSpec = tween(700), label = "aporte")
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(colorDe(aporte.color))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (aporte.esTu) "${aporte.nombre} (tú)" else aporte.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (aporte.esTu) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Text(aporte.duracion, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "${aporte.porcentaje}%",
                style = MaterialTheme.typography.titleSmall,
                color = colorDe(aporte.color),
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        LinearProgressIndicator(
            progress = { avance },
            color = colorDe(aporte.color),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MisProyectosScreenPreview() {
    DeepSourceHorasTheme {
        MisProyectosScreen(
            state = MisProyectosUiState(
                proyectos = listOf(
                    ProyectoDetalle(
                        "Rediseño Web", "AFC", 1440, 0.65f, fijado = true, id = 1,
                        aportes = listOf(
                            AporteUi("Carla Muñoz", "9h 45min", 585, 41, color = 0),
                            AporteUi("Usuario", "9h 15min", 555, 38, esTu = true, color = 1),
                            AporteUi("Diego Rojas", "5h", 300, 21, color = 2)
                        )
                    ),
                    ProyectoDetalle("Portal Cliente", "AFC", 180, 0.10f, id = 2)
                )
            ),
            onBack = {},
            onFiltroChange = {}
        )
    }
}
