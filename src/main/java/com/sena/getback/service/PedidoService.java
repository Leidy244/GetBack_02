package com.sena.getback.service;

import com.sena.getback.model.Pedido;
import com.sena.getback.model.Usuario;
import com.sena.getback.model.Menu;
import com.sena.getback.model.Estado;
import com.sena.getback.repository.MesaRepository;
import com.sena.getback.repository.PedidoRepository;
import com.sena.getback.repository.UsuarioRepository;
import com.sena.getback.repository.MenuRepository;
import com.sena.getback.repository.EstadoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MenuRepository menuRepository;
    private final EstadoRepository estadoRepository;

    public PedidoService(PedidoRepository pedidoRepository, MesaRepository mesaRepository, 
                        UsuarioRepository usuarioRepository, MenuRepository menuRepository, 
                        EstadoRepository estadoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.mesaRepository = mesaRepository;
        this.usuarioRepository = usuarioRepository;
        this.menuRepository = menuRepository;
        this.estadoRepository = estadoRepository;
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
        
        // Guardamos el JSON de items en la columna comentario para que la vista pueda parsearlo
        // Si hay comentarios generales, los combinamos con el JSON
        if (comentarios != null && !comentarios.trim().isEmpty()) {
            // Crear un objeto que contenga tanto los items como los comentarios
            String combinedData = "{\"items\":" + itemsJson + ",\"comentarios\":\"" + comentarios.replace("\"", "\\\"") + "\"}";
            pedido.setComentario(combinedData);
        } else {
            pedido.setComentario(itemsJson);
        }
        // Estado de pago inicial
        pedido.setEstadoPago("PENDIENTE");
        // Total del pedido
        pedido.setTotal(total);
        
        // Asociar la mesa (obligatorio)
        mesaRepository.findById(mesaId).ifPresent(pedido::setMesa);
        
        // Configurar usuario por defecto (primer usuario disponible o crear uno genérico)
        usuarioRepository.findAll().stream().findFirst().ifPresent(pedido::setUsuario);
        
        // Configurar menú por defecto (primer menú disponible)
        menuRepository.findAll().stream().findFirst().ifPresent(pedido::setMenu);
        
        // Configurar estado por defecto (buscar estado "PENDIENTE" o el primero disponible)
        estadoRepository.findAll().stream()
            .filter(estado -> "PENDIENTE".equalsIgnoreCase(estado.getNombreEstado()))
            .findFirst()
            .or(() -> estadoRepository.findAll().stream().findFirst())
            .ifPresent(pedido::setEstado);
        
        return pedidoRepository.save(pedido);
    }

    // Obtener historial por mesa
    public List<Pedido> obtenerHistorialPorMesa(Integer mesaId) {
        return pedidoRepository.findByMesaId(mesaId);
    }

    public long contarPedidosPendientes() {
        return pedidoRepository.findAll().stream()
                .filter(p -> p.getEstadoPago() != null && p.getEstadoPago().equalsIgnoreCase("PENDIENTE"))
                .count();
    }
}