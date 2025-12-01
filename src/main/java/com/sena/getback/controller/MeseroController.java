package com.sena.getback.controller;

import com.sena.getback.service.MenuService;
import com.sena.getback.model.Usuario;
import com.sena.getback.model.Pedido;
import com.sena.getback.model.Mesa;
import com.sena.getback.repository.UsuarioRepository;
import com.sena.getback.service.CategoriaService;
import com.sena.getback.service.PedidoService;
import com.sena.getback.service.LocationService;
import com.sena.getback.service.InventarioService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import jakarta.servlet.http.HttpSession;
import com.sena.getback.service.MesaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class MeseroController {

	private final MenuService menuService;
	private final CategoriaService categoriaService;
	private final PedidoService pedidoService;
	private final MesaService mesaService;
	private final LocationService locationService;
	private final UsuarioRepository usuarioRepository;
    private final InventarioService inventarioService;

	public MeseroController(MenuService menuService, CategoriaService categoriaService,
			PedidoService pedidoService, MesaService mesaService,
			LocationService locationService, UsuarioRepository usuarioRepository,
			InventarioService inventarioService) {
		this.menuService = menuService;
		this.categoriaService = categoriaService;
		this.pedidoService = pedidoService;
		this.mesaService = mesaService;
		this.locationService = locationService;
		this.usuarioRepository = usuarioRepository;
		this.inventarioService = inventarioService;
	}

    @GetMapping("/mesero")
    public String mesas(Model model, HttpSession session) {
        try {
            // Sincronizar estados de las mesas con los pedidos pendientes/pagados
            pedidoService.sincronizarEstadoMesas();

            var mesas = mesaService.findAll();
            var ubicaciones = locationService.findAll(); // ← AÑADIDO: Carga las ubicaciones
            
            model.addAttribute("mesas", mesas);
            model.addAttribute("ubicaciones", ubicaciones); // ← AÑADIDO: Añade al modelo
            model.addAttribute("totalMesas", mesas.size());
            model.addAttribute("mesasDisponibles", mesas.stream().filter(m -> "DISPONIBLE".equals(m.getEstado())).count());
            model.addAttribute("mesasOcupadas", mesas.stream().filter(m -> "OCUPADA".equals(m.getEstado())).count());
            // Mesas con pedido COMPLETADO (listo para recoger) y no pagado
            // Usamos solo los completados NO marcados como recogidos, para no afectar la vista de caja
            try {
                var completados = pedidoService.obtenerPedidosCompletadosNoRecogidos();
                java.util.Set<Integer> mesasConPedidoListo = new java.util.HashSet<>();
                java.util.Map<Integer, Integer> pedidoListoPorMesa = new java.util.HashMap<>();
                for (var p : completados) {
                    if (p.getMesa() != null && p.getMesa().getId() != null && p.getId() != null) {
                        mesasConPedidoListo.add(p.getMesa().getId());
                        pedidoListoPorMesa.put(p.getMesa().getId(), p.getId());
                    }
                }
                model.addAttribute("mesasConPedidoListo", mesasConPedidoListo);
                model.addAttribute("pedidoListoPorMesa", pedidoListoPorMesa);
            } catch (Exception ignored) {}
            
            Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
            boolean esAdmin = usuario != null && usuario.getRol() != null && usuario.getRol().getNombre() != null
                    && "ADMIN".equalsIgnoreCase(usuario.getRol().getNombre());
            model.addAttribute("esAdmin", esAdmin);
            
            // limpiar borrador al regresar a la vista de mesas
            clearDraft(session);
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar las mesas: " + e.getMessage());
        }
        return "mesero/fusionMesas";
    }


	@GetMapping("/mesero/menu")
	public String mesasMenu(@RequestParam(name = "mesa", required = false, defaultValue = "1") Integer mesaId,
			Model model, HttpSession session) {
		try {
			var categorias = categoriaService.findAll();
			var productos = menuService.findAll();
			var mesaOpt = mesaService.findById(mesaId);
			var mesa = mesaOpt.isPresent() ? mesaOpt.get() : null;

			// Calcular stock para productos del área BAR usando InventarioService
            Map<Long, Integer> stockBarPorProducto = new HashMap<>();
            for (var p : productos) {
                if (p.getCategoria() != null && p.getCategoria().getArea() != null
                        && "BAR".equalsIgnoreCase(p.getCategoria().getArea())) {
                    int stock = p.getStock() != null ? p.getStock() : 0;
                    stockBarPorProducto.put(p.getId(), stock);
                }
            }

			model.addAttribute("categorias", categorias);
			model.addAttribute("productos", productos);
			model.addAttribute("stockBarPorProducto", stockBarPorProducto);
			model.addAttribute("mesaId", mesaId);
			model.addAttribute("mesa", mesa);

			// Cargar borrador si pertenece a esta mesa (para el flujo de "Editar")
			Integer draftMesaId = (Integer) session.getAttribute("draftMesaId");
			String draftItemsJson = (String) session.getAttribute("draftItemsJson");
			String draftComentarios = (String) session.getAttribute("draftComentarios");
			Double draftTotal = (Double) session.getAttribute("draftTotal");

			if (draftMesaId != null && draftMesaId.equals(mesaId) && draftItemsJson != null && draftTotal != null) {
				model.addAttribute("draftItemsJson", draftItemsJson);
				model.addAttribute("draftComentarios", draftComentarios);
				model.addAttribute("draftTotal", draftTotal);
			}
		} catch (Exception e) {
			model.addAttribute("error", "Error al cargar el menú: " + e.getMessage());
			model.addAttribute("mesaId", mesaId);
		}
		return "mesero/fusionMesero";
	}

	@GetMapping("/verpedido")
	public String verPedidoActual(@RequestParam("mesa") Integer mesaId, Model model, HttpSession session) {
		try {
			var pedido = pedidoService.obtenerPedidoActivoPorMesa(mesaId);
			var mesaOpt = mesaService.findById(mesaId);
            var mesa = mesaOpt.isPresent() ? mesaOpt.get() : null;

			model.addAttribute("pedido", pedido);
			model.addAttribute("mesaId", mesaId);
			model.addAttribute("mesa", mesa);

			// Back URL: depende del rol del usuario (admin o mesero)
			Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
			String backUrl = "/mesero"; // valor por defecto (admin - fusionMesas)
			if (usuario != null && usuario.getRol() != null && usuario.getRol().getNombre() != null) {
				String rolNombre = usuario.getRol().getNombre().toUpperCase();
				if ("MESERO".equals(rolNombre)) {
					backUrl = "/mesero/mesas"; // vista fusionMesasMesero
				} else if ("ADMIN".equals(rolNombre)) {
					backUrl = "/mesero"; // vista fusionMesas
				}
			}
			model.addAttribute("backUrl", backUrl);

			// Cargar borrador si existe (tiene prioridad para vista de confirmacion)
			sessionDraft(model, mesaId, session);
			boolean hasDraft = Boolean.TRUE.equals(model.getAttribute("hasDraft"));
			
			if (!hasDraft && pedido != null && pedido.getOrden() != null && !pedido.getOrden().isEmpty()) {
				try {
					Map<String, Object> itemsMap = procesarItemsPedido(pedido.getOrden());
					model.addAttribute("itemsMap", itemsMap);
				} catch (Exception e) {
					model.addAttribute("error", "Error al procesar los items del pedido");
				}
			}

		} catch (Exception e) {
			model.addAttribute("error", "Error al cargar el pedido: " + e.getMessage());
			model.addAttribute("mesaId", mesaId);
		}
		return "mesero/ver_pedido_actual";
	}

	@PostMapping("/pedidos/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearPedido(@RequestParam Integer mesaId,
                                    @RequestParam String itemsJson,
                                    @RequestParam(required = false) String comentarios,
                                    @RequestParam Double total) {
        Map<String, Object> res = new HashMap<>();
        try {
            var pedido = pedidoService.crearPedido(mesaId, itemsJson, comentarios, total);
            res.put("success", true);
            res.put("message", "Pedido creado");
            res.put("pedidoId", pedido != null ? pedido.getId() : null);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @GetMapping("/mesero/pedido/resumen")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerResumenPedidoMesa(@RequestParam("mesaId") Integer mesaId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Mesa mesa = mesaService.findById(mesaId).orElse(null);
            Pedido pedidoActivo = pedidoService.obtenerPedidoActivoPorMesa(mesaId);

            res.put("mesaId", mesaId);
            res.put("mesaNumero", mesa != null ? mesa.getNumero() : null);

            if (pedidoActivo != null) {
                res.put("tienePedidoActivo", true);
                res.put("pedidoActivoId", pedidoActivo.getId());
                res.put("total", pedidoActivo.getTotal());
                res.put("fechaCreacion", pedidoActivo.getFechaCreacion());

                String ordenRaw = pedidoActivo.getOrden();
                if (ordenRaw != null && !ordenRaw.trim().isEmpty()) {
                    res.put("ordenJson", ordenRaw);
                } else {
                    res.put("ordenJson", null);
                }
            } else {
                res.put("tienePedidoActivo", false);
                res.put("ordenJson", null);
            }

            // Historial ligero para evitar grafos enormes de Hibernate
            List<Pedido> historial = pedidoService.obtenerHistorialPedidosPorMesa(mesaId);
            List<Map<String, Object>> historialLigero = new ArrayList<>();
            if (historial != null) {
                for (Pedido p : historial) {
                    Map<String, Object> h = new HashMap<>();
                    h.put("id", p.getId());
                    h.put("total", p.getTotal());
                    h.put("fechaCreacion", p.getFechaCreacion());
                    historialLigero.add(h);
                }
            }
            res.put("historial", historialLigero);

            res.put("success", true);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    // Guarda borrador en sesión para confirmar en vista
    @PostMapping("/pedidos/preparar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> prepararPedido(HttpSession session,
                                      @RequestParam Integer mesaId,
                                      @RequestParam String itemsJson,
                                      @RequestParam(required = false) String comentarios,
                                      @RequestParam Double total) {
        Map<String, Object> res = new HashMap<>();
        session.setAttribute("draftMesaId", mesaId);
        session.setAttribute("draftItemsJson", itemsJson);
        session.setAttribute("draftComentarios", comentarios);
        session.setAttribute("draftTotal", total);
        res.put("success", true);
        res.put("redirect", "/verpedido?mesa=" + mesaId);
        return ResponseEntity.ok(res);
    }

    // Confirmar y persistir pedido desde borrador
    @PostMapping("/pedidos/confirmar")
    public String confirmarPedido(HttpSession session,
                                  @RequestParam(required = false) Integer mesaId,
                                  @RequestParam(required = false) String itemsJson,
                                  @RequestParam(required = false) String comentarios,
                                  @RequestParam(required = false) Double total) {
        if (mesaId == null) mesaId = (Integer) session.getAttribute("draftMesaId");
        // Si itemsJson llega nulo O vacío desde el formulario, reutilizar el borrador en sesión
        if (itemsJson == null || itemsJson.trim().isEmpty()) {
            itemsJson = (String) session.getAttribute("draftItemsJson");
        }
        if (comentarios == null) comentarios = (String) session.getAttribute("draftComentarios");
        if (total == null) total = (Double) session.getAttribute("draftTotal");

        System.out.println("=== CONFIRMANDO PEDIDO ===");
        System.out.println("Mesa ID: " + mesaId);
        System.out.println("Items JSON: " + itemsJson);
        System.out.println("Comentarios: " + comentarios);
        System.out.println("Total: " + total);

        // Validar datos críticos mínimos antes de crear el pedido
        // Solo mesaId y total son estrictamente obligatorios; itemsJson puede ser null y
        // PedidoService.crearPedido se encargará de normalizarlo si llega vacío.
        if (mesaId == null || total == null) {
            System.out.println("ERROR: Datos incompletos en sesión (mesaId o total nulos)");
            return "redirect:/mesero/menu?mesa=" + (mesaId != null ? mesaId : 1) + "&error=Borrador incompleto";
        }

        try {
            Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
            Pedido pedidoCreado = pedidoService.crearPedido(mesaId, itemsJson, comentarios, total, usuario);
            System.out.println("ÉXITO: Pedido creado con ID: " + pedidoCreado.getId());
            
            // limpiar borrador
            clearDraft(session);
            
            // Redireccionar a la vista de Mesas del mesero
            return "redirect:/mesero";
        } catch (Exception e) {
            System.out.println("ERROR al crear pedido: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/mesero/menu?mesa=" + mesaId + "&error=" + e.getMessage();
        }
    }

    @PostMapping("/mesero/pedido/recogido")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcarPedidoRecogido(@RequestParam Integer pedidoId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            pedidoService.marcarPedidoComoRecogido(pedidoId);
            resp.put("success", true);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
        }
        return ResponseEntity.ok(resp);
    }

    // Cargar un pedido existente como borrador para editarlo en fusionMesero
	@PostMapping("/pedidos/editar")
	public String editarPedido(@RequestParam Integer mesaId, HttpSession session) {
	    Pedido pedido = pedidoService.obtenerPedidoActivoPorMesa(mesaId);
	    if (pedido != null) {
	        session.setAttribute("draftMesaId", mesaId);
	        session.setAttribute("draftItemsJson", pedido.getOrden());
	        String draftComentarios = null;
	        try {
	            // 1) Intentar usar el campo dedicado en la entidad
	            if (pedido.getComentariosGenerales() != null && !pedido.getComentariosGenerales().trim().isEmpty()) {
	                draftComentarios = pedido.getComentariosGenerales().trim();
	            } else if (pedido.getOrden() != null && !pedido.getOrden().isEmpty()) {
	                // 2) Si no hay en la entidad, intentar leer desde el JSON almacenado en orden
	                ObjectMapper mapper = new ObjectMapper();
	                Map<String, Object> json = mapper.readValue(pedido.getOrden(), Map.class);
	                Object c = json.get("comentarios");
	                if (c instanceof String && !((String) c).trim().isEmpty()) {
	                    draftComentarios = ((String) c).trim();
	                }
	            }
	        } catch (Exception ignored) {}
	        session.setAttribute("draftComentarios", draftComentarios);
	        session.setAttribute("draftTotal", pedido.getTotal());
	    }
	    return "redirect:/mesero/menu?mesa=" + mesaId;
	}

    // Carga borrador en el modelo si existe
    private void sessionDraft(Model model, Integer mesaId, HttpSession session) {
        Integer draftMesaId = (Integer) session.getAttribute("draftMesaId");
        String draftItemsJson = (String) session.getAttribute("draftItemsJson");
        String draftComentarios = (String) session.getAttribute("draftComentarios");
        Double draftTotal = (Double) session.getAttribute("draftTotal");

        if (draftMesaId != null && draftItemsJson != null && draftTotal != null) {
            try {
                Map<String, Object> itemsMap = procesarItemsPedido(draftItemsJson);
                var mesaOpt = mesaService.findById(mesaId);
                var mesa = mesaOpt.isPresent() ? mesaOpt.get() : null;
                model.addAttribute("mesa", mesa);
                model.addAttribute("itemsMap", itemsMap);
                model.addAttribute("draftTotal", draftTotal);
                model.addAttribute("draftComentarios", draftComentarios);
                model.addAttribute("hasDraft", true);
            } catch (Exception e) {
                model.addAttribute("error", "Error al procesar los items del pedido");
            }
        }
    }

	private void clearDraft(HttpSession session) {
	    session.removeAttribute("draftMesaId");
	    session.removeAttribute("draftItemsJson");
	    session.removeAttribute("draftComentarios");
	    session.removeAttribute("draftTotal");
	}

	@GetMapping("/pedidos/cancelar")
	public String cancelarPedido(HttpSession session) {
	    clearDraft(session);
	    return "redirect:/mesero";
	}

	private Map<String, Object> procesarItemsPedido(String itemsJson) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = mapper.readValue(itemsJson, Map.class);
        return map;
    }

    @GetMapping("/historial")
    public String mostrarHistorialPedidos(
	        @RequestParam(name = "page", defaultValue = "0") int page,
	        @RequestParam(name = "size", defaultValue = "5") int size,
	        @RequestParam(name = "mesa", required = false) String mesa,
	        @RequestParam(name = "estado", required = false) String estado,
	        @RequestParam(name = "meseroId", required = false) Long meseroId,
	        @RequestParam(name = "fechaDesde", required = false)
	        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
	        @RequestParam(name = "fechaHasta", required = false)
	        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
	        Model model,
	        HttpSession session) {

	    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
	    String rol = usuario != null && usuario.getRol() != null && usuario.getRol().getNombre() != null
	            ? usuario.getRol().getNombre().toUpperCase() : "";

	    boolean esAdmin = "ADMIN".equals(rol);
	    boolean esMesero = "MESERO".equals(rol);

	    Long usuarioIdFiltro = null;
	    if (esMesero && usuario != null) {
	        // Mesero solo ve su propio historial
	        usuarioIdFiltro = usuario.getId();
	    } else if (esAdmin) {
	        // Admin puede filtrar por cualquier mesero
	        usuarioIdFiltro = meseroId;
	    }

	    Page<Pedido> pagePedidos = pedidoService.obtenerHistorialPaginado(
	            mesa, estado, fechaDesde, fechaHasta, usuarioIdFiltro, page, size);

	    model.addAttribute("pedidos", pagePedidos.getContent());
	    model.addAttribute("currentPage", pagePedidos.getNumber());
	    model.addAttribute("totalPages", pagePedidos.getTotalPages());
	    model.addAttribute("pageSize", pagePedidos.getSize());
	    model.addAttribute("totalElements", pagePedidos.getTotalElements());

	    model.addAttribute("mesaFiltro", mesa);
	    model.addAttribute("estadoFiltro", estado);
	    model.addAttribute("fechaDesdeFiltro", fechaDesde);
	    model.addAttribute("fechaHastaFiltro", fechaHasta);
	    model.addAttribute("meseroFiltro", meseroId);
	    model.addAttribute("esAdmin", esAdmin);
	    model.addAttribute("esMesero", esMesero);

	    if (esAdmin) {
	        model.addAttribute("meseros", usuarioRepository.findByRol_Nombre("MESERO"));
	    }

        return "mesero/historial";
    }

    @PostMapping("/mesero/pedidos/confirmar")
    public String confirmarPedidoPendiente(@RequestParam("pedidoId") Integer pedidoId,
                                           HttpSession session) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
            Pedido p = pedidoService.findById(pedidoId);
            if (p != null && p.getUsuario() != null && usuario != null &&
                p.getUsuario().getId() != null && usuario.getId() != null &&
                p.getUsuario().getId().longValue() == usuario.getId().longValue()) {
                // Marcar como COMPLETADO (flujo de BAR/CAJA existente)
                pedidoService.marcarPedidoComoCompletadoBar(pedidoId);
            }
        } catch (Exception ignored) {}
        // Volver al historial filtrado en pendientes para visualizar el cambio
        return "redirect:/historial?estado=PENDIENTE";
    }

	@GetMapping("/configuracion")
	public String mostrarConfiguracion() {
		return "mesero/configuracion";
	}
}
