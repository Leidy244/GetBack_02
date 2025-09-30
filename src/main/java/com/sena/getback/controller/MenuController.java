package com.sena.getback.controller;

import com.sena.getback.model.Menu;
import com.sena.getback.service.CategoriaService;
import com.sena.getback.service.MenuService;
import com.sena.getback.service.UploadFileService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/menu") // Ruta base para acciones de productos
public class MenuController {

	private final MenuService menuService;
	private final CategoriaService categoriaService;
	private final UploadFileService uploadFileService;

	public MenuController(MenuService menuService, CategoriaService categoriaService,
			UploadFileService uploadFileService) {
		this.menuService = menuService;
		this.categoriaService = categoriaService;
		this.uploadFileService = uploadFileService;
	}

	/** Guardar o actualizar producto */
	@PostMapping("/productos/guardar")
	public String guardarProducto(@ModelAttribute("newProduct") Menu producto,
			@RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
			RedirectAttributes redirect) {
		try {
			if (imagenFile != null && !imagenFile.isEmpty()) {
				String nombreImagen = uploadFileService.saveImages(imagenFile, producto.getNombreProducto());
				producto.setImagen(nombreImagen);
			}

			menuService.guardar(producto);
			redirect.addFlashAttribute("success", "Producto guardado correctamente.");
		} catch (Exception e) {
			redirect.addFlashAttribute("error", "Error al guardar el producto: " + e.getMessage());
		}

		return "redirect:/admin?activeSection=products"; // vuelve al panel con productos
	}

	/** Eliminar producto */
	@GetMapping("/productos/eliminar/{id}")
	public String eliminarProducto(@PathVariable Integer id, RedirectAttributes redirect) {
		try {
			Menu producto = menuService.obtenerPorId(id);
			if (producto != null) {
				if (producto.getImagen() != null) {
					uploadFileService.deleteImage(producto.getImagen());
				}
				menuService.eliminar(id);
				redirect.addFlashAttribute("success", "Producto eliminado correctamente.");
			} else {
				redirect.addFlashAttribute("error", "Producto no encontrado.");
			}
		} catch (Exception e) {
			redirect.addFlashAttribute("error", "Error al eliminar el producto: " + e.getMessage());
		}
		return "redirect:/admin?activeSection=products";
	}

	/** Obtener producto por id (AJAX) */
	@GetMapping("/productos/obtener/{id}")
	@ResponseBody
	public Menu obtenerProducto(@PathVariable Integer id) {
		return menuService.obtenerPorId(id);
	}

}
