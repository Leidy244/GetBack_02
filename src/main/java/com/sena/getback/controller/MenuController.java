package com.sena.getback.controller;

import com.sena.getback.model.Menu;
import com.sena.getback.service.MenuService;
import com.sena.getback.service.UploadFileService;
import com.sena.getback.service.CategoriaService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/admin/menu/productos")
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

	// Listar productos
	@GetMapping
	public String listarProductos(Model model) {
		model.addAttribute("products", menuService.findAll());
		model.addAttribute("categorias", categoriaService.findAll());
		model.addAttribute("newProduct", new Menu());
		model.addAttribute("activeSection", "products"); // activa sección de productos en admin.html
		return "admin/admin";
	}

	// Guardar o editar producto
	@PostMapping("/guardar")
	public String guardarProducto(@ModelAttribute Menu producto, @RequestParam("imagenFile") MultipartFile imagenFile,
			RedirectAttributes redirectAttributes) {
		try {
			if (!imagenFile.isEmpty()) {
				// Subir nueva imagen y guardar con prefijo "/images/"
				String nombreImagen = uploadFileService.saveImages(imagenFile, producto.getNombreProducto());
				producto.setImagen("/images/" + nombreImagen);
			} else if (producto.getId() != null) {
				// Mantener imagen existente si no se sube una nueva
				Menu existente = menuService.findById(producto.getId());
				if (existente != null) {
					producto.setImagen(existente.getImagen());
				}
			}

			menuService.save(producto);
			redirectAttributes.addFlashAttribute("success", "Producto guardado correctamente");

		} catch (IOException e) {
			redirectAttributes.addFlashAttribute("error", "Error al guardar el producto");
			e.printStackTrace();
		}

		return "redirect:/admin?activeSection=products";
	}

	// Eliminar producto
	@GetMapping("/eliminar/{id}")
	public String eliminarProducto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			Menu producto = menuService.findById(id);
			if (producto != null) {
				if (producto.getImagen() != null) {
					// Quitar prefijo "/images/" antes de eliminar físicamente
					String nombreArchivo = producto.getImagen().replace("/images/", "");
					uploadFileService.deleteImage(nombreArchivo);
				}
				menuService.delete(id);
				redirectAttributes.addFlashAttribute("success", "Producto eliminado correctamente");
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al eliminar el producto");
		}
		return "redirect:/admin?activeSection=products";
	}
}
