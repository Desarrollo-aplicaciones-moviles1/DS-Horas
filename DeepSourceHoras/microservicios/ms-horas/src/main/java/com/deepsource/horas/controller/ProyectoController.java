package com.deepsource.horas.controller;

import com.deepsource.horas.dto.AporteResponse;
import com.deepsource.horas.dto.ProyectoRequest;
import com.deepsource.horas.dto.ProyectoResponse;
import com.deepsource.horas.service.ProyectoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// CRUD de proyectos: /api/proyectos
@RestController
@RequestMapping("/api/proyectos")
public class ProyectoController {

    private final ProyectoService servicio;

    public ProyectoController(ProyectoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<ProyectoResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public ProyectoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProyectoResponse crear(@Valid @RequestBody ProyectoRequest datos) {
        return servicio.crear(datos);
    }

    @PutMapping("/{id}")
    public ProyectoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProyectoRequest datos) {
        return servicio.actualizar(id, datos);
    }

    // GET /api/proyectos/{id}/aportes -> [{"usuarioId","usuario","minutos","porcentaje"}]
    @GetMapping("/{id}/aportes")
    public List<AporteResponse> aportes(@PathVariable Long id) {
        return servicio.aportes(id);
    }

    @PatchMapping("/{id}/finalizar")
    public ProyectoResponse finalizar(@PathVariable Long id) {
        return servicio.finalizar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
