package com.deepsource.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Ingresa tu correo") @Email(message = "Correo no válido") String email,
        @NotBlank(message = "Ingresa tu contraseña") String password
) {
}
