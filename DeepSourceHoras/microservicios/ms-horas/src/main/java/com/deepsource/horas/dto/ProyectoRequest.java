package com.deepsource.horas.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProyectoRequest(
        @NotBlank(message = "El nombre del proyecto es obligatorio")
        @Size(min = 3, max = 40, message = "El nombre debe tener entre 3 y 40 caracteres") String nombre,
        @NotBlank(message = "El cliente es obligatorio")
        @Size(min = 2, max = 40, message = "El cliente debe tener entre 2 y 40 caracteres") String cliente,
        @DecimalMin(value = "0.0", message = "El progreso va de 0 a 1")
        @DecimalMax(value = "1.0", message = "El progreso va de 0 a 1") Double progreso,
        Boolean fijado
) {
}
