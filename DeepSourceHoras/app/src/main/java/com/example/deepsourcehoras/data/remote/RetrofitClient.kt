package com.example.deepsourcehoras.data.remote

import com.example.deepsourcehoras.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Arma un Retrofit por cada servidor. Las URL salen de BuildConfig (ver app/build.gradle.kts)
object RetrofitClient {

    private val cliente: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            // En debug se ven las peticiones en el Logcat (filtro "okhttp")
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
                }
            )
            .build()
    }

    private fun crear(urlBase: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(urlBase)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val usuariosApi: UsuariosApi by lazy { crear(BuildConfig.URL_USUARIOS).create(UsuariosApi::class.java) }

    val horasApi: HorasApi by lazy { crear(BuildConfig.URL_HORAS).create(HorasApi::class.java) }

    val feriadosApi: FeriadosApi by lazy { crear(BuildConfig.URL_FERIADOS).create(FeriadosApi::class.java) }
}
