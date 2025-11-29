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

	// Listar productos
	@GetMapping
    public String listarProductos(Model model) {
        java.util.List<Menu> productosBar = menuService.findAll().stream()
                .filter(m -> {
                    var cat = m.getCategoria();
                    String area = cat != null ? cat.getArea() : null;
                    String nombreCat = cat != null ? cat.getNombre() : null;
                    boolean areaBar = area != null && area.trim().equalsIgnoreCase("Bar");
                    boolean nombreBarHeur = nombreCat != null && nombreCat.toLowerCase().matches(".*(bebida|bar|licor).*");
                    return areaBar || (area == null && nombreBarHeur);
                })
                .toList();
        model.addAttribute("products", productosBar);
        java.util.List<com.sena.getback.model.Categoria> categoriasBar = categoriaService.findAll().stream()
                .filter(c -> c.getArea() != null && c.getArea().trim().equalsIgnoreCase("Bar"))
                .toList();
        model.addAttribute("categorias", categoriasBar.isEmpty() ? categoriaService.findAll() : categoriasBar);
		model.addAttribute("newProduct", new Menu());
		model.addAttribute("activeSection", "products"); // activa sección de productos en admin.html

		// Asegurar que la vista principal usada es 'admin' y proveer lista de nombres para el datalist (solo nombres del menú)
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

		// Importar productos desde CSV - VERSIÓN CORREGIDA
		@PostMapping("/import")
		public String importarProductos(@RequestParam("file") MultipartFile file, RedirectAttributes redirect) {
		    System.out.println("=== INICIANDO IMPORTACIÓN DE PRODUCTOS ===");
		    
		    if (file == null || file.isEmpty()) {
		        redirect.addFlashAttribute("error", "Selecciona un archivo CSV de productos.");
		        return "redirect:/admin?activeSection=products";
		    }

		    int created = 0, updated = 0, fail = 0;
		    List<String> errores = new ArrayList<>();
		    List<String> lineasProcesadas = new ArrayList<>();

		    try {
		        // Leer todo el contenido del archivo y remover BOM
		        String contenido = new String(file.getBytes(), StandardCharsets.UTF_8);
		        contenido = contenido.replace("\uFEFF", ""); // Remover BOM
		        System.out.println("Contenido del archivo (sin BOM):");
		        System.out.println("----------------------------------------");
		        System.out.println(contenido);
		        System.out.println("----------------------------------------");
		        
		        String[] lineas = contenido.split("\\r?\\n");
		        System.out.println("Número total de líneas: " + lineas.length);

		        // Obtener categorías disponibles
		        List<Categoria> todasCategorias = categoriaService.findAll();
		        List<String> nombresCategorias = todasCategorias.stream()
		                .map(Categoria::getNombre)
		                .collect(Collectors.toList());
		        
		        System.out.println("Categorías disponibles en BD: " + nombresCategorias);

		        for (int i = 0; i < lineas.length; i++) {
		            String linea = lineas[i].trim();
		            int numeroLinea = i + 1;
		            
		            // Saltar líneas vacías, comentarios o encabezados
		            if (linea.isEmpty() || 
		                linea.startsWith("#") || 
		                linea.toLowerCase().contains("plantilla") ||
		                linea.toLowerCase().contains("columnas") ||
		                linea.toLowerCase().contains("formato") ||
		                linea.toLowerCase().contains("categorías disponibles")) {
		                System.out.println("Línea " + numeroLinea + ": comentario/encabezado, saltando...");
		                continue;
		            }

		            // Saltar encabezado de columnas
		            if (linea.toLowerCase().startsWith("nombre") && 
		                linea.toLowerCase().contains("descripcion") && 
		                linea.toLowerCase().contains("precio") && 
		                linea.toLowerCase().contains("disponible") && 
		                linea.toLowerCase().contains("categoria")) {
		                System.out.println("Línea " + numeroLinea + ": encabezado de columnas, saltando...");
		                continue;
		            }

		            System.out.println("Procesando línea " + numeroLinea + ": " + linea);

		            // Detectar separador
		            String separador = linea.contains(";") ? ";" : ",";
		            System.out.println("Separador detectado: '" + separador + "'");

		            // Dividir la línea
		            String[] partes = linea.split(separador, -1); // -1 para mantener campos vacíos
		            System.out.println("Número de partes: " + partes.length);

		            // Validar número de columnas
		            if (partes.length < 5) {
		                String error = "Línea " + numeroLinea + ": se esperaban 5 columnas pero hay " + partes.length + ". Formato requerido: nombre;descripcion;precio;disponible;categoria";
		                errores.add(error);
		                fail++;
		                System.out.println("ERROR: " + error);
		                continue;
		            }

		            // Extraer y limpiar datos
		            String nombre = partes[0].trim();
		            String descripcion = partes[1].trim();
		            String precioStr = partes[2].trim();
		            String disponibleStr = partes[3].trim();
		            String categoriaNombre = partes[4].trim();

		            System.out.println("Datos extraídos:");
		            System.out.println("  Nombre: '" + nombre + "'");
		            System.out.println("  Descripción: '" + descripcion + "'");
		            System.out.println("  Precio: '" + precioStr + "'");
		            System.out.println("  Disponible: '" + disponibleStr + "'");
		            System.out.println("  Categoría: '" + categoriaNombre + "'");

		            // Validaciones
		            if (nombre.isEmpty() || nombre.equalsIgnoreCase("nombre")) {
		                errores.add("Línea " + numeroLinea + ": el nombre no puede estar vacío");
		                fail++;
		                continue;
		            }

		            if (categoriaNombre.isEmpty() || categoriaNombre.equalsIgnoreCase("categoria")) {
		                errores.add("Línea " + numeroLinea + ": la categoría no puede estar vacía");
		                fail++;
		                continue;
		            }

		            // Buscar categoría (case-insensitive)
		            Categoria categoria = null;
		            for (Categoria cat : todasCategorias) {
		                if (cat.getNombre().equalsIgnoreCase(categoriaNombre)) {
		                    categoria = cat;
		                    break;
		                }
		            }

		            if (categoria == null) {
		                errores.add("Línea " + numeroLinea + ": categoría '" + categoriaNombre + "' no existe. Categorías disponibles: " + String.join(", ", nombresCategorias));
		                fail++;
		                continue;
		            }

		            // Validar y convertir precio
		            double precio;
		            try {
		                precio = Double.parseDouble(precioStr.replace(",", "."));
		                if (precio <= 0) {
		                    errores.add("Línea " + numeroLinea + ": el precio debe ser mayor a 0");
		                    fail++;
		                    continue;
		                }
		            } catch (NumberFormatException e) {
		                errores.add("Línea " + numeroLinea + ": precio inválido '" + precioStr + "'. Use números con punto o coma decimal");
		                fail++;
		                continue;
		            }

		            // Convertir disponible
		            boolean disponible = true;
		            if (!disponibleStr.isEmpty()) {
		                String dispLower = disponibleStr.trim().toLowerCase();
		                if (dispLower.equals("false") || dispLower.equals("no") || dispLower.equals("0") || dispLower.equals("n")) {
		                    disponible = false;
		                }
		            }

		            try {
		                // Buscar producto existente
		                Menu productoExistente = menuService.findByNombreExact(nombre);
		                
		                if (productoExistente != null) {
		                    // Actualizar producto existente
		                    productoExistente.setDescripcion(descripcion.isEmpty() ? productoExistente.getDescripcion() : descripcion);
		                    productoExistente.setPrecio(precio);
		                    productoExistente.setDisponible(disponible);
		                    productoExistente.setCategoria(categoria);
		                    menuService.save(productoExistente);
		                    updated++;
		                    System.out.println("✅ PRODUCTO ACTUALIZADO: " + nombre);
		                } else {
		                    // Crear nuevo producto
		                    Menu nuevoProducto = new Menu();
		                    nuevoProducto.setNombreProducto(nombre);
		                    nuevoProducto.setDescripcion(descripcion.isEmpty() ? "Producto sin descripción" : descripcion);
		                    nuevoProducto.setPrecio(precio);
		                    nuevoProducto.setDisponible(disponible);
		                    nuevoProducto.setCategoria(categoria);
		                    menuService.save(nuevoProducto);
		                    created++;
		                    System.out.println("✅ PRODUCTO CREADO: " + nombre);
		                }
		                
		                lineasProcesadas.add(nombre);
		                
		            } catch (Exception e) {
		                String errorMsg = "Línea " + numeroLinea + ": error al guardar - " + e.getMessage();
		                errores.add(errorMsg);
		                fail++;
		                System.out.println("ERROR: " + errorMsg);
		                e.printStackTrace();
		            }
		        }

		    } catch (Exception e) {
		        String errorMsg = "Error al procesar el archivo: " + e.getMessage();
		        redirect.addFlashAttribute("error", errorMsg);
		        System.out.println("ERROR GENERAL: " + errorMsg);
		        e.printStackTrace();
		        return "redirect:/admin?activeSection=products";
		    }

		    // Mostrar resumen
		    System.out.println("=== RESUMEN IMPORTACIÓN ===");
		    System.out.println("Productos creados: " + created);
		    System.out.println("Productos actualizados: " + updated);
		    System.out.println("Errores: " + fail);
		    System.out.println("Líneas procesadas exitosamente: " + lineasProcesadas);

		    // Preparar mensajes para el usuario
		    if (created > 0 || updated > 0) {
		        String mensajeExito = String.format(
		            "✅ Importación completada: %d productos creados, %d actualizados", 
		            created, updated
		        );
		        if (fail > 0) {
		            mensajeExito += String.format(" (%d errores)", fail);
		        }
		        redirect.addFlashAttribute("success", mensajeExito);
		    }

		    if (fail > 0 && (created == 0 && updated == 0)) {
		        redirect.addFlashAttribute("error", 
		            "❌ Importación fallida: " + fail + " errores. Verifica que las categorías existan en el sistema.");
		    }

		    if (!errores.isEmpty()) {
		        redirect.addFlashAttribute("importErrors", errores);
		    }

		    return "redirect:/admin?activeSection=products";
		}

}



