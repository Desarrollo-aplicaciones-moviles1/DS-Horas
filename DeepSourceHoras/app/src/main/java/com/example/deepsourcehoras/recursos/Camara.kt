package com.example.deepsourcehoras.recursos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

// Recurso nativo 3: cámara para sacar una foto de evidencia de la tarea
object Camara {
    private const val CARPETA = "fotos"

    // Archivo vacío donde la app de cámara deja la foto (carpeta privada de la app)
    fun crearArchivo(context: Context): File {
        val carpeta = File(context.filesDir, CARPETA)
        carpeta.mkdirs()
        return File(carpeta, "registro_${System.currentTimeMillis()}.jpg")
    }

    // La cámara es otra app: se le entrega una Uri con permiso temporal, no la ruta real
    fun uriPara(context: Context, archivo: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)

    fun borrar(ruta: String?) {
        if (ruta != null) File(ruta).delete()
    }

    // Imagen elegida de la galería: se copia a la carpeta privada de la app.
    // Así sigue disponible aunque el usuario la borre de su galería, y se trata igual que una foto de la cámara.
    // Devuelve la ruta de la copia, o null si no se pudo leer.
    fun copiarDesdeGaleria(context: Context, uri: Uri): String? {
        val destino = crearArchivo(context)
        return try {
            val entrada = context.contentResolver.openInputStream(uri) ?: return null
            entrada.use { origen -> destino.outputStream().use { origen.copyTo(it) } }
            if (destino.length() > 0) destino.absolutePath else null.also { destino.delete() }
        } catch (e: Exception) {
            destino.delete()
            null
        }
    }

    // Carga la foto achicada y derecha (algunas cámaras la guardan girada)
    fun cargarMiniatura(ruta: String, ladoMaximo: Int = 720): Bitmap? {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, medidas)
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null

        var escala = 1
        while (medidas.outWidth / (escala * 2) >= ladoMaximo &&
            medidas.outHeight / (escala * 2) >= ladoMaximo
        ) {
            escala *= 2
        }
        val opciones = BitmapFactory.Options().apply { inSampleSize = escala }
        val foto = BitmapFactory.decodeFile(ruta, opciones) ?: return null

        val grados = when (orientacion(ruta)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (grados == 0f) return foto

        val matriz = Matrix().apply { postRotate(grados) }
        return Bitmap.createBitmap(foto, 0, 0, foto.width, foto.height, matriz, true)
    }

    private fun orientacion(ruta: String): Int =
        try {
            ExifInterface(ruta).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
}
