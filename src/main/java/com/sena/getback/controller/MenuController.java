package com.sena.getback.controller;

import com.sena.getback.model.Menu;
import com.sena.getback.model.Categoria;
import com.sena.getback.service.MenuService;
import com.sena.getback.service.UploadFileService;
import com.sena.getback.service.CategoriaService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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

	// Listar productos (todas las áreas: Cocina y Bar)
	@GetMapping
    public String listarProductos(Model model) {
        // Mostrar todos los productos registrados en el menú
        java.util.List<Menu> productos = menuService.findAll();
        model.addAttribute("products", productos);
        // Proveer TODAS las categorías para que el formulario permita elegir Cocina o Bar
        java.util.List<com.sena.getback.model.Categoria> categorias = categoriaService.findAll();
        model.addAttribute("categorias", categorias);
		model.addAttribute("newProduct", new Menu());
		model.addAttribute("activeSection", "products");

		java.util.List<String> menuNombres = new java.util.ArrayList<>();
		menuService.findAll().forEach(m -> {
			if (m.getNombreProducto() != null && !m.getNombreProducto().isBlank()) {
				menuNombres.add(m.getNombreProducto().trim());
			}
		});
		model.addAttribute("inventarioNombres", menuNombres);

		return "admin";
	}

	// Guardar o editar producto
	@PostMapping("/guardar")
	public String guardarProducto(@ModelAttribute Menu producto, @RequestParam("imagenFile") MultipartFile imagenFile,
			RedirectAttributes redirectAttributes) {
		try {
			// Validación: evitar crear o renombrar a un producto con nombre ya existente (case-insensitive)
			if (producto.getNombreProducto() != null && !producto.getNombreProducto().isBlank()) {
				Menu existente = menuService.findByNombreExact(producto.getNombreProducto());
				if (existente != null) {
					// Si es creación nueva o el existente tiene distinto id -> bloqueo
					if (producto.getId() == null || !existente.getId().equals(producto.getId())) {
						redirectAttributes.addFlashAttribute("error", "Ya existe un producto con ese nombre: '" + producto.getNombreProducto() + "'. Cambia el nombre o edita el existente.");
						return "redirect:/admin?activeSection=products";
					}
				}
			}
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
	// Descargar plantilla CSV para productos - SOLO CATEGORÍAS EXISTENTES
		@GetMapping("/import/template")
		public ResponseEntity<byte[]> descargarPlantillaProductos() {
		    // Obtener categorías disponibles para la plantilla
		    List<Categoria> categorias = categoriaService.findAll();
		    String categoriasDisponibles = categorias.stream()
		            .map(Categoria::getNombre)
		            .collect(Collectors.joining(", "));
		    
		    // Crear ejemplos usando solo categorías existentes
		    String ejemplo1 = "", ejemplo2 = "", ejemplo3 = "";
		    if (categorias.stream().anyMatch(c -> "Bebidas".equalsIgnoreCase(c.getNombre()))) {
		        ejemplo1 = "Café Americano;Café negro caliente;4500;true;Bebidas\n";
		    }
		    if (categorias.stream().anyMatch(c -> "Comidas".equalsIgnoreCase(c.getNombre()))) {
		        ejemplo2 = "Hamburguesa Clásica;Carne, lechuga, tomate y queso;12000;true;Comidas\n";
		    }
		    if (categorias.stream().anyMatch(c -> "A".equalsIgnoreCase(c.getNombre()))) {
		        ejemplo3 = "Pastel de Chocolate;Porción individual de pastel de chocolate;3500;false;A\n";
		    }
		    
		    String csv = "# Plantilla para importar productos\n" +
		            "# Columnas requeridas: nombre, descripcion, precio, disponible, categoria\n" +
		            "# Categorías disponibles en tu sistema: " + categoriasDisponibles + "\n" +
		            "# Formato: use ';' como separador, 'true'/'false' para disponible\n\n" +
		            "nombre;descripcion;precio;disponible;categoria\n" +
		            ejemplo1 + ejemplo2 + ejemplo3;
		    
		    byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
		    return ResponseEntity.ok()
		            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla_productos_getback.csv")
		            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
		            .body(bytes);
		}
}



