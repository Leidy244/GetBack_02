package com.sena.getback.controller;

import com.sena.getback.model.*;
import com.sena.getback.repository.*;
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

	private final UsuarioService usuarioService;
	@Autowired
	private UploadFileService uploadFileService;

	private final CategoriaRepository categoriaRepository;
	private final MenuRepository menuRepository;
	private final EventoRepository eventoRepository;
	private final UsuarioRepository usuarioRepository;

	public AdminController(CategoriaRepository categoriaRepository, MenuRepository menuRepository,
			EventoRepository eventoRepository, UsuarioRepository usuarioRepository, UsuarioService usuarioService) {
		this.categoriaRepository = categoriaRepository;
		this.menuRepository = menuRepository;
		this.eventoRepository = eventoRepository;
		this.usuarioRepository = usuarioRepository;
		this.usuarioService = usuarioService;
	}

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
			model.addAttribute("productos", menuRepository.findAll());
			model.addAttribute("eventos", eventoRepository.findAll());
			model.addAttribute("usuarios", usuarioRepository.findAll());

			// Objetos vacíos para formularios
			model.addAttribute("producto", new Menu());
			model.addAttribute("categoria", new Categoria());
			model.addAttribute("evento", new Evento());
			model.addAttribute("usuario", new Usuario());
			Usuario admin = usuarioRepository.findById(1)
					.orElseThrow(() -> new RuntimeException("Admin no encontrado"));

			model.addAttribute("usuario", admin);

		} catch (Exception e) {
			System.err.println("❌ Error cargando datos: " + e.getMessage());
			e.printStackTrace();

			// Asegurar que las listas estén vacías pero no nulas
			model.addAttribute("categorias", java.util.Collections.emptyList());
			model.addAttribute("productos", java.util.Collections.emptyList());
			model.addAttribute("eventos", java.util.Collections.emptyList());
			model.addAttribute("usuarios", java.util.Collections.emptyList());
		}

		return "admin/admin";
	}

	// Mostrar perfil con información del admin
	@GetMapping("/perfil")
	public String mostrarPerfil(Model model) {
		Usuario admin = usuarioService.obtenerAdmin().orElseThrow(() -> new RuntimeException("Admin no encontrado"));

		model.addAttribute("usuario", admin);
		return "redirect:/admin?activeSection=perfil";
	}

	// Actualizar datos del admin
	@PostMapping("/actualizar-datos")
	public String actualizarPerfil(@ModelAttribute("usuario") Usuario adminActualizado,
			RedirectAttributes redirectAttrs) {
		Usuario admin = usuarioService.obtenerAdmin().orElseThrow(() -> new RuntimeException("Admin no encontrado"));

		// Solo actualiza si no viene nulo ni vacío
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

		// Conservar la foto si no se subió nueva
		if (admin.getFoto() != null) {
			adminActualizado.setFoto(admin.getFoto());
		}

		// Mantener ID y Rol del admin
		admin.setId(admin.getId());
		admin.setRol(admin.getRol());

		// Guardar el objeto admin ya fusionado
		usuarioService.updateUsuario(admin.getId(), admin);

		redirectAttrs.addFlashAttribute("success", "Perfil actualizado correctamente");
		return "redirect:/admin?activeSection=perfil";
	}

	// Actualizar foto
	@PostMapping("/actualizar-foto")
	public String actualizarFoto(@RequestParam("id") Integer id, @RequestParam("foto") MultipartFile foto) {
		usuarioRepository.findById(id).ifPresent(admin -> {
			try {
				// Borrar foto anterior (si tiene)
				uploadFileService.deleteImage(admin.getFoto());

				// Guardar nueva
				String fileName = uploadFileService.saveImages(foto, admin.getNombre());
				admin.setFoto(fileName);

				usuarioRepository.save(admin);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		return "redirect:/admin?activeSection=perfil";
	}
}