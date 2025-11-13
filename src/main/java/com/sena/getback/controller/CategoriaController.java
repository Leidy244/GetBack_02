package com.sena.getback.controller;

import com.sena.getback.model.Categoria;
import com.sena.getback.service.CategoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
        model.addAttribute("activeSection", "categories");
        model.addAttribute("title", "Gestión de Categorías");
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
                esNueva ? "Categoría creada correctamente" : "Categoría actualizada correctamente ");

        return "redirect:/admin?activeSection=categories";
    }

    /**
     * Eliminar categoría
     */
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirect) {
        categoriaService.delete(id);
        redirect.addFlashAttribute("success", "Categoría eliminada correctamente ");
        return "redirect:/admin?activeSection=categories";
    }

    /**
     * Descargar plantilla CSV
     */
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> descargarPlantilla() {
        // Usamos ';' como separador para mejor compatibilidad con configuraciones regionales de Excel
        String csv = "\uFEFF" + // BOM UTF-8 para que Excel muestre bien acentos/ñ
                "nombre;descripcion\n" +
                "Bebidas;Productos líquidos\n" +
                "Comidas;Platos principales\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla_categorias.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    /**
     * Importar categorías desde CSV (nombre,descripcion)
     */
    @PostMapping("/import")
    public String importarCsv(@RequestParam("file") MultipartFile file, RedirectAttributes redirect) {
        if (file == null || file.isEmpty()) {
            redirect.addFlashAttribute("error", "Selecciona un archivo CSV.");
            return "redirect:/admin?activeSection=categories";
        }
        int created = 0, updated = 0, fail = 0, row = 0;
        List<String> errores = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                row++;
                if (row == 1 && line.toLowerCase().startsWith("nombre")) continue;
                // Detectar separador por línea: priorizar ';' si está presente, de lo contrario ','
                String delim = line.contains(";") ? ";" : ",";
                String[] parts = line.split(java.util.regex.Pattern.quote(delim), -1);
                String nombre = parts.length > 0 ? parts[0].trim() : "";
                String descripcion = parts.length > 1 ? parts[1].trim() : null;
                if (nombre.isEmpty()) { fail++; errores.add("Fila " + row + ": nombre requerido"); continue; }
                try {
                    // Regla de duplicados para categorías (por controlador):
                    // Si existe por nombre (case-insensitive) -> actualizar descripción; si no, crear.
                    Categoria existente = categoriaService.findByNombre(nombre);
                    if (existente != null) {
                        existente.setDescripcion((descripcion != null && !descripcion.isEmpty()) ? descripcion : null);
                        categoriaService.save(existente);
                        updated++;
                    } else {
                        Categoria c = new Categoria();
                        c.setNombre(nombre);
                        c.setDescripcion((descripcion != null && !descripcion.isEmpty()) ? descripcion : null);
                        categoriaService.save(c);
                        created++;
                    }
                } catch (Exception ex) {
                    fail++;
                    errores.add("Fila " + row + ": " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error leyendo CSV: " + e.getMessage());
            return "redirect:/admin?activeSection=categories";
        }
        String resumen = "Importación completada. Creadas: " + created + ", Actualizadas: " + updated + ", Fallas: " + fail;
        redirect.addFlashAttribute("success", resumen);
        return "redirect:/admin?activeSection=categories";
    }
}
