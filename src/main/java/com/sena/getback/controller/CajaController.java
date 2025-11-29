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

	// Pedidos marcados como "completados" visualmente en el inicio de caja (pero
	// aún PENDIENTES de pago)
	private final Set<Integer> pedidosCompletadosInicioCaja = ConcurrentHashMap.newKeySet();

	public CajaController(MenuService menuService, CategoriaService categoriaService,
			FacturaRepository facturaRepository, PedidoRepository pedidoRepository, UsuarioRepository usuarioRepository,
			PedidoService pedidoService, ClienteFrecuenteRepository clienteFrecuenteRepository, MesaService mesaService,
			EstadoRepository estadoRepository) {

		this.menuService = menuService;
		this.categoriaService = categoriaService;
		this.facturaRepository = facturaRepository;
		this.pedidoRepository = pedidoRepository;
		this.usuarioRepository = usuarioRepository;
		this.pedidoService = pedidoService;
		this.clienteFrecuenteRepository = clienteFrecuenteRepository;
		this.mesaService = mesaService;
		this.estadoRepository = estadoRepository;
	}

	@GetMapping
	public String panelCaja(@RequestParam(required = false) String section,
			@RequestParam(required = false) String categoria, @RequestParam(required = false) String filtro,
			Model model) {

		String activeSection = (section != null && !section.isEmpty()) ? section : "inicio-caja";
		model.addAttribute("activeSection", activeSection);
		model.addAttribute("title", "Panel principal - Cajero");

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
			model.addAttribute("ingresosHoy", ingresosHoy);

			double ingresosTotales = facturas.stream().filter(f -> "PAGADO".equals(f.getEstadoPago()))
					.mapToDouble(f -> f.getTotalPagar().doubleValue()).sum();
			model.addAttribute("ingresosTotales", ingresosTotales);

			// Cantidad de pedidos pendientes de cobro (se usa en la tarjeta verde del
			// dashboard y en la sección Pagos)
			long pedidosPendientesPago = pedidoService.obtenerPedidosPendientes().size();
			model.addAttribute("pedidosPendientesPago", pedidosPendientesPago);

			long ventasCanceladas = facturas.stream().filter(f -> "CANCELADO".equals(f.getEstadoPago())).count();
			model.addAttribute("ventasCanceladas", ventasCanceladas);

			List<Factura> actividadReciente = facturas.stream()
					.sorted(Comparator.comparing(Factura::getFechaEmision).reversed()).limit(5)
					.collect(Collectors.toList());
			model.addAttribute("actividadReciente", actividadReciente);

			// Pedidos para mostrar en cards en el inicio
			List<Pedido> pedidosPendientesBarTodos = pedidoService.obtenerPedidosPendientesBar();

			List<Pedido> pedidosPendientesBar = pedidosPendientesBarTodos.stream()
					.filter(p -> p.getId() != null && !pedidosCompletadosInicioCaja.contains(p.getId()))
					.collect(Collectors.toList());

			List<Pedido> pedidosCompletadosBar = pedidosPendientesBarTodos.stream()
					.filter(p -> p.getId() != null && pedidosCompletadosInicioCaja.contains(p.getId()))
					.collect(Collectors.toList());

			model.addAttribute("pedidosPendientesBar", pedidosPendientesBar);
			model.addAttribute("pedidosPagadosBar", pedidosCompletadosBar);

			// Para el dashboard, "Pedidos Pendientes" refleja los pedidos de BAR aún no
			// marcados como completados en el inicio
			long pedidosPendientes = pedidosPendientesBar.size();
			model.addAttribute("pedidosPendientes", pedidosPendientes);
		}

		if ("punto-venta".equals(activeSection)) {
			model.addAttribute("categorias", categoriaService.findAll());

			if (categoria != null && !categoria.isEmpty()) {
				model.addAttribute("menus", menuService.findByCategoriaNombre(categoria));
			} else {
				model.addAttribute("menus", menuService.findAll());
			}

			model.addAttribute("clientes", clienteFrecuenteRepository.findAll());
			model.addAttribute("mesas", mesaService.findAll());
		}

		if ("pagos".equals(activeSection)) {
			model.addAttribute("pagosPendientes", pedidoService.obtenerPedidosPendientes());
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
					// Rango de la semana actual: lunes a domingo
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

			List<Pedido> pagosPagadosHoy = pedidoService.obtenerPedidosPagados().stream()
					.filter(p -> p.getFechaCreacion() != null && p.getFechaCreacion().toLocalDate().equals(hoy))
					.collect(Collectors.toList());

			long ventasDia = pagosPagadosHoy.size();
			double ingresosDia = pagosPagadosHoy.stream().mapToDouble(p -> p.getTotal() != null ? p.getTotal() : 0.0)
					.sum();

			model.addAttribute("fechaCorte", hoy);
			model.addAttribute("ventasDia", ventasDia);
			model.addAttribute("ingresosDia", ingresosDia);
		}

		return "caja/panel_caja";
	}

	@PostMapping("/pagar")
	public String pagarPedido(@RequestParam("pedidoId") Integer pedidoId,
			@RequestParam(value = "montoRecibido", required = false) Double montoRecibido) {
		pedidoService.marcarPedidoComoPagado(pedidoId, montoRecibido);
		return "redirect:/caja?section=pagos";
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

	        // Guardar carrito como string simple
	        pedido.setOrden(requestData.toString());

	        // Guardar pedido
	        pedido = pedidoRepository.save(pedido);

	        System.out.println("✅ Pedido pendiente creado con ID: " + pedido.getId());

	        return ResponseEntity.ok(Map.of("success", true, "mensaje", "Pedido creado exitosamente"));

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

	        // 2. Buscar estado PENDIENTE
	        Estado estadoPendiente = estadoRepository.findAll().stream()
	                .filter(estado -> "PENDIENTE".equals(estado.getNombreEstado()))
	                .findFirst()
	                .orElseGet(() -> {
	                    Estado nuevoEstado = new Estado();
	                    nuevoEstado.setNombreEstado("PENDIENTE");
	                    return estadoRepository.save(nuevoEstado);
	                });
	        pedido.setEstado(estadoPendiente);

	        // 3. Manejar la mesa (opcional)
	        if (mesaId != null) {
	            Mesa mesa = mesaService.findById(mesaId)
	                    .orElseThrow(() -> new RuntimeException("Mesa no encontrada"));
	            pedido.setMesa(mesa);
	        } else {
	            // Si no se selecciona mesa, buscar una mesa llamada "Bar" o crear una genérica
	            Mesa mesaBar = mesaService.findAll().stream()
	                    .filter(m -> "Bar".equalsIgnoreCase(m.getDescripcion()) || 
	                                "Bar".equalsIgnoreCase(m.getNumero()))
	                    .findFirst()
	                    .orElse(null);
	            
	            if (mesaBar == null) {
	                // Si no existe mesa Bar, usar la primera disponible
	                mesaBar = mesaService.findAll().stream()
	                        .findFirst()
	                        .orElse(null);
	            }
	            pedido.setMesa(mesaBar);
	        }

	        // 4. Asignar menú por defecto (mantener como null si cambiaste la entidad)
	        // Si mantienes menu_id como NOT NULL, necesitas asignar uno:
	        Menu menuDefault = menuService.findAll().stream()
	                .findFirst()
	                .orElse(null);
	        if (menuDefault != null) {
	            pedido.setMenu(menuDefault);
	        }

	        pedido.setOrden(items);

	        // 5. Guardar pedido
	        pedido = pedidoRepository.save(pedido);

	        System.out.println("✅ Pedido pendiente creado con ID: " + pedido.getId());

	        ra.addFlashAttribute("success", "✅ Pedido creado exitosamente. Ahora aparece en 'Pagos de Pedidos'.");
	        return "redirect:/caja?section=pagos";

	    } catch (Exception e) {
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