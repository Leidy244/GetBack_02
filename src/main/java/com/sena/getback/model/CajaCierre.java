package com.sena.getback.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "caja_cierres")
public class CajaCierre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    private Double ingresosEfectivo;
    private Double gastosEfectivo;
    private Double baseApertura;
    private Double baseSiguiente;
    private Double retiro;
    private Long ventasDia;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Double getIngresosEfectivo() { return ingresosEfectivo; }
    public void setIngresosEfectivo(Double ingresosEfectivo) { this.ingresosEfectivo = ingresosEfectivo; }
    public Double getGastosEfectivo() { return gastosEfectivo; }
    public void setGastosEfectivo(Double gastosEfectivo) { this.gastosEfectivo = gastosEfectivo; }
    public Double getBaseApertura() { return baseApertura; }
    public void setBaseApertura(Double baseApertura) { this.baseApertura = baseApertura; }
    public Double getBaseSiguiente() { return baseSiguiente; }
    public void setBaseSiguiente(Double baseSiguiente) { this.baseSiguiente = baseSiguiente; }
    public Double getRetiro() { return retiro; }
    public void setRetiro(Double retiro) { this.retiro = retiro; }
    public Long getVentasDia() { return ventasDia; }
    public void setVentasDia(Long ventasDia) { this.ventasDia = ventasDia; }
}
