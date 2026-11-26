package com.example.deepsourcehoras

import com.example.deepsourcehoras.data.local.AporteEntidad
import com.example.deepsourcehoras.data.remote.ErroresRed
import com.example.deepsourcehoras.data.remote.dto.FeriadoDto
import com.example.deepsourcehoras.data.remote.dto.ProyectoDto
import com.example.deepsourcehoras.data.remote.dto.RegistroDto
import com.example.deepsourcehoras.data.remote.dto.aEntidad
import com.example.deepsourcehoras.data.remote.dto.aLocal
import com.example.deepsourcehoras.data.remote.dto.aModelo
import com.example.deepsourcehoras.domain.aportes.CalculoAportes
import com.example.deepsourcehoras.domain.cronometro.EstadoCronometro
import com.example.deepsourcehoras.domain.validation.DatosRegistro
import com.example.deepsourcehoras.domain.validation.LoginValidator
import com.example.deepsourcehoras.domain.validation.ProyectoValidator
import com.example.deepsourcehoras.domain.validation.RegistroHorasValidator
import com.example.deepsourcehoras.model.FiltroProyectos
import com.example.deepsourcehoras.model.HistorialUiState
import com.example.deepsourcehoras.model.HomeUiState
import com.example.deepsourcehoras.model.LoginErrores
import com.example.deepsourcehoras.model.MisProyectosUiState
import com.example.deepsourcehoras.model.ProyectoDetalle
import com.example.deepsourcehoras.model.ProyectoErrores
import com.example.deepsourcehoras.model.Proyecto
import com.example.deepsourcehoras.model.RegistroHorasErrores
import com.example.deepsourcehoras.model.RegistroItemUi
import com.example.deepsourcehoras.util.baseADia
import com.example.deepsourcehoras.util.diaABase
import com.example.deepsourcehoras.util.diaDe
import com.example.deepsourcehoras.util.etiquetaDia
import com.example.deepsourcehoras.util.formatearCronometro
import com.example.deepsourcehoras.util.formatearDuracion
import com.example.deepsourcehoras.util.hora
import com.example.deepsourcehoras.util.hoy
import com.example.deepsourcehoras.util.hoyBase
import com.example.deepsourcehoras.util.inicioSemanaBase
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas de la lógica que no depende de Android: validaciones, cronómetro, aportes, formatos y modelos
class LogicaTest {

    @Test
    fun `validacion del login`() {
        assertFalse(LoginValidator.validar("usuario@deepsource.io", "123456").hayErrores)
        assertEquals("Ingresa tu correo", LoginValidator.validar("  ", "123456").email)
        assertEquals("Correo no válido", LoginValidator.validar("usuario@", "123456").email)
        assertEquals("Ingresa tu contraseña", LoginValidator.validar("a@b.cl", "").password)
        assertEquals("Mínimo 6 caracteres", LoginValidator.validar("a@b.cl", "123").password)
    }

    @Test
    fun `validacion del registro de horas`() {
        val hoy = "19/11/2026"
        fun datos(horas: String = "2", minutos: String = "15", tarea: String = "Maquetado", dia: String = hoy, proyecto: String = "Web") =
            DatosRegistro(proyecto, dia, horas, minutos, tarea)

        assertFalse(RegistroHorasValidator.validar(datos(), hoy).hayErrores)

        // Mientras se escribe: 24 h, 59 min y tope del día
        val enVivo = RegistroHorasValidator.erroresDeTiempo("50", "75")
        assertEquals("Máximo 24 horas", enVivo.horas)
        assertEquals("Máximo 59 minutos", enVivo.minutos)
        assertNull(enVivo.tiempo)
        assertFalse(RegistroHorasValidator.erroresDeTiempo("", "").hayErrores)
        assertEquals("Un registro no puede pasar de 24 horas", RegistroHorasValidator.erroresDeTiempo("24", "30").tiempo)
        assertTrue(RegistroHorasValidator.erroresDeTiempo("22", "0", minutosDelDia = 135).tiempo!!.contains("Te quedan 21h 45min"))

        // Al apretar Registrar
        val vacio = RegistroHorasValidator.validar(datos(horas = "", minutos = "", tarea = "", proyecto = "", dia = ""), hoy)
        assertEquals("Ingresa un tiempo mayor a cero", vacio.tiempo)
        assertEquals("Escribe el nombre de la tarea", vacio.tarea)
        assertEquals("Selecciona un proyecto", vacio.proyecto)
        assertEquals("Selecciona un día", vacio.dia)
        assertEquals("Mínimo 3 caracteres", RegistroHorasValidator.validar(datos(tarea = "ab"), hoy).tarea)
        assertEquals("Máximo 60 caracteres", RegistroHorasValidator.validar(datos(tarea = "x".repeat(61)), hoy).tarea)
        assertEquals("No puedes registrar días futuros", RegistroHorasValidator.validar(datos(dia = "20/11/2026"), hoy).dia)
        assertEquals("Fecha no válida", RegistroHorasValidator.validar(datos(dia = "31/02/2026"), hoy).dia)
        assertTrue(RegistroHorasValidator.validar(datos(horas = "23"), hoy, minutosDelDia = 120).tiempo!!.startsWith("Ese día ya tienes 2h registradas"))
        val fueraDeRango = RegistroHorasValidator.validar(datos(horas = "30"), hoy)
        assertEquals("Máximo 24 horas", fueraDeRango.horas)
        assertNull(fueraDeRango.tiempo)
    }

