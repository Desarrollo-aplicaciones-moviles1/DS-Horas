package com.deepsource.usuarios.config;

import com.deepsource.usuarios.model.Usuario;
import com.deepsource.usuarios.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Cuentas de prueba del equipo. Se crean las que falten (todas con la clave 123456).
// El orden importa: ms-horas usa los id 1, 2 y 3 para repartir los registros de ejemplo
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final String[][] EQUIPO = {
            {"Usuario", "usuario@deepsource.io"},
            {"Carla Muñoz", "carla@deepsource.io"},
            {"Diego Rojas", "diego@deepsource.io"}
    };

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;

    public DatosIniciales(UsuarioRepository repositorio, PasswordEncoder encoder) {
        this.repositorio = repositorio;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        for (String[] integrante : EQUIPO) {
            if (!repositorio.existsByEmailIgnoreCase(integrante[1])) {
                repositorio.save(new Usuario(integrante[0], integrante[1], encoder.encode("123456")));
            }
        }
    }
}
