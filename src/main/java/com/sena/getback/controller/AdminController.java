package com.sena.getback.controller;

import com.sena.getback.model.*;
import com.sena.getback.repository.*;
import com.sena.getback.service.UsuarioService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminController {

	private final UsuarioService usuarioService;

	private final CategoriaRepository categoriaRepository;
	private final MenuRepository menuRepository;
	private final EventoRepository eventoRepository;
	private final UsuarioRepository usuarioRepository;

	@Autowired
	public AdminController(CategoriaRepository categoriaRepository, MenuRepository menuRepository,
			EventoRepository eventoRepository, UsuarioRepository usuarioRepository, UsuarioService usuarioService) {
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

}