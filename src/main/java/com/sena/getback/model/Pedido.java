package com.sena.getback.model;

import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "pedidos")
public class Pedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Lob
	private String comentario;

	@Column(name = "estado_pago", length = 50)
	private String estadoPago;

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

	@OneToMany(mappedBy = "pedido", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY) // ←
																													// Cascade
																													// ajustado
	private List<Bar> bares;

	@OneToMany(mappedBy = "pedido", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY) // ←
																													// Cascade
																													// ajustado
	private List<Cocina> cocinas;

	@OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Factura factura;

	// Constructores
	public Pedido() {
	}

	public Pedido(String comentario, Usuario usuario, Menu menu, Mesa mesa, Estado estado) {
		this.comentario = comentario;
		this.usuario = usuario;
		this.menu = menu;
		this.mesa = mesa;
		this.estado = estado;
	}

	public Pedido(Integer id, String comentario, Usuario usuario, Menu menu, Mesa mesa, Estado estado) {
		this.id = id;
		this.comentario = comentario;
		this.usuario = usuario;
		this.menu = menu;
		this.mesa = mesa;
		this.estado = estado;
	}

	// Getters y Setters
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getComentario() {
		return comentario;
	}

	public void setComentario(String comentario) {
		this.comentario = comentario;
	}

	public String getEstadoPago() {
		return estadoPago;
	}

	public void setEstadoPago(String estadoPago) {
		this.estadoPago = estadoPago;
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

	@Override
	public String toString() {
		return "Pedido [id=" + id + ", comentario=" + comentario + ", mesa=" + (mesa != null ? mesa.getId() : "null")
				+ "]";
	}
}