package com.example.deepsourcehoras.data.remote

import com.example.deepsourcehoras.data.remote.dto.FeriadoDto
import retrofit2.http.GET
import retrofit2.http.Path

// API externa pública y gratuita: https://date.nager.at
// GET https://date.nager.at/api/v3/PublicHolidays/2026/CL
interface FeriadosApi {
    @GET("api/v3/PublicHolidays/{anio}/CL")
    suspend fun feriados(@Path("anio") anio: Int): List<FeriadoDto>
}
