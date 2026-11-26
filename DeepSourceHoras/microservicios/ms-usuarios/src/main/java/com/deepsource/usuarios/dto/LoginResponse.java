package com.deepsource.usuarios.dto;

public record LoginResponse(String token, Long id, String nombre, String email) {
}
