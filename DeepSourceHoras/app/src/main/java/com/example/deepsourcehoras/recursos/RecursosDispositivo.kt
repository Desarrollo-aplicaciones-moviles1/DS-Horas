package com.example.deepsourcehoras.recursos

import android.content.Context
import android.net.Uri
import com.example.deepsourcehoras.widget.Widgets
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Foto recién creada: la ruta para guardarla y la Uri que se le pasa a la cámara
data class FotoNueva(val ruta: String, val uri: Uri)

// Todo lo que los ViewModel le piden al teléfono (vibración, notificaciones, cámara, widgets).
// Al ser una interfaz, en las pruebas unitarias se reemplaza por un simulado.
interface RecursosDispositivo {
    fun vibrarCorta()
    fun vibrarExito()
    fun vibrarError()
    fun notificarHorasRegistradas(proyecto: String, minutos: Int)
    fun mostrarCronometro(proyecto: String, transcurridoMillis: Long, pausado: Boolean)
    fun ocultarCronometro()
    suspend fun actualizarWidget()
    fun nuevaFoto(): FotoNueva
    fun fotoValida(ruta: String): Boolean
    fun borrarFoto(ruta: String?)
    // Copia una imagen de la galería a la carpeta privada; devuelve la ruta o null si falla
    suspend fun copiarDeGaleria(uri: Uri): String?
}

class RecursosAndroid(private val context: Context) : RecursosDispositivo {
    override fun vibrarCorta() = Vibracion.corta(context)
    override fun vibrarExito() = Vibracion.exito(context)
    override fun vibrarError() = Vibracion.error(context)

    override fun notificarHorasRegistradas(proyecto: String, minutos: Int) =
        Notificaciones.mostrarHorasRegistradas(context, proyecto, minutos)

    override fun mostrarCronometro(proyecto: String, transcurridoMillis: Long, pausado: Boolean) =
        Notificaciones.mostrarCronometro(context, proyecto, transcurridoMillis, pausado)

    override fun ocultarCronometro() = Notificaciones.ocultarCronometro(context)

    // Actualiza los tres widgets de la pantalla de inicio
    override suspend fun actualizarWidget() = Widgets.actualizarTodos(context)

    override fun nuevaFoto(): FotoNueva {
        val archivo = Camara.crearArchivo(context)
        return FotoNueva(archivo.absolutePath, Camara.uriPara(context, archivo))
    }

    override fun fotoValida(ruta: String): Boolean = File(ruta).length() > 0

    override fun borrarFoto(ruta: String?) = Camara.borrar(ruta)

    // La copia se hace en segundo plano para no trabar la pantalla con imágenes grandes
    override suspend fun copiarDeGaleria(uri: Uri): String? =
        withContext(Dispatchers.IO) { Camara.copiarDesdeGaleria(context, uri) }
}
