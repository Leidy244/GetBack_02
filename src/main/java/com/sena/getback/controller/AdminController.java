package com.sena.getback.controller;

import com.sena.getback.model.*;
import com.sena.getback.repository.*;
import com.sena.getback.service.CategoriaService;
import com.sena.getback.service.MenuService;
import com.sena.getback.service.UploadFileService;
import com.sena.getback.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

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

	@Autowired
	public AdminController(CategoriaRepository categoriaRepository, MenuRepository menuRepository,
			EventoRepository eventoRepository, UsuarioRepository usuarioRepository, UsuarioService usuarioService,
			MenuService menuService, CategoriaService categoriaService, UploadFileService uploadFileService) {
		this.menuService = menuService;
		this.categoriaService = categoriaService;
		this.categoriaRepository = categoriaRepository;
		this.menuRepository = menuRepository;
		this.eventoRepository = eventoRepository;
		this.usuarioRepository = usuarioRepository;
		this.usuarioService = usuarioService;
		this.uploadFileService = uploadFileService;
	}

	/** PANEL PRINCIPAL */
	@GetMapping("/admin")
	public String panel(@RequestParam(value = "activeSection", required = false) String activeSection, Model model) {

		String section = (activeSection != null && !activeSection.isEmpty()) ? activeSection : "dashboard";
		model.addAttribute("activeSection", section);
		model.addAttribute("title", "Panel de Administración");

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

			// Admin
			Usuario admin = usuarioService.getFirstUser().orElse(new Usuario());
			model.addAttribute("usuario", admin);

		} catch (Exception e) {
			System.err.println("❌ Error cargando datos: " + e.getMessage());

			model.addAttribute("categorias", java.util.Collections.emptyList());
			model.addAttribute("products", java.util.Collections.emptyList());
			model.addAttribute("eventos", java.util.Collections.emptyList());
			model.addAttribute("usuarios", java.util.Collections.emptyList());
			model.addAttribute("newProduct", new Menu());
			model.addAttribute("usuario", new Usuario());
		}

		return "admin";
	}

	/** PERFIL DEL ADMIN */
	@GetMapping("/perfil")
	public String mostrarPerfil(Model model) {
		Usuario admin = usuarioService.getFirstUser().orElse(new Usuario());
		model.addAttribute("usuario", admin);
		model.addAttribute("activeSection", "perfil");
		return "admin";
	}

	@PostMapping("/actualizar-datos")
	public String actualizarPerfil(@ModelAttribute Usuario adminActualizado, HttpSession session,
			RedirectAttributes redirectAttrs) {
		try {
			// Obtener el usuario logueado desde la sesión
			Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
			if (usuarioLogueado == null) {
				redirectAttrs.addFlashAttribute("error", "Sesión expirada. Por favor inicia sesión de nuevo.");
				return "redirect:/login";
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

		return "redirect:/admin?activeSection=perfil";
	}

	/** ACTUALIZAR FOTO */
	@PostMapping("/actualizar-foto")
	public String actualizarFoto(@RequestParam("id") Long id, @RequestParam("foto") MultipartFile foto,
			RedirectAttributes redirectAttrs, HttpSession session) {

		try {
			// Buscar el usuario
			Usuario admin = usuarioRepository.findById(id)
					.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

			// Validar que el archivo no esté vacío
			if (foto.isEmpty()) {
				redirectAttrs.addFlashAttribute("error", "Por favor seleccione una foto");
				return "redirect:/admin?activeSection=perfil";
			}

			// Eliminar foto anterior si existe
			if (admin.getFoto() != null && !admin.getFoto().isEmpty()) {
				try {
					uploadFileService.deleteImage(admin.getFoto());
				} catch (Exception e) {
					System.err.println("⚠️ No se pudo borrar la foto anterior: " + e.getMessage());
				}
			}

			// Guardar nueva imagen
			String fileName = uploadFileService.saveImages(foto, admin.getNombre());
			admin.setFoto(fileName);
			usuarioRepository.save(admin);

			// 🔥 ACTUALIZAR USUARIO EN SESIÓN 🔥
			session.setAttribute("usuarioLogueado", admin);

			redirectAttrs.addFlashAttribute("success", "Foto actualizada correctamente");

		} catch (IOException e) {
			redirectAttrs.addFlashAttribute("error", "Error al guardar la foto: " + e.getMessage());
		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al actualizar foto: " + e.getMessage());
		}

		return "redirect:/admin?activeSection=perfil";
	}

}