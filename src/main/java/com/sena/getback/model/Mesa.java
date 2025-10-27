package com.sena.getback.model;

import jakarta.persistence.*;

@Entity
@Table(name = "mesas")
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ❌ Quitar unique = true para permitir duplicados por ubicación
    @Column(name = "numero", nullable = false, length = 50)
    private String numero;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "DISPONIBLE"; // DISPONIBLE u OCUPADA

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ubicacion_id")
    private Location ubicacion;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    // ───────────────────────────────
    // Constructores
    // ───────────────────────────────
    public Mesa() {}

    public Mesa(Integer id, String numero, Integer capacidad, String estado, Location ubicacion, String descripcion) {
        this.id = id;
        this.numero = numero;
        this.capacidad = capacidad;
        this.estado = estado;
        this.ubicacion = ubicacion;
        this.descripcion = descripcion;
    }

    // ───────────────────────────────
    // Métodos de negocio
    // ───────────────────────────────
    public boolean estaDisponible() {
        return "DISPONIBLE".equalsIgnoreCase(this.estado);
    }

    public boolean estaOcupada() {
        return "OCUPADA".equalsIgnoreCase(this.estado);
    }

    public void ocupar() {
        this.estado = "OCUPADA";
    }

    public void liberar() {
        this.estado = "DISPONIBLE";
    }

    // ───────────────────────────────
    // Getters & Setters
    // ───────────────────────────────
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Location getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(Location ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getUbicacionNombre() {
        return ubicacion != null ? ubicacion.getNombre() : "Sin ubicación";
    }

    @Override
    public String toString() {
        return "Mesa{" +
                "id=" + id +
                ", numero='" + numero + '\'' +
                ", capacidad=" + capacidad +
                ", estado='" + estado + '\'' +
                ", ubicacion=" + (ubicacion != null ? ubicacion.getNombre() : "null") +
                ", descripcion='" + descripcion + '\'' +
                '}';
    }
}
