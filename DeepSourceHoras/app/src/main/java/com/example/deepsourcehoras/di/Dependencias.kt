package com.example.deepsourcehoras.di

import android.content.Context
import com.example.deepsourcehoras.data.local.DsHorasDbHelper
import com.example.deepsourcehoras.data.local.HorasLocalSqlite
import com.example.deepsourcehoras.data.preferences.CronometroPrefs
import com.example.deepsourcehoras.data.preferences.SesionPrefs
import com.example.deepsourcehoras.data.preferences.SesionStorage
import com.example.deepsourcehoras.model.Autor
import com.example.deepsourcehoras.data.remote.RetrofitClient
import com.example.deepsourcehoras.data.repository.AuthRepository
import com.example.deepsourcehoras.data.repository.FeriadosRepository
import com.example.deepsourcehoras.data.repository.HorasRepository
import com.example.deepsourcehoras.recursos.RecursosAndroid

// Arma las dependencias reales de la app en un solo lugar (inyección manual).
// Los ViewModel las reciben por constructor y así se pueden probar con simulados.
object Dependencias {

    @Volatile
    private var horas: HorasRepository? = null

    fun horasRepository(context: Context): HorasRepository =
        horas ?: synchronized(this) {
            horas ?: HorasRepository(
                api = RetrofitClient.horasApi,
                local = HorasLocalSqlite(DsHorasDbHelper(context.applicationContext)),
                // El autor de cada registro es quien inició sesión
                autor = {
                    val app = context.applicationContext
                    SesionStorage.usuarioId(app)?.let { Autor(it, SesionStorage.usuario(app) ?: "Usuario") }
                }
            ).also { horas = it }
        }

    val authRepository: AuthRepository by lazy { AuthRepository(RetrofitClient.usuariosApi) }

    val feriadosRepository: FeriadosRepository by lazy { FeriadosRepository(RetrofitClient.feriadosApi) }

    fun recursos(context: Context) = RecursosAndroid(context.applicationContext)

    fun sesion(context: Context) = SesionPrefs(context.applicationContext)

    fun cronometro(context: Context) = CronometroPrefs(context.applicationContext)
}
