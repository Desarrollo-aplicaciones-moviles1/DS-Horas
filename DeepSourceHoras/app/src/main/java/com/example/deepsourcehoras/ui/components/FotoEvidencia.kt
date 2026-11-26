package com.example.deepsourcehoras.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.deepsourcehoras.R
import com.example.deepsourcehoras.recursos.Camara
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Botón para sacar la foto de la tarea y, si ya hay una, su vista previa
@Composable
fun FotoEvidencia(
    ruta: String?,
    onTomarFoto: () -> Unit,
    onQuitarFoto: () -> Unit,
    modifier: Modifier = Modifier,
    onElegirGaleria: () -> Unit = {},
    enabled: Boolean = true
) {
    Column(modifier = modifier.animateContentSize()) {
        Text(
            text = "Foto de evidencia (opcional)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (ruta == null) {
            // Dos formas de adjuntar la evidencia: sacar una foto o elegir una imagen ya guardada
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onTomarFoto,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_camara),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tomar foto")
                }
                OutlinedButton(
                    onClick = onElegirGaleria,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_galeria),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galería")
                }
            }
        } else {
            Box {
                MiniaturaFoto(
                    ruta = ruta,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                FilledTonalIconButton(
                    onClick = onQuitarFoto,
                    enabled = enabled,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Quitar foto")
                }
            }
            Row {
                TextButton(onClick = onTomarFoto, enabled = enabled) {
                    Text("Tomar otra")
                }
                TextButton(onClick = onElegirGaleria, enabled = enabled) {
                    Text("Elegir otra de la galería")
                }
            }
        }
    }
}

// Carga la foto en segundo plano para no trabar la pantalla
@Composable
fun MiniaturaFoto(ruta: String, modifier: Modifier = Modifier) {
    val miniatura by produceState<ImageBitmap?>(null, ruta) {
        value = withContext(Dispatchers.IO) {
            Camara.cargarMiniatura(ruta)?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        miniatura?.let {
            Image(
                bitmap = it,
                contentDescription = "Foto de evidencia",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
