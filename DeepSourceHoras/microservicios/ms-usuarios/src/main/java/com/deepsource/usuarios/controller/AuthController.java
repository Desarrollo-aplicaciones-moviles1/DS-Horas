package com.deepsource.usuarios.controller;

import com.deepsource.usuarios.dto.LoginRequest;
import com.deepsource.usuarios.dto.LoginResponse;
import com.deepsource.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService servicio;

    public AuthController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    // POST /api/auth/login  {"email": "...", "password": "..."}
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest datos) {
        return servicio.login(datos);
    }
}