    @Test
    fun `validacion de un proyecto nuevo`() {
        assertFalse(ProyectoValidator.validar("Portal", "AFC", listOf("Web")).hayErrores)
        assertEquals("Escribe el nombre del proyecto", ProyectoValidator.validar(" ", "AFC", emptyList()).nombre)
        assertEquals("Mínimo 3 caracteres", ProyectoValidator.validar("ab", "AFC", emptyList()).nombre)
        assertEquals("Máximo 40 caracteres", ProyectoValidator.validar("x".repeat(41), "AFC", emptyList()).nombre)
        assertEquals("Ya existe un proyecto con ese nombre", ProyectoValidator.validar("web", "AFC", listOf("Web")).nombre)
        assertEquals("Escribe el cliente", ProyectoValidator.validar("Portal", "", emptyList()).cliente)
        assertEquals("Mínimo 2 caracteres", ProyectoValidator.validar("Portal", "A", emptyList()).cliente)
        assertEquals("Máximo 40 caracteres", ProyectoValidator.validar("Portal", "x".repeat(41), emptyList()).cliente)
    }

    @Test
    fun `cronometro con pausa`() {
        val sinIniciar = EstadoCronometro()
        assertFalse(sinIniciar.activo)
        assertEquals(0L, sinIniciar.transcurrido(10_000))
        assertSame(sinIniciar, sinIniciar.pausar(5))
        assertSame(sinIniciar, sinIniciar.reanudar(5))

        val corriendo = EstadoCronometro.iniciar("Web", 1_000)
        assertTrue(corriendo.activo)
        assertEquals(4_000L, corriendo.transcurrido(5_000))
        assertSame(corriendo, corriendo.reanudar(2_000))

        // El tiempo en pausa no cuenta: 60 s corriendo + 30 s después de reanudar = 90 s
        val pausado = corriendo.pausar(61_000)
        assertTrue(pausado.pausado)
        assertEquals(60_000L, pausado.transcurrido(500_000))
        assertSame(pausado, pausado.pausar(600_000))
        assertEquals(90_000L, pausado.reanudar(500_000).transcurrido(530_000))
        assertTrue(corriendo.alternar(10).pausado)
        assertFalse(pausado.alternar(20).pausado)
    }

    @Test
    fun `aporte de cada integrante`() {
        fun a(usuarioId: Long?, nombre: String, minutos: Int, proyecto: Long = 1) = AporteEntidad(proyecto, usuarioId, nombre, minutos)

        val dos = CalculoAportes.calcular(listOf(a(2, "Carla", 60), a(1, "Usuario", 180)), usuarioActual = 1)
        assertEquals(listOf("Usuario", "Carla"), dos.map { it.nombre })
        assertEquals(listOf(75, 25), dos.map { it.porcentaje })
        assertEquals("3h", dos[0].duracion)
        assertTrue(dos[0].esTu)
        assertFalse(dos[1].esTu)

        // El redondeo siempre suma 100
        assertEquals(listOf(34, 33, 33), CalculoAportes.calcular(listOf(a(1, "A", 100), a(2, "B", 100), a(3, "C", 100)), null).map { it.porcentaje })
        assertEquals(listOf(41, 38, 21), CalculoAportes.calcular(listOf(a(1, "A", 555), a(2, "B", 585), a(3, "C", 300)), null).map { it.porcentaje })
        assertTrue(CalculoAportes.calcular(emptyList(), 1).isEmpty())
        assertTrue(CalculoAportes.calcular(listOf(a(1, "A", 0)), 1).isEmpty())
        assertFalse(CalculoAportes.calcular(listOf(a(null, "Sin asignar", 30)), null).single().esTu)

        val porProyecto = CalculoAportes.porProyecto(listOf(a(1, "A", 60, proyecto = 1), a(2, "B", 60, proyecto = 2)), 1)
        assertEquals(setOf(1L, 2L), porProyecto.keys)
        assertEquals(100, porProyecto.getValue(2).single().porcentaje)
    }

