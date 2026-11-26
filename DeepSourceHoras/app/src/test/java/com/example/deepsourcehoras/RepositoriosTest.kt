package com.example.deepsourcehoras

import com.example.deepsourcehoras.data.local.AporteEntidad
import com.example.deepsourcehoras.data.local.HorasLocal
import com.example.deepsourcehoras.data.local.ProyectoEntidad
import com.example.deepsourcehoras.data.remote.FeriadosApi
import com.example.deepsourcehoras.data.remote.HorasApi
import com.example.deepsourcehoras.data.remote.UsuariosApi
import com.example.deepsourcehoras.data.remote.dto.FeriadoDto
import com.example.deepsourcehoras.data.remote.dto.LoginResponseDto
import com.example.deepsourcehoras.data.remote.dto.ProyectoDto
import com.example.deepsourcehoras.data.remote.dto.RegistroDto
import com.example.deepsourcehoras.data.repository.AuthRepository
import com.example.deepsourcehoras.data.repository.FeriadosRepository
import com.example.deepsourcehoras.data.repository.HorasRepository
import com.example.deepsourcehoras.model.Autor
import com.example.deepsourcehoras.model.LoginResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

// Pruebas de los repositorios: hablan con la API (simulada con MockK) y con la copia local
@OptIn(ExperimentalCoroutinesApi::class)
class RepositoriosTest {

    private val horasApi = mockk<HorasApi>()
    private val local = mockk<HorasLocal>(relaxed = true)
    private val horas = HorasRepository(horasApi, local, UnconfinedTestDispatcher()) { Autor(2, "Carla Muñoz") }
    private val proyectoDto = ProyectoDto(1, "Web", "AFC", 0.5, true, null, 60)
    private val registroDto = RegistroDto(7, 1, "Web", "Tarea", "2026-11-19", 30, null, 5, 2, "Carla Muñoz")

    @Test
    fun `login contra el microservicio de usuarios`() = runTest {
        val api = mockk<UsuariosApi>()
        val auth = AuthRepository(api, UnconfinedTestDispatcher())

        coEvery { api.login(any()) } returns LoginResponseDto("tok", 1, "Usuario", "u@d.io")
        assertEquals(LoginResult.Success("tok", "Usuario", 1), auth.loginUser("u@d.io", "123456"))

        coEvery { api.login(any()) } throws errorHttp(401, """{"mensaje":"x"}""")
        assertEquals(LoginResult.Error("Correo o contraseña incorrectos"), auth.loginUser("u@d.io", "mala"))

        coEvery { api.login(any()) } throws errorHttp(500, """{"mensaje":"Caído"}""")
        assertEquals(LoginResult.Error("Caído"), auth.loginUser("u@d.io", "123456"))

        coEvery { api.login(any()) } throws IOException()
        assertTrue((auth.loginUser("u@d.io", "123456") as LoginResult.Error).mensaje.contains("conectar"))
    }

    @Test
    fun `sincronizar copia los datos del servidor al telefono`() = runTest {
        coEvery { horasApi.proyectos() } returns listOf(proyectoDto)
        coEvery { horasApi.registros(null) } returns listOf(registroDto)
        assertTrue(horas.sincronizar().isSuccess)
        verify { local.reemplazarTodo(match { it.single().id == 1L }, match { it.single().usuario == "Carla Muñoz" }) }

        // Sin conexión devuelve el error y no toca la copia local
        coEvery { horasApi.proyectos() } throws IOException()
        assertTrue(horas.sincronizar().exceptionOrNull() is IOException)
        verify(exactly = 1) { local.reemplazarTodo(any(), any()) }
    }

