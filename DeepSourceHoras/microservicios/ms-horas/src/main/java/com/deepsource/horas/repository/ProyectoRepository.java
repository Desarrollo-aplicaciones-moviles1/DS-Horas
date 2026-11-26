package com.deepsource.horas.repository;

import com.deepsource.horas.model.Proyecto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    // Primero los fijados
    List<Proyecto> findAllByOrderByFijadoDescIdAsc();
}
