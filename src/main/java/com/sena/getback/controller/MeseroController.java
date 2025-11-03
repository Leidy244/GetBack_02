package com.sena.getback.controller;

import com.sena.getback.service.MenuService;
import com.sena.getback.model.Usuario;
import com.sena.getback.service.CategoriaService;
import com.sena.getback.service.PedidoService;

import jakarta.servlet.http.HttpSession;

import com.sena.getback.service.MesaService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
public class MeseroController {

	private final MenuService menuService;
	private final CategoriaService categoriaService;
	private final PedidoService pedidoService;
	private final MesaService mesaService;

	public MeseroController(MenuService menuService, CategoriaService categoriaService, PedidoService pedidoService,
			MesaService mesaService) {
		this.menuService = menuService;
		this.categoriaService = categoriaService;
		this.pedidoService = pedidoService;
		this.mesaService = mesaService;

	}

	@GetMapping("/mesero")
	public String mesas(Model model) {
		try {
			var mesas = mesaService.findAll();
			model.addAttribute("mesas", mesas);
		} catch (Exception e) {
			model.addAttribute("error", "Error al cargar las mesas: " + e.getMessage());
		}
		return "mesero/fusionMesas";
	}

	@GetMapping("/mesero/menu")
	public String mesasMenu(@RequestParam(name = "mesa", required = false, defaultValue = "1") Integer mesaId,
			Model model) {
		try {
			var categorias = categoriaService.findAll();
			var productos = menuService.findAll();
			var mesa = mesaService.findById(mesaId);

			model.addAttribute("categorias", categorias);
			model.addAttribute("productos", productos);
			model.addAttribute("mesaId", mesaId);
			model.addAttribute("mesa", mesa);
		} catch (Exception e) {
			model.addAttribute("error", "Error al cargar el menú: " + e.getMessage());
			model.addAttribute("mesaId", mesaId);
		}
		return "mesero/fusionMesero";
	}

	@GetMapping("/verpedido")
	public String verPedidoActual(@RequestParam("mesa") Integer mesaId, Model model) {
		try {
			var pedido = pedidoService.obtenerPedidoActivoPorMesa(mesaId);
			var mesa = mesaService.findById(mesaId);

			model.addAttribute("pedido", pedido);
			model.addAttribute("mesaId", mesaId);
			model.addAttribute("mesa", mesa);

			// Procesar items del pedido si existe
			if (pedido != null && pedido.getComentario() != null && !pedido.getComentario().isEmpty()) {
				try {
					Map<String, Object> itemsMap = procesarItemsPedido(pedido.getComentario());
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
	public String crearPedido(@RequestParam Integer mesaId, @RequestParam String itemsJson,
			@RequestParam(required = false) String comentarios, @RequestParam Double total) {
		try {
			pedidoService.crearPedido(mesaId, itemsJson, comentarios, total);
			return "redirect:/verpedido?mesa=" + mesaId;
		} catch (Exception e) {
			return "redirect:/mesero/menu?mesa=" + mesaId + "&error=" + e.getMessage();
		}
	}

	// Método auxiliar para procesar items del pedido
	private Map<String, Object> procesarItemsPedido(String comentario) {
		Map<String, Object> result = new HashMap<>();
		// Lógica de procesamiento del JSON
		return result;
	}

	// redirige a la vista configuracion
	@GetMapping("/configuracion")
	public String mostrarConfiguracion() {
		return "/mesero/configuracion";
	}

	@GetMapping("/PerfilMesero")
	public String perfilMesero(HttpSession session, Model model) {
		Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

		if (usuario == null) {
			// Si no hay sesión, redirige al login
			return "redirect:/login";
		}

		model.addAttribute("usuario", usuario);
		return "/configuracion";
	}

	@GetMapping("/configuracionMesero")
	public String mostrarConfiguracionMesero() {
		return "configuracion"; // debe coincidir con el nombre del HTML
	}
}
