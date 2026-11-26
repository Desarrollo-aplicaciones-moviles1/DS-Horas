package com.deepsource.horas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "proyectos")
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String cliente;

    // Avance de 0 a 1
    @Column(nullable = false)
    private double progreso;

    @Column(nullable = false)
    private boolean fijado;

    // null = proyecto en curso
    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    public Proyecto() {
    }

    public Proyecto(String nombre, String cliente, double progreso, boolean fijado, LocalDate fechaCierre) {
        this.nombre = nombre;
        this.cliente = cliente;
        this.progreso = progreso;
        this.fijado = fijado;
        this.fechaCierre = fechaCierre;
    }

    public boolean isEnCurso() {
        return fechaCierre == null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public double getProgreso() { return progreso; }
    public void setProgreso(double progreso) { this.progreso = progreso; }
    public boolean isFijado() { return fijado; }
    public void setFijado(boolean fijado) { this.fijado = fijado; }
    public LocalDate getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDate fechaCierre) { this.fechaCierre = fechaCierre; }
}
