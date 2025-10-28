package com.sena.getback.controller;

import com.sena.getback.service.MenuService;
import com.sena.getback.model.Usuario;
import com.sena.getback.repository.UsuarioRepository;
import com.sena.getback.service.CategoriaService;
import com.sena.getback.service.PedidoService;
import com.sena.getback.service.UploadFileService;
import com.sena.getback.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

import com.sena.getback.service.MesaService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MeseroController {

	private final MenuService menuService;
	private final CategoriaService categoriaService;
	private final PedidoService pedidoService;
	private final MesaService mesaService;
	private final UsuarioService usuarioService;
	private final UsuarioRepository usuarioRepository;
	@Autowired
	private UploadFileService uploadFileService;

	public MeseroController(MenuService menuService, CategoriaService categoriaService, PedidoService pedidoService,
			MesaService mesaService, UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
		this.menuService = menuService;
		this.categoriaService = categoriaService;
		this.pedidoService = pedidoService;
		this.mesaService = mesaService;
		this.usuarioService = usuarioService;
		this.usuarioRepository = usuarioRepository;
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

	@PostMapping("/actualizar-datos-mesero")
	public String actualizarPerfilMesero(@ModelAttribute Usuario adminActualizado, HttpSession session,
			RedirectAttributes redirectAttrs) {
		try {
			// Obtener el usuario logueado desde la sesión
			Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
			if (usuarioLogueado == null) {
				redirectAttrs.addFlashAttribute("error", "Sesión expirada. Por favor inicia sesión de nuevo.");
				return "redirect:/configuracion";
			}

			// Actualizar los campos que vienen del formulario
			if (adminActualizado.getNombre() != null && !adminActualizado.getNombre().isEmpty()) {
				usuarioLogueado.setNombre(adminActualizado.getNombre());
			}
			if (adminActualizado.getApellido() != null && !adminActualizado.getApellido().isEmpty()) {
				usuarioLogueado.setApellido(adminActualizado.getApellido());
			}
			if (adminActualizado.getCorreo() != null && !adminActualizado.getCorreo().isEmpty()) {
				usuarioLogueado.setCorreo(adminActualizado.getCorreo());
			}
			if (adminActualizado.getDireccion() != null) {
				usuarioLogueado.setDireccion(adminActualizado.getDireccion());
			}
			if (adminActualizado.getTelefono() != null) {
				usuarioLogueado.setTelefono(adminActualizado.getTelefono());
			}
			if (adminActualizado.getClave() != null && !adminActualizado.getClave().isEmpty()) {
				usuarioLogueado.setClave(adminActualizado.getClave());
			}

			// Guardar cambios en la BD
			usuarioService.updateUser(usuarioLogueado);

			// Actualizar la sesión con el usuario modificado
			session.setAttribute("usuarioLogueado", usuarioLogueado);

			redirectAttrs.addFlashAttribute("success", "Perfil actualizado correctamente");

		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al actualizar perfil: " + e.getMessage());
		}

		return "redirect:/configuracion";
	}

	/** ACTUALIZAR FOTO */
	@PostMapping("/actualizar-foto-mesero")
	public String actualizarFotoMesero(@RequestParam("id") Long id, @RequestParam("foto") MultipartFile foto,
			RedirectAttributes redirectAttrs, HttpSession session) {

		try {
			// Buscar el usuario
			Usuario mesero = usuarioRepository.findById(id)
					.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

			// Validar que el archivo no esté vacío
			if (foto.isEmpty()) {
				redirectAttrs.addFlashAttribute("error", "Por favor seleccione una foto");
				return "redirect:/configuracion";
			}

			// Eliminar foto anterior si existe
			if (mesero.getFoto() != null && !mesero.getFoto().isEmpty()) {
				try {
					uploadFileService.deleteImage(mesero.getFoto());
				} catch (Exception e) {
					System.err.println("⚠️ No se pudo borrar la foto anterior: " + e.getMessage());
				}
			}

			// Guardar nueva imagen
			String fileName = uploadFileService.saveImages(foto, mesero.getNombre());
			mesero.setFoto(fileName);
			usuarioRepository.save(mesero);

			session.setAttribute("usuarioLogueado", mesero);

			redirectAttrs.addFlashAttribute("success", "Foto actualizada correctamente");

		} catch (IOException e) {
			redirectAttrs.addFlashAttribute("error", "Error al guardar la foto: " + e.getMessage());
		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al actualizar foto: " + e.getMessage());
		}

		return "redirect:/configuracion";
	}

}
