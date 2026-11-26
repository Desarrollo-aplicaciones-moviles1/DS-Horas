package com.deepsource.horas;

import static org.assertj.core.api.Assertions.assertThat;

import com.deepsource.horas.dto.RegistroRequest;
import com.deepsource.horas.service.ProyectoService;
import com.deepsource.horas.service.RegistroService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// Arranca un PostgreSQL real (el mismo motor de Supabase), corre supabase/schema.sql
// y levanta el microservicio contra esa base: si esto pasa, la conexión a Supabase funciona igual.
@SpringBootTest
class SupabaseCompatibilidadTest {

    private static EmbeddedPostgres postgres;

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registro) throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        try (Connection c = postgres.getPostgresDatabase().getConnection()) {
            String sql = Files.readString(Path.of("../../supabase/schema.sql"));
            c.createStatement().execute(sql);
        }
        registro.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
        registro.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // validate: Hibernate revisa que las tablas de schema.sql calcen exacto con las entidades
        registro.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @AfterAll
    static void apagar() throws Exception {
        if (postgres != null) postgres.close();
    }

    @Autowired ProyectoService proyectos;
    @Autowired RegistroService registros;

    @Test
    void elEsquemaDeSupabaseCalzaYGuardaRegistrosConAporte() {
        // DatosIniciales cargó los proyectos y registros de ejemplo en PostgreSQL
        assertThat(proyectos.listar()).hasSize(7);
        assertThat(registros.listar(null)).isNotEmpty();

        var web = proyectos.listar().stream().filter(p -> p.nombre().equals("Rediseño Web")).findFirst().orElseThrow();
        String hoy = LocalDate.now(ZoneId.of("America/Santiago")).toString();
        var creado = registros.crear(new RegistroRequest(web.id(), "Prueba en PostgreSQL", hoy, 30, null, 3L, "Diego Rojas"));

        assertThat(creado.id()).isNotNull();
        var aportes = proyectos.aportes(web.id());
        assertThat(aportes.stream().mapToInt(a -> a.porcentaje()).sum()).isEqualTo(100);
        assertThat(aportes).anyMatch(a -> a.usuario().equals("Diego Rojas"));
    }
}
