package com.sena.getback.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "facturas")
public class Factura {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "numero_factura", nullable = false, unique = true, length = 50)
    private String numeroFactura;
    
    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;
    
    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;
    
    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;
    
    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodoPago;
    
    @Column(name = "referencia_pago", length = 100)
    private String referenciaPago;
    
    @Column(name = "estado_pago", nullable = false, length = 50) // ← Longitud 50 (consistente)
    private String estadoPago;
    
    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;
    
    @Column(name = "valor_descuento", precision = 10, scale = 2)
    private BigDecimal valorDescuento;
    
    @Column(name = "total_pagar", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPagar;
    
    @Column(name = "cliente_nombre", length = 100)
    private String clienteNombre;
    
    @Column(name = "numero_mesa")
    private Integer numeroMesa;
    
    @Column(name = "estado_factura", nullable = false, length = 20)
    private String estadoFactura;
    
    @OneToOne // ← CORREGIDO: OneToOne en lugar de ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false, unique = true) // ← agregado unique
    private Pedido pedido;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY) // ← NUEVA RELACIÓN CON ESTADO
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;
    
    // Constructores
    public Factura() {
        this.fechaEmision = LocalDateTime.now();
        this.fechaPago = LocalDateTime.now();
        this.estadoFactura = "GENERADA";
        this.estadoPago = "PENDIENTE";
    }
    
    public Factura(String numeroFactura, BigDecimal monto, String metodoPago, 
                  Pedido pedido, Usuario usuario, Estado estado) {
        this();
        this.numeroFactura = numeroFactura;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.pedido = pedido;
        this.usuario = usuario;
        this.estado = estado;
        this.totalPagar = monto;
        this.subtotal = monto;
        this.valorDescuento = BigDecimal.ZERO;
    }
    
    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }
    
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }
    
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    
    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
    
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    
    public String getReferenciaPago() { return referenciaPago; }
    public void setReferenciaPago(String referenciaPago) { this.referenciaPago = referenciaPago; }
    
    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getValorDescuento() { return valorDescuento; }
    public void setValorDescuento(BigDecimal valorDescuento) { this.valorDescuento = valorDescuento; }
    
    public BigDecimal getTotalPagar() { return totalPagar; }
    public void setTotalPagar(BigDecimal totalPagar) { this.totalPagar = totalPagar; }
    
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    
    public Integer getNumeroMesa() { return numeroMesa; }
    public void setNumeroMesa(Integer numeroMesa) { this.numeroMesa = numeroMesa; }
    
    public String getEstadoFactura() { return estadoFactura; }
    public void setEstadoFactura(String estadoFactura) { this.estadoFactura = estadoFactura; }
    
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Estado getEstado() { return estado; } // ← NUEVO GETTER
    public void setEstado(Estado estado) { this.estado = estado; } // ← NUEVO SETTER
    

    public boolean tieneDescuento() {
        return valorDescuento != null && valorDescuento.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public void aplicarDescuento(BigDecimal porcentaje) {
        if (porcentaje != null && porcentaje.compareTo(BigDecimal.ZERO) > 0) {
            this.valorDescuento = this.subtotal.multiply(porcentaje).divide(BigDecimal.valueOf(100));
            this.totalPagar = this.subtotal.subtract(this.valorDescuento);
        }
    }
    
    @Override
    public String toString() {
        return "Factura [id=" + id + ", numeroFactura=" + numeroFactura + ", totalPagar=" + totalPagar + "]";
    }
}