package com.deepsource.horas.dto;

import com.deepsource.horas.model.Registro;

public record RegistroResponse(
        Long id,
        Long proyectoId,
        String proyecto,
        String tarea,
        String fecha,
        int minutos,
        String foto,
        long creadoEn,
        Long usuarioId,
        String usuarioNombre
) {
    public static RegistroResponse desde(Registro r) {
        return new RegistroResponse(
                r.getId(),
                r.getProyecto().getId(),
                r.getProyecto().getNombre(),
                r.getTarea(),
                r.getFecha().toString(),
                r.getMinutos(),
                r.getFoto(),
                r.getCreadoEn(),
                r.getUsuarioId(),
                r.getUsuarioNombre()
        );
    }
}
