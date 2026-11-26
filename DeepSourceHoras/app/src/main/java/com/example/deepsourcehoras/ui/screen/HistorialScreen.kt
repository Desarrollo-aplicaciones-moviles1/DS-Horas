package com.example.deepsourcehoras.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deepsourcehoras.R
import com.example.deepsourcehoras.di.Fabricas
import com.example.deepsourcehoras.model.HistorialUiState
import com.example.deepsourcehoras.model.RegistroItemUi
import com.example.deepsourcehoras.ui.theme.DeepSourceHorasTheme
import com.example.deepsourcehoras.ui.viewmodel.HistorialViewModel

@Composable
fun HistorialScreen(
    onBack: () -> Unit,
    onEditar: (Long) -> Unit,
    viewModel: HistorialViewModel = viewModel(factory = Fabricas.historial)
) {
    val state by viewModel.uiState.collectAsState()
    HistorialScreen(
        state = state,
        onBack = onBack,
        onActualizar = viewModel::actualizar,
        onEditar = onEditar,
        onEliminar = viewModel::onEliminarClick,
        onConfirmarEliminar = viewModel::onConfirmarEliminar,
        onCancelarEliminar = viewModel::onCancelarEliminar,
        onMensajeMostrado = viewModel::onMensajeMostrado
    )
}

// 2) Solo dibuja
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    state: HistorialUiState,
    onBack: () -> Unit,
    onActualizar: () -> Unit,
    onEditar: (Long) -> Unit,
    onEliminar: (RegistroItemUi) -> Unit,
    onConfirmarEliminar: () -> Unit,
    onCancelarEliminar: () -> Unit,
    onMensajeMostrado: () -> Unit
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
                title = { Text("Mis registros") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onActualizar) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            state.cargando -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.registros.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Todavía no hay registros", style = MaterialTheme.typography.bodyMedium)
            }
            else -> LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.registros, key = { it.id }) { registro ->
                    RegistroCard(
                        registro = registro,
                        onEditar = { onEditar(registro.id) },
                        onEliminar = { onEliminar(registro) }
                    )
                }
            }
        }
    }

    state.porEliminar?.let { registro ->
        AlertDialog(
            onDismissRequest = onCancelarEliminar,
            title = { Text("Eliminar registro") },
            text = { Text("Se eliminará \"${registro.tarea}\" (${registro.duracion}). Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = onConfirmarEliminar, enabled = !state.eliminando) {
                    if (state.eliminando) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelarEliminar, enabled = !state.eliminando) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun RegistroCard(registro: RegistroItemUi, onEditar: () -> Unit, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(registro.tarea, style = MaterialTheme.typography.titleSmall)
                Text(registro.detalle, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(registro.duracion, color = MaterialTheme.colorScheme.primary)
                    if (registro.foto != null) {
                        Icon(
                            painter = painterResource(R.drawable.ic_camara),
                            contentDescription = "Con foto",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp).size(14.dp)
                        )
                    }
                }
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar registro")
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar registro", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistorialScreenPreview() {
    DeepSourceHorasTheme {
        HistorialScreen(
            state = HistorialUiState(
                cargando = false,
                registros = listOf(RegistroItemUi(1, "Maquetado", "Rediseño Web · Hoy", "2h 15min"))
            ),
            onBack = {}, onActualizar = {}, onEditar = {}, onEliminar = {},
            onConfirmarEliminar = {}, onCancelarEliminar = {}, onMensajeMostrado = {}
        )
    }
}
