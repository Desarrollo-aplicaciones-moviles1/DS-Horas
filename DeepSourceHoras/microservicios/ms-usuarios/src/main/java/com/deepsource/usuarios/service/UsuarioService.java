package com.deepsource.usuarios.service;

import com.deepsource.usuarios.dto.LoginRequest;
import com.deepsource.usuarios.dto.LoginResponse;
import com.deepsource.usuarios.dto.UsuarioRequest;
import com.deepsource.usuarios.dto.UsuarioResponse;
import com.deepsource.usuarios.exception.ApiException;
import com.deepsource.usuarios.model.Usuario;
import com.deepsource.usuarios.repository.UsuarioRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Lógica de negocio de usuarios. Los controladores solo reciben y responden
@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder encoder) {
        this.repositorio = repositorio;
        this.encoder = encoder;
    }

    public LoginResponse login(LoginRequest datos) {
        Usuario usuario = repositorio.findByEmailIgnoreCase(datos.email().trim())
                .filter(u -> encoder.matches(datos.password(), u.getPasswordHash()))
                // Mismo mensaje si no existe o si la clave no coincide, para no revelar qué correos existen
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));
        String token = UUID.randomUUID().toString();
        return new LoginResponse(token, usuario.getId(), usuario.getNombre(), usuario.getEmail());
    }

    public List<UsuarioResponse> listar() {
        return repositorio.findAll().stream().map(UsuarioResponse::desde).toList();
    }

    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.desde(buscar(id));
    }

    public UsuarioResponse crear(UsuarioRequest datos) {
        String email = datos.email().trim().toLowerCase();
        if (repositorio.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
        }
        Usuario usuario = new Usuario(datos.nombre().trim(), email, encoder.encode(datos.password()));
        return UsuarioResponse.desde(repositorio.save(usuario));
    }

    public UsuarioResponse actualizar(Long id, UsuarioRequest datos) {
        Usuario usuario = buscar(id);
        String email = datos.email().trim().toLowerCase();
        boolean cambiaCorreo = !usuario.getEmail().equalsIgnoreCase(email);
        if (cambiaCorreo && repositorio.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
        }
        usuario.setNombre(datos.nombre().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(encoder.encode(datos.password()));
        return UsuarioResponse.desde(repositorio.save(usuario));
    }

    public void eliminar(Long id) {
        repositorio.delete(buscar(id));
    }

    private Usuario buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe el usuario " + id));
    }
}
