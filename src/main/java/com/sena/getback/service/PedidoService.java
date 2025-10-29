package com.sena.getback.service;


import com.sena.getback.model.Pedido;
import com.sena.getback.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PedidoService {

	private final PedidoRepository pedidoRepository;

	public PedidoService(PedidoRepository pedidoRepository) {
		this.pedidoRepository = pedidoRepository;
	}

	// Listar todos los pedidos
	public List<Pedido> findAll() {
		return pedidoRepository.findAll();
	}

	// Buscar pedido por id
	public Pedido findById(Integer id) {
		return pedidoRepository.findById(id).orElse(null);
	}

	// Guardar o editar pedido
	public Pedido save(Pedido pedido) {
		return pedidoRepository.save(pedido);
	}

	// Eliminar pedido por id
	public void delete(Integer id) {
		pedidoRepository.deleteById(id);
	}

	// Obtener pedido activo por mesa
	public Pedido obtenerPedidoActivoPorMesa(Integer mesaId) {
		// Implementación básica - buscar el primer pedido pendiente de la mesa
		List<Pedido> pedidos = pedidoRepository.findByMesaId(mesaId);
		return pedidos.stream().filter(p -> "PENDIENTE".equals(p.getEstadoPago())).findFirst().orElse(null);
	}

	// Crear nuevo pedido
	public Pedido crearPedido(Integer mesaId, String itemsJson, String comentarios, Double total) {
		Pedido pedido = new Pedido();
		pedido.setComentario(comentarios);
		pedido.setEstadoPago("PENDIENTE");
		// Aquí se deberían setear los demás campos necesarios
		return pedidoRepository.save(pedido);
	}

	// Obtener historial por mesa
	public List<Pedido> obtenerHistorialPorMesa(Integer mesaId) {
		return pedidoRepository.findByMesaId(mesaId);
	}
}