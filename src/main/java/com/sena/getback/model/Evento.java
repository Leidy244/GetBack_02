package com.sena.getback.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "eventos")
public class Evento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String titulo;  // ✅ Asegurar que esta propiedad existe
    private LocalDate fecha;
    
    // Constructores
    public Evento() {}
    
    public Evento(String titulo, LocalDate fecha) {
        this.titulo = titulo;
        this.fecha = fecha;
    }
    
    // Getters y Setters (IMPORTANTE: deben coincidir exactamente)
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getTitulo() {  // ✅ Debe existir getTitulo()
        return titulo;
    }
    
    public void setTitulo(String titulo) {  // ✅ Debe existir setTitulo()
        this.titulo = titulo;
    }
    
    public LocalDate getFecha() {
        return fecha;
    }
    
    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
}