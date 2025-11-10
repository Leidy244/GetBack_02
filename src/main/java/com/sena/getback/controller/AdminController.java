package com.sena.getback.controller;

import com.sena.getback.model.*;
import com.sena.getback.repository.*;
import com.sena.getback.service.*;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

	private final MenuService menuService;
	private final CategoriaService categoriaService;
	private final UsuarioService usuarioService;
	private final LocationService locationService;
	private final MesaService mesaService;

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

	@Autowired
	public AdminController(CategoriaRepository categoriaRepository, MenuRepository menuRepository,
			EventoRepository eventoRepository, UsuarioRepository usuarioRepository, UsuarioService usuarioService,
			MenuService menuService, CategoriaService categoriaService, UploadFileService uploadFileService,
			LocationService locationService, LocationRepository locationRepository, MesaService mesaService,
			MesaRepository mesaRepository, RolRepository rolRepository,
			FacturaRepository facturaRepository, PedidoRepository pedidoRepository) {

		this.menuService = menuService;
		this.categoriaService = categoriaService;
		this.usuarioService = usuarioService;
		this.uploadFileService = uploadFileService;
		this.locationService = locationService;
		this.mesaService = mesaService;

		this.categoriaRepository = categoriaRepository;
		this.menuRepository = menuRepository;
		this.eventoRepository = eventoRepository;
		this.usuarioRepository = usuarioRepository;
		this.locationRepository = locationRepository;
		this.mesaRepository = mesaRepository;
		this.rolRepository = rolRepository;
		this.facturaRepository = facturaRepository;
		this.pedidoRepository = pedidoRepository;
	}

	/** ==================== PANEL PRINCIPAL ==================== **/
	@GetMapping
	public String panel(@RequestParam(value = "activeSection", required = false) String activeSection, Model model) {

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
				model.addAttribute("ventasHoy", ventasHoy);
				
				// Ingresos de hoy
				double ingresosHoy = facturas.stream()
					.filter(f -> f.getFechaEmision().toLocalDate().equals(java.time.LocalDate.now()))
					.filter(f -> "PAGADO".equals(f.getEstadoPago()))
					.mapToDouble(f -> f.getTotalPagar().doubleValue())
					.sum();
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
				model.addAttribute("products", menuRepository.findAll());
				model.addAttribute("newProduct", new Menu());
				model.addAttribute("categorias", categoriaRepository.findAll());
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

			// USUARIOS
			if ("users".equals(section)) {
				model.addAttribute("users", usuarioService.findAllUsers());
				model.addAttribute("newUser", new Usuario());
				model.addAttribute("roles", rolRepository.findAll());
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