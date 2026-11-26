package com.example.deepsourcehoras

import android.net.Uri
import com.example.deepsourcehoras.data.local.AporteEntidad
import com.example.deepsourcehoras.data.remote.ErroresRed
import com.example.deepsourcehoras.data.repository.AuthRepository
import com.example.deepsourcehoras.data.repository.FeriadosRepository
import com.example.deepsourcehoras.data.repository.HorasRepository
import com.example.deepsourcehoras.domain.cronometro.EstadoCronometro
import com.example.deepsourcehoras.model.Feriado
import com.example.deepsourcehoras.model.FiltroProyectos
import com.example.deepsourcehoras.model.LoginResult
import com.example.deepsourcehoras.model.ResumenRegistro
import com.example.deepsourcehoras.recursos.FotoNueva
import com.example.deepsourcehoras.recursos.RecursosDispositivo
import com.example.deepsourcehoras.ui.viewmodel.HistorialViewModel
import com.example.deepsourcehoras.ui.viewmodel.HomeViewModel
import com.example.deepsourcehoras.ui.viewmodel.LoginViewModel
import com.example.deepsourcehoras.ui.viewmodel.MisProyectosViewModel
import com.example.deepsourcehoras.ui.viewmodel.RegistroHorasViewModel
import com.example.deepsourcehoras.util.diaABase
import com.example.deepsourcehoras.util.hoy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Pruebas de los ViewModel: el repositorio y los recursos del teléfono se reemplazan por simulados (MockK)

class LoginViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val auth = mockk<AuthRepository>()
    private val sesion = SesionFalsa()
    private val recursos = mockk<RecursosDispositivo>(relaxed = true)
    private val vm = LoginViewModel(auth, sesion, recursos)

    @Test
    fun `valida los campos y entra con datos correctos`() {
        vm.onLoginClick {}
        assertTrue(vm.uiState.value.errores.hayErrores)
        coVerify(exactly = 0) { auth.loginUser(any(), any()) }
        verify { recursos.vibrarError() }

        coEvery { auth.loginUser("u@d.io", "123456") } returns LoginResult.Success("tok", "Usuario", 7)
        vm.onEmailChange(" u@d.io ")
        vm.onPasswordChange("123456")
        assertFalse(vm.uiState.value.errores.hayErrores)
        var entro = false
        vm.onLoginClick { entro = true }
        assertTrue(entro)
        assertEquals("tok", sesion.token)
        assertEquals(7L, sesion.usuarioId())
    }

    @Test
    fun `muestra el error del servidor y el aviso de Google`() {
        coEvery { auth.loginUser(any(), any()) } returns LoginResult.Error("Correo o contraseña incorrectos")
        vm.onEmailChange("u@d.io")
        vm.onPasswordChange("999999")
        vm.onLoginClick {}
        assertEquals("Correo o contraseña incorrectos", vm.uiState.value.mensajeError)
        assertNull(sesion.token)
        vm.onGoogleClick()
        assertTrue(vm.uiState.value.aviso!!.contains("Google"))
    }
}

class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `muestra datos, avisa sin conexion y cierra sesion`() {
        val repo = mockk<HorasRepository>()
        val sesion = SesionFalsa("tok", "Carla")
        every { repo.observarProyectos() } returns flowOf(listOf(proyecto(1, "Web", 90), proyecto(2, "Viejo", 30, cierre = "2026-01-01")))
        every { repo.observarRegistros(5) } returns flowOf(listOf(registro(1, foto = "/f.jpg")))
        coEvery { repo.sincronizar() } returns Result.failure(IOException())

        val vm = HomeViewModel(repo, sesion)
        val estado = vm.uiState.value
        assertEquals("Carla", estado.userName)
        assertEquals(listOf("Web"), estado.proyectos.map { it.nombre }) // solo proyectos en curso
        assertEquals(90, estado.totalMinutos)
        assertTrue(estado.registros.single().conFoto)
        assertEquals(ErroresRed.SIN_CONEXION, estado.errorConexion)

        coEvery { repo.sincronizar() } returns Result.success(Unit)
        vm.sincronizar()
        assertNull(vm.uiState.value.errorConexion)
        vm.onCerrarSesion()
        assertFalse(sesion.haySesion())
    }
}

class HistorialViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val repo = mockk<HorasRepository>()
    private val recursos = mockk<RecursosDispositivo>(relaxed = true)

    private fun crear(sincroniza: Result<Unit> = Result.success(Unit)): HistorialViewModel {
        every { repo.observarRegistros(null) } returns flowOf(listOf(registro(1, foto = "/f.jpg", usuario = "Carla Muñoz"), registro(2)))
        coEvery { repo.sincronizar() } returns sincroniza
        return HistorialViewModel(repo, recursos)
    }

    @Test
    fun `lista los registros y elimina con confirmacion`() {
        coEvery { repo.eliminarRegistro(1) } returns Result.success(Unit)
        val vm = crear()
        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertEquals(listOf(1L, 2L), estado.registros.map { it.id })
        assertTrue(estado.registros[0].detalle.endsWith("· Carla Muñoz"))

        val item = estado.registros[0]
        vm.onEliminarClick(item)
        vm.onCancelarEliminar()
        vm.onConfirmarEliminar()
        coVerify(exactly = 0) { repo.eliminarRegistro(any()) }

        vm.onEliminarClick(item)
        vm.onConfirmarEliminar()
        assertEquals("Registro eliminado", vm.uiState.value.mensaje)
        verify { recursos.borrarFoto("/f.jpg") }
        vm.onMensajeMostrado()
        assertNull(vm.uiState.value.mensaje)
    }

    @Test
    fun `avisa si no hay conexion o si no se pudo eliminar`() {
        coEvery { repo.eliminarRegistro(1) } returns Result.failure(IOException())
        val vm = crear(Result.failure(IOException()))
        assertEquals(ErroresRed.SIN_CONEXION, vm.uiState.value.mensaje)

        vm.onEliminarClick(vm.uiState.value.registros[0])
        vm.onConfirmarEliminar()
        assertEquals(ErroresRed.SIN_CONEXION, vm.uiState.value.mensaje)
        verify(exactly = 0) { recursos.borrarFoto(any()) }
    }
}

class MisProyectosViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val repo = mockk<HorasRepository>()
    private val recursos = mockk<RecursosDispositivo>(relaxed = true)

    private fun crear(): MisProyectosViewModel {
        every { repo.observarProyectos() } returns flowOf(listOf(proyecto(1, "Web", fijado = true), proyecto(2, "Intranet", cierre = "2026-09-15")))
        every { repo.observarAportes() } returns flowOf(
            listOf(AporteEntidad(1, 1, "Usuario", 120), AporteEntidad(1, 2, "Carla Muñoz", 60), AporteEntidad(2, 3, "Diego Rojas", 30))
        )
        return MisProyectosViewModel(repo, recursos) { 1L }
    }

    @Test
    fun `lista proyectos con el aporte del equipo y crea uno nuevo`() {
        coEvery { repo.crearProyecto("Portal", "AFC") } returns Result.success(proyecto(3, "Portal"))
        val vm = crear()
        val web = vm.uiState.value.visibles.single()
        assertEquals(listOf("Usuario", "Carla Muñoz"), web.aportes.map { it.nombre })
        assertEquals(listOf(67, 33), web.aportes.map { it.porcentaje })
        assertTrue(web.aportes[0].esTu)

        vm.onAlternarEquipo(1)
        assertTrue(1L in vm.uiState.value.expandidos)
        vm.onAlternarEquipo(1)
        assertFalse(1L in vm.uiState.value.expandidos)
        vm.onFiltroChange(FiltroProyectos.FINALIZADOS)
        assertEquals("15/09/2026", vm.uiState.value.visibles.single().fechaCierre)

        vm.onNuevoClick()
        vm.onNombreChange(" Portal ")
        vm.onClienteChange("AFC")
        vm.onGuardarProyecto()
        assertFalse(vm.uiState.value.dialogoNuevo)
        assertEquals(FiltroProyectos.EN_CURSO, vm.uiState.value.filtro)
        assertEquals("Proyecto Portal creado", vm.uiState.value.mensaje)
        vm.onMensajeMostrado()
        assertNull(vm.uiState.value.mensaje)
    }

    @Test
    fun `valida el formulario, maneja errores y finaliza proyectos`() {
        coEvery { repo.crearProyecto(any(), any()) } returns Result.failure(IOException())
        val vm = crear()
        vm.onNuevoClick()
        vm.onNombreChange("web")
        vm.onGuardarProyecto()
        assertEquals("Ya existe un proyecto con ese nombre", vm.uiState.value.erroresNuevo.nombre)
        assertEquals("Escribe el cliente", vm.uiState.value.erroresNuevo.cliente)
        coVerify(exactly = 0) { repo.crearProyecto(any(), any()) }
        vm.onNombreChange("Portal")
        vm.onClienteChange("AFC")
        vm.onGuardarProyecto()
        assertTrue(vm.uiState.value.dialogoNuevo) // si el servidor falla, el formulario sigue abierto
        assertEquals(ErroresRed.SIN_CONEXION, vm.uiState.value.mensaje)
        vm.onCerrarDialogo()
        assertFalse(vm.uiState.value.dialogoNuevo)

        val web = vm.uiState.value.visibles.first()
        vm.onFinalizarClick(web)
        vm.onCancelarFinalizar()
        vm.onConfirmarFinalizar()
        coVerify(exactly = 0) { repo.finalizarProyecto(any()) }

        coEvery { repo.finalizarProyecto(1) } returns Result.success(proyecto(1, "Web", cierre = "2026-11-19"))
        vm.onFinalizarClick(web)
        vm.onConfirmarFinalizar()
        assertEquals("Web quedó finalizado", vm.uiState.value.mensaje)
        coEvery { repo.finalizarProyecto(1) } returns Result.failure(IOException())
        vm.onFinalizarClick(web)
        vm.onConfirmarFinalizar()
        assertEquals(ErroresRed.SIN_CONEXION, vm.uiState.value.mensaje)
    }
}

class RegistroHorasViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val repo = mockk<HorasRepository>()
    private val feriados = mockk<FeriadosRepository>()
    private val recursos = mockk<RecursosDispositivo>(relaxed = true)
    private val cronometro = CronometroFalso()
    private var ahora = System.currentTimeMillis()

    @Before
    fun preparar() {
        coEvery { repo.proyectosEnCurso() } returns listOf(proyecto(1, "Web"), proyecto(2, "App"))
        coEvery { repo.minutosDelDia(any(), any()) } returns 0
        coEvery { feriados.buscar(any()) } returns null
    }

    private fun crear(id: Long? = null) = RegistroHorasViewModel(id, repo, feriados, recursos, cronometro) { ahora }

    @Test
    fun `errores mientras se escribe y aviso de feriado`() {
        coEvery { repo.minutosDelDia(diaABase(hoy())!!, null) } returns 135
        coEvery { feriados.buscar("2026-09-18") } returns Feriado("2026-09-18", "Independencia Nacional")
        val vm = crear()
        assertEquals(listOf("Web", "App"), vm.uiState.value.proyectos)
        assertEquals(hoy(), vm.uiState.value.dia)

        vm.onHorasChange("50")
        assertEquals("Máximo 24 horas", vm.uiState.value.errores.horas)
        vm.onMinutosChange("75")
        assertEquals("Máximo 59 minutos", vm.uiState.value.errores.minutos)
        verify(exactly = 1) { recursos.vibrarError() } // vibra solo cuando aparece el error
        vm.onMinutosChange("")
        vm.onHorasChange("22")
        assertTrue(vm.uiState.value.errores.tiempo!!.contains("Te quedan 21h 45min"))
        vm.onHorasChange("1a2b3")
        assertEquals("12", vm.uiState.value.horas)

        vm.onDiaChange("18/09/2026")
        assertEquals("Este día es feriado: Independencia Nacional", vm.uiState.value.avisoFeriado)
        vm.onDiaChange("17/09/2026")
        assertNull(vm.uiState.value.avisoFeriado)
    }

    @Test
    fun `registrar horas en el servidor`() {
        val vm = crear()
        vm.onRegistrarClick { }
        assertEquals("Selecciona un proyecto", vm.uiState.value.errores.proyecto)
        assertEquals("Ingresa un tiempo mayor a cero", vm.uiState.value.errores.tiempo)

        vm.onProyectoChange("Inventado")
        vm.onHorasChange("2")
        vm.onMinutosChange("15")
        vm.onTareaChange("Maquetado")
        vm.onRegistrarClick { }
        assertEquals("Selecciona un proyecto de la lista", vm.uiState.value.errores.proyecto)

        coEvery { repo.registrar(any(), any(), any(), any(), any()) } returns Result.failure(IOException())
        vm.onProyectoChange("Web")
        var resumen: ResumenRegistro? = null
        vm.onRegistrarClick { resumen = it }
        assertNull(resumen)
        assertEquals(ErroresRed.SIN_CONEXION, vm.uiState.value.mensaje)
        vm.onMensajeMostrado()

        coEvery { repo.registrar(1, any(), 135, "Maquetado", null) } returns Result.success(registro(9))
        vm.onRegistrarClick { resumen = it }
        assertEquals(ResumenRegistro("Web", 2, 15, hoy(), null), resumen)
        verify { recursos.notificarHorasRegistradas("Web", 135) }
        coVerify { recursos.actualizarWidget() }
    }

    @Test
    fun `cronometro con pausa y retomarlo al volver a abrir`() {
        val vm = crear()
        vm.onIniciarClick()
        assertEquals("Selecciona un proyecto para iniciar", vm.uiState.value.errores.proyecto)

        vm.onProyectoChange("Web")
        vm.onIniciarClick()
        assertTrue(vm.uiState.value.corriendo)
        ahora += 60_000
        vm.onPausarClick()
        assertTrue(vm.uiState.value.pausado)
        assertEquals(60, vm.uiState.value.segundos)
        verify { recursos.mostrarCronometro("Web", 60_000, true) }
        ahora += 10 * 60_000 // diez minutos en pausa no cuentan
        vm.onReanudarClick()
        ahora += 30_000
        vm.onProyectoChange("App")
        assertEquals("App", cronometro.estado.proyecto)
        vm.onFinalizarClick()
        assertEquals(90, vm.uiState.value.segundos)
        assertEquals("2", vm.uiState.value.minutos) // 1:30 se redondea a 2 minutos
        assertFalse(cronometro.estado.activo)
        vm.finalizarSiCorre()

        // Al volver a abrir la app retoma el cronómetro que quedó en pausa
        cronometro.estado = EstadoCronometro("App", primerInicio = ahora - 900_000, acumulado = 300_000, pausado = true)
        val retomado = crear()
        assertTrue(retomado.uiState.value.pausado)
        assertEquals(300, retomado.uiState.value.segundos)
        retomado.onReanudarClick()
        retomado.finalizarSiCorre()
        assertFalse(retomado.uiState.value.corriendo)
    }

    @Test
    fun `editar un registro con foto de camara o galeria`() {
        coEvery { repo.obtenerRegistro(5) } returns registro(5, proyecto = "Cerrado", minutos = 135, fecha = "2026-11-18", foto = "/vieja.jpg")
        coEvery { repo.actualizarRegistro(5, 1, "2026-11-18", 135, "Tarea 5", "/b.jpg") } returns Result.success(registro(5))
        val uri = mockk<Uri>()
        every { recursos.nuevaFoto() } returnsMany listOf(FotoNueva("/a.jpg", uri), FotoNueva("/c.jpg", uri))
        every { recursos.fotoValida(any()) } returns true
        coEvery { recursos.copiarDeGaleria(uri) } returns "/b.jpg"

        val vm = crear(5)
        assertTrue(vm.uiState.value.editando)
        assertEquals("18/11/2026", vm.uiState.value.dia)
        assertTrue("Cerrado" in vm.uiState.value.proyectos)

        assertSame(uri, vm.prepararFoto())
        vm.onFotoTomada(true)
        assertEquals("/a.jpg", vm.uiState.value.fotoRuta)
        vm.onImagenGaleria(uri) // la de la galería reemplaza a la de la cámara
        assertEquals("/b.jpg", vm.uiState.value.fotoRuta)
        verify { recursos.borrarFoto("/a.jpg") }
        vm.prepararFoto()
        vm.onFotoTomada(false) // se canceló la cámara
        verify { recursos.borrarFoto("/c.jpg") }
        verify(exactly = 0) { recursos.borrarFoto("/vieja.jpg") } // la original se borra recién al guardar

        var resumen: ResumenRegistro? = null
        vm.onRegistrarClick { resumen = it }
        assertTrue(resumen!!.editado)
        verify { recursos.borrarFoto("/vieja.jpg") }

        vm.onPermisoCamaraDenegado()
        assertNotNull(vm.uiState.value.mensaje)
        vm.onPermisoNotificacionesDenegado()
        vm.onCamaraNoDisponible()
        assertTrue(vm.uiState.value.mensaje!!.contains("cámara"))
        coEvery { recursos.copiarDeGaleria(uri) } returns null
        vm.onImagenGaleria(uri)
        assertEquals("No se pudo cargar la imagen. Prueba con otra", vm.uiState.value.mensaje)
        vm.onImagenGaleria(null)
        vm.onQuitarFoto()
        assertNull(vm.uiState.value.fotoRuta)

        coEvery { repo.obtenerRegistro(99) } returns null
        assertEquals("No encontramos ese registro", crear(99).uiState.value.mensaje)
    }
}
