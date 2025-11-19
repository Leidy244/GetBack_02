package com.sena.getback.model;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Lob
    @Column(name = "orden", columnDefinition = "LONGTEXT")
    private String orden;

    @Column(name = "total")
    private Double total;

    @Column(name = "monto_recibido")
    private Double montoRecibido;

    @Column(name = "cambio")
    private Double cambio;

    @Column(name = "comentarios_generales")
    private String comentariosGenerales;

    @Column(name = "fechaCreacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id", nullable = false)
    private Mesa mesa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @OneToMany(mappedBy = "pedido", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    private List<Bar> bares;

    @OneToMany(mappedBy = "pedido", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    private List<Cocina> cocinas;

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Factura factura;


    public Pedido() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public Pedido(String orden, Usuario usuario, Menu menu, Mesa mesa, Estado estado) {
        this.orden = orden;
        this.usuario = usuario;
        this.menu = menu;
        this.mesa = mesa;
        this.estado = estado;
        this.fechaCreacion = LocalDateTime.now();
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOrden() {
        return orden;
    }

    public void setOrden(String orden) {
        this.orden = orden;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getMontoRecibido() {
        return montoRecibido;
    }

    public void setMontoRecibido(Double montoRecibido) {
        this.montoRecibido = montoRecibido;
    }

    public Double getCambio() {
        return cambio;
    }

    public void setCambio(Double cambio) {
        this.cambio = cambio;
    }

    public String getComentariosGenerales() {
        return comentariosGenerales;
    }

    public void setComentariosGenerales(String comentariosGenerales) {
        this.comentariosGenerales = comentariosGenerales;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Menu getMenu() {
        return menu;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    public Mesa getMesa() {
        return mesa;
    }

    public void setMesa(Mesa mesa) {
        this.mesa = mesa;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public List<Bar> getBares() {
        return bares;
    }

    public void setBares(List<Bar> bares) {
        this.bares = bares;
    }

    public List<Cocina> getCocinas() {
        return cocinas;
    }

    public void setCocinas(List<Cocina> cocinas) {
        this.cocinas = cocinas;
    }

    public Factura getFactura() {
        return factura;
    }

    public void setFactura(Factura factura) {
        this.factura = factura;
    }
}
