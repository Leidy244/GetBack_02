package com.sena.getback.controller;

import com.sena.getback.model.*;
import com.sena.getback.repository.*;
import com.sena.getback.service.CategoriaService;
import com.sena.getback.service.MenuService;
import com.sena.getback.service.UploadFileService;
import com.sena.getback.service.UsuarioService;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

	private final MenuService menuService;
	private final CategoriaService categoriaService;
	private final UsuarioService usuarioService;

	@Autowired
	private UploadFileService uploadFileService;

	private final CategoriaRepository categoriaRepository;
	private final MenuRepository menuRepository;
	private final EventoRepository eventoRepository;
	private final UsuarioRepository usuarioRepository;

	public AdminController(CategoriaRepository categoriaRepository, MenuRepository menuRepository,
			EventoRepository eventoRepository, UsuarioRepository usuarioRepository, UsuarioService usuarioService,
			MenuService menuService, CategoriaService categoriaService) {
		this.menuService = menuService;
		this.categoriaService = categoriaService;
		this.categoriaRepository = categoriaRepository;
		this.menuRepository = menuRepository;
		this.eventoRepository = eventoRepository;
		this.usuarioRepository = usuarioRepository;
		this.usuarioService = usuarioService;
	}

	/** PANEL PRINCIPAL */
	@GetMapping("/admin")
	public String panel(@RequestParam(value = "activeSection", required = false) String activeSection, Model model) {

		String section = (activeSection != null && !activeSection.isEmpty()) ? activeSection : "dashboard";
		model.addAttribute("activeSection", section);
		model.addAttribute("title", "Panel de Administración - " + section.toUpperCase());

		try {
			// Estadísticas
			model.addAttribute("totalCategorias", categoriaRepository.count());
			model.addAttribute("totalProductos", menuRepository.count());
			model.addAttribute("totalEventos", eventoRepository.count());
			model.addAttribute("totalUsuarios", usuarioRepository.count());

			// Listas para las tablas
			model.addAttribute("categorias", categoriaRepository.findAll());
			model.addAttribute("products", menuRepository.findAll());
			model.addAttribute("eventos", eventoRepository.findAll());
			model.addAttribute("usuarios", usuarioRepository.findAll());

			// Objetos vacíos para formularios
			model.addAttribute("newProduct", new Menu());
			model.addAttribute("categoria", new Categoria());
			model.addAttribute("evento", new Evento());

			// Admin cargado
			Usuario admin = usuarioRepository.findById(1)
					.orElseThrow(() -> new RuntimeException("Admin no encontrado"));
			model.addAttribute("usuario", admin);

		} catch (Exception e) {
			System.err.println("❌ Error cargando datos: " + e.getMessage());
			e.printStackTrace();

			// Evita nulos en las vistas
			model.addAttribute("categorias", java.util.Collections.emptyList());
			model.addAttribute("products", java.util.Collections.emptyList());
			model.addAttribute("eventos", java.util.Collections.emptyList());
			model.addAttribute("usuarios", java.util.Collections.emptyList());
			model.addAttribute("newProduct", new Menu());
		}

		return "admin/admin";
	}

	/** PERFIL DEL ADMIN */
	@GetMapping("/perfil")
	public String mostrarPerfil(Model model) {
		Usuario admin = usuarioService.obtenerAdmin().orElseThrow(() -> new RuntimeException("Admin no encontrado"));

		model.addAttribute("usuario", admin);
		return "redirect:/admin?activeSection=perfil";
	}

	@PostMapping("/actualizar-datos")
	public String actualizarPerfil(@ModelAttribute("usuario") Usuario adminActualizado,
			RedirectAttributes redirectAttrs) {
		Usuario admin = usuarioService.obtenerAdmin().orElseThrow(() -> new RuntimeException("Admin no encontrado"));

		// Actualizar solo si viene valor nuevo
		if (adminActualizado.getNombre() != null && !adminActualizado.getNombre().isEmpty()) {
			admin.setNombre(adminActualizado.getNombre());
		}
		if (adminActualizado.getApellido() != null && !adminActualizado.getApellido().isEmpty()) {
			admin.setApellido(adminActualizado.getApellido());
		}
		if (adminActualizado.getCorreo() != null && !adminActualizado.getCorreo().isEmpty()) {
			admin.setCorreo(adminActualizado.getCorreo());
		}
		if (adminActualizado.getDireccion() != null && !adminActualizado.getDireccion().isEmpty()) {
			admin.setDireccion(adminActualizado.getDireccion());
		}
		if (adminActualizado.getTelefono() != null && !adminActualizado.getTelefono().isEmpty()) {
			admin.setTelefono(adminActualizado.getTelefono());
		}
		if (adminActualizado.getClave() != null && !adminActualizado.getClave().isEmpty()) {
			admin.setClave(adminActualizado.getClave());
		}

		// Conservar foto si no se subió nueva
		if (admin.getFoto() != null) {
			adminActualizado.setFoto(admin.getFoto());
		}

		// Mantener ID y Rol
		admin.setId(admin.getId());
		admin.setRol(admin.getRol());

		usuarioService.updateUsuario(admin.getId(), admin);

		redirectAttrs.addFlashAttribute("success", "Perfil actualizado correctamente");
		return "redirect:/admin?activeSection=perfil";
	}

	@PostMapping("/actualizar-foto")
	public String actualizarFoto(@RequestParam("id") Integer id, @RequestParam("foto") MultipartFile foto) {
		usuarioRepository.findById(id).ifPresent(admin -> {
			try {
				uploadFileService.deleteImage(admin.getFoto()); // borrar la vieja
				String fileName = uploadFileService.saveImages(foto, admin.getNombre()); // guardar la nueva
				admin.setFoto(fileName);
				usuarioRepository.save(admin);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		return "redirect:/admin?activeSection=perfil";
	}
}
