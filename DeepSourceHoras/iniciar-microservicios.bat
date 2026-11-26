@echo off
REM ============================================================
REM  DS Horas - Enciende la API (los dos microservicios)
REM  Si existe microservicios\supabase.properties usa SUPABASE;
REM  si no, usa una base H2 local con los mismos datos de ejemplo.
REM  Requiere Java 21 o superior (sirve el que trae Android Studio).
REM ============================================================
REM Busca el Java que trae Android Studio en las carpetas donde suele instalarse
if "%JAVA_HOME%"=="" if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if "%JAVA_HOME%"=="" if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
if "%JAVA_HOME%"=="" if exist "C:\Program Files\Android\Android Studio Preview\jbr\bin\java.exe" set "JAVA_HOME=C:\Program Files\Android\Android Studio Preview\jbr"
if "%JAVA_HOME%"=="" (
    where java >nul 2>nul || (
        echo No se encontro Java. Instala Android Studio con las opciones por defecto
        echo o crea la variable JAVA_HOME apuntando a la carpeta jbr de Android Studio.
        pause
        exit /b 1
    )
)

set "PERFIL="
if exist "%~dp0microservicios\supabase.properties" (
    set "PERFIL=--args=--spring.profiles.active=supabase"
    echo Base de datos: SUPABASE ^(microservicios\supabase.properties^)
) else (
    echo Base de datos: H2 local ^(no se encontro microservicios\supabase.properties^)
    echo Para usar Supabase: copia microservicios\supabase.properties.ejemplo como supabase.properties y completalo.
)

REM pushd entra a la carpeta (funciona aunque la ruta tenga espacios) y start abre una ventana ahi
pushd "%~dp0microservicios\ms-usuarios"
start "ms-usuarios (8081)" cmd /k gradlew.bat bootRun %PERFIL%
popd
pushd "%~dp0microservicios\ms-horas"
start "ms-horas (8082)" cmd /k gradlew.bat bootRun %PERFIL%
popd

echo.
echo Iniciando ms-usuarios en http://localhost:8081 y ms-horas en http://localhost:8082
echo Cuando las dos ventanas digan "Started ...Application" ya se puede abrir la app.
pause
