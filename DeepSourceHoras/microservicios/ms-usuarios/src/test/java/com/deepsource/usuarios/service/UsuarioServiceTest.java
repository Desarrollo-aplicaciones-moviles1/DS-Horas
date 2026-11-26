package com.deepsource.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deepsource.usuarios.dto.LoginRequest;
import com.deepsource.usuarios.dto.UsuarioRequest;
import com.deepsource.usuarios.exception.ApiException;
import com.deepsource.usuarios.model.Usuario;
import com.deepsource.usuarios.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Pruebas del servicio de usuarios: el repositorio se simula con Mockito
class UsuarioServiceTest {

    private UsuarioRepository repositorio;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        servicio = new UsuarioService(repositorio, encoder);
    }

    private static HttpStatus estado(Throwable e) {
        return ((ApiException) e).getStatus();
    }

    private Usuario usuarioDePrueba() {
        Usuario usuario = new Usuario("Usuario", "usuario@deepsource.io", encoder.encode("123456"));
        usuario.setId(1L);
        return usuario;
    }

    @Test
    void loginConClaveCorrectaEIncorrecta() {
        when(repositorio.findByEmailIgnoreCase("usuario@deepsource.io")).thenReturn(Optional.of(usuarioDePrueba()));

        var respuesta = servicio.login(new LoginRequest("usuario@deepsource.io", "123456"));
        assertThat(respuesta.token()).isNotBlank();
        assertThat(respuesta.nombre()).isEqualTo("Usuario");

        // Clave incorrecta: 401
        assertThatThrownBy(() -> servicio.login(new LoginRequest("usuario@deepsource.io", "otra")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void crearActualizarYBuscarUsuarios() {
        when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // La clave se guarda cifrada con BCrypt y el correo en minúsculas
        servicio.crear(new UsuarioRequest("Ana", "A@B.cl", "123456"));
        verify(repositorio).save(argThat(u -> u.getEmail().equals("a@b.cl") && encoder.matches("123456", u.getPasswordHash())));

        // Correo repetido: 409
        when(repositorio.existsByEmailIgnoreCase("a@b.cl")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(new UsuarioRequest("Ana", "a@b.cl", "123456")))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.CONFLICT));

        when(repositorio.findById(1L)).thenReturn(Optional.of(usuarioDePrueba()));
        assertThat(servicio.actualizar(1L, new UsuarioRequest("Nuevo", "usuario@deepsource.io", "123456")).nombre()).isEqualTo("Nuevo");

        // No existe: 404
        when(repositorio.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.obtener(99L))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
