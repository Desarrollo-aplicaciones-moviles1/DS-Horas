package com.example.deepsourcehoras.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deepsourcehoras.R
import com.example.deepsourcehoras.di.Fabricas
import com.example.deepsourcehoras.model.HomeUiState
import com.example.deepsourcehoras.model.Proyecto
import com.example.deepsourcehoras.model.RegistroHoras
import com.example.deepsourcehoras.ui.components.AvisoConexion
import com.example.deepsourcehoras.ui.components.BarraProgreso
import com.example.deepsourcehoras.ui.theme.DeepSourceHorasTheme
import com.example.deepsourcehoras.ui.viewmodel.HomeViewModel
import com.example.deepsourcehoras.util.formatearDuracion

@Composable
fun HomeScreen(
    onRegistrarHoras: () -> Unit,
    onVerProyectos: () -> Unit,
    onVerRegistros: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = Fabricas.home)
) {
    val state by viewModel.uiState.collectAsState()

    HomeScreen(
        state = state,
        onRegistrarHoras = onRegistrarHoras,
        onVerProyectos = onVerProyectos,
        onVerRegistros = onVerRegistros,
        onSincronizar = viewModel::sincronizar,
        onCerrarSesion = {
            viewModel.onCerrarSesion()
            onCerrarSesion()
        }
    )
}

// 2) Solo dibuja
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onRegistrarHoras: () -> Unit,
    onVerProyectos: () -> Unit,
    onVerRegistros: () -> Unit,
    onSincronizar: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var confirmarSalida by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hola, ${state.userName}",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = onSincronizar, enabled = !state.sincronizando) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar datos")
                    }
                    IconButton(onClick = { confirmarSalida = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_salir),
                            contentDescription = "Cerrar sesión"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (state.sincronizando) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            }
            state.errorConexion?.let {
                AvisoConexion(texto = it, onReintentar = onSincronizar, modifier = Modifier.padding(bottom = 12.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRegistrarHoras, modifier = Modifier.weight(1f)) {
                    Text("Registrar horas")
                }
                OutlinedButton(onClick = onVerRegistros, modifier = Modifier.weight(1f)) {
                    Text("Mis registros")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Proyectos activos", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onVerProyectos) {
                    Text("Ver todos")
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (state.proyectos.isEmpty()) {
                        Text(
                            text = if (state.sincronizando) "Cargando proyectos..." else "No tienes proyectos en curso",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    state.proyectos.forEach { proyecto ->
                        ProyectoItem(proyecto)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", style = MaterialTheme.typography.titleSmall)
                        Text(formatearDuracion(state.totalMinutos), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Últimos registros", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onVerRegistros) {
                    Text("Ver todos")
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (state.registros.isEmpty()) {
                        Text("Todavía no hay registros", style = MaterialTheme.typography.bodySmall)
                    }
                    state.registros.forEachIndexed { index, registro ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        RegistroItem(registro)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("Cerrar sesión") },
            text = { Text("Vas a tener que ingresar de nuevo con tu correo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    onCerrarSesion()
                }) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ProyectoItem(proyecto: Proyecto) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(proyecto.nombre)
            Text(formatearDuracion(proyecto.minutos))
        }
        BarraProgreso(
            progreso = proyecto.progreso,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}

@Composable
private fun RegistroItem(registro: RegistroHoras) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(registro.tarea)
            Text(registro.detalle, style = MaterialTheme.typography.bodySmall)
        }
        // Los registros con foto de evidencia muestran el ícono de cámara
        if (registro.conFoto) {
            Icon(
                painter = painterResource(R.drawable.ic_camara),
                contentDescription = "Con foto",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(16.dp)
            )
        }
        Text(registro.duracion, modifier = Modifier.padding(start = 8.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    DeepSourceHorasTheme {
        HomeScreen(
            state = HomeUiState(
                userName = "Usuario",
                proyectos = listOf(Proyecto("Rediseño Web", 1440, 0.65f)),
                registros = listOf(RegistroHoras("Maquetado", "Rediseño Web · Hoy", "2h 15min", conFoto = true))
            ),
            onRegistrarHoras = {},
            onVerProyectos = {},
            onVerRegistros = {},
            onSincronizar = {},
            onCerrarSesion = {}
        )
    }
}
