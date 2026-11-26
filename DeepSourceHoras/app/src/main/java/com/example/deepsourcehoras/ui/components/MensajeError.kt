package com.example.deepsourcehoras.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Texto de error con ícono, igual en todos los campos
@Composable
fun MensajeError(texto: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = texto,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// Animación de error: el campo se mueve de lado a lado cuando aparece un error.
// Devuelve cuánto hay que desplazarlo (en dp) en este momento
@Composable
fun desplazamientoError(error: String?): Float {
    val desplazamiento = remember { Animatable(0f) }
    LaunchedEffect(error) {
        if (error != null) {
            for (valor in listOf(-8f, 8f, -5f, 5f, -2f, 0f)) {
                desplazamiento.animateTo(valor, animationSpec = tween(durationMillis = 45))
            }
        }
    }
    return desplazamiento.value
}
