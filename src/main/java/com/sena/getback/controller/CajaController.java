package com.sena.getback.controller;

import com.sena.getback.model.Estado;
import com.sena.getback.model.Factura;
import com.sena.getback.model.Menu;
import com.sena.getback.model.Mesa;
import com.sena.getback.model.Pedido;
import com.sena.getback.model.Usuario;
import com.sena.getback.repository.FacturaRepository;
import com.sena.getback.repository.PedidoRepository;
import com.sena.getback.repository.UsuarioRepository;
import com.sena.getback.service.MenuService;
import com.sena.getback.service.MesaService;
import com.sena.getback.service.CategoriaService;
import com.sena.getback.service.PedidoService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.sena.getback.repository.ClienteFrecuenteRepository;
import com.sena.getback.repository.EstadoRepository;
import com.sena.getback.repository.GastoRepository;
import com.sena.getback.repository.CajaCierreRepository;
import com.sena.getback.model.Gasto;
import com.sena.getback.model.CajaCierre;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/caja")
public class CajaController {
	private final MesaService mesaService;
	private final MenuService menuService;
	private final CategoriaService categoriaService;
	private final FacturaRepository facturaRepository;
	private final PedidoRepository pedidoRepository;
	private final UsuarioRepository usuarioRepository;
	private final PedidoService pedidoService;
	private final ClienteFrecuenteRepository clienteFrecuenteRepository;
    private final EstadoRepository estadoRepository;
    private final GastoRepository gastoRepository;
    private final CajaCierreRepository cajaCierreRepository;

	// Pedidos marcados como "completados" visualmente en el inicio de caja (pero
	// aún PENDIENTES de pago)
	private final Set<Integer> pedidosCompletadosInicioCaja = ConcurrentHashMap.newKeySet();

	public CajaController(MenuService menuService, CategoriaService categoriaService,
			FacturaRepository facturaRepository, PedidoRepository pedidoRepository, UsuarioRepository usuarioRepository,
			PedidoService pedidoService, ClienteFrecuenteRepository clienteFrecuenteRepository, MesaService mesaService,
            EstadoRepository estadoRepository, GastoRepository gastoRepository, CajaCierreRepository cajaCierreRepository) {

		this.menuService = menuService;
		this.categoriaService = categoriaService;
		this.facturaRepository = facturaRepository;
		this.pedidoRepository = pedidoRepository;
		this.usuarioRepository = usuarioRepository;
		this.pedidoService = pedidoService;
		this.clienteFrecuenteRepository = clienteFrecuenteRepository;
		this.mesaService = mesaService;
        this.estadoRepository = estadoRepository;
        this.gastoRepository = gastoRepository;
        this.cajaCierreRepository = cajaCierreRepository;
	}

