package com.deepsource.horas.config;

import com.deepsource.horas.model.Proyecto;
import com.deepsource.horas.model.Registro;
import com.deepsource.horas.repository.ProyectoRepository;
import com.deepsource.horas.repository.RegistroRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Proyectos y registros de ejemplo para que la app no parta vacía. Solo se cargan si la base está vacía
@Component
public class DatosIniciales implements CommandLineRunner {

    private final ProyectoRepository proyectos;
    private final RegistroRepository registros;
    private final Clock reloj;

    public DatosIniciales(ProyectoRepository proyectos, RegistroRepository registros, Clock reloj) {
        this.proyectos = proyectos;
        this.registros = registros;
        this.reloj = reloj;
    }

    @Override
    public void run(String... args) {
        if (proyectos.count() > 0) return;

        Map<String, Proyecto> p = new HashMap<>();
        guardar(p, new Proyecto("Rediseño Web", "AFC", 0.65, true, null));
        guardar(p, new Proyecto("App Móvil", "Deepsource", 0.80, true, null));
        guardar(p, new Proyecto("Dashboard", "AFC", 0.30, true, null));
        guardar(p, new Proyecto("API Integración", "Deepsource", 0.15, true, null));
        guardar(p, new Proyecto("Portal Cliente", "AFC", 0.10, false, null));
        guardar(p, new Proyecto("Intranet Soporte", "AFC", 1.0, false, LocalDate.of(2026, 9, 15)));
        guardar(p, new Proyecto("Sitio Corporativo", "Deepsource", 1.0, false, LocalDate.of(2026, 8, 28)));

        LocalDate hoy = LocalDate.now(reloj);
        Object[][] recientes = {
                {"Rediseño Web", "Maquetado pantallas principales", 135, 0},
                {"API Integración", "Revisión de endpoints", 90, 1},
                {"App Móvil", "Login biométrico", 240, 1},
                {"Dashboard", "Componentes del dashboard", 180, 2},
                {"App Móvil", "Pantalla de reservas", 420, 2},
                {"Rediseño Web", "Ajustes responsive", 240, 3},
                {"Dashboard", "Gráficos de KPIs", 240, 3},
                {"App Móvil", "Integración de pagos", 480, 4},
                {"Portal Cliente", "Levantamiento de requisitos", 180, 4},
                {"Rediseño Web", "Revisión con cliente", 120, 5},
                {"API Integración", "Endpoint de tickets", 210, 5},
                {"Rediseño Web", "Maquetado pantallas principales", 300, 6},
                {"Dashboard", "Filtros por fecha", 300, 6},
                {"App Móvil", "Pantalla de reservas", 360, 7},
                {"Rediseño Web", "Componentes UI", 345, 8},
                {"API Integración", "Documentación Swagger", 120, 8},
                {"App Móvil", "Pruebas en dispositivos", 300, 9},
                {"Rediseño Web", "Diseño de flujo", 300, 10},
                {"App Móvil", "Integración de pagos", 480, 12}
        };
        long ahora = reloj.millis();
        int i = 0;
        for (Object[] r : recientes) {
            registros.save(conAutor(new Registro(p.get((String) r[0]), (String) r[1],
                    hoy.minusDays((Integer) r[3]), (Integer) r[2], null, ahora - i * 60_000L), i));
            i++;
        }

        Object[][] cerrados = {
                {"Intranet Soporte", "Soporte a usuarios", "2026-09-12"},
                {"Intranet Soporte", "Soporte a usuarios", "2026-09-10"},
                {"Intranet Soporte", "Corrección de incidencias", "2026-09-08"},
                {"Intranet Soporte", "Soporte a usuarios", "2026-09-05"},
                {"Intranet Soporte", "Corrección de incidencias", "2026-09-03"},
                {"Intranet Soporte", "Soporte a usuarios", "2026-09-01"},
                {"Sitio Corporativo", "Publicación del sitio", "2026-08-27"},
                {"Sitio Corporativo", "Migración de contenidos", "2026-08-26"},
                {"Sitio Corporativo", "Migración de contenidos", "2026-08-24"},
                {"Sitio Corporativo", "Maquetado de secciones", "2026-08-21"},
                {"Sitio Corporativo", "Maquetado de secciones", "2026-08-19"}
        };
        for (Object[] r : cerrados) {
            int minutos = r[1].equals("Publicación del sitio") ? 300 : 540;
            registros.save(conAutor(new Registro(p.get((String) r[0]), (String) r[1],
                    LocalDate.parse((String) r[2]), minutos, null, ahora - i * 60_000L), i));
            i++;
        }
    }

    // Integrantes de ejemplo (mismos id que crea ms-usuarios). Los registros se reparten entre ellos
    private static final Object[][] INTEGRANTES = {
            {1L, "Usuario"},
            {2L, "Carla Muñoz"},
            {3L, "Diego Rojas"}
    };

    // Cuántos registros lleva cada proyecto, para ir rotando a los integrantes dentro de cada uno
    private final Map<String, Integer> contadorPorProyecto = new HashMap<>();

    private Registro conAutor(Registro registro, int indice) {
        // Reparto desigual a propósito (Usuario, Carla, Usuario, Diego, Carla...),
        // para que los porcentajes no salgan todos iguales
        int[] patron = {0, 1, 0, 2, 1};
        int n = contadorPorProyecto.merge(registro.getProyecto().getNombre(), 1, Integer::sum) - 1;
        Object[] autor = INTEGRANTES[patron[n % patron.length]];
        registro.setUsuarioId((Long) autor[0]);
        registro.setUsuarioNombre((String) autor[1]);
        return registro;
    }

    private void guardar(Map<String, Proyecto> mapa, Proyecto proyecto) {
        mapa.put(proyecto.getNombre(), proyectos.save(proyecto));
    }
}
