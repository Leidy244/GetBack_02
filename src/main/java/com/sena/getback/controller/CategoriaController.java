package com.sena.getback.controller;

import com.sena.getback.model.Categoria;
import com.sena.getback.service.CategoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categorias")
public class CategoriaController {

	private final CategoriaService categoriaService;

	public CategoriaController(CategoriaService categoriaService) {
		this.categoriaService = categoriaService;
	}

	/**
	 * Listar categorías y preparar objeto para nueva
	 */
	@GetMapping
	public String listar(Model model) {
		model.addAttribute("categorias", categoriaService.findAll());
		model.addAttribute("newCategoria", new Categoria()); // para el modal de creación
		return "admin";
	}

	/**
	 * Guardar o actualizar categoría
	 */
	@PostMapping("/guardar")
	public String guardar(@ModelAttribute Categoria categoria, RedirectAttributes redirect) {
		boolean esNueva = (categoria.getId() == null);
		categoriaService.save(categoria);

		redirect.addFlashAttribute("success",
				esNueva ? "Categoría creada correctamente" : "Categoría actualizada correctamente ✏️");

		return "redirect:/admin?activeSection=categories";
	}

	/**
	 * Eliminar categoría
	 */
	@GetMapping("/eliminar/{id}")
	public String eliminar(@PathVariable Integer id, RedirectAttributes redirect) {
		categoriaService.delete(id);
		redirect.addFlashAttribute("success", "Categoría eliminada correctamente 🗑️");
		return "redirect:/admin?activeSection=categories";
	}
}
