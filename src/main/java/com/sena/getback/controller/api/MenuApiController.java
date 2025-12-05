package com.sena.getback.controller.api;

import com.sena.getback.model.Categoria;
import com.sena.getback.model.Menu;
import com.sena.getback.repository.CategoriaRepository;
import com.sena.getback.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/menus")
public class MenuApiController {

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
        return ResponseEntity.ok(menuService.save(m));
    }

    @DeleteMapping("/{id}")
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
}

