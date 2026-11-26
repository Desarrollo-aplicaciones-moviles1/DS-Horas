package com.deepsource.usuarios.dto;

import com.deepsource.usuarios.model.Usuario;

// Lo que se devuelve hacia afuera: nunca incluye la contraseña
public record UsuarioResponse(Long id, String nombre, String email) {
    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail());
    }
}
