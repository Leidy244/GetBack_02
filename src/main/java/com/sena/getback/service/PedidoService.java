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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> root = mapper.readValue(itemsJson, Map.class);
            Object itemsObj = root.get("items");
            if (!(itemsObj instanceof List<?> itemsList)) {
                return;
            }

            // Acumular cantidad requerida por productoNombre
            Map<String, Integer> requeridos = new HashMap<>();
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

    // Marcar un pedido como PAGADO y registrar datos de pago (monto recibido y cambio),
    // además de liberar la mesa si ya no quedan pedidos pendientes en esa mesa
    // y generar una Factura asociada para el historial de ventas del admin
    public void marcarPedidoComoPagado(Integer pedidoId, Double montoRecibido) {
        Pedido pedido = findById(pedidoId);
        if (pedido == null) {
            return;
        }

        // Cambiar el estado usando la tabla estados: ID 2 = PAGADO
        estadoRepository.findById(2)
                .ifPresent(pedido::setEstado);

        // Guardar monto recibido y calcular cambio si es posible
        if (montoRecibido != null) {
            pedido.setMontoRecibido(montoRecibido);
            if (pedido.getTotal() != null) {
                double cambio = montoRecibido - pedido.getTotal();
                pedido.setCambio(cambio);
            }
        }

        // Persistir cambios del pedido primero
        pedidoRepository.save(pedido);

        // Crear factura solo si aún no existe una asociada
        if (pedido.getFactura() == null && pedido.getTotal() != null) {
            Factura factura = new Factura();

            // Número de factura simple basado en timestamp (puedes mejorar esta lógica luego)
            factura.setNumeroFactura("FAC-" + System.currentTimeMillis());
            factura.setFechaEmision(LocalDateTime.now());
            factura.setFechaPago(LocalDateTime.now());

            BigDecimal total = BigDecimal.valueOf(pedido.getTotal());
            factura.setMonto(total);
            factura.setSubtotal(total);
            factura.setValorDescuento(BigDecimal.ZERO);
            factura.setTotalPagar(total);

            factura.setMetodoPago("EFECTIVO");
            factura.setEstadoPago("PAGADO");
            factura.setEstadoFactura("GENERADA");

            if (pedido.getMesa() != null && pedido.getMesa().getNumero() != null) {
                try {
                    factura.setNumeroMesa(Integer.valueOf(pedido.getMesa().getNumero()));
                } catch (NumberFormatException e) {
                    // Si el número de mesa no es numérico, lo dejamos nulo
                }
            }

            // Datos de relación
            factura.setPedido(pedido);
            if (pedido.getUsuario() != null) {
                factura.setUsuario(pedido.getUsuario());
            } else {
                // Usuario por defecto: primero de la lista si existe
                usuarioRepository.findAll().stream().findFirst().ifPresent(factura::setUsuario);
            }

            // Usar mismo estado que el pedido (PAGADO)
            if (pedido.getEstado() != null) {
                factura.setEstado(pedido.getEstado());
            } else {
                estadoRepository.findById(2).ifPresent(factura::setEstado);
            }

            Factura guardada = facturaRepository.save(factura);
            pedido.setFactura(guardada);
            pedidoRepository.save(pedido);
        }

        if (pedido.getMesa() != null && pedido.getMesa().getId() != null) {
            Integer mesaId = pedido.getMesa().getId();

            boolean hayPendientes = pedidoRepository.findByMesaId(mesaId).stream()
                    .anyMatch(p -> p.getEstado() != null
                            && "PENDIENTE".equalsIgnoreCase(p.getEstado().getNombreEstado()));

            if (!hayPendientes) {
                mesaRepository.findById(mesaId).ifPresent(mesa -> {
                    mesa.setEstado("DISPONIBLE");
                    mesaRepository.save(mesa);
                });
            }
        }
    }
}