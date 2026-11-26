package com.deepsource.horas.repository;

import com.deepsource.horas.model.Registro;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroRepository extends JpaRepository<Registro, Long> {

    // Los más nuevos primero
    List<Registro> findAllByOrderByFechaDescCreadoEnDesc();

    // Minutos por persona en un proyecto: [usuarioId, usuarioNombre, minutos]
    @Query("select r.usuarioId, r.usuarioNombre, sum(r.minutos) from Registro r "
            + "where r.proyecto.id = :proyectoId group by r.usuarioId, r.usuarioNombre "
            + "order by sum(r.minutos) desc")
    List<Object[]> minutosPorUsuario(@Param("proyectoId") Long proyectoId);

    @Query("select coalesce(sum(r.minutos), 0) from Registro r where r.proyecto.id = :proyectoId")
    long sumarMinutosDeProyecto(@Param("proyectoId") Long proyectoId);

    // Minutos ya registrados en un día, sin contar el registro que se está editando
    @Query("select coalesce(sum(r.minutos), 0) from Registro r "
            + "where r.fecha = :fecha and (:excluir is null or r.id <> :excluir)")
    long sumarMinutosDelDia(@Param("fecha") LocalDate fecha, @Param("excluir") Long excluir);
}