	@GetMapping
	public String panelCaja(@RequestParam(required = false) String section,
	        @RequestParam(required = false) String categoria, @RequestParam(required = false) String filtro,
	        Model model, HttpSession session) {

	    String activeSection = (section != null && !section.isEmpty()) ? section : "inicio-caja";
	    model.addAttribute("activeSection", activeSection);
	    model.addAttribute("title", "Panel principal - Cajero");
	    
	    // ✅ SIEMPRE CARGAR LAS MESAS PARA EL MODAL
	    model.addAttribute("mesas", mesaService.findAll());

        if ("inicio-caja".equals(activeSection) || "pedidos".equals(activeSection)) {
	        List<Factura> facturas = facturaRepository.findAll();
	        List<Pedido> pedidos = pedidoRepository.findAll();
	        long totalUsuarios = usuarioRepository.count();

	        long totalVentas = facturas.size();
	        model.addAttribute("totalVentas", totalVentas);
	        model.addAttribute("totalUsuarios", totalUsuarios);

	        // Ventas del día basadas en los pedidos pagados que genera caja
	        List<Pedido> pedidosPagados = pedidoService.obtenerPedidosPagados();
	        LocalDate hoy = LocalDate.now();

            long ventasHoy = pedidosPagados.stream()
                .filter(p -> p.getFechaCreacion() != null && p.getFechaCreacion().toLocalDate().equals(hoy))
                .count();
            model.addAttribute("ventasHoy", ventasHoy);

            double ingresosHoy = pedidosPagados.stream()
                .filter(p -> p.getFechaCreacion() != null && p.getFechaCreacion().toLocalDate().equals(hoy))
                .mapToDouble(p -> p.getTotal() != null ? p.getTotal() : 0.0).sum();
            Object estInicio = session.getAttribute("cajaEstado");
            boolean abiertaInicio = false;
            if (estInicio instanceof java.util.Map<?, ?> estMapInicio) {
                Object abiertaValInicio = estMapInicio.get("abierta");
                abiertaInicio = (abiertaValInicio instanceof Boolean) ? (Boolean) abiertaValInicio : false;
            }
            if (!abiertaInicio) {
                ventasHoy = 0;
                ingresosHoy = 0.0;
            }
            model.addAttribute("ingresosHoy", ingresosHoy);
            model.addAttribute("ventasHoy", ventasHoy);

	        double ingresosTotales = facturas.stream().filter(f -> "PAGADO".equals(f.getEstadoPago()))
	                .mapToDouble(f -> f.getTotalPagar().doubleValue()).sum();
	        model.addAttribute("ingresosTotales", ingresosTotales);

            long pedidosPendientesPago = pedidoService.obtenerPedidosCompletados().size();
            model.addAttribute("pedidosPendientesPago", pedidosPendientesPago);

	        long ventasCanceladas = facturas.stream().filter(f -> "CANCELADO".equals(f.getEstadoPago())).count();
	        model.addAttribute("ventasCanceladas", ventasCanceladas);

	        List<Factura> actividadReciente = facturas.stream()
	                .sorted(Comparator.comparing(Factura::getFechaEmision).reversed()).limit(5)
	                .collect(Collectors.toList());
	        model.addAttribute("actividadReciente", actividadReciente);

            List<Pedido> pedidosPendientesBar = pedidoService.obtenerPedidosPendientesBar();
            List<Pedido> pedidosCompletadosBar = pedidoService.obtenerPedidosCompletados();

            model.addAttribute("pedidosPendientesBar", pedidosPendientesBar);
            model.addAttribute("pedidosPagadosBar", pedidosCompletadosBar);

            long pedidosPendientes = pedidosPendientesBar.size();
            model.addAttribute("pedidosPendientes", pedidosPendientes);

            // Últimos gastos (para panel inicio). Si la caja está cerrada, no mostrar.
            boolean abiertaUI = false;
            Object est = session.getAttribute("cajaEstado");
            if (est instanceof Map<?, ?> estMap) {
                Object abiertaVal = estMap.get("abierta");
                abiertaUI = (abiertaVal instanceof Boolean) ? (Boolean) abiertaVal : false;
            }
            List<Gasto> ultimosGastos = abiertaUI ? gastoRepository.findAll().stream()
                    .sorted(Comparator.comparing(Gasto::getFecha).reversed())
                    .limit(5)
                    .collect(Collectors.toList()) : java.util.List.of();
            model.addAttribute("ultimosGastos", ultimosGastos);
	    }

	    // Estado de caja para badge en header (desde sesión backend)
	    Object estadoObj = session.getAttribute("cajaEstado");
	    boolean cajaAbierta = false;
	    if (estadoObj instanceof Map) {
	        Object abiertaVal = ((Map<?, ?>) estadoObj).get("abierta");
	        cajaAbierta = (abiertaVal instanceof Boolean) ? (Boolean) abiertaVal : false;
	    }
	    model.addAttribute("cajaAbierta", cajaAbierta);

	    if ("punto-venta".equals(activeSection)) {
	        model.addAttribute("categorias", categoriaService.findAll());

	        if (categoria != null && !categoria.isEmpty()) {
	            model.addAttribute("menus", menuService.findByCategoriaNombre(categoria));
	        } else {
	            model.addAttribute("menus", menuService.findAll());
	        }

	        model.addAttribute("clientes", clienteFrecuenteRepository.findAll());
	        // Las mesas ya están cargadas arriba ✅
	    }

        if ("pagos".equals(activeSection)) {
            List<com.sena.getback.model.Pedido> completados = pedidoService.obtenerPedidosCompletados();

            java.util.Map<Integer, java.util.List<com.sena.getback.model.Pedido>> porMesa = new java.util.HashMap<>();
            for (var p : completados) {
                Integer mesaId = (p.getMesa() != null) ? p.getMesa().getId() : null;
                porMesa.computeIfAbsent(mesaId != null ? mesaId : -1, k -> new java.util.ArrayList<>()).add(p);
            }

            java.util.List<java.util.Map<String, Object>> pagosAgrupados = new java.util.ArrayList<>();
            for (var entry : porMesa.entrySet()) {
                var lista = entry.getValue();
                if (lista.isEmpty()) continue;
                var primero = lista.get(0);
                double total = lista.stream().mapToDouble(x -> x.getTotal() != null ? x.getTotal() : 0.0).sum();
                java.time.LocalDateTime fechaMax = lista.stream()
                        .map(com.sena.getback.model.Pedido::getFechaCreacion)
                        .filter(java.util.Objects::nonNull)
                        .max(java.util.Comparator.naturalOrder())
                        .orElse(null);
                java.util.List<Integer> ids = lista.stream()
                        .map(com.sena.getback.model.Pedido::getId)
                        .filter(java.util.Objects::nonNull)
                        .toList();

                // Construir detalle agrupado (items + comentarios)
                java.util.Map<String, Integer> itemsAgregados = new java.util.HashMap<>();
                java.util.Map<String, Double> preciosPorProducto = new java.util.HashMap<>();
                java.util.List<String> comentarios = new java.util.ArrayList<>();
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                for (var ped : lista) {
                    String ordenJson = ped.getOrden();
                    if (ordenJson != null && !ordenJson.isBlank()) {
                        try {
                            // Intenta parsear como Map raíz
                            Object rootObj = mapper.readValue(ordenJson, Object.class);
                            java.util.List<?> itemsList = null;
                            String comentariosStr = null;

                            if (rootObj instanceof java.util.Map<?,?> root) {
                                Object itemsObj = root.get("items");
                                if (itemsObj == null) itemsObj = root.get("Items");
                                if (itemsObj instanceof java.util.List<?>) {
                                    itemsList = (java.util.List<?>) itemsObj;
                                } else if (itemsObj instanceof String sItems) {
                                    // Cuando items está como String JSON
                                    try {
                                        Object nested = mapper.readValue(sItems, Object.class);
                                        if (nested instanceof java.util.List<?>) itemsList = (java.util.List<?>) nested;
                                        else if (nested instanceof java.util.Map<?,?> nestedMap) {
                                            Object nestedItems = nestedMap.get("items");
                                            if (nestedItems == null) nestedItems = nestedMap.get("Items");
                                            if (nestedItems instanceof java.util.List<?>) itemsList = (java.util.List<?>) nestedItems;
                                        }
                                    } catch (Exception ignored) {}
                                } else if (itemsObj instanceof java.util.Map<?,?> itemsMap) {
                                    Object nestedItems = itemsMap.get("items");
                                    if (nestedItems == null) nestedItems = itemsMap.get("Items");
                                    if (nestedItems instanceof java.util.List<?>) itemsList = (java.util.List<?>) nestedItems;
                                } else {
                                    // Fallback: algunos flujos guardan 'itemsJson'
                                    Object itemsJsonObj = root.get("itemsJson");
                                    if (itemsJsonObj instanceof String sJson) {
                                        try {
                                            Object nested = mapper.readValue(sJson, Object.class);
                                            if (nested instanceof java.util.List<?>) itemsList = (java.util.List<?>) nested;
                                            else if (nested instanceof java.util.Map<?,?> nestedMap) {
                                                Object nestedItems = nestedMap.get("items");
                                                if (nestedItems == null) nestedItems = nestedMap.get("Items");
                                                if (nestedItems instanceof java.util.List<?>) itemsList = (java.util.List<?>) nestedItems;
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                }
                                Object commRoot = root.get("comentarios");
                                if (commRoot instanceof String s && !s.isBlank()) comentariosStr = s;
                            } else if (rootObj instanceof java.util.List<?>) {
                                itemsList = (java.util.List<?>) rootObj;
                            }

                            if (itemsList != null) {
                                for (Object it : itemsList) {
                                    if (it instanceof java.util.Map<?,?> itemMap) {
                                        Object nombreObj = itemMap.get("nombre");
                                        if (nombreObj == null) nombreObj = itemMap.get("productoNombre");
                                        if (nombreObj == null) nombreObj = itemMap.get("producto");
                                        if (nombreObj == null) nombreObj = itemMap.get("nombreProducto");
                                        if (nombreObj == null) {
                                            // Buscar la primera clave tipo String que parezca nombre
                                            for (var k : itemMap.keySet()) {
                                                if (!(k instanceof String)) continue;
                                                String ks = (String) k;
                                                if (ks.toLowerCase().contains("nombre")) { nombreObj = itemMap.get(k); break; }
                                                if (ks.toLowerCase().contains("producto")) { nombreObj = itemMap.get(k); break; }
                                                if (ks.toLowerCase().contains("descripcion")) { nombreObj = itemMap.get(k); break; }
                                            }
                                        }
                                        Object cantObj = itemMap.get("cantidad");
                                        if (cantObj == null) cantObj = itemMap.get("qty");
                                        if (cantObj == null) cantObj = itemMap.get("cantidadPedido");
                                        Object precioObj = itemMap.get("precio");
                                        if (precioObj == null) precioObj = itemMap.get("precioUnitario");
                                        if (precioObj == null) precioObj = itemMap.get("precio_unitario");
                                        if (precioObj == null) precioObj = itemMap.get("valor");
                                        if (nombreObj != null && cantObj != null) {
                                            String nom = String.valueOf(nombreObj);
                                            int cant = (cantObj instanceof Number) ? ((Number)cantObj).intValue() : Integer.parseInt(String.valueOf(cantObj));
                                            if (cant > 0 && nom != null && !nom.isBlank()) {
                                                itemsAgregados.merge(nom, cant, Integer::sum);
                                                if (precioObj != null) {
                                                    double precio = (precioObj instanceof Number) ? ((Number)precioObj).doubleValue() : Double.parseDouble(String.valueOf(precioObj));
                                                    if (precio > 0) preciosPorProducto.put(nom, precio);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (comentariosStr != null && !comentariosStr.isBlank()) comentarios.add(comentariosStr);
                        } catch (Exception ignored) {}
                        // Fallback adicional: usar servicio para extraer cantidades
                        if (itemsAgregados.isEmpty()) {
                            var resumen = pedidoService.extraerCantidadesDesdeOrden(ordenJson);
                            for (var e : resumen.entrySet()) {
                                itemsAgregados.merge(e.getKey(), e.getValue(), Integer::sum);
                            }
                        }
                    }
                    if (ped.getComentariosGenerales() != null && !ped.getComentariosGenerales().isBlank()) {
                        comentarios.add(ped.getComentariosGenerales());
                    }
                }

                java.util.Map<String,Object> detalle = new java.util.HashMap<>();
                java.util.List<java.util.Map<String,Object>> itemsListOut = new java.util.ArrayList<>();
                for (var e2 : itemsAgregados.entrySet()) {
                    String nom = e2.getKey();
                    int cant = e2.getValue();
                    double precio = preciosPorProducto.getOrDefault(nom, 0.0);
                    double subtotal = precio * cant;
                    java.util.Map<String,Object> itemOut = new java.util.HashMap<>();
                    itemOut.put("nombre", nom);
                    itemOut.put("cantidad", cant);
                    itemOut.put("precio", precio);
                    itemOut.put("subtotal", subtotal);
                    itemsListOut.add(itemOut);
                }
                detalle.put("items", itemsListOut);
                detalle.put("comentarios", comentarios);
                detalle.put("pedidoIds", ids);
                String detalleJson;
                try { detalleJson = mapper.writeValueAsString(detalle); }
                catch (Exception ex) { detalleJson = detalle.toString(); }

                java.util.Map<String, Object> g = new java.util.HashMap<>();
                g.put("mesaLabel", primero.getLabelMesa());
                g.put("mesaId", primero.getMesa() != null ? primero.getMesa().getId() : null);
                g.put("pedidoIds", ids);
                g.put("total", total);
                g.put("primerPedidoId", primero.getId());
                g.put("primerTotal", primero.getTotal());
                g.put("detalleJson", detalleJson);
                g.put("fecha", fechaMax);
                g.put("estado", "COMPLETADO");
                pagosAgrupados.add(g);
            }

            // También proveer la lista original por compatibilidad
            model.addAttribute("pagosPendientes", completados);
            model.addAttribute("pagosPendientesAgrupados", pagosAgrupados);
            model.addAttribute("clientes", clienteFrecuenteRepository.findAll());
        }

	    if ("historial-pagos".equals(activeSection)) {
	        List<Pedido> pagosPagados = pedidoService.obtenerPedidosPagados();

	        if (filtro != null && !filtro.isEmpty()) {
	            LocalDate hoy = LocalDate.now();

	            if (filtro.equalsIgnoreCase("hoy")) {
	                pagosPagados = pagosPagados.stream()
	                        .filter(p -> p.getFechaCreacion() != null && p.getFechaCreacion().toLocalDate().equals(hoy))
	                        .collect(Collectors.toList());

	            } else if (filtro.equalsIgnoreCase("semana")) {
	                LocalDate inicioSemana = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	                LocalDate finSemana = hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

	                pagosPagados = pagosPagados.stream().filter(p -> p.getFechaCreacion() != null).filter(p -> {
	                    LocalDate fecha = p.getFechaCreacion().toLocalDate();
	                    return (fecha.isEqual(inicioSemana) || fecha.isAfter(inicioSemana))
	                            && (fecha.isEqual(finSemana) || fecha.isBefore(finSemana));
	                }).collect(Collectors.toList());

	            } else if (filtro.equalsIgnoreCase("mes")) {
	                pagosPagados = pagosPagados.stream().filter(p -> p.getFechaCreacion() != null).filter(p -> {
	                    LocalDate fecha = p.getFechaCreacion().toLocalDate();
	                    return fecha.getYear() == hoy.getYear() && fecha.getMonth() == hoy.getMonth();
	                }).collect(Collectors.toList());

	            } else if (filtro.equalsIgnoreCase("anio")) {
	                pagosPagados = pagosPagados.stream().filter(p -> p.getFechaCreacion() != null)
	                        .filter(p -> p.getFechaCreacion().toLocalDate().getYear() == hoy.getYear())
	                        .collect(Collectors.toList());
	            }
	        }

	        model.addAttribute("pagosPagados", pagosPagados);
	        model.addAttribute("filtro", filtro);
	    }

        if ("corte-caja".equals(activeSection)) {
            LocalDate hoy = LocalDate.now();

            // Ingresos por EFECTIVO del día (según facturas pagadas)
            double ingresosEfectivoDia = facturaRepository.findAll().stream()
                    .filter(f -> f.getFechaEmision() != null && f.getFechaEmision().toLocalDate().equals(hoy))
                    .filter(f -> "PAGADO".equalsIgnoreCase(f.getEstadoPago()))
                    .filter(f -> f.getMetodoPago() != null && f.getMetodoPago().equalsIgnoreCase("EFECTIVO"))
                    .mapToDouble(f -> f.getTotalPagar() != null ? f.getTotalPagar().doubleValue() : 0.0)
                    .sum();

            // Ventas del día (conteo de facturas pagadas)
            long ventasDia = facturaRepository.findAll().stream()
                    .filter(f -> f.getFechaEmision() != null && f.getFechaEmision().toLocalDate().equals(hoy))
                    .filter(f -> "PAGADO".equalsIgnoreCase(f.getEstadoPago()))
                    .count();

            // Gastos del día en EFECTIVO
            double gastosEfectivoDia = gastoRepository.findAll().stream()
                    .filter(g -> g.getFecha() != null && g.getFecha().equals(hoy))
                    .filter(g -> g.getMetodo() != null && g.getMetodo().equalsIgnoreCase("EFECTIVO"))
                    .mapToDouble(g -> g.getMonto() != null ? g.getMonto() : 0.0)
                    .sum();

            boolean abiertaUI = false;
            Object est = session.getAttribute("cajaEstado");
            if (est instanceof Map<?, ?> estMap) {
                Object abiertaVal = estMap.get("abierta");
                abiertaUI = (abiertaVal instanceof Boolean) ? (Boolean) abiertaVal : false;
            }
            if (!abiertaUI) {
                ventasDia = 0;
                ingresosEfectivoDia = 0.0;
                gastosEfectivoDia = 0.0;
            }

            model.addAttribute("fechaCorte", hoy);
            model.addAttribute("ventasDia", ventasDia);
            model.addAttribute("ingresosEfectivoDia", ingresosEfectivoDia);
            model.addAttribute("gastosEfectivoDia", gastosEfectivoDia);
        }

	    return "caja/panel_caja";
	}

	// ==========================
	// API de estado de caja (sesión)
	// ==========================

	@GetMapping("/estado")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> obtenerEstadoCaja(HttpSession session) {
	    Map<String, Object> estado = (Map<String, Object>) session.getAttribute("cajaEstado");
	    if (estado == null) {
	        estado = Map.of(
	                "abierta", false,
	                "mensaje", "Caja cerrada"
	        );
	    }
	    return ResponseEntity.ok(estado);
	}

	@PostMapping("/abrir")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> abrirCaja(@RequestParam(name = "base", required = false) Double base,
	                                                    HttpSession session) {
	    double baseVal = base != null && base >= 0 ? base : 0.0;
	    Map<String, Object> estado = new java.util.HashMap<>();
	    estado.put("abierta", true);
	    estado.put("fechaApertura", java.time.LocalDateTime.now().toString());
	    estado.put("base", baseVal);
	    session.setAttribute("cajaEstado", estado);
	    return ResponseEntity.ok(Map.of("success", true, "estado", estado));
	}

    @PostMapping("/cerrar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cerrarCaja(@RequestParam(name = "retiro", required = false) Double retiro,
                                                    @RequestParam(name = "baseSiguiente", required = false) Double baseSiguiente,
                                                    HttpSession session) {
        double retiroVal = retiro != null && retiro >= 0 ? retiro : 0.0;
        double baseSig = baseSiguiente != null && baseSiguiente >= 0 ? baseSiguiente : 0.0;
        Map<String, Object> estado = (Map<String, Object>) session.getAttribute("cajaEstado");
        if (estado == null) estado = new java.util.HashMap<>();
        estado.put("abierta", false);
        estado.put("fechaCierre", java.time.LocalDateTime.now().toString());
        estado.put("retiro", retiroVal);
        estado.put("baseSiguiente", baseSig);
        session.setAttribute("cajaEstado", estado);

        // Guardar cierre del día
        java.time.LocalDate hoy = java.time.LocalDate.now();
        double ingresosEfectivoDia = facturaRepository.findAll().stream()
                .filter(f -> f.getFechaEmision() != null && f.getFechaEmision().toLocalDate().equals(hoy))
                .filter(f -> "PAGADO".equalsIgnoreCase(f.getEstadoPago()))
                .filter(f -> f.getMetodoPago() != null && f.getMetodoPago().equalsIgnoreCase("EFECTIVO"))
                .mapToDouble(f -> f.getTotalPagar() != null ? f.getTotalPagar().doubleValue() : 0.0)
                .sum();
        long ventasDia = facturaRepository.findAll().stream()
                .filter(f -> f.getFechaEmision() != null && f.getFechaEmision().toLocalDate().equals(hoy))
                .filter(f -> "PAGADO".equalsIgnoreCase(f.getEstadoPago()))
                .count();
        double gastosEfectivoDia = gastoRepository.findAll().stream()
                .filter(g -> g.getFecha() != null && g.getFecha().equals(hoy))
                .filter(g -> g.getMetodo() != null && g.getMetodo().equalsIgnoreCase("EFECTIVO"))
                .mapToDouble(g -> g.getMonto() != null ? g.getMonto() : 0.0)
                .sum();
        Double baseApertura = null;
        Object baseObj = estado.get("base");
        if (baseObj instanceof Number) baseApertura = ((Number) baseObj).doubleValue();

        CajaCierre cierre = new CajaCierre();
        cierre.setFecha(hoy);
        cierre.setIngresosEfectivo(ingresosEfectivoDia);
        cierre.setGastosEfectivo(gastosEfectivoDia);
        cierre.setVentasDia(ventasDia);
        cierre.setBaseApertura(baseApertura);
        cierre.setBaseSiguiente(baseSig);
        cierre.setRetiro(retiroVal);
        cajaCierreRepository.save(cierre);

        return ResponseEntity.ok(Map.of("success", true, "estado", estado));
    }

    @GetMapping("/gastos/historial")
    @ResponseBody
    public ResponseEntity<List<Gasto>> historialGastos(@RequestParam(name = "mes", required = false) String mes) {
        List<Gasto> lista;
        if (mes != null && mes.matches("\\d{4}-\\d{2}")) {
            String[] parts = mes.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            java.time.LocalDate start = java.time.LocalDate.of(year, month, 1);
            java.time.LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            lista = gastoRepository.findAll().stream()
                    .filter(g -> g.getFecha() != null && !g.getFecha().isBefore(start) && !g.getFecha().isAfter(end))
                    .sorted(Comparator.comparing(Gasto::getFecha).reversed())
                    .collect(Collectors.toList());
        } else {
            lista = gastoRepository.findAll().stream()
                    .sorted(Comparator.comparing(Gasto::getFecha).reversed())
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/historial")
    @ResponseBody
    public ResponseEntity<java.util.List<CajaCierre>> historial(@RequestParam(name = "mes", required = false) String mes) {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.util.List<CajaCierre> lista;
        if (mes != null && mes.matches("\\d{4}-\\d{2}")) {
            String[] parts = mes.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            java.time.LocalDate start = java.time.LocalDate.of(year, month, 1);
            java.time.LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            lista = cajaCierreRepository.findByFechaBetween(start, end);
        } else {
            lista = cajaCierreRepository.findAll();
        }
        lista.sort(java.util.Comparator.comparing(CajaCierre::getFecha).reversed());
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/pagar")
    public String pagarPedido(@RequestParam(value = "pedidoId", required = false) Integer pedidoId,
                              @RequestParam(value = "pedidoIds", required = false) String pedidoIds,
                              @RequestParam(value = "montoRecibido", required = false) Double montoRecibido,
                              @RequestParam(value = "metodoPago", required = false) String metodoPago,
                              @RequestParam(value = "referenciaPago", required = false) String referenciaPago,
                              @RequestParam(value = "clienteId", required = false) Long clienteId,
                              RedirectAttributes ra) {
        try {
            if (pedidoIds != null && !pedidoIds.isBlank()) {
                String[] parts = pedidoIds.split(",");
                for (String part : parts) {
                    if (part == null || part.isBlank()) continue;
                    Integer id = Integer.valueOf(part.trim());
                    Pedido p = pedidoRepository.findById(id).orElse(null);
                    Double recibidoPorPedido = null;
                    if (p != null && "EFECTIVO".equalsIgnoreCase(metodoPago)) {
                        recibidoPorPedido = p.getTotal();
                    }
                    pedidoService.marcarPedidoComoPagadoConMetodo(id, metodoPago, recibidoPorPedido != null ? recibidoPorPedido : montoRecibido, referenciaPago, clienteId);
                }
                ra.addFlashAttribute("success", "Venta exitosa");
            } else if (pedidoId != null) {
                pedidoService.marcarPedidoComoPagadoConMetodo(pedidoId, metodoPago, montoRecibido, referenciaPago, clienteId);
                ra.addFlashAttribute("success", "Venta exitosa");
            } else {
                ra.addFlashAttribute("error", "No se especificó pedido a pagar");
            }
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Error al registrar el pago: " + ex.getMessage());
        }
        return "redirect:/caja?section=pagos";
    }

    @GetMapping("/pagos/detalle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDetalleAgrupado(@RequestParam("ids") String ids) {
        try {
            String[] parts = ids.split(",");
            java.util.List<Integer> idList = new java.util.ArrayList<>();
            for (String part : parts) {
                if (part == null || part.isBlank()) continue;
                idList.add(Integer.valueOf(part.trim()));
            }
            java.util.List<com.sena.getback.model.Pedido> lista = new java.util.ArrayList<>();
            for (Integer id : idList) {
                pedidoRepository.findById(id).ifPresent(lista::add);
            }
            // Reutilizar construcción de detalle
            java.util.Map<String, Integer> itemsAgregados = new java.util.HashMap<>();
            java.util.Map<String, Double> preciosPorProducto = new java.util.HashMap<>();
            java.util.List<String> comentarios = new java.util.ArrayList<>();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            for (var ped : lista) {
                String ordenJson = ped.getOrden();
                if (ordenJson != null && !ordenJson.isBlank()) {
                    try {
                        Object rootObj = mapper.readValue(ordenJson, Object.class);
                        java.util.List<?> itemsList = null;
                        String comentariosStr = null;
                        if (rootObj instanceof java.util.Map<?,?> root) {
                            Object itemsObj = root.get("items");
                            if (itemsObj == null) itemsObj = root.get("Items");
                            if (itemsObj instanceof java.util.List<?>) {
                                itemsList = (java.util.List<?>) itemsObj;
                            } else if (itemsObj instanceof String sItems) {
                                Object nested = mapper.readValue(sItems, Object.class);
                                if (nested instanceof java.util.List<?>) itemsList = (java.util.List<?>) nested;
                                else if (nested instanceof java.util.Map<?,?> nestedMap) {
                                    Object nestedItems = nestedMap.get("items");
                                    if (nestedItems == null) nestedItems = nestedMap.get("Items");
                                    if (nestedItems instanceof java.util.List<?>) itemsList = (java.util.List<?>) nestedItems;
                                }
                            } else if (itemsObj instanceof java.util.Map<?,?> itemsMap) {
                                Object nestedItems = itemsMap.get("items");
                                if (nestedItems == null) nestedItems = itemsMap.get("Items");
                                if (nestedItems instanceof java.util.List<?>) itemsList = (java.util.List<?>) nestedItems;
                            } else {
                                Object itemsJsonObj = root.get("itemsJson");
                                if (itemsJsonObj instanceof String sJson) {
                                    Object nested = mapper.readValue(sJson, Object.class);
                                    if (nested instanceof java.util.List<?>) itemsList = (java.util.List<?>) nested;
                                    else if (nested instanceof java.util.Map<?,?> nestedMap) {
                                        Object nestedItems = nestedMap.get("items");
                                        if (nestedItems == null) nestedItems = nestedMap.get("Items");
                                        if (nestedItems instanceof java.util.List<?>) itemsList = (java.util.List<?>) nestedItems;
                                    }
                                }
                            }
                            Object commRoot = root.get("comentarios");
                            if (commRoot instanceof String s && !s.isBlank()) comentariosStr = s;
                        } else if (rootObj instanceof java.util.List<?>) {
                            itemsList = (java.util.List<?>) rootObj;
                        }
                        if (itemsList != null) {
                            for (Object it : itemsList) {
                                if (it instanceof java.util.Map<?,?> itemMap) {
                                    Object nombreObj = itemMap.get("nombre");
                                    if (nombreObj == null) nombreObj = itemMap.get("productoNombre");
                                    if (nombreObj == null) nombreObj = itemMap.get("producto");
                                    if (nombreObj == null) nombreObj = itemMap.get("nombreProducto");
                                    if (nombreObj == null) {
                                        for (var k : itemMap.keySet()) {
                                            if (!(k instanceof String)) continue;
                                            String ks = (String) k;
                                            if (ks.toLowerCase().contains("nombre")) { nombreObj = itemMap.get(k); break; }
                                            if (ks.toLowerCase().contains("producto")) { nombreObj = itemMap.get(k); break; }
                                            if (ks.toLowerCase().contains("descripcion")) { nombreObj = itemMap.get(k); break; }
                                        }
                                    }
                                    Object cantObj = itemMap.get("cantidad");
                                    if (cantObj == null) cantObj = itemMap.get("qty");
                                    if (cantObj == null) cantObj = itemMap.get("cantidadPedido");
                                    Object precioObj = itemMap.get("precio");
                                    if (precioObj == null) precioObj = itemMap.get("precioUnitario");
                                    if (precioObj == null) precioObj = itemMap.get("precio_unitario");
                                    if (precioObj == null) precioObj = itemMap.get("valor");
                                    if (nombreObj != null && cantObj != null) {
                                        String nom = String.valueOf(nombreObj);
                                        int cant = (cantObj instanceof Number) ? ((Number)cantObj).intValue() : Integer.parseInt(String.valueOf(cantObj));
                                        if (cant > 0 && nom != null && !nom.isBlank()) {
                                            itemsAgregados.merge(nom, cant, Integer::sum);
                                            if (precioObj != null) {
                                                double precio = (precioObj instanceof Number) ? ((Number)precioObj).doubleValue() : Double.parseDouble(String.valueOf(precioObj));
                                                if (precio > 0) preciosPorProducto.put(nom, precio);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (comentariosStr != null && !comentariosStr.isBlank()) comentarios.add(comentariosStr);
                    } catch (Exception ignored) {}
                }
                if (ped.getComentariosGenerales() != null && !ped.getComentariosGenerales().isBlank()) {
                    comentarios.add(ped.getComentariosGenerales());
                }
            }
            java.util.List<java.util.Map<String,Object>> itemsOut = new java.util.ArrayList<>();
            for (var e : itemsAgregados.entrySet()) {
                String nom = e.getKey();
                int cant = e.getValue();
                double precio = preciosPorProducto.getOrDefault(nom, 0.0);
                double subtotal = precio * cant;
                itemsOut.add(java.util.Map.of(
                        "nombre", nom,
                        "cantidad", cant,
                        "precio", precio,
                        "subtotal", subtotal
                ));
            }
            double total = lista.stream().mapToDouble(x -> x.getTotal() != null ? x.getTotal() : 0.0).sum();
            Map<String,Object> res = new java.util.HashMap<>();
            res.put("items", itemsOut);
            res.put("comentarios", comentarios);
            res.put("total", total);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/gastos/registrar")
    public String registrarGasto(@RequestParam String concepto,
                                 @RequestParam String fecha,
                                 @RequestParam Double monto,
                                 @RequestParam String metodo,
                                 @RequestParam(required = false) String nota,
                                 RedirectAttributes ra,
                                 HttpSession session) {
        boolean abierta = false;
        Object estado = session != null ? session.getAttribute("cajaEstado") : null;
        if (estado instanceof java.util.Map<?, ?> map) {
            Object abiertaVal = map.get("abierta");
            abierta = (abiertaVal instanceof Boolean) ? (Boolean) abiertaVal : false;
        }
        if (!abierta) {
            ra.addFlashAttribute("error", "No se pueden registrar gastos con la caja cerrada");
            return "redirect:/caja?section=inicio-caja";
        }
        try {
            Gasto g = new Gasto();
            g.setConcepto(concepto);
            g.setFecha(LocalDate.parse(fecha));
            g.setMonto(monto);
            g.setMetodo(metodo);
            g.setNota(nota);
            gastoRepository.save(g);
            ra.addFlashAttribute("success", "Gasto registrado correctamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al registrar gasto: " + e.getMessage());
        }
        return "redirect:/caja?section=inicio-caja";
    }

	@PostMapping("/marcar-completado")
	public String marcarPedidoComoCompletadoDesdeInicio(@RequestParam("pedidoId") Integer pedidoId,
			@RequestParam(value = "section", required = false) String section,
			@RequestParam(value = "accion", required = false) String accion) {
		// Solo marcar visualmente como "completado" en el panel de inicio de caja.
		// El estado de pago (PAGADO) se gestiona exclusivamente desde la vista de pagos
		// (/caja?section=pagos).
		if (pedidoId != null) {
			if (accion != null && accion.equalsIgnoreCase("revertir")) {
				pedidoService.marcarPedidoComoPendienteBar(pedidoId);
				pedidosCompletadosInicioCaja.remove(pedidoId);
			} else {
				pedidoService.marcarPedidoComoCompletadoBar(pedidoId);
				pedidosCompletadosInicioCaja.add(pedidoId);
			}
		}

		if (section != null && !section.isEmpty()) {
			return "redirect:/caja?section=" + section;
		}

		return "redirect:/caja";
	}

	// VENTA RÁPIDA DESDE PUNTO DE VENTA
    @PostMapping("/crear-pedido-pendiente")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearPedidoPendiente(@RequestBody Map<String, Object> requestData,
                                                                   HttpSession session) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
            if (usuario == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "mensaje", "Usuario no autenticado"));
            }

	        System.out.println("=== CREANDO PEDIDO PENDIENTE ===");
	        System.out.println("Datos recibidos: " + requestData);

	        // Crear pedido básico
            Pedido pedido = new Pedido();
            pedido.setTotal(Double.valueOf(requestData.get("total").toString()));
            pedido.setComentariosGenerales("Pedido desde punto de venta");
            pedido.setFechaCreacion(LocalDateTime.now());
            pedido.setUsuario(usuario);

	        // Buscar estado PENDIENTE
	        Estado estadoPendiente = estadoRepository.findAll().stream()
	                .filter(estado -> "PENDIENTE".equals(estado.getNombreEstado()))
	                .findFirst()
	                .orElseGet(() -> {
	                    Estado nuevoEstado = new Estado();
	                    nuevoEstado.setNombreEstado("PENDIENTE");
	                    return estadoRepository.save(nuevoEstado);
	                });

            pedido.setEstado(estadoPendiente);

            try {
                ObjectMapper mapper = new ObjectMapper();
                String ordenJson = mapper.writeValueAsString(requestData);
                pedido.setOrden(ordenJson);
            } catch (Exception ex) {
                pedido.setOrden(requestData.toString());
            }

            Object mesaIdObj = requestData.get("mesaId");
            if (mesaIdObj != null) {
                try {
                    Integer mesaId = Integer.valueOf(String.valueOf(mesaIdObj));
                    Mesa mesaSeleccionada = mesaService.findById(mesaId).orElse(null);
                    if (mesaSeleccionada != null) {
                        pedido.setMesa(mesaSeleccionada);
                    }
                } catch (Exception ignored) {}
            }
            if (pedido.getMesa() == null) {
                Mesa mesaBar = mesaService.findAll().stream()
                        .filter(m -> (m.getDescripcion() != null && (
                                            m.getDescripcion().equalsIgnoreCase("Bar") ||
                                            m.getDescripcion().equalsIgnoreCase("Barra") ||
                                            m.getDescripcion().equalsIgnoreCase("Mostrador")))
                                || (m.getNumero() != null && (
                                            m.getNumero().equalsIgnoreCase("Bar") ||
                                            m.getNumero().equalsIgnoreCase("Barra"))) )
                        .findFirst()
                        .orElse(null);
                if (mesaBar != null) {
                    pedido.setMesa(mesaBar);
                }
            }

	        // Guardar pedido
            pedido = pedidoRepository.save(pedido);

	        System.out.println("✅ Pedido pendiente creado con ID: " + pedido.getId());

            return ResponseEntity.ok(Map.of("success", true, "mensaje", "Venta exitosa"));

	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(500).body(Map.of("success", false, "mensaje", "Error: " + e.getMessage()));
	    }
	}
	
	@PostMapping("/crear-pedido-pendiente-form")
	public String crearPedidoPendienteForm(@RequestParam String items,
	                                      @RequestParam Double total,
	                                      @RequestParam(required = false) Integer mesaId,
	                                      HttpSession session,
	                                      RedirectAttributes ra) {
	    try {
	        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
	        if (usuario == null) {
	            ra.addFlashAttribute("error", "Usuario no autenticado");
	            return "redirect:/login";
	        }

	        System.out.println("=== CREANDO PEDIDO PENDIENTE DESDE FORMULARIO ===");
	        System.out.println("Total: " + total);
	        System.out.println("Items: " + items);
	        System.out.println("Mesa ID: " + mesaId);

	        // 1. Crear el Pedido
	        Pedido pedido = new Pedido();
	        pedido.setTotal(total);
	        pedido.setComentariosGenerales("Pedido desde punto de venta");
	        pedido.setFechaCreacion(LocalDateTime.now());
	        pedido.setUsuario(usuario);

	        // 2. Buscar estado PENDIENTE (OBLIGATORIO)
	        Estado estadoPendiente = estadoRepository.findAll().stream()
	                .filter(estado -> "PENDIENTE".equals(estado.getNombreEstado()))
	                .findFirst()
	                .orElseGet(() -> {
	                    Estado nuevoEstado = new Estado();
	                    nuevoEstado.setNombreEstado("PENDIENTE");
	                    return estadoRepository.save(nuevoEstado);
	                });
	        pedido.setEstado(estadoPendiente);

	        // 3. Manejar la mesa (OPCIONAL)
	        if (mesaId != null) {
	            // Buscar la mesa seleccionada
	            Mesa mesaSeleccionada = mesaService.findById(mesaId)
	                    .orElseThrow(() -> new RuntimeException("Mesa no encontrada con ID: " + mesaId));
	            pedido.setMesa(mesaSeleccionada);
	            System.out.println("Mesa asignada: " + mesaSeleccionada.getNumero());
        } else {
            // Si no se selecciona mesa, buscar una mesa llamada "Bar" o "Mostrador"
            Mesa mesaBar = mesaService.findAll().stream()
                    .filter(m -> (m.getDescripcion() != null && (
                                    m.getDescripcion().equalsIgnoreCase("Bar") ||
                                    m.getDescripcion().equalsIgnoreCase("Barra") ||
                                    m.getDescripcion().equalsIgnoreCase("Mostrador")))
                            || (m.getNumero() != null && (
                                    m.getNumero().equalsIgnoreCase("Bar") ||
                                    m.getNumero().equalsIgnoreCase("Barra"))) )
                    .findFirst()
                    .orElse(null);

            if (mesaBar != null) {
                pedido.setMesa(mesaBar);
                System.out.println("Mesa por defecto asignada: " + mesaBar.getNumero());
            } else {
                System.out.println("No se asignó mesa - será null");
                // Dejar null para que en UI se muestre "Bar/Mostrador"
            }
        }

        // 4. Asignar menú por defecto solo si existe en BD
        // Si no hay menús, dejar null para evitar referencias transientes
        Menu menuDefault = menuService.findAll().stream().findFirst().orElse(null);
        if (menuDefault != null) {
            pedido.setMenu(menuDefault);
            System.out.println("Menú asignado: " + menuDefault.getNombreProducto());
        } else {
            System.out.println("⚠️ No hay menús disponibles, se dejará menu=null para evitar errores");
        }

        pedido.setOrden(items);

	        // 6. Guardar pedido
	        pedido = pedidoRepository.save(pedido);

	        System.out.println("✅ Pedido pendiente creado con ID: " + pedido.getId());
	        System.out.println("📊 Total: $" + total);
	        System.out.println("🍽️  Items: " + items);

	        // 7. Limpiar carrito del localStorage (se hará en el frontend al recargar)
        ra.addFlashAttribute("success", "Venta exitosa");
	        return "redirect:/caja?section=pagos";

	    } catch (Exception e) {
	        System.err.println("❌ Error al crear pedido: " + e.getMessage());
	        e.printStackTrace();
	        ra.addFlashAttribute("error", "❌ Error al crear pedido: " + e.getMessage());
	        return "redirect:/caja?section=punto-venta";
	    }
	}
	
	// Método temporal para debug
	@GetMapping("/debug-rutas")
	@ResponseBody
	public String debugRutas() {
	    return "Rutas disponibles en CajaController:\n" +
	           "POST /caja/crear-pedido-pendiente-form\n" +
	           "GET  /caja\n" +
	           "POST /caja/pagar\n" +
	           "POST /caja/marcar-completado\n" +
	           "POST /caja/punto-venta/registrar";
	}
}
