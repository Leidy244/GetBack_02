package com.sena.getback.controller;

import com.sena.getback.model.Estado;
import com.sena.getback.model.Factura;
import com.sena.getback.model.Mesa;
import com.sena.getback.model.Pedido;
import com.sena.getback.model.Usuario;
import com.sena.getback.repository.FacturaRepository;
import com.sena.getback.repository.PedidoRepository;
import com.sena.getback.repository.UsuarioRepository;
import com.sena.getback.service.MenuService;
import com.sena.getback.service.MesaService;
import com.sena.getback.service.CategoriaService;
import com.sena.getback.service.FacturaService;
import com.sena.getback.service.PedidoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.sena.getback.repository.ClienteFrecuenteRepository;
import com.sena.getback.repository.EstadoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

import java.util.Comparator;
import java.util.List;
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
	@PostMapping("/punto-venta/registrar")
	public String registrarVentaRapida(@RequestParam("total") Double total,
			@RequestParam("montoRecibido") Double montoRecibido,
			@RequestParam(value = "mesaId", required = false) Integer mesaId,
			@RequestParam(value = "metodoPago", defaultValue = "EFECTIVO") String metodoPago,
			@RequestParam("carrito") String carrito, HttpSession session, RedirectAttributes ra) {

		try {
			System.out.println("=== DATOS RECIBIDOS ===");
			System.out.println("Total: " + total);
			System.out.println("Monto recibido: " + montoRecibido);
			System.out.println("Mesa ID: " + mesaId);
			System.out.println("Método pago: " + metodoPago);
			System.out.println("Carrito: " + carrito);

			// OBTENER USUARIO DESDE LA SESIÓN HTTP
			Usuario cajero = (Usuario) session.getAttribute("usuarioLogueado");

			if (cajero == null) {
				ra.addFlashAttribute("mensajeError", "Usuario no autenticado. Por favor inicie sesión.");
				return "redirect:/caja?section=punto-venta";
			}

			System.out.println("Cajero desde sesión: " + cajero.getNombre() + " " + cajero.getApellido());

			// Validar datos esenciales
			if (total == null || total <= 0) {
				ra.addFlashAttribute("mensajeError", "Total inválido");
				return "redirect:/caja?section=punto-venta";
			}

			if (montoRecibido == null || montoRecibido < total) {
				ra.addFlashAttribute("mensajeError", "Monto recibido insuficiente");
				return "redirect:/caja?section=punto-venta";
			}

			// 1. Crear el Pedido
			Pedido pedido = new Pedido();
			pedido.setTotal(total);
			pedido.setMontoRecibido(montoRecibido);
			pedido.setCambio(montoRecibido - total);
			pedido.setComentariosGenerales("Venta rápida - POS");
			pedido.setFechaCreacion(LocalDateTime.now());
			pedido.setUsuario(cajero); // ASIGNAR EL CAJERO DESDE LA SESIÓN

			// Estado PAGADO - CÓDIGO CORREGIDO
			// SOLUCIÓN 1: Buscar manualmente en la lista de estados
			List<Estado> todosEstados = estadoRepository.findAll();
			Estado estadoPagado = todosEstados.stream().filter(estado -> "PAGADO".equals(estado.getNombreEstado()))
					.findFirst().orElse(null);

			// Si no existe estado PAGADO → crearlo
			if (estadoPagado == null) {
			    estadoPagado = new Estado();
			    estadoPagado.setNombreEstado("PAGADO");
			    estadoPagado = estadoRepository.save(estadoPagado);
			    System.out.println("✅ Estado PAGADO creado con ID: " + estadoPagado.getId());
			}

			// 💥 SIEMPRE asignar el estado al pedido (esté dentro o fuera del IF)
			pedido.setEstado(estadoPagado);

			// Mesa opcional
			if (mesaId != null && mesaId > 0) {
			    Mesa mesa = mesaService.findById(mesaId).orElse(null);
			    pedido.setMesa(mesa);
			}

			// Guardar carrito JSON
			if (carrito != null && !carrito.trim().isEmpty()) {
			    pedido.setOrden(carrito);
			} else {
			    pedido.setOrden("[]");
			}

			pedido = pedidoRepository.save(pedido);

			// ------------------
			//  CREAR FACTURA
			// ------------------
			Factura factura = new Factura();
			factura.setPedido(pedido);
			factura.setNumeroFactura("POS-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
			factura.setTotalPagar(BigDecimal.valueOf(total));
			factura.setSubtotal(BigDecimal.valueOf(total));
			factura.setMonto(BigDecimal.valueOf(total));
			factura.setMetodoPago(metodoPago);
			factura.setEstadoPago("PAGADO");
			factura.setEstadoFactura("GENERADA");
			factura.setFechaEmision(LocalDateTime.now());
			factura.setFechaPago(LocalDateTime.now());
			factura.setUsuario(cajero);

			if (mesaId != null && mesaId > 0) {
			    factura.setNumeroMesa(mesaId);
			}

			facturaRepository.save(factura);

			ra.addFlashAttribute("mensajeExito",
			        "Venta rápida registrada → Factura: " + factura.getNumeroFactura()
			                + " | Cajero: " + cajero.getNombre() + " " + cajero.getApellido());

			System.out.println("Venta registrada exitosamente: " + factura.getNumeroFactura());
			System.out.println("Cajero: " + cajero.getNombre() + " " + cajero.getApellido());

		} catch (Exception e) {
			e.printStackTrace();
			ra.addFlashAttribute("mensajeError", "Error al registrar venta rápida: " + e.getMessage());
		}

		return "redirect:/caja?section=punto-venta";
	}
}