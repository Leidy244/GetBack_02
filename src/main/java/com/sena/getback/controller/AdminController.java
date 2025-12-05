package com.sena.getback.controller;

import com.sena.getback.model.*;
import com.sena.getback.repository.*;
import com.sena.getback.service.*;
import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controlador para la administración del sistema.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

	private final MenuService menuService;
	private final CategoriaService categoriaService;
	private final UsuarioService usuarioService;
	private final LocationService locationService;
	private final MesaService mesaService;
	private final InventarioService inventarioService;

	@Autowired
	private UploadFileService uploadFileService;

	private final CategoriaRepository categoriaRepository;

	private final MenuRepository menuRepository;
	private final EventoRepository eventoRepository;
	private final UsuarioRepository usuarioRepository;
	private final LocationRepository locationRepository;
	private final MesaRepository mesaRepository;
	private final RolRepository rolRepository;
	private final FacturaRepository facturaRepository;
	private final PedidoRepository pedidoRepository;
	private final ClienteFrecuenteRepository clienteFrecuenteRepository;
	private final MovimientoCreditoRepository movimientoCreditoRepository;

	
	
	@Autowired
	public AdminController(CategoriaRepository categoriaRepository, MenuRepository menuRepository,
			EventoRepository eventoRepository, UsuarioRepository usuarioRepository, UsuarioService usuarioService,
			MenuService menuService, CategoriaService categoriaService, UploadFileService uploadFileService,
			LocationService locationService, LocationRepository locationRepository, MesaService mesaService,
			MesaRepository mesaRepository, RolRepository rolRepository, InventarioService inventarioService,
			FacturaRepository facturaRepository, PedidoRepository pedidoRepository,
			ClienteFrecuenteRepository clienteFrecuenteRepository,
			MovimientoCreditoRepository movimientoCreditoRepository) {

		this.menuService = menuService;
		this.categoriaService = categoriaService;
		this.usuarioService = usuarioService;
		this.uploadFileService = uploadFileService;
		this.locationService = locationService;
		this.mesaService = mesaService;
		this.inventarioService = inventarioService;

		this.categoriaRepository = categoriaRepository;
		this.menuRepository = menuRepository;
		this.eventoRepository = eventoRepository;
		this.usuarioRepository = usuarioRepository;
		this.locationRepository = locationRepository;
		this.mesaRepository = mesaRepository;
		this.rolRepository = rolRepository;
		this.facturaRepository = facturaRepository;
		this.pedidoRepository = pedidoRepository;
		this.clienteFrecuenteRepository = clienteFrecuenteRepository;
		this.movimientoCreditoRepository = movimientoCreditoRepository;
	}
	

	/** ==================== CLIENTES FRECUENTES ==================== **/

	@PostMapping("/clientes/guardar")
	public String guardarClienteFrecuente(@RequestParam(required = false) Long id,
			@RequestParam String nombre,
			@RequestParam(required = false) String documento,
			@RequestParam(required = false) String telefono,
			@RequestParam(required = false) String nota,
			RedirectAttributes redirectAttributes) {

		try {
			ClienteFrecuente cliente;
			if (id != null) {
				cliente = clienteFrecuenteRepository.findById(id).orElse(new ClienteFrecuente());
			} else {
				cliente = new ClienteFrecuente();
			}

			cliente.setNombre(nombre);
			cliente.setDocumento(documento);
			cliente.setTelefono(telefono);
			cliente.setNota(nota);

			clienteFrecuenteRepository.save(cliente);
			redirectAttributes.addFlashAttribute("success", "Cliente frecuente guardado correctamente");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al guardar cliente frecuente: " + e.getMessage());
		}

		return "redirect:/admin?activeSection=clientes";
	}

	@PostMapping("/clientes/abono")
	public String registrarAbonoCliente(@RequestParam("clienteId") Long clienteId,
			@RequestParam("monto") Double monto,
			@RequestParam(value = "descripcion", required = false) String descripcion,
			RedirectAttributes redirectAttributes) {

		if (clienteId == null || monto == null || monto <= 0) {
			redirectAttributes.addFlashAttribute("error", "Monto de abono inválido");
			return "redirect:/admin?activeSection=clientes";
		}

		try {
			ClienteFrecuente cliente = clienteFrecuenteRepository.findById(clienteId).orElse(null);
			if (cliente == null) {
				redirectAttributes.addFlashAttribute("error", "Cliente no encontrado");
				return "redirect:/admin?activeSection=clientes";
			}

			// Actualizar saldo
			Double saldoActual = cliente.getSaldo() != null ? cliente.getSaldo() : 0.0;
			cliente.setSaldo(saldoActual + monto);
			clienteFrecuenteRepository.save(cliente);

			// Registrar movimiento
			MovimientoCredito mov = new MovimientoCredito();
			mov.setCliente(cliente);
			mov.setTipo("ABONO");
			mov.setMonto(monto);
			mov.setDescripcion(descripcion != null ? descripcion : "Abono a favor del cliente");
			movimientoCreditoRepository.save(mov);

			redirectAttributes.addFlashAttribute("success", "Abono registrado correctamente");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al registrar abono: " + e.getMessage());
		}

		return "redirect:/admin?activeSection=clientes";
	}

	@PostMapping("/clientes/consumo")
	public String registrarConsumoCliente(@RequestParam("clienteId") Long clienteId,
	                                      @RequestParam("monto") Double monto,
	                                      @RequestParam(value = "descripcion", required = false) String descripcion,
	                                      @RequestParam(value = "fromCaja", required = false) Boolean fromCaja,
	                                      RedirectAttributes redirectAttributes) {

	    if (clienteId == null || monto == null || monto <= 0) {
	        redirectAttributes.addFlashAttribute("error", "Monto de consumo inválido");
	        // si vino de caja, vuelve a caja; si no, a admin
	        return (fromCaja != null && fromCaja)
	                ? "redirect:/caja?section=caja"
	                : "redirect:/admin?activeSection=clientes";
	    }

	    try {
	        ClienteFrecuente cliente = clienteFrecuenteRepository.findById(clienteId).orElse(null);
	        if (cliente == null) {
	            redirectAttributes.addFlashAttribute("error", "Cliente no encontrado");
	            return (fromCaja != null && fromCaja)
	                    ? "redirect:/caja?section=caja"
	                    : "redirect:/admin?activeSection=clientes";
	        }

	        Double saldoActual = cliente.getSaldo() != null ? cliente.getSaldo() : 0.0;
	        cliente.setSaldo(saldoActual - monto);
	        clienteFrecuenteRepository.save(cliente);

	        MovimientoCredito mov = new MovimientoCredito();
	        mov.setCliente(cliente);
	        mov.setTipo("CONSUMO");
	        mov.setMonto(monto);
	        mov.setDescripcion(descripcion != null ? descripcion : "Consumo a crédito del cliente");
	        movimientoCreditoRepository.save(mov);

	        redirectAttributes.addFlashAttribute("success", "Consumo registrado correctamente");
	    } catch (Exception e) {
	        redirectAttributes.addFlashAttribute("error", "Error al registrar consumo: " + e.getMessage());
	    }

	    // Redirigir según el origen
	    return (fromCaja != null && fromCaja)
	            ? "redirect:/caja?section=caja"
	            : "redirect:/admin?activeSection=clientes";
	}
	
	@Transactional
	@PostMapping("/clientes/eliminar")
	public String eliminarClienteFrecuente(@RequestParam("clienteId") Long clienteId,
	                                       RedirectAttributes redirectAttributes) {

	    if (clienteId == null) {
	        redirectAttributes.addFlashAttribute("error", "ID de cliente inválido");
	        return "redirect:/admin?activeSection=clientes";
	    }

	    try {
	        // 1) Borrar movimientos del cliente
	        movimientoCreditoRepository.deleteByCliente_Id(clienteId);

	        // 2) Borrar cliente
	        clienteFrecuenteRepository.deleteById(clienteId);

	        redirectAttributes.addFlashAttribute("success", "Cliente frecuente eliminado correctamente");
	    } catch (Exception e) {
	        redirectAttributes.addFlashAttribute("error", "Error al eliminar cliente frecuente: " + e.getMessage());
	    }

	    return "redirect:/admin?activeSection=clientes";
	}

	/** ==================== PANEL PRINCIPAL ==================== **/

    @GetMapping
    public String panel(@RequestParam(value = "activeSection", required = false) String activeSection, Model model, jakarta.servlet.http.HttpSession session) {

		String section = (activeSection != null && !activeSection.isEmpty()) ? activeSection : "dashboard";
		model.addAttribute("activeSection", section);
		model.addAttribute("title", "Panel de Administración");

		try {
			// DASHBOARD
			if ("dashboard".equals(section)) {
				model.addAttribute("totalCategorias", (long) categoriaRepository.findAll().size());
				model.addAttribute("totalProductos", (long) menuRepository.findAll().size());
				model.addAttribute("totalEventos", (long) eventoRepository.findAll().size());
				model.addAttribute("totalUsuarios", (long) usuarioRepository.findAll().size());
				model.addAttribute("totalUbicaciones", (long) locationRepository.findAll().size());
				model.addAttribute("totalMesas", (long) mesaRepository.findAll().size());

				// NUEVAS ESTADÍSTICAS PARA EL DASHBOARD MEJORADO
				java.util.List<Factura> facturas = facturaRepository.findAll();
				java.util.List<Pedido> pedidos = pedidoRepository.findAll();

				model.addAttribute("totalVentas", (long) facturas.size());

				// Ventas de hoy
                long ventasHoy = facturas.stream()
                    .filter(f -> f.getFechaEmision().toLocalDate().equals(java.time.LocalDate.now()))
                    .count();
                double ingresosHoy = facturas.stream()
                    .filter(f -> f.getFechaEmision().toLocalDate().equals(java.time.LocalDate.now()))
                    .filter(f -> "PAGADO".equals(f.getEstadoPago()))
                    .mapToDouble(f -> f.getTotalPagar().doubleValue())
                    .sum();
                Object estadoCaja = session != null ? session.getAttribute("cajaEstado") : null;
                boolean abiertaCaja = false;
                if (estadoCaja instanceof java.util.Map<?, ?> map) {
                    Object abiertaVal = map.get("abierta");
                    abiertaCaja = (abiertaVal instanceof Boolean) ? (Boolean) abiertaVal : false;
                }
                if (!abiertaCaja) {
                    ventasHoy = 0;
                    ingresosHoy = 0.0;
                }
                model.addAttribute("ventasHoy", ventasHoy);

				// Ingresos de hoy
                model.addAttribute("ingresosHoy", ingresosHoy);

				// Ingresos totales
				double ingresosTotales = facturas.stream()
					.filter(f -> "PAGADO".equals(f.getEstadoPago()))
					.mapToDouble(f -> f.getTotalPagar().doubleValue())
					.sum();
				model.addAttribute("ingresosTotales", ingresosTotales);

				// Pedidos pendientes
				long pedidosPendientes = pedidos.stream()
					.filter(p -> "PENDIENTE".equals(p.getEstado()))
					.count();
				model.addAttribute("pedidosPendientes", pedidosPendientes);

				// Actividad reciente (últimas 5 facturas)
				java.util.List<Factura> actividadReciente = facturas.stream()
					.sorted((f1, f2) -> f2.getFechaEmision().compareTo(f1.getFechaEmision()))
					.limit(5)
					.collect(java.util.stream.Collectors.toList());
				model.addAttribute("actividadReciente", actividadReciente);

				// Resumen de stock para el panel de control
				int stockThreshold = 5;
				model.addAttribute("stockPorProducto", inventarioService.calcularStockPorProducto());
				model.addAttribute("bajoStock", inventarioService.obtenerProductosBajoStock(stockThreshold));
				model.addAttribute("stockThreshold", stockThreshold);
			}

			// LOCATIONS
			if ("locations".equals(section)) {
				model.addAttribute("locations", locationRepository.findAll());
				model.addAttribute("totalUbicaciones", (long) locationRepository.findAll().size());
				model.addAttribute("location",
						model.containsAttribute("location") ? model.getAttribute("location") : new Location());
			}

			// MESAS
			if ("mesas".equals(section)) {
				model.addAttribute("mesas", mesaRepository.findAll());
				model.addAttribute("totalMesas", (long) mesaRepository.findAll().size());
				model.addAttribute("ubicaciones", locationRepository.findAll());
				model.addAttribute("mesa", model.containsAttribute("mesa") ? model.getAttribute("mesa") : new Mesa());
			}

			// PRODUCTOS
            if ("products".equals(section)) {
                // Mostrar TODOS los productos del menú (Cocina y Bar)
				java.util.List<com.sena.getback.model.Menu> productos = menuRepository.findAll();
				model.addAttribute("products", productos);
				model.addAttribute("newProduct", new Menu());
				// Proveer TODAS las categorías para el select del formulario (no solo Bar)
				java.util.List<com.sena.getback.model.Categoria> categorias = categoriaRepository.findAll();
				model.addAttribute("categorias", categorias);
				// Stock calculado (seguimos usándolo para el badge, principalmente para productos de Bar)
				model.addAttribute("stockPorProducto", inventarioService.calcularStockPorProducto());
				model.addAttribute("nombresInventario", inventarioService.listarNombresProductosInventario());
				// Umbral para badges de stock
				model.addAttribute("stockThreshold", 5);

				// Para la sección products, mostrar SOLO los nombres del menú (no incluir nombres desde inventario)
				java.util.List<String> menuNombres = new java.util.ArrayList<>();
				menuRepository.findAll().forEach(m -> {
					if (m.getNombreProducto() != null && !m.getNombreProducto().isBlank()) {
						menuNombres.add(m.getNombreProducto().trim());
            }
				});
				model.addAttribute("inventarioNombres", menuNombres);
			}

			// CATEGORÍAS
			if ("categories".equals(section)) {
				model.addAttribute("categorias", categoriaRepository.findAll());
				model.addAttribute("categoria", new Categoria());
			}

			// EVENTOS
			if ("events".equals(section)) {
				model.addAttribute("eventos", eventoRepository.findAll());
				model.addAttribute("evento", new Evento());
			}

            // CLIENTES FRECUENTES
            if ("clientes".equals(section)) {
                java.util.List<ClienteFrecuente> clientes = clienteFrecuenteRepository.findAll();
                model.addAttribute("clientes", clientes);
                model.addAttribute("nuevoCliente", new ClienteFrecuente());

                java.util.Map<Long, java.util.List<MovimientoCredito>> movimientosPorCliente = new java.util.HashMap<>();
                for (ClienteFrecuente c : clientes) {
                    try {
                        java.util.List<MovimientoCredito> movs = movimientoCreditoRepository.findByClienteOrderByFechaDesc(c);
                        movimientosPorCliente.put(c.getId(), movs);
                    } catch (Exception ignored) {}
                }
                model.addAttribute("movimientosPorCliente", movimientosPorCliente);

                double saldoThreshold = 20000.0;
                java.util.List<ClienteFrecuente> clientesSaldoBajo = new java.util.ArrayList<>();
                for (ClienteFrecuente c : clientes) {
                    Double s = c.getSaldo();
                    if (s != null && s <= saldoThreshold) {
                        clientesSaldoBajo.add(c);
                    }
                }
                model.addAttribute("saldoThreshold", saldoThreshold);
                model.addAttribute("clientesSaldoBajo", clientesSaldoBajo);
            }

			// USUARIOS
			if ("users".equals(section)) {
				model.addAttribute("users", usuarioService.findAllUsers());
				model.addAttribute("newUser", new Usuario());
				model.addAttribute("roles", rolRepository.findAll());
			}

			// ESTADISTICAS
			if ("estadisticas".equals(section)) {
				int stockThreshold = 5;
				model.addAttribute("stockPorProducto", inventarioService.calcularStockPorProducto());
				model.addAttribute("bajoStock", inventarioService.obtenerProductosBajoStock(stockThreshold));
				model.addAttribute("stockThreshold", stockThreshold);
			}

			// VENTAS - NUEVA SECCIÓN
			if ("ventas".equals(section)) {
				java.util.List<Factura> facturas = facturaRepository.findAll();
				model.addAttribute("facturas", facturas);
				model.addAttribute("totalVentas", (long) facturas.size());

				// Calcular estadísticas básicas sin queries
                long ventasHoy = facturas.stream()
                    .filter(f -> f.getFechaEmision().toLocalDate().equals(java.time.LocalDate.now()))
                    .count();
                Object estadoCajaVentas = session != null ? session.getAttribute("cajaEstado") : null;
                boolean abiertaCajaVentas = false;
                if (estadoCajaVentas instanceof java.util.Map<?, ?> map2) {
                    Object abiertaVal2 = map2.get("abierta");
                    abiertaCajaVentas = (abiertaVal2 instanceof Boolean) ? (Boolean) abiertaVal2 : false;
                }
                if (!abiertaCajaVentas) {
                    ventasHoy = 0;
                }
                model.addAttribute("ventasHoy", ventasHoy);

				long ventasCanceladas = facturas.stream()
					.filter(f -> "CANCELADO".equals(f.getEstadoPago()))
					.count();
				model.addAttribute("ventasCanceladas", ventasCanceladas);

				double totalIngresos = facturas.stream()
					.filter(f -> "PAGADO".equals(f.getEstadoPago()))
					.mapToDouble(f -> f.getTotalPagar().doubleValue())
					.sum();
				model.addAttribute("totalIngresos", totalIngresos);

				double promedioVenta = ventasHoy > 0 ? totalIngresos / ventasHoy : 0.0;
				model.addAttribute("promedioVenta", promedioVenta);
			}

			// PERFIL ADMIN
			Usuario admin = usuarioService.getFirstUser().orElse(new Usuario());
			model.addAttribute("usuario", admin);

		} catch (Exception e) {
			System.err.println("❌ Error cargando datos: " + e.getMessage());
			model.addAttribute("categorias", java.util.Collections.emptyList());
			model.addAttribute("products", java.util.Collections.emptyList());
			model.addAttribute("eventos", java.util.Collections.emptyList());
			model.addAttribute("users", java.util.Collections.emptyList());
			model.addAttribute("locations", java.util.Collections.emptyList());
			model.addAttribute("mesas", java.util.Collections.emptyList());
			model.addAttribute("roles", java.util.Collections.emptyList());
			model.addAttribute("facturas", java.util.Collections.emptyList());
			model.addAttribute("actividadReciente", java.util.Collections.emptyList());

			model.addAttribute("newProduct", new Menu());
			model.addAttribute("categoria", new Categoria());
			model.addAttribute("evento", new Evento());
			model.addAttribute("location", new Location());
			model.addAttribute("mesa", new Mesa());
			model.addAttribute("usuario", new Usuario());
			model.addAttribute("newUser", new Usuario());

			// AGREGAR ESTADÍSTICAS POR DEFECTO PARA VENTAS Y DASHBOARD
			model.addAttribute("totalVentas", 0L);
			model.addAttribute("ventasHoy", 0L);
			model.addAttribute("ingresosHoy", 0.0);
			model.addAttribute("ingresosTotales", 0.0);
			model.addAttribute("pedidosPendientes", 0L);
			model.addAttribute("ventasCanceladas", 0L);
			model.addAttribute("totalIngresos", 0.0);
			model.addAttribute("promedioVenta", 0.0);
		}

		return "admin";
	}

}
