package com.deepsource.horas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotNull(message = "Selecciona un proyecto") Long proyectoId,
        @NotBlank(message = "Escribe el nombre de la tarea")
        @Size(min = 3, max = 60, message = "La tarea debe tener entre 3 y 60 caracteres") String tarea,
        // Formato yyyy-MM-dd
        @NotBlank(message = "Selecciona un día") String fecha,
        @Min(value = 1, message = "Ingresa un tiempo mayor a cero")
        @Max(value = 1440, message = "Un registro no puede pasar de 24 horas") int minutos,
        String foto,
        // Autor del registro (usuario que inició sesión en la app)
        Long usuarioId,
        @Size(max = 60, message = "El nombre del usuario es muy largo") String usuarioNombre
) {
    // Constructor corto para quien no envía el autor (se guarda como "Sin asignar")
    public RegistroRequest(Long proyectoId, String tarea, String fecha, int minutos, String foto) {
        this(proyectoId, tarea, fecha, minutos, foto, null, null);
    }
}