    @Test
    fun `formatos de fecha y duracion`() {
        assertEquals("2h 15min", formatearDuracion(135))
        assertEquals("2h", formatearDuracion(120))
        assertEquals("45min", formatearDuracion(45))
        assertEquals("01:02:05", formatearCronometro(3725))
        assertEquals("2026-11-19", diaABase("19/11/2026"))
        assertEquals("19/11/2026", baseADia("2026-11-19"))
        assertNull(diaABase("31/02/2026"))
        assertNull(diaABase("1/1/26"))
        assertEquals("texto", baseADia("texto"))
        assertEquals(hoyBase(), diaABase(hoy()))
        assertEquals(hoy(), diaDe(System.currentTimeMillis()))
        assertTrue(hora(0).matches(Regex("\\d{2}:\\d{2}")))

        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendario = Calendar.getInstance()
        assertEquals("Hoy", etiquetaDia(formato.format(calendario.time)))
        calendario.add(Calendar.DAY_OF_YEAR, -1)
        assertEquals("Ayer", etiquetaDia(formato.format(calendario.time)))
        assertEquals("06/10", etiquetaDia("2026-10-06"))
        assertEquals("cualquiera", etiquetaDia("cualquiera"))

        val lunes = inicioSemanaBase()
        assertTrue(lunes <= hoyBase())
        calendario.time = formato.parse(lunes)!!
        assertEquals(Calendar.MONDAY, calendario.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun `modelos, conversion de datos y errores de red`() {
        assertEquals(90, HomeUiState(proyectos = listOf(Proyecto("A", 60, 0f), Proyecto("B", 30, 0f))).totalMinutos)
        val lista = listOf(ProyectoDetalle("A", "X", 1), ProyectoDetalle("B", "X", 1, fechaCierre = "01/01/2026"))
        assertEquals(listOf("A"), MisProyectosUiState(proyectos = lista).visibles.map { it.nombre })
        assertEquals(listOf("B"), MisProyectosUiState(filtro = FiltroProyectos.FINALIZADOS, proyectos = lista).visibles.map { it.nombre })
        assertTrue(LoginErrores(email = "x").hayErrores)
        assertFalse(RegistroHorasErrores().hayErrores)
        assertTrue(ProyectoErrores(cliente = "x").hayErrores)
        assertEquals("1 registros", HistorialUiState(registros = listOf(RegistroItemUi(1, "a", "b", "c"))).totalMinutosTexto)

        val proyectoDto = ProyectoDto(1, "Web", "AFC", 0.5, true, null, 120)
        assertEquals(120, proyectoDto.aEntidad().minutos)
        assertEquals(0.5f, proyectoDto.aLocal().progreso)
        val registroDto = RegistroDto(2, 1, "Web", "Tarea", "2026-11-19", 30, null, 9, usuarioId = 2, usuarioNombre = "Carla Muñoz")
        assertEquals("Carla Muñoz", registroDto.aEntidad().usuario)
        assertEquals(2L, registroDto.aLocal().usuarioId)
        assertEquals("Navidad", FeriadoDto("2026-12-25", "Navidad", "Christmas", true).aModelo().nombre)

        assertEquals("Sin horas", ErroresRed.mensaje(errorHttp(400, """{"mensaje":"Sin horas"}""")))
        assertTrue(ErroresRed.mensaje(errorHttp(500)).contains("500"))
        assertTrue(ErroresRed.mensaje(errorHttp(500, "no es json")).contains("500"))
        assertEquals(ErroresRed.SIN_CONEXION, ErroresRed.mensaje(IOException()))
        assertEquals("falló", ErroresRed.mensaje(IllegalStateException("falló")))
        assertEquals("Ocurrió un error inesperado", ErroresRed.mensaje(IllegalStateException()))
    }
}
