package com.example.deepsourcehoras.data.remote

import com.example.deepsourcehoras.data.remote.dto.LoginRequestDto
import com.example.deepsourcehoras.data.remote.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

// Microservicio ms-usuarios (puerto 8081)
interface UsuariosApi {
    @POST("api/auth/login")
    suspend fun login(@Body datos: LoginRequestDto): LoginResponseDto
}
