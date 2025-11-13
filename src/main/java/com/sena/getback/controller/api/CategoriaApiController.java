package com.sena.getback.controller.api;

import com.sena.getback.model.Categoria;
import com.sena.getback.service.CategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
public class CategoriaApiController {

    private final CategoriaService categoriaService;

    public CategoriaApiController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // Listar todas
    @GetMapping
    public ResponseEntity<List<Categoria>> listAll() {
        return ResponseEntity.ok(categoriaService.findAll());
    }

    // Obtener por id
    @GetMapping("/{id}")
    public ResponseEntity<Categoria> getOne(@PathVariable Integer id) {
        Categoria found = categoriaService.findById(id);
        if (found == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(found);
    }

    // Crear
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Categoria body) {
        if (body == null || body.getNombre() == null || body.getNombre().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre es obligatorio");
        }
        // Regla: si existe por nombre (case-insensitive), actualizar descripción en lugar de duplicar
        Categoria existente = categoriaService.findByNombre(body.getNombre());
        Categoria saved;
        if (existente != null) {
            existente.setDescripcion(body.getDescripcion());
            saved = categoriaService.save(existente);
            return ResponseEntity.ok(saved);
        } else {
            body.setId(null);
            saved = categoriaService.save(body);
            return ResponseEntity.created(URI.create("/api/categories/" + saved.getId())).body(saved);
        }
    }

    // Actualizar total o parcial
    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(@PathVariable Integer id, @RequestBody Categoria body) {
        Optional<Categoria> opt = categoriaService.findByIdOptional(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No encontrada");
        Categoria c = opt.get();
        if (body.getNombre() != null && !body.getNombre().trim().isEmpty()) c.setNombre(body.getNombre().trim());
        if (body.getDescripcion() != null) c.setDescripcion(body.getDescripcion());
        return ResponseEntity.ok(categoriaService.save(c));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> put(@PathVariable Integer id, @RequestBody Categoria body) {
        Optional<Categoria> opt = categoriaService.findByIdOptional(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No encontrada");
        if (body == null || body.getNombre() == null || body.getNombre().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre es obligatorio");
        }
        Categoria c = opt.get();
        c.setNombre(body.getNombre().trim());
        c.setDescripcion(body.getDescripcion());
        return ResponseEntity.ok(categoriaService.save(c));
    }

    // Eliminar
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        Optional<Categoria> opt = categoriaService.findByIdOptional(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        categoriaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
