package com.example.deepsourcehoras.data.preferences

import android.content.Context

// Lo que necesita el ViewModel de la sesión (en las pruebas se usa un simulado)
interface SesionStore {
    fun guardar(token: String, usuario: String, usuarioId: Long = 0)
    fun usuarioId(): Long?
    fun haySesion(): Boolean
    fun usuario(): String?
    fun cerrar()
}

// Guarda la sesión para no pedir el login cada vez que se abre la app o el widget
object SesionStorage {
    private const val ARCHIVO = "sesion"
    private const val TOKEN = "token"
    private const val USUARIO = "usuario"
    private const val USUARIO_ID = "usuario_id"

    private fun prefs(context: Context) =
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun guardar(context: Context, token: String, usuario: String, usuarioId: Long = 0) {
        prefs(context).edit()
            .putString(TOKEN, token)
            .putString(USUARIO, usuario)
            .putLong(USUARIO_ID, usuarioId)
            .apply()
    }

    // id del usuario en ms-usuarios (null si no hay sesión)
    fun usuarioId(context: Context): Long? =
        prefs(context).getLong(USUARIO_ID, 0L).takeIf { it > 0 }

    fun haySesion(context: Context): Boolean = prefs(context).getString(TOKEN, null) != null

    fun usuario(context: Context): String? = prefs(context).getString(USUARIO, null)

    fun cerrar(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

class SesionPrefs(private val context: Context) : SesionStore {
    override fun guardar(token: String, usuario: String, usuarioId: Long) =
        SesionStorage.guardar(context, token, usuario, usuarioId)
    override fun usuarioId(): Long? = SesionStorage.usuarioId(context)
    override fun haySesion(): Boolean = SesionStorage.haySesion(context)
    override fun usuario(): String? = SesionStorage.usuario(context)
    override fun cerrar() = SesionStorage.cerrar(context)
}
