package com.example.deepsourcehoras.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.deepsourcehoras.R

@Composable
fun CampoTexto(
    valor: String,
    label: String,
    error: String?,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    icono: Painter? = null,
    esPassword: Boolean = false,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val sacudida = desplazamientoError(error)

    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = if (icono != null) {
            {
                Icon(
                    painter = icono,
                    contentDescription = null,
                    tint = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            null
        },
        // En la contraseña va el ojo para mostrarla u ocultarla
        trailingIcon = if (esPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(if (passwordVisible) R.drawable.ic_ojo_tachado else R.drawable.ic_ojo),
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            }
        } else {
            null
        },
        isError = error != null,
        supportingText = {
            error?.let { MensajeError(it) }
        },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (esPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (esPassword) KeyboardType.Password else tipoTeclado
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = sacudida.dp.toPx() }
    )
}
