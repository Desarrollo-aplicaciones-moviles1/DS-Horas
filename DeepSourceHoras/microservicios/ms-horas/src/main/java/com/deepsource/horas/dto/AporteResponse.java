package com.deepsource.horas.dto;

// Cuánto lleva trabajado una persona en un proyecto
public record AporteResponse(Long usuarioId, String usuario, long minutos, int porcentaje) {
}
