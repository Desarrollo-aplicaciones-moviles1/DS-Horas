package com.deepsource.horas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "registros")
public class Registro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @Column(nullable = false, length = 60)
    private String tarea;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private int minutos;

    // Ruta de la foto de evidencia en el teléfono (opcional)
    @Column(length = 300)
    private String foto;

    @Column(name = "creado_en", nullable = false)
    private long creadoEn;

    // Quién hizo el registro (id y nombre del usuario en ms-usuarios).
    // Se guarda el nombre para no tener que consultar el otro microservicio al mostrar los aportes
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nombre", length = 60)
    private String usuarioNombre;

    public Registro() {
    }

    public Registro(Proyecto proyecto, String tarea, LocalDate fecha, int minutos, String foto, long creadoEn) {
        this.proyecto = proyecto;
        this.tarea = tarea;
        this.fecha = fecha;
        this.minutos = minutos;
        this.foto = foto;
        this.creadoEn = creadoEn;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    public String getTarea() { return tarea; }
    public void setTarea(String tarea) { this.tarea = tarea; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public int getMinutos() { return minutos; }
    public void setMinutos(int minutos) { this.minutos = minutos; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    public long getCreadoEn() { return creadoEn; }
    public void setCreadoEn(long creadoEn) { this.creadoEn = creadoEn; }
}