    @Test
    fun `registrar, editar y eliminar horas`() = runTest {
        coEvery { horasApi.crearRegistro(any()) } returns registroDto
        assertEquals("Carla Muñoz", horas.registrar(1, "2026-11-19", 30, "Tarea", null).getOrThrow().usuario)
        // El autor es quien inició sesión
        coVerify { horasApi.crearRegistro(match { it.usuarioId == 2L && it.usuarioNombre == "Carla Muñoz" && it.minutos == 30 }) }

        coEvery { horasApi.actualizarRegistro(7, any()) } returns registroDto.copy(minutos = 45)
        assertEquals(45, horas.actualizarRegistro(7, 1, "2026-11-19", 45, "Tarea", null).getOrThrow().minutos)
        verify(exactly = 2) { local.guardarRegistro(any()) }

        coEvery { horasApi.eliminarRegistro(7) } returns Response.success(204, Unit)
        assertTrue(horas.eliminarRegistro(7).isSuccess)
        coEvery { horasApi.eliminarRegistro(8) } returns Response.error(404, "".toResponseBody())
        assertTrue(horas.eliminarRegistro(8).isSuccess) // ya no existía en el servidor: igual se limpia lo local
        coEvery { horasApi.eliminarRegistro(9) } returns Response.error(500, "".toResponseBody())
        assertTrue(horas.eliminarRegistro(9).isFailure)
        verify { local.eliminarRegistro(7) }

        // Si el servidor rechaza el registro no se guarda nada en el teléfono
        coEvery { horasApi.crearRegistro(any()) } throws errorHttp(400, """{"mensaje":"No"}""")
        assertTrue(horas.registrar(1, "2026-11-19", 30, "Tarea", null).isFailure)
        verify(exactly = 2) { local.guardarRegistro(any()) }
    }

    @Test
    fun `crear y finalizar proyectos`() = runTest {
        coEvery { horasApi.crearProyecto(any()) } returns proyectoDto
        coEvery { horasApi.finalizarProyecto(1) } returns proyectoDto.copy(fechaCierre = "2026-11-19")
        assertEquals("Web", horas.crearProyecto("Web", "AFC").getOrThrow().nombre)
        assertEquals("2026-11-19", horas.finalizarProyecto(1).getOrThrow().fechaCierre)
        verify(exactly = 2) { local.guardarProyecto(any()) }
    }

    @Test
    fun `consultas a la copia local`() = runTest {
        every { local.proyectos() } returns listOf(
            ProyectoEntidad(1, "Web", "AFC", 0.5f, true, null, 60),
            ProyectoEntidad(2, "Cerrado", "AFC", 1f, false, "2026-01-01", 60)
        )
        every { local.registros(5) } returns listOf(registro(7))
        every { local.registros(1) } returns listOf(registro(8))
        every { local.registro(7) } returns registro(7)
        every { local.minutosDelDia("2026-11-19", 3) } returns 120
        every { local.minutosEntre("2026-11-16", "2026-11-19") } returns 600
        every { local.aportes() } returns listOf(AporteEntidad(1, 2, "Carla Muñoz", 90))

        assertEquals(2, horas.observarProyectos().first().size)
        assertEquals(7L, horas.observarRegistros(5).first().single().id)
        assertEquals("Carla Muñoz", horas.observarAportes().first().single().usuario)
        assertEquals(listOf("Web"), horas.proyectosEnCurso().map { it.nombre })
        assertEquals(120, horas.minutosDelDia("2026-11-19", 3))
        assertEquals(600, horas.minutosEntre("2026-11-16", "2026-11-19"))
        assertEquals(7L, horas.obtenerRegistro(7)?.id)
        assertEquals(8L, horas.ultimoRegistro()?.id)
    }

    @Test
    fun `feriados desde la API externa`() = runTest {
        val api = mockk<FeriadosApi>()
        val feriados = FeriadosRepository(api, UnconfinedTestDispatcher())
        coEvery { api.feriados(2026) } returns listOf(
            FeriadoDto("2026-12-25", "Navidad", "Christmas", true),
            FeriadoDto("2026-06-07", "Asalto y Toma del Morro de Arica", "x", false), // regional: se descarta
            FeriadoDto("2026-01-01", "Año Nuevo", "x", true)
        )

        assertEquals(listOf("2026-01-01", "2026-12-25"), feriados.delAnio(2026).getOrThrow().map { it.fecha })
        assertEquals("Navidad", feriados.buscar("2026-12-25")?.nombre)
        assertNull(feriados.buscar("2026-12-24"))
        assertNull(feriados.buscar("xx"))
        coVerify(exactly = 1) { api.feriados(2026) } // se guarda en memoria y no se vuelve a pedir

        coEvery { api.feriados(2027) } throws IOException()
        assertTrue(feriados.delAnio(2027).isFailure)
        assertNull(feriados.buscar("2027-01-01"))
    }
}
