package com.deepsource.horas.service;

import com.deepsource.horas.dto.AporteResponse;
import com.deepsource.horas.dto.ProyectoRequest;
import com.deepsource.horas.dto.ProyectoResponse;
import com.deepsource.horas.exception.ApiException;
import com.deepsource.horas.model.Proyecto;
import com.deepsource.horas.repository.ProyectoRepository;
import com.deepsource.horas.repository.RegistroRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProyectoService {

    private final ProyectoRepository proyectos;
    private final RegistroRepository registros;
    private final Clock reloj;

    public ProyectoService(ProyectoRepository proyectos, RegistroRepository registros, Clock reloj) {
        this.proyectos = proyectos;
        this.registros = registros;
        this.reloj = reloj;
    }

    public List<ProyectoResponse> listar() {
        return proyectos.findAllByOrderByFijadoDescIdAsc().stream().map(this::aRespuesta).toList();
    }

    public ProyectoResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    public ProyectoResponse crear(ProyectoRequest datos) {
        String nombre = datos.nombre().trim();
        if (proyectos.existsByNombreIgnoreCase(nombre)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un proyecto con ese nombre");
        }
        Proyecto proyecto = new Proyecto(
                nombre,
                datos.cliente().trim(),
                datos.progreso() == null ? 0.0 : datos.progreso(),
                Boolean.TRUE.equals(datos.fijado()),
                null
        );
        return aRespuesta(proyectos.save(proyecto));
    }

    public ProyectoResponse actualizar(Long id, ProyectoRequest datos) {
        Proyecto proyecto = buscar(id);
        String nombre = datos.nombre().trim();
        if (proyectos.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un proyecto con ese nombre");
        }
        proyecto.setNombre(nombre);
        proyecto.setCliente(datos.cliente().trim());
        if (datos.progreso() != null) proyecto.setProgreso(datos.progreso());
        if (datos.fijado() != null) proyecto.setFijado(datos.fijado());
        return aRespuesta(proyectos.save(proyecto));
    }

    // Cierra el proyecto con la fecha de hoy y el avance al 100 %
    public ProyectoResponse finalizar(Long id) {
        Proyecto proyecto = buscar(id);
        if (!proyecto.isEnCurso()) {
            throw new ApiException(HttpStatus.CONFLICT, "El proyecto ya está finalizado");
        }
        proyecto.setFechaCierre(LocalDate.now(reloj));
        proyecto.setProgreso(1.0);
        proyecto.setFijado(false);
        return aRespuesta(proyectos.save(proyecto));
    }

    @Transactional
    public void eliminar(Long id) {
        Proyecto proyecto = buscar(id);
        if (registros.sumarMinutosDeProyecto(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede eliminar un proyecto que tiene horas registradas");
        }
        proyectos.delete(proyecto);
    }

    // Aporte de cada integrante: minutos y porcentaje sobre el total del proyecto.
    // Los porcentajes se redondean para que siempre sumen 100
    public List<AporteResponse> aportes(Long id) {
        buscar(id);
        List<Object[]> filas = registros.minutosPorUsuario(id);
        long total = filas.stream().mapToLong(f -> ((Number) f[2]).longValue()).sum();
        if (total == 0) return List.of();

        int[] porcentajes = new int[filas.size()];
        double[] restos = new double[filas.size()];
        int asignado = 0;
        for (int i = 0; i < filas.size(); i++) {
            double exacto = ((Number) filas.get(i)[2]).longValue() * 100.0 / total;
            porcentajes[i] = (int) Math.floor(exacto);
            restos[i] = exacto - porcentajes[i];
            asignado += porcentajes[i];
        }
        // Lo que falta para llegar a 100 se reparte entre los de mayor decimal
        for (int falta = 100 - asignado; falta > 0; falta--) {
            int mayor = 0;
            for (int i = 1; i < restos.length; i++) if (restos[i] > restos[mayor]) mayor = i;
            porcentajes[mayor]++;
            restos[mayor] = -1;
        }

        List<AporteResponse> resultado = new java.util.ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            Object[] f = filas.get(i);
            String nombre = f[1] == null ? "Sin asignar" : (String) f[1];
            resultado.add(new AporteResponse((Long) f[0], nombre, ((Number) f[2]).longValue(), porcentajes[i]));
        }
        return resultado;
    }

    Proyecto buscar(Long id) {
        return proyectos.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe el proyecto " + id));
    }

    private ProyectoResponse aRespuesta(Proyecto p) {
        return ProyectoResponse.desde(p, registros.sumarMinutosDeProyecto(p.getId()));
    }
}
