package com.deepsource.horas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.deepsource.horas.dto.ProyectoRequest;
import com.deepsource.horas.exception.ApiException;
import com.deepsource.horas.model.Proyecto;
import com.deepsource.horas.repository.ProyectoRepository;
import com.deepsource.horas.repository.RegistroRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// Pruebas del servicio de proyectos: crear, finalizar, eliminar y aporte de cada integrante
class ProyectoServiceTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-19T15:00:00Z"), ZoneId.of("America/Santiago"));

    @Test
    void cicloDeUnProyectoYAporteDelEquipo() {
        ProyectoRepository proyectos = mock(ProyectoRepository.class);
        RegistroRepository registros = mock(RegistroRepository.class);
        ProyectoService servicio = new ProyectoService(proyectos, registros, reloj);
        when(proyectos.save(any(Proyecto.class))).thenAnswer(inv -> {
            Proyecto p = inv.getArgument(0);
            if (p.getId() == null) p.setId(3L);
            return p;
        });

        // Crear: nombre sin espacios y en curso; nombre repetido se rechaza
        var creado = servicio.crear(new ProyectoRequest(" Portal ", "AFC", null, null));
        assertThat(creado.nombre()).isEqualTo("Portal");
        assertThat(creado.fechaCierre()).isNull();
        when(proyectos.existsByNombreIgnoreCase("Portal")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(new ProyectoRequest("Portal", "AFC", null, null))).isInstanceOf(ApiException.class);

        Proyecto portal = new Proyecto("Portal", "AFC", 0.4, true, null);
        portal.setId(3L);
        when(proyectos.findById(3L)).thenReturn(Optional.of(portal));

        // El aporte de cada integrante siempre suma 100 %
        when(registros.minutosPorUsuario(3L)).thenReturn(List.of(
                new Object[]{1L, "Usuario", 100L}, new Object[]{2L, "Carla Muñoz", 100L}, new Object[]{3L, "Diego Rojas", 100L}));
        assertThat(servicio.aportes(3L)).extracting(a -> a.porcentaje()).containsExactly(34, 33, 33);

        // No se elimina un proyecto con horas; al finalizar queda con la fecha de hoy y 100 % de avance
        when(registros.sumarMinutosDeProyecto(3L)).thenReturn(30L);
        assertThatThrownBy(() -> servicio.eliminar(3L)).isInstanceOf(ApiException.class);
        var finalizado = servicio.finalizar(3L);
        assertThat(finalizado.fechaCierre()).isEqualTo("2026-11-19");
        assertThat(finalizado.progreso()).isEqualTo(1.0);
    }
}
