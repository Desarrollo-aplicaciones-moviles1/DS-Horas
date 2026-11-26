package com.deepsource.horas.dto;

import com.deepsource.horas.model.Proyecto;

public record ProyectoResponse(
        Long id,
        String nombre,
        String cliente,
        double progreso,
        boolean fijado,
        String fechaCierre,
        long minutos
) {
    public static ProyectoResponse desde(Proyecto p, long minutos) {
        return new ProyectoResponse(
                p.getId(),
                p.getNombre(),
                p.getCliente(),
                p.getProgreso(),
                p.isFijado(),
                p.getFechaCierre() == null ? null : p.getFechaCierre().toString(),
                minutos
        );
    }
}
