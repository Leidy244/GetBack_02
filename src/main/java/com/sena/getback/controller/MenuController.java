package com.sena.getback.controller;

import com.sena.getback.model.Menu;
import com.sena.getback.service.MenuService;
import com.sena.getback.service.CategoriaService;
import com.sena.getback.service.UploadFileService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/productos")
public class MenuController {

	private final MenuService menuService;

	private final UploadFileService uploadFileService;

	public MenuController(MenuService menuService, CategoriaService categoriaService,
			UploadFileService uploadFileService) {
		this.menuService = menuService;

		this.uploadFileService = uploadFileService;
	}

	/** GUARDAR O ACTUALIZAR PRODUCTO */
	@PostMapping("/guardar")
	public String guardarProducto(@ModelAttribute Menu producto,
			@RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
			RedirectAttributes redirect) {
		try {
			// Si se sube una nueva imagen, la guardamos
			if (imagenFile != null && !imagenFile.isEmpty()) {
				String nombreImagen = uploadFileService.saveImages(imagenFile, producto.getNombreProducto());
				producto.setImagen(nombreImagen);
			}

			menuService.guardar(producto);
			redirect.addFlashAttribute("success", "Producto guardado correctamente.");
		} catch (Exception e) {
			redirect.addFlashAttribute("error", "Error al guardar el producto: " + e.getMessage());
		}
		// ✅ vuelve al panel en la sección de productos
		return "redirect:/admin?activeSection=products";
	}

	/** ELIMINAR PRODUCTO */
	@GetMapping("/eliminar/{id}")
	public String eliminarProducto(@PathVariable Integer id, RedirectAttributes redirect) {
		try {
			Menu producto = menuService.obtenerPorId(id);
			if (producto != null) {
				// Borrar también la imagen asociada (si existe)
				uploadFileService.deleteImage(producto.getImagen());
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

	/** OBTENER PRODUCTO POR ID (para edición en modal vía JS) */
	@GetMapping("/obtener/{id}")
	@ResponseBody
	public Menu obtenerProducto(@PathVariable Integer id) {
		return menuService.obtenerPorId(id);
	}
}