package com.sena.getback.controller.api;

import com.sena.getback.model.Categoria;
import com.sena.getback.model.Menu;
<<<<<<< HEAD
import com.sena.getback.repository.CategoriaRepository;
import com.sena.getback.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
=======
import com.sena.getback.service.CategoriaService;
import com.sena.getback.service.MenuService;
>>>>>>> 191c8cb3756fa0b72cc2c59cd5804b83200ba645
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

<<<<<<< HEAD
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
=======
import java.net.URI;
import java.util.List;
import java.util.Optional;
>>>>>>> 191c8cb3756fa0b72cc2c59cd5804b83200ba645

@RestController
@RequestMapping("/api/menus")
public class MenuApiController {

<<<<<<< HEAD
    @Autowired
    private MenuService menuService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping
    public ResponseEntity<List<Menu>> listar(
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "area", required = false) String area,
            @RequestParam(value = "disponible", required = false) Boolean disponible,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "agotados", required = false) Boolean agotados
    ) {
        List<Menu> base = menuService.findAll();
        if (categoria != null && !categoria.isBlank()) {
            String cat = categoria.trim().toLowerCase(Locale.ROOT);
            base = base.stream().filter(m -> m.getCategoria() != null && m.getCategoria().getNombre() != null && m.getCategoria().getNombre().trim().toLowerCase(Locale.ROOT).equals(cat)).collect(Collectors.toList());
        }
        if (area != null && !area.isBlank()) {
            String ar = area.trim().toLowerCase(Locale.ROOT);
            base = base.stream().filter(m -> m.getCategoria() != null && m.getCategoria().getArea() != null && m.getCategoria().getArea().trim().toLowerCase(Locale.ROOT).equals(ar)).collect(Collectors.toList());
        }
        if (disponible != null) {
            base = base.stream().filter(m -> Boolean.TRUE.equals(m.getDisponible()) == disponible).collect(Collectors.toList());
        }
        if (agotados != null) {
            base = base.stream().filter(m -> (m.getStock() != null ? m.getStock() : 0) == 0 == agotados).collect(Collectors.toList());
        }
        if (q != null && !q.isBlank()) {
            String term = q.toLowerCase(Locale.ROOT).trim();
            base = base.stream().filter(m -> safe(m.getNombreProducto()).contains(term) || safe(m.getDescripcion()).contains(term)).collect(Collectors.toList());
        }
        return ResponseEntity.ok(base);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Menu> obtener(@PathVariable Long id) {
        return menuService.findByIdOptional(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<Menu> crear(@RequestBody Menu body,
                                      @RequestParam(value = "categoriaId", required = false) Integer categoriaId,
                                      @RequestParam(value = "categoria", required = false) String categoriaNombre) {
        asignarCategoria(body, categoriaId, categoriaNombre);
        Menu saved = menuService.save(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Menu> actualizar(@PathVariable Long id,
                                           @RequestBody Menu body,
                                           @RequestParam(value = "categoriaId", required = false) Integer categoriaId,
                                           @RequestParam(value = "categoria", required = false) String categoriaNombre) {
        Optional<Menu> existente = menuService.findByIdOptional(id);
        if (existente.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        body.setId(id);
        asignarCategoria(body, categoriaId, categoriaNombre);
        Menu updated = menuService.save(body);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/disponible")
    public ResponseEntity<Menu> disponible(@PathVariable Long id,
                                           @RequestParam(value = "value", required = false) Boolean value) {
        Optional<Menu> opt = menuService.findByIdOptional(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Menu m = opt.get();
        if (value == null) {
            m.setDisponible(!Boolean.TRUE.equals(m.getDisponible()));
        } else {
            m.setDisponible(value);
        }
=======
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
>>>>>>> 191c8cb3756fa0b72cc2c59cd5804b83200ba645
        return ResponseEntity.ok(menuService.save(m));
    }

    @DeleteMapping("/{id}")
<<<<<<< HEAD
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!menuService.existsById(id)) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        menuService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<Categoria>> categorias() {
        return ResponseEntity.ok(categoriaRepository.findAll());
    }

    private void asignarCategoria(Menu m, Integer categoriaId, String categoriaNombre) {
        if (m.getCategoria() != null && m.getCategoria().getId() != null) return;
        Categoria c = null;
        if (categoriaId != null) {
            c = categoriaRepository.findById(categoriaId).orElse(null);
        }
        if (c == null && categoriaNombre != null && !categoriaNombre.isBlank()) {
            String nombre = categoriaNombre.trim();
            c = categoriaRepository.findAll().stream()
                    .filter(x -> x.getNombre() != null && x.getNombre().trim().equalsIgnoreCase(nombre))
                    .findFirst().orElse(null);
            if (c == null) {
                c = new Categoria();
                c.setNombre(nombre);
                c = categoriaRepository.save(c);
            }
        }
        if (c != null) m.setCategoria(c);
    }

    private String safe(String v) { return v == null ? "" : v.toLowerCase(Locale.ROOT); }
=======
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!menuService.existsById(id)) return ResponseEntity.notFound().build();
        menuService.delete(id);
        return ResponseEntity.noContent().build();
    }
>>>>>>> 191c8cb3756fa0b72cc2c59cd5804b83200ba645
}

