package com.sena.getback.model;

import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "estados")
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre_estado", nullable = false, length = 50)
    private String nombreEstado;

    @Column(name = "tipo_estado", nullable = false, length = 20)
    private String tipoEstado;

    @OneToMany(mappedBy = "estado", fetch = FetchType.LAZY)
    private List<Usuario> usuarios;

    @OneToMany(mappedBy = "estado", fetch = FetchType.LAZY)
    private List<Cocina> cocinas;

    @OneToMany(mappedBy = "estado", fetch = FetchType.LAZY)
    private List<Bar> bares;

    @OneToMany(mappedBy = "estado", fetch = FetchType.LAZY)
    private List<Pedido> pedidos;

    @OneToMany(mappedBy = "estado", fetch = FetchType.LAZY)
    private List<Mesa> mesas;

    @OneToMany(mappedBy = "estado", fetch = FetchType.LAZY)
    private List<Factura> facturas;

    public Estado() {}

    public Estado(String nombreEstado, String tipoEstado) {
        this.nombreEstado = nombreEstado;
        this.tipoEstado = tipoEstado;
    }

    public Estado(Integer id, String nombreEstado, String tipoEstado) {
        this.id = id;
        this.nombreEstado = nombreEstado;
        this.tipoEstado = tipoEstado;
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombreEstado() { return nombreEstado; }
    public void setNombreEstado(String nombreEstado) { this.nombreEstado = nombreEstado; }

    public String getTipoEstado() { return tipoEstado; }
    public void setTipoEstado(String tipoEstado) { this.tipoEstado = tipoEstado; }

    public List<Usuario> getUsuarios() { return usuarios; }
    public void setUsuarios(List<Usuario> usuarios) { this.usuarios = usuarios; }

    public List<Cocina> getCocinas() { return cocinas; }
    public void setCocinas(List<Cocina> cocinas) { this.cocinas = cocinas; }

    public List<Bar> getBares() { return bares; }
    public void setBares(List<Bar> bares) { this.bares = bares; }

    public List<Pedido> getPedidos() { return pedidos; }
    public void setPedidos(List<Pedido> pedidos) { this.pedidos = pedidos; }

    public List<Mesa> getMesas() { return mesas; }
    public void setMesas(List<Mesa> mesas) { this.mesas = mesas; }

    public List<Factura> getFacturas() { return facturas; } 
    public void setFacturas(List<Factura> facturas) { this.facturas = facturas; } 

    @Override
    public String toString() {
        return "Estado [id=" + id + ", nombreEstado=" + nombreEstado + ", tipoEstado=" + tipoEstado + "]";
    }
}