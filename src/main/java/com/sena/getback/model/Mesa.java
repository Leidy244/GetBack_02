package com.sena.getback.model;

import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "mesas")
public class Mesa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false)
	private Integer capacidad;

	@Column(length = 100)
	private String ubicacion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "estado_id", nullable = false)
	private Estado estado;

	@OneToMany(mappedBy = "mesa", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Pedido> pedidos;

	// Constructores
	public Mesa() {
	}

	public Mesa(Integer id, Integer capacidad, String ubicacion, Estado estado) {
		this.id = id;
		this.capacidad = capacidad;
		this.ubicacion = ubicacion;
		this.estado = estado;
	}

	// Getters y Setters
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getCapacidad() {
		return capacidad;
	}

	public void setCapacidad(Integer capacidad) {
		this.capacidad = capacidad;
	}

	public String getUbicacion() {
		return ubicacion;
	}

	public void setUbicacion(String ubicacion) {
		this.ubicacion = ubicacion;
	}

	public Estado getEstado() {
		return estado;
	}

	public void setEstado(Estado estado) {
		this.estado = estado;
	}

	public List<Pedido> getPedidos() {
		return pedidos;
	}

	public void setPedidos(List<Pedido> pedidos) {
		this.pedidos = pedidos;
	}

	@Override
	public String toString() {
		return "Mesa [id=" + id + ", capacidad=" + capacidad + "]";
	}
}