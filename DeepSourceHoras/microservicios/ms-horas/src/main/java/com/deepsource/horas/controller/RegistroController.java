package com.deepsource.horas.controller;

import com.deepsource.horas.dto.MinutosDiaResponse;
import com.deepsource.horas.dto.RegistroRequest;
import com.deepsource.horas.dto.RegistroResponse;
import com.deepsource.horas.service.RegistroService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// CRUD de registros de horas: /api/registros
@RestController
@RequestMapping("/api/registros")
public class RegistroController {

    private final RegistroService servicio;

    public RegistroController(RegistroService servicio) {
        this.servicio = servicio;
    }

    // GET /api/registros?limite=5
    @GetMapping
    public List<RegistroResponse> listar(@RequestParam(required = false) Integer limite) {
        return servicio.listar(limite);
    }

    // GET /api/registros/minutos-dia?fecha=2026-11-19
    @GetMapping("/minutos-dia")
    public MinutosDiaResponse minutosDelDia(@RequestParam String fecha) {
        return servicio.minutosDelDia(fecha);
    }

    @GetMapping("/{id}")
    public RegistroResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroResponse crear(@Valid @RequestBody RegistroRequest datos) {
        return servicio.crear(datos);
    }

    @PutMapping("/{id}")
    public RegistroResponse actualizar(@PathVariable Long id, @Valid @RequestBody RegistroRequest datos) {
        return servicio.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
