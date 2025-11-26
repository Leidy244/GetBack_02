package com.sena.getback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sena.getback.model.Pedido;
import com.sena.getback.model.Usuario;
import com.sena.getback.model.Menu;
import com.sena.getback.model.Estado;
import com.sena.getback.model.Factura;
import com.sena.getback.repository.MesaRepository;
import com.sena.getback.repository.PedidoRepository;
import com.sena.getback.repository.UsuarioRepository;
import com.sena.getback.repository.MenuRepository;
import com.sena.getback.repository.EstadoRepository;
import com.sena.getback.repository.FacturaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MenuRepository menuRepository;
    private final EstadoRepository estadoRepository;
    private final FacturaRepository facturaRepository;
    private final InventarioService inventarioService;

    public PedidoService(PedidoRepository pedidoRepository, MesaRepository mesaRepository,
                         UsuarioRepository usuarioRepository, MenuRepository menuRepository,
                         EstadoRepository estadoRepository, FacturaRepository facturaRepository,
                         InventarioService inventarioService) {
        this.pedidoRepository = pedidoRepository;
        this.mesaRepository = mesaRepository;
        this.usuarioRepository = usuarioRepository;
        this.menuRepository = menuRepository;
        this.estadoRepository = estadoRepository;
        this.facturaRepository = facturaRepository;
        this.inventarioService = inventarioService;
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
        // Buscar el primer pedido cuyo estado (tabla estados) sea PENDIENTE
        List<Pedido> pedidos = pedidoRepository.findByMesaId(mesaId);
        return pedidos.stream()
                .filter(p -> p.getEstado() != null
                        && "PENDIENTE".equalsIgnoreCase(p.getEstado().getNombreEstado()))
                .findFirst()
                .orElse(null);
    }

    // Crear nuevo pedido
    public Pedido crearPedido(Integer mesaId, String itemsJson, String comentarios, Double total) {
        // 1) Validar stock a partir de itemsJson antes de crear el pedido
        validarStockParaItems(itemsJson);

        Pedido pedido = new Pedido();

        // Guardamos el JSON de items en la columna orden para que la vista pueda parsearlo
        // Si hay comentarios generales, los combinamos con el JSON
        if (comentarios != null && !comentarios.trim().isEmpty()) {
            // Crear un objeto que contenga tanto los items como los comentarios
            String combinedData = "{\"items\":" + itemsJson + ",\"comentarios\":\"" + comentarios.replace("\"", "\\\"") + "\"}";
            pedido.setOrden(combinedData);
            pedido.setComentariosGenerales(comentarios.trim());
        } else {
            pedido.setOrden(itemsJson);
        }
        // Total del pedido
        pedido.setTotal(total);

        // Asociar la mesa (obligatorio) y marcarla como OCUPADA mientras haya pedido pendiente
        mesaRepository.findById(mesaId).ifPresent(mesa -> {
            mesa.setEstado("OCUPADA");
            mesaRepository.save(mesa);
            pedido.setMesa(mesa);
        });

        // Configurar usuario por defecto (primer usuario disponible o crear uno genérico)
        usuarioRepository.findAll().stream().findFirst().ifPresent(pedido::setUsuario);

        // Configurar menú por defecto (primer menú disponible)
        menuRepository.findAll().stream().findFirst().ifPresent(pedido::setMenu);

        // Configurar estado por defecto usando la tabla estados: ID 1 = PENDIENTE
        estadoRepository.findById(1)
                .ifPresent(pedido::setEstado);

        // 2) Registrar consumo real de inventario por los productos del pedido
        try {
            Map<String, Integer> requeridos = extraerCantidadesPorProducto(itemsJson);
            for (Map.Entry<String, Integer> entry : requeridos.entrySet()) {
                String nombre = entry.getKey();
                int cantidad = entry.getValue();
                if (cantidad > 0) {
                    inventarioService.registrarConsumo(nombre, cantidad);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al registrar consumo de inventario para el pedido: " + e.getMessage());
        }

        return pedidoRepository.save(pedido);
    }

    /**
     * Valida el stock disponible en Inventario para los productos incluidos en itemsJson.
     * itemsJson tiene la forma: { items: [{ productoNombre, cantidad, ... }], total: ... }
     */
    private void validarStockParaItems(String itemsJson) {
        if (itemsJson == null || itemsJson.isBlank()) {
            return;
        }
        try {
            Map<String, Integer> requeridos = extraerCantidadesPorProducto(itemsJson);

            // Validar contra inventario
            for (Map.Entry<String, Integer> entry : requeridos.entrySet()) {
                String nombre = entry.getKey();
                int solicitado = entry.getValue();

                int disponible = inventarioService.obtenerStockDisponible(nombre);
                if (solicitado > disponible) {
                    throw new IllegalStateException(
                            "No hay stock suficiente de '" + nombre + "' (disponible: " + disponible
                                    + ", solicitado: " + solicitado + ")");
                }
            }
        } catch (IllegalStateException e) {
            // Propagar tal cual para que llegue al controlador y a la UI
            throw e;
        } catch (Exception e) {
            // Si hay un error al parsear, no bloqueamos el pedido pero registramos en logs
            System.err.println("Error al validar stock para itemsJson: " + e.getMessage());
        }
    }

    /**
     * Parsea itemsJson y devuelve un mapa productoNombre -> cantidad total solicitada.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Integer> extraerCantidadesPorProducto(String itemsJson) throws Exception {
        Map<String, Integer> requeridos = new HashMap<>();
        if (itemsJson == null || itemsJson.isBlank()) {
            return requeridos;
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> root = mapper.readValue(itemsJson, Map.class);
        Object itemsObj = root.get("items");
        if (!(itemsObj instanceof List<?> itemsList)) {
            return requeridos;
        }

        for (Object o : itemsList) {
            if (!(o instanceof Map<?, ?> itemMap)) continue;
            Object nombreObj = itemMap.get("productoNombre");
            Object cantidadObj = itemMap.get("cantidad");
            if (nombreObj == null || cantidadObj == null) continue;
            String nombre = String.valueOf(nombreObj);
            int cant = (cantidadObj instanceof Number)
                    ? ((Number) cantidadObj).intValue()
                    : Integer.parseInt(String.valueOf(cantidadObj));
            if (cant <= 0) continue;
            requeridos.merge(nombre, cant, Integer::sum);
        }

        return requeridos;
    }

    // Obtener historial por mesa
    public List<Pedido> obtenerHistorialPorMesa(Integer mesaId) {
        return pedidoRepository.findByMesaId(mesaId);
    }

    // Wrapper usado por controladores para mantener compatibilidad de nombre
    public List<Pedido> obtenerHistorialPedidosPorMesa(Integer mesaId) {
        return obtenerHistorialPorMesa(mesaId);
    }

    public long contarPedidosPendientes() {
        return pedidoRepository.findAll().stream()
                .filter(p -> p.getEstado() != null
                        && p.getEstado().getId() != null
                        && p.getEstado().getId() == 1) // 1 = PENDIENTE
                .count();
    }

    // Sincronizar estado de las mesas según si tienen pedidos pendientes
    public void sincronizarEstadoMesas() {
        mesaRepository.findAll().forEach(mesa -> {
            boolean hayPendientes = pedidoRepository.findByMesaId(mesa.getId()).stream()
                    .anyMatch(p -> p.getEstado() != null
                            && p.getEstado().getId() != null
                            && p.getEstado().getId() == 1); // 1 = PENDIENTE

            if (hayPendientes && !"OCUPADA".equalsIgnoreCase(mesa.getEstado())) {
                mesa.setEstado("OCUPADA");
                mesaRepository.save(mesa);
            } else if (!hayPendientes && !"DISPONIBLE".equalsIgnoreCase(mesa.getEstado())) {
                mesa.setEstado("DISPONIBLE");
                mesaRepository.save(mesa);
            }
        });
    }

    // Obtener todos los pedidos con estado de pago PENDIENTE (ID 1)
    public List<Pedido> obtenerPedidosPendientes() {
        return pedidoRepository.findAll().stream()
                .filter(p -> p.getEstado() != null
                        && p.getEstado().getId() != null
                        && p.getEstado().getId() == 1)
                .toList();
    }

    // Obtener todos los pedidos con estado de pago PAGADO (historial, ID 2)
    public List<Pedido> obtenerPedidosPagados() {
        return pedidoRepository.findAll().stream()
                .filter(p -> p.getEstado() != null
                        && p.getEstado().getId() != null
                        && p.getEstado().getId() == 2)
                .toList();
    }

    // Obtener pedidos PENDIENTES para el panel de inicio de caja
    // (por ahora no filtramos por área para evitar dejar la vista vacía
    // si las categorías no tienen configurado correctamente el campo "area")
    public List<Pedido> obtenerPedidosPendientesBar() {
        return obtenerPedidosPendientes();
    }

    // Obtener pedidos PAGADOS para el panel de inicio de caja
    public List<Pedido> obtenerPedidosPagadosBar() {
        return obtenerPedidosPagados();
    }

    // Obtener estadísticas de pedidos por mesero
    public Map<String, Long> obtenerEstadisticasPorMesero(Integer usuarioId) {
        List<Pedido> todosLosPedidos = pedidoRepository.findAll();

        return todosLosPedidos.stream()
                .filter(p -> p.getUsuario() != null && p.getUsuario().getNombre() != null)
                .filter(p -> usuarioId == null || p.getUsuario().getId().equals(usuarioId.longValue()))
                .collect(Collectors.groupingBy(
                        p -> p.getUsuario().getNombre() + " " + p.getUsuario().getApellido(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    // Historial paginado y filtrado para la vista de mesero / admin
    public Page<Pedido> obtenerHistorialPaginado(String mesa,
                                                 String estado,
                                                 LocalDate fechaDesde,
                                                 LocalDate fechaHasta,
                                                 Long usuarioId,
                                                 int page,
                                                 int size) {

        int pageNumber = Math.max(page, 0);
        int pageSize = size > 0 ? size : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Direction.DESC, "fechaCreacion"));

        LocalDateTime desdeDateTime = null;
        LocalDateTime hastaDateTime = null;

        if (fechaDesde != null) {
            desdeDateTime = fechaDesde.atStartOfDay();
        }
        if (fechaHasta != null) {
            // incluir todo el día
            hastaDateTime = fechaHasta.plusDays(1).atStartOfDay();
        }

        return pedidoRepository.buscarHistorial(mesa, estado, desdeDateTime, hastaDateTime, usuarioId, pageable);
    }

    // Marcar pedido como pagado desde caja y liberar la mesa
    public void marcarPedidoComoPagado(Integer pedidoId, Double montoRecibido) {
        if (pedidoId == null) {
            return;
        }

        Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido == null) {
            return;
        }

        // Cambiar estado del pedido a PAGADO (ID 2)
        estadoRepository.findById(2).ifPresent(pedido::setEstado);

        if (montoRecibido != null && pedido.getTotal() != null) {
            pedido.setMontoRecibido(montoRecibido);
            pedido.setCambio(montoRecibido - pedido.getTotal());
        }

        pedidoRepository.save(pedido);

        // Liberar mesa asociada
        if (pedido.getMesa() != null && pedido.getMesa().getId() != null) {
            mesaRepository.findById(pedido.getMesa().getId()).ifPresent(mesa -> {
                mesa.setEstado("DISPONIBLE");
                mesaRepository.save(mesa);
            });
        }
    }
}