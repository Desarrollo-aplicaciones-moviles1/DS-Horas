package com.deepsource.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import com.deepsource.usuarios.dto.LoginRequest;
import com.deepsource.usuarios.service.UsuarioService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// Igual que en ms-horas: PostgreSQL real + supabase/schema.sql + validación de las tablas
@SpringBootTest
class SupabaseCompatibilidadTest {

    private static EmbeddedPostgres postgres;

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registro) throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        try (Connection c = postgres.getPostgresDatabase().getConnection()) {
            c.createStatement().execute(Files.readString(Path.of("../../supabase/schema.sql")));
        }
        registro.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
        registro.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registro.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @AfterAll
    static void apagar() throws Exception {
        if (postgres != null) postgres.close();
    }

    @Autowired UsuarioService servicio;

    @Test
    void lasCuentasDelEquipoQuedanEnPostgresYElLoginFunciona() {
        assertThat(servicio.listar()).extracting(u -> u.email())
                .containsExactly("usuario@deepsource.io", "carla@deepsource.io", "diego@deepsource.io");
        assertThat(servicio.login(new LoginRequest("carla@deepsource.io", "123456")).id()).isEqualTo(2L);
    }
}
