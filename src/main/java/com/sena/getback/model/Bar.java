package com.sena.getback.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bar") 
public class Bar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;
    
    // Constructores
    public Bar() {}

    public Bar(Pedido pedido, Usuario usuario, Estado estado) {
        this.pedido = pedido;
        this.usuario = usuario;
        this.estado = estado;
    }

    public Bar(Integer id, Pedido pedido, Usuario usuario, Estado estado) {
        this.id = id;
        this.pedido = pedido;
        this.usuario = usuario;
        this.estado = estado;
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    @Override
    public String toString() {
        return "Bar [id=" + id + ", pedido=" + (pedido != null ? pedido.getId() : "null") + 
               ", usuario=" + (usuario != null ? usuario.getNombre() : "null") + "]";
    }
}