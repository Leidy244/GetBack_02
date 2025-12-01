package com.sena.getback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sena.getback.model.Pedido;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MenuRepository menuRepository;
    private final EstadoRepository estadoRepository;
    private final FacturaRepository facturaRepository;
    private final ActivityLogService activityLogService;
    private final InventarioService inventarioService;
    private final com.sena.getback.repository.ClienteFrecuenteRepository clienteFrecuenteRepository;
    private final com.sena.getback.repository.MovimientoCreditoRepository movimientoCreditoRepository;

    public PedidoService(PedidoRepository pedidoRepository, MesaRepository mesaRepository,
            UsuarioRepository usuarioRepository, MenuRepository menuRepository,
            EstadoRepository estadoRepository, FacturaRepository facturaRepository,
            InventarioService inventarioService,
            ActivityLogService activityLogService,
            com.sena.getback.repository.ClienteFrecuenteRepository clienteFrecuenteRepository,
            com.sena.getback.repository.MovimientoCreditoRepository movimientoCreditoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.mesaRepository = mesaRepository;
        this.usuarioRepository = usuarioRepository;
        this.menuRepository = menuRepository;
        this.estadoRepository = estadoRepository;
        this.facturaRepository = facturaRepository;
        this.inventarioService = inventarioService;
        this.activityLogService = activityLogService;
        this.clienteFrecuenteRepository = clienteFrecuenteRepository;
        this.movimientoCreditoRepository = movimientoCreditoRepository;
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

    public Pedido crearPedido(Integer mesaId, String itemsJson, String comentarios, Double total) {
        return crearPedido(mesaId, itemsJson, comentarios, total, null);
    }

    public Pedido crearPedido(Integer mesaId, String itemsJson, String comentarios, Double total, com.sena.getback.model.Usuario usuarioOverride) {
        // 1) Validar stock a partir de itemsJson antes de crear el pedido
        validarStockParaItems(itemsJson);

        Pedido pedido = new Pedido();

        // Guardamos el JSON de items en la columna orden para que la vista pueda
        // parsearlo
        // Si hay comentarios generales, los combinamos con el JSON
        if (comentarios != null && !comentarios.trim().isEmpty()) {
            // Crear un objeto que contenga tanto los items como los comentarios
            String combinedData = "{\"items\":" + itemsJson + ",\"comentarios\":\"" + comentarios.replace("\"", "\\\"")
                    + "\"}";
            pedido.setOrden(combinedData);
            pedido.setComentariosGenerales(comentarios.trim());
        } else {
            pedido.setOrden(itemsJson);
        }
        // Total del pedido
        pedido.setTotal(total);

        // Asociar la mesa (obligatorio) y marcarla como OCUPADA mientras haya pedido
        // pendiente
        mesaRepository.findById(mesaId).ifPresent(mesa -> {
            mesa.setEstado("OCUPADA");
            mesaRepository.save(mesa);
            pedido.setMesa(mesa);
        });

        if (usuarioOverride != null) {
            pedido.setUsuario(usuarioOverride);
        } else {
            usuarioRepository.findAll().stream().findFirst().ifPresent(pedido::setUsuario);
        }

        // Configurar menú por defecto (primer menú disponible)
        menuRepository.findAll().stream().findFirst().ifPresent(pedido::setMenu);

        estadoRepository.findById(1).ifPresent(pedido::setEstado);

        // 2) Registrar consumo real de inventario por los productos del pedido
        try {
            Map<String, Integer> requeridos = extraerCantidadesPorProducto(itemsJson);
            for (Map.Entry<String, Integer> entry : requeridos.entrySet()) {
                String nombre = entry.getKey();
                int cantidad = entry.getValue();
                if (cantidad > 0) {
                    // Descuenta inventario; el stock del menú se sincroniza automáticamente desde InventarioService
                    inventarioService.registrarConsumo(nombre, cantidad);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al registrar consumo de inventario para el pedido: " + e.getMessage());
        }

        Pedido saved = pedidoRepository.save(pedido);
        try {
            String mesaNombre = (saved.getMesa() != null && saved.getMesa().getNumero() != null)
                    ? saved.getMesa().getNumero() : (saved.getMesa() != null ? ("#" + saved.getMesa().getId()) : "sin mesa");
            String msg = "Se creó el pedido \"" + saved.getId() + "\" para la mesa " + mesaNombre;
            activityLogService.log("ORDER", msg, currentUser(), null);
        } catch (Exception ignored) {}
        return saved;
    }

    /**
     * Valida el stock disponible en Inventario para los productos incluidos en
     * itemsJson.
     * itemsJson tiene la forma: { items: [{ productoNombre, cantidad, ... }],
     * total: ... }
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
    public List<Pedido> obtenerPedidosPendientes() {
        System.out.println("🔍 Buscando pedidos pendientes...");
        
        // Opción 1: Buscar por estado PENDIENTE
        List<Pedido> pedidos = pedidoRepository.findAll().stream()
                .filter(p -> {
                    String nombreEstado = (p.getEstado() != null && p.getEstado().getNombreEstado() != null)
                            ? p.getEstado().getNombreEstado().trim()
                            : null;
                    boolean esPendiente = nombreEstado != null &&
                            nombreEstado.equalsIgnoreCase("PENDIENTE");
                    if (esPendiente) {
                        System.out.println("✅ Pedido pendiente encontrado - ID: " + p.getId() +
                                ", Total: " + p.getTotal() +
                                ", Estado: " + nombreEstado);
                    }
                    return esPendiente;
                })
                .collect(Collectors.toList());
        
        System.out.println("📊 Total pedidos pendientes encontrados: " + pedidos.size());
        return pedidos;
    }
   

    // Obtener todos los pedidos con estado de pago PAGADO (historial, ID 2)
    public List<Pedido> obtenerPedidosPagados() {
        return pedidoRepository.findAll().stream()
                .filter(p -> p.getEstado() != null
                        && p.getEstado().getId() != null
                        && p.getEstado().getId() == 2)
                .toList();
    }

    // Estadísticas de pedidos por mesero (nombre completo -> cantidad de pedidos)
    public Map<String, Long> obtenerEstadisticasPorMesero(Integer usuarioId) {
        return pedidoRepository.findAll().stream()
                .filter(p -> p.getUsuario() != null)
                .filter(p -> usuarioId == null || (p.getUsuario().getId() != null
                        && p.getUsuario().getId().intValue() == usuarioId))
                .collect(Collectors.groupingBy(p -> {
                    String nombre = p.getUsuario().getNombre() != null ? p.getUsuario().getNombre() : "";
                    String apellido = p.getUsuario().getApellido() != null ? p.getUsuario().getApellido() : "";
                    String nombreCompleto = (nombre + " " + apellido).trim();
                    return nombreCompleto.isEmpty() ? ("Usuario " + p.getUsuario().getId()) : nombreCompleto;
                }, Collectors.counting()));
    }

    // Obtener pedidos PENDIENTES para el panel de inicio de caja
    public List<Pedido> obtenerPedidosPendientesBar() {
        return obtenerPedidosPendientes();
    }

    public List<Pedido> obtenerPedidosCompletados() {
        return pedidoRepository.findAll().stream()
                .filter(p -> {
                    Integer id = (p.getEstado() != null) ? p.getEstado().getId() : null;
                    String nombre = (p.getEstado() != null && p.getEstado().getNombreEstado() != null)
                            ? p.getEstado().getNombreEstado().trim()
                            : null;
                    return (id != null && id == 3) || (nombre != null && nombre.equalsIgnoreCase("COMPLETADO"));
                })
                .collect(java.util.stream.Collectors.toList());
    }

    // Marcar pedido como COMPLETADO (estado id=3) para flujo de BAR/CAJA
    public void marcarPedidoComoCompletadoBar(Integer pedidoId) {
        Pedido pedido = findById(pedidoId);
        if (pedido == null) {
            return;
        }

        estadoRepository.findById(3)
                .ifPresent(pedido::setEstado);

        pedidoRepository.save(pedido);
    }

    // Revertir pedido a PENDIENTE (estado id=1) para flujo de BAR/CAJA
    public void marcarPedidoComoPendienteBar(Integer pedidoId) {
        Pedido pedido = findById(pedidoId);
        if (pedido == null) {
            return;
        }

        estadoRepository.findById(1)
                .ifPresent(pedido::setEstado);

        pedidoRepository.save(pedido);
    }

    /**
     * Marca un pedido como PAGADO desde caja, actualizando montos y generando la factura asociada.
     */
    public void marcarPedidoComoPagado(Integer pedidoId, Double montoRecibido) {
        Pedido pedido = findById(pedidoId);
        if (pedido == null) {
            return;
        }

        Double total = pedido.getTotal() != null ? pedido.getTotal() : 0.0;
        if (montoRecibido == null) {
            montoRecibido = total;
        }

        pedido.setMontoRecibido(montoRecibido);
        pedido.setCambio(montoRecibido - total);

        // Cambiar estado del pedido a PAGADO (id = 2) si existe
        estadoRepository.findById(2)
                .ifPresent(pedido::setEstado);

        // Actualizar estado de la mesa a DISPONIBLE si aplica
        if (pedido.getMesa() != null) {
            pedido.getMesa().setEstado("DISPONIBLE");
            mesaRepository.save(pedido.getMesa());
        }

        // Crear factura asociada al pedido si aún no existe
        if (pedido.getFactura() == null) {
            Factura factura = new Factura();

            factura.setPedido(pedido);
            factura.setUsuario(pedido.getUsuario());

            // Estado de la factura: PAGADO (usamos el mismo estado id=2 si existe)
            estadoRepository.findById(2).ifPresent(factura::setEstado);

            factura.setNumeroFactura("F-" + System.currentTimeMillis());
            factura.setMonto(BigDecimal.valueOf(total));
            factura.setSubtotal(BigDecimal.valueOf(total));
            factura.setTotalPagar(BigDecimal.valueOf(total));
            factura.setValorDescuento(BigDecimal.ZERO);
            factura.setMetodoPago("EFECTIVO");
            factura.setEstadoPago("PAGADO");

            if (pedido.getMesa() != null) {
                factura.setNumeroMesa(pedido.getMesa().getId());
            }

            facturaRepository.save(factura);
            pedido.setFactura(factura);
        }

        pedidoRepository.save(pedido);
        try {
            String mesaNombre = (pedido.getMesa() != null && pedido.getMesa().getNumero() != null)
                    ? pedido.getMesa().getNumero() : (pedido.getMesa() != null ? ("#" + pedido.getMesa().getId()) : "sin mesa");
            String msg = "Se registró pago del pedido \"" + pedido.getId() + "\" por " + String.format("%.2f", montoRecibido != null ? montoRecibido : 0.0)
                    + ", total " + String.format("%.2f", total) + ", cambio " + String.format("%.2f", pedido.getCambio())
                    + " (mesa " + mesaNombre + ")";
            activityLogService.log("PAYMENT", msg, currentUser(), null);
        } catch (Exception ignored) {}
    }

    public void marcarPedidoComoPagadoConMetodo(Integer pedidoId, String metodoPago, Double montoRecibido, String referenciaPago, Long clienteId) {
        Pedido pedido = findById(pedidoId);
        if (pedido == null) {
            return;
        }

        Double total = pedido.getTotal() != null ? pedido.getTotal() : 0.0;
        String metodo = (metodoPago != null && !metodoPago.isBlank()) ? metodoPago : "EFECTIVO";

        if (!"EFECTIVO".equalsIgnoreCase(metodo) && !"MIXTO".equalsIgnoreCase(metodo)) {
            montoRecibido = total;
        } else if (montoRecibido == null) {
            montoRecibido = total;
        }

        pedido.setMontoRecibido(montoRecibido);
        pedido.setCambio(montoRecibido - total);

        estadoRepository.findById(2).ifPresent(pedido::setEstado);

        if (pedido.getMesa() != null) {
            pedido.getMesa().setEstado("DISPONIBLE");
            mesaRepository.save(pedido.getMesa());
        }

        if ("CLIENTE_FRECUENTE".equalsIgnoreCase(metodo)) {
            if (clienteId == null) {
                throw new IllegalStateException("Debe seleccionar un cliente frecuente");
            }
            var clienteOpt = clienteFrecuenteRepository.findById(clienteId);
            if (clienteOpt.isEmpty()) {
                throw new IllegalStateException("Cliente frecuente no encontrado");
            }
            var cliente = clienteOpt.get();
            Double saldoActual = cliente.getSaldo() != null ? cliente.getSaldo() : 0.0;
            Double nuevoSaldo = saldoActual - total; // permitir saldo negativo
            cliente.setSaldo(nuevoSaldo);
            clienteFrecuenteRepository.save(cliente);

            com.sena.getback.model.MovimientoCredito mov = new com.sena.getback.model.MovimientoCredito();
            mov.setCliente(cliente);
            mov.setTipo("CONSUMO");
            mov.setMonto(total);
            String deficitInfo = (saldoActual < total)
                    ? " (saldo insuficiente: falta " + String.format("%.2f", (total - saldoActual)) + ")"
                    : "";
            mov.setDescripcion("Consumo por pedido " + pedido.getId() + deficitInfo);
            movimientoCreditoRepository.save(mov);
        }

        if (pedido.getFactura() == null) {
            Factura factura = new Factura();
            factura.setPedido(pedido);
            factura.setUsuario(pedido.getUsuario());
            estadoRepository.findById(2).ifPresent(factura::setEstado);
            factura.setNumeroFactura("F-" + System.currentTimeMillis());
            factura.setMonto(java.math.BigDecimal.valueOf(total));
            factura.setSubtotal(java.math.BigDecimal.valueOf(total));
            factura.setTotalPagar(java.math.BigDecimal.valueOf(total));
            factura.setValorDescuento(java.math.BigDecimal.ZERO);
            factura.setMetodoPago(metodo.toUpperCase());
            factura.setEstadoPago("PAGADO");
            factura.setReferenciaPago(referenciaPago != null ? referenciaPago : "");
            if (clienteId != null) {
                var c = clienteFrecuenteRepository.findById(clienteId).orElse(null);
                if (c != null) factura.setClienteNombre(c.getNombre());
            }
            if (pedido.getMesa() != null) {
                factura.setNumeroMesa(pedido.getMesa().getId());
            }
            facturaRepository.save(factura);
            pedido.setFactura(factura);
        } else {
            Factura factura = pedido.getFactura();
            factura.setMetodoPago(metodo.toUpperCase());
            factura.setEstadoPago("PAGADO");
            factura.setReferenciaPago(referenciaPago != null ? referenciaPago : "");
            if (clienteId != null) {
                var c = clienteFrecuenteRepository.findById(clienteId).orElse(null);
                if (c != null) factura.setClienteNombre(c.getNombre());
            }
            facturaRepository.save(factura);
        }

        pedidoRepository.save(pedido);
        try {
            String mesaNombre = (pedido.getMesa() != null && pedido.getMesa().getNumero() != null)
                    ? pedido.getMesa().getNumero() : (pedido.getMesa() != null ? ("#" + pedido.getMesa().getId()) : "sin mesa");
            String msg = "Se registró pago (" + metodo + ") del pedido \"" + pedido.getId() + "\" por " + String.format("%.2f", montoRecibido != null ? montoRecibido : 0.0)
                    + ", total " + String.format("%.2f", total) + ", cambio " + String.format("%.2f", pedido.getCambio())
                    + " (mesa " + mesaNombre + ")";
            activityLogService.log("PAYMENT", msg, currentUser(), null);
        } catch (Exception ignored) {}
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

    private String currentUser() {
        try {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            return (a != null && a.getName() != null) ? a.getName() : "system";
        } catch (Exception e) { return "system"; }
    }
}
