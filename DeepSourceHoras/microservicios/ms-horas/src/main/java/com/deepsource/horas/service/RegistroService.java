package com.deepsource.horas.service;

import com.deepsource.horas.dto.MinutosDiaResponse;
import com.deepsource.horas.dto.RegistroRequest;
import com.deepsource.horas.dto.RegistroResponse;
import com.deepsource.horas.exception.ApiException;
import com.deepsource.horas.model.Proyecto;
import com.deepsource.horas.model.Registro;
import com.deepsource.horas.repository.ProyectoRepository;
import com.deepsource.horas.repository.RegistroRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

// Mismas reglas que valida la app, repetidas acá para que nadie se las salte llamando directo a la API
@Service
public class RegistroService {

    public static final int MAX_MINUTOS_DIA = 24 * 60;

    private final RegistroRepository registros;
    private final ProyectoRepository proyectos;
    private final Clock reloj;

    public RegistroService(RegistroRepository registros, ProyectoRepository proyectos, Clock reloj) {
        this.registros = registros;
        this.proyectos = proyectos;
        this.reloj = reloj;
    }

    public List<RegistroResponse> listar(Integer limite) {
        var todos = registros.findAllByOrderByFechaDescCreadoEnDesc().stream();
        if (limite != null && limite > 0) todos = todos.limit(limite);
        return todos.map(RegistroResponse::desde).toList();
    }

    public RegistroResponse obtener(Long id) {
        return RegistroResponse.desde(buscar(id));
    }

    public RegistroResponse crear(RegistroRequest datos) {
        Proyecto proyecto = proyectoEnCurso(datos.proyectoId());
        LocalDate fecha = validarFecha(datos.fecha());
        validarTopeDiario(fecha, datos.minutos(), null);
        Registro registro = new Registro(
                proyecto,
                datos.tarea().trim(),
                fecha,
                datos.minutos(),
                datos.foto(),
                reloj.millis()
        );
        registro.setUsuarioId(datos.usuarioId());
        registro.setUsuarioNombre(nombreAutor(datos.usuarioNombre()));
        return RegistroResponse.desde(registros.save(registro));
    }

    public RegistroResponse actualizar(Long id, RegistroRequest datos) {
        Registro registro = buscar(id);
        // Se puede mantener el proyecto aunque ya esté cerrado, pero no mover el registro a uno cerrado
        Proyecto proyecto = registro.getProyecto().getId().equals(datos.proyectoId())
                ? registro.getProyecto()
                : proyectoEnCurso(datos.proyectoId());
        LocalDate fecha = validarFecha(datos.fecha());
        validarTopeDiario(fecha, datos.minutos(), id);
        registro.setProyecto(proyecto);
        registro.setTarea(datos.tarea().trim());
        registro.setFecha(fecha);
        registro.setMinutos(datos.minutos());
        registro.setFoto(datos.foto());
        // El autor se mantiene al editar; solo se completa si el registro no tenía
        if (registro.getUsuarioId() == null && datos.usuarioId() != null) {
            registro.setUsuarioId(datos.usuarioId());
            registro.setUsuarioNombre(nombreAutor(datos.usuarioNombre()));
        }
        return RegistroResponse.desde(registros.save(registro));
    }

    public void eliminar(Long id) {
        registros.delete(buscar(id));
    }

    public MinutosDiaResponse minutosDelDia(String fechaTexto) {
        LocalDate fecha = parsear(fechaTexto);
        long usados = registros.sumarMinutosDelDia(fecha, null);
        return new MinutosDiaResponse(fecha.toString(), usados, Math.max(0, MAX_MINUTOS_DIA - usados));
    }

    private String nombreAutor(String nombre) {
        return (nombre == null || nombre.isBlank()) ? "Sin asignar" : nombre.trim();
    }

    private Registro buscar(Long id) {
        return registros.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe el registro " + id));
    }

    private Proyecto proyectoEnCurso(Long id) {
        Proyecto proyecto = proyectos.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe el proyecto " + id));
        if (!proyecto.isEnCurso()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El proyecto " + proyecto.getNombre() + " ya está finalizado");
        }
        return proyecto;
    }

    private LocalDate validarFecha(String texto) {
        LocalDate fecha = parsear(texto);
        if (fecha.isAfter(LocalDate.now(reloj))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes registrar días futuros");
        }
        return fecha;
    }

    private LocalDate parsear(String texto) {
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Fecha no válida, usa el formato yyyy-MM-dd");
        }
    }

    private void validarTopeDiario(LocalDate fecha, int minutos, Long excluir) {
        long usados = registros.sumarMinutosDelDia(fecha, excluir);
        if (usados + minutos > MAX_MINUTOS_DIA) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ese día no puede pasar de 24 horas registradas");
        }
    }
}
