# Base de datos en Supabase

DS Horas guarda usuarios, proyectos y registros de horas en **Supabase**, que es PostgreSQL administrado en la nube.

```
App Android  ──Retrofit/HTTP──►  API (microservicios Spring Boot)  ──JDBC/SSL──►  Supabase (PostgreSQL)
                                  ms-usuarios :8081  → tabla usuarios
                                  ms-horas    :8082  → tablas proyectos y registros
```

La app **nunca** se conecta directo a la base: siempre pasa por nuestra API, que valida los datos (día no futuro, máximo 24 h por día, proyecto en curso, contraseñas con BCrypt) y es la única que conoce la clave de la base.

## Conectar el proyecto de Supabase (una sola vez)

1. Entrar a [supabase.com](https://supabase.com) → **New project**. Elegir nombre (`ds-horas`), una **clave de base de datos** y la región *South America (São Paulo)*.
2. Ir a **SQL Editor → New query**, pegar el contenido de [`schema.sql`](schema.sql) y apretar **Run**. Esto crea:
   - las tablas `usuarios`, `proyectos` y `registros`,
   - los índices para las consultas más usadas,
   - **RLS activado** en las tres tablas (nadie puede leerlas con la API pública de Supabase; solo nuestros microservicios),
   - la vista `aporte_equipo` con las horas y el % de cada integrante por proyecto.
3. Apretar **Connect** (arriba en el proyecto) → pestaña **Session pooler** y copiar el host, el usuario (`postgres.xxxx`) y el puerto `5432`.
4. En la carpeta `microservicios/`, copiar `supabase.properties.ejemplo` como **`supabase.properties`** y completar:
   ```properties
   SUPABASE_DB_URL=jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require
   SUPABASE_DB_USER=postgres.xxxxxxxxxxxx
   SUPABASE_DB_PASSWORD=la-clave-del-paso-1
   ```
   Este archivo está en `.gitignore`: la clave no se sube a GitHub.
5. Ejecutar **`iniciar-microservicios.bat`**. Al ver `Base de datos: SUPABASE` los dos microservicios quedan conectados a la nube y, la primera vez, cargan solos las cuentas del equipo y los datos de ejemplo.

Para revisar los datos: Supabase → **Table Editor** (tablas `usuarios`, `proyectos`, `registros` y la vista `aporte_equipo`).

## Sin Supabase

Si no existe `microservicios/supabase.properties`, los microservicios usan **H2 local** (un archivo en `microservicios/*/data`) con las mismas tablas. Sirve para trabajar sin internet; el código es el mismo, solo cambia la conexión (perfil `supabase` de Spring).

## Cómo sabemos que funciona con Supabase

Cada microservicio tiene la prueba `SupabaseCompatibilidadTest`, que levanta un **PostgreSQL 16 real**, ejecuta este mismo `schema.sql` y arranca el microservicio con `ddl-auto=validate`, o sea, Hibernate revisa que cada tabla y columna calce con el código. Después crea un registro y calcula el aporte del equipo en PostgreSQL. Se corre con `gradlew test` dentro de cada microservicio.
