package com.deepsource.horas.exception;

// Formato único de error que lee la app: {"mensaje": "..."}
public record ErrorResponse(String mensaje) {
}
