package com.sena.getback.controller.api;

import com.sena.getback.model.Categoria;
import com.sena.getback.model.Menu;
import com.sena.getback.service.CategoriaService;
import com.sena.getback.service.MenuService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/menus")
public class MenuApiController {

    private final MenuService menuService;
    private final CategoriaService categoriaService;

    public MenuApiController(MenuService menuService, CategoriaService categoriaService) {
        this.menuService = menuService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<List<Menu>> listAll(@RequestParam(value = "categoria", required = false) String categoriaNombre,
                                              @RequestParam(value = "nombre", required = false) String nombreContiene,
                                              @RequestParam(value = "disponibles", required = false) Boolean disponibles) {
        if (Boolean.TRUE.equals(disponibles)) {
            return ResponseEntity.ok(menuService.listarDisponibles());
        }
        if (categoriaNombre != null && !categoriaNombre.isBlank()) {
            return ResponseEntity.ok(menuService.findByCategoriaNombre(categoriaNombre));
        }
        if (nombreContiene != null && !nombreContiene.isBlank()) {
            return ResponseEntity.ok(menuService.findByNombreContaining(nombreContiene));
        }
        return ResponseEntity.ok(menuService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Menu> getOne(@PathVariable Long id) {
        Menu found = menuService.findById(id);
        if (found == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(found);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Menu body,
                                    @RequestParam(value = "categoriaId", required = false) Integer categoriaId) {
        if (body == null || body.getNombreProducto() == null || body.getNombreProducto().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre del producto es obligatorio");
        }
        if (body.getPrecio() == null) {
            return ResponseEntity.badRequest().body("El precio es obligatorio");
        }

        Categoria categoria = null;
        if (categoriaId != null) {
            categoria = categoriaService.findById(categoriaId);
            if (categoria == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Categoría no encontrada");
            }
        } else if (body.getCategoria() != null && body.getCategoria().getId() != null) {
            categoria = categoriaService.findById(body.getCategoria().getId());
            if (categoria == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Categoría no válida");
            }
        } else {
            return ResponseEntity.badRequest().body("La categoría es obligatoria");
        }

        body.setId(null);
        body.setCategoria(categoria);
        Menu saved = menuService.save(body);
        return ResponseEntity.created(URI.create("/api/menus/" + saved.getId())).body(saved);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(@PathVariable Long id,
                                   @RequestBody Menu body,
                                   @RequestParam(value = "categoriaId", required = false) Integer categoriaId) {
        Optional<Menu> opt = menuService.findByIdOptional(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No encontrado");
        Menu m = opt.get();

        if (body.getNombreProducto() != null && !body.getNombreProducto().trim().isEmpty()) {
            m.setNombreProducto(body.getNombreProducto().trim());
        }
        if (body.getDescripcion() != null) {
            m.setDescripcion(body.getDescripcion());
        }
        if (body.getPrecio() != null) {
            m.setPrecio(body.getPrecio());
        }
        if (body.getDisponible() != null) {
            m.setDisponible(body.getDisponible());
        }
        if (body.getImagen() != null) {
            m.setImagen(body.getImagen());
        }
        if (body.getStock() != null) {
            m.setStock(body.getStock());
        }

        if (categoriaId != null) {
            Categoria c = categoriaService.findById(categoriaId);
            if (c == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Categoría no encontrada");
            m.setCategoria(c);
        } else if (body.getCategoria() != null && body.getCategoria().getId() != null) {
            Categoria c = categoriaService.findById(body.getCategoria().getId());
            if (c == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Categoría no válida");
            m.setCategoria(c);
        }

        return ResponseEntity.ok(menuService.save(m));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> put(@PathVariable Long id, @RequestBody Menu body,
                                 @RequestParam(value = "categoriaId", required = false) Integer categoriaId) {
        Optional<Menu> opt = menuService.findByIdOptional(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No encontrado");
        if (body == null || body.getNombreProducto() == null || body.getNombreProducto().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre del producto es obligatorio");
        }
        if (body.getPrecio() == null) {
            return ResponseEntity.badRequest().body("El precio es obligatorio");
        }

        Categoria categoria = null;
        if (categoriaId != null) {
            categoria = categoriaService.findById(categoriaId);
            if (categoria == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Categoría no encontrada");
        } else if (body.getCategoria() != null && body.getCategoria().getId() != null) {
            categoria = categoriaService.findById(body.getCategoria().getId());
            if (categoria == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Categoría no válida");
        } else {
            return ResponseEntity.badRequest().body("La categoría es obligatoria");
        }

        Menu m = opt.get();
        m.setNombreProducto(body.getNombreProducto().trim());
        m.setDescripcion(body.getDescripcion());
        m.setPrecio(body.getPrecio());
        m.setDisponible(body.getDisponible() != null ? body.getDisponible() : Boolean.TRUE);
        m.setImagen(body.getImagen());
        m.setStock(body.getStock() != null ? body.getStock() : m.getStock());
        m.setCategoria(categoria);
        return ResponseEntity.ok(menuService.save(m));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!menuService.existsById(id)) return ResponseEntity.notFound().build();
        menuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

