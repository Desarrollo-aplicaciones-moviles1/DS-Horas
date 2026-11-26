package com.example.deepsourcehoras.data.remote

import com.example.deepsourcehoras.data.remote.dto.ProyectoDto
import com.example.deepsourcehoras.data.remote.dto.ProyectoRequestDto
import com.example.deepsourcehoras.data.remote.dto.RegistroDto
import com.example.deepsourcehoras.data.remote.dto.RegistroRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// Microservicio ms-horas (puerto 8082)
interface HorasApi {

    @GET("api/proyectos")
    suspend fun proyectos(): List<ProyectoDto>

    @POST("api/proyectos")
    suspend fun crearProyecto(@Body datos: ProyectoRequestDto): ProyectoDto

    @PATCH("api/proyectos/{id}/finalizar")
    suspend fun finalizarProyecto(@Path("id") id: Long): ProyectoDto

    @GET("api/registros")
    suspend fun registros(@Query("limite") limite: Int? = null): List<RegistroDto>

    @POST("api/registros")
    suspend fun crearRegistro(@Body datos: RegistroRequestDto): RegistroDto

    @PUT("api/registros/{id}")
    suspend fun actualizarRegistro(@Path("id") id: Long, @Body datos: RegistroRequestDto): RegistroDto

    // Responde 204 sin cuerpo
    @DELETE("api/registros/{id}")
    suspend fun eliminarRegistro(@Path("id") id: Long): Response<Unit>
}
