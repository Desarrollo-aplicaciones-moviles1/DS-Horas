package com.deepsource.horas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.deepsource.horas.dto.RegistroRequest;
import com.deepsource.horas.exception.ApiException;
import com.deepsource.horas.model.Proyecto;
import com.deepsource.horas.model.Registro;
import com.deepsource.horas.repository.ProyectoRepository;
import com.deepsource.horas.repository.RegistroRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// Pruebas del servicio de registros: reglas de negocio con el repositorio simulado (Mockito)
class RegistroServiceTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-19T15:00:00Z"), ZoneId.of("America/Santiago"));

    @Test
    void reglasAlRegistrarYEditarHoras() {
        RegistroRepository registros = mock(RegistroRepository.class);
        ProyectoRepository proyectos = mock(ProyectoRepository.class);
        RegistroService servicio = new RegistroService(registros, proyectos, reloj);
        Proyecto web = new Proyecto("Rediseño Web", "AFC", 0.5, true, null);
        web.setId(1L);
        when(proyectos.findById(1L)).thenReturn(Optional.of(web));
        when(registros.save(any(Registro.class))).thenAnswer(inv -> {
            Registro r = inv.getArgument(0);
            if (r.getId() == null) r.setId(10L);
            return r;
        });

        // Registro válido: guarda la tarea sin espacios y el autor
        when(registros.sumarMinutosDelDia(eq(LocalDate.of(2026, 11, 19)), isNull())).thenReturn(60L);
        var creado = servicio.crear(new RegistroRequest(1L, " Maquetado ", "2026-11-19", 90, null, 2L, "Carla Muñoz"));
        assertThat(creado.tarea()).isEqualTo("Maquetado");
        assertThat(creado.usuarioNombre()).isEqualTo("Carla Muñoz");

        // Días futuros, fechas mal escritas y más de 24 horas en un día se rechazan
        assertThatThrownBy(() -> servicio.crear(new RegistroRequest(1L, "Tarea", "2026-11-20", 30, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("futuros");
        assertThatThrownBy(() -> servicio.crear(new RegistroRequest(1L, "Tarea", "19/11/2026", 30, null)))
                .hasMessageContaining("Fecha no válida");
        when(registros.sumarMinutosDelDia(eq(LocalDate.of(2026, 11, 18)), isNull())).thenReturn(1400L);
        assertThatThrownBy(() -> servicio.crear(new RegistroRequest(1L, "Tarea", "2026-11-18", 60, null)))
                .hasMessageContaining("24 horas");

        // Proyecto finalizado: no acepta horas nuevas
        Proyecto cerrado = new Proyecto("Intranet", "AFC", 1.0, false, LocalDate.of(2026, 9, 15));
        cerrado.setId(2L);
        when(proyectos.findById(2L)).thenReturn(Optional.of(cerrado));
        assertThatThrownBy(() -> servicio.crear(new RegistroRequest(2L, "Tarea", "2026-11-17", 60, null)))
                .hasMessageContaining("finalizado");

        // Al editar, el tope del día no cuenta el mismo registro
        Registro existente = new Registro(web, "Antes", LocalDate.of(2026, 11, 18), 600, null, 0L);
        existente.setId(5L);
        when(registros.findById(5L)).thenReturn(Optional.of(existente));
        when(registros.sumarMinutosDelDia(LocalDate.of(2026, 11, 18), 5L)).thenReturn(800L);
        assertThat(servicio.actualizar(5L, new RegistroRequest(1L, "Después", "2026-11-18", 640, null)).minutos()).isEqualTo(640);

        // Minutos disponibles del día y registro inexistente
        when(registros.sumarMinutosDelDia(LocalDate.of(2026, 11, 19), null)).thenReturn(600L);
        assertThat(servicio.minutosDelDia("2026-11-19").disponibles()).isEqualTo(840L);
        when(registros.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.eliminar(99L)).isInstanceOf(ApiException.class);
    }
}
