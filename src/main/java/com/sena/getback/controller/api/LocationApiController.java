package com.sena.getback.controller.api;

import com.sena.getback.model.Location;
import com.sena.getback.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/locations")
public class LocationApiController {

    @Autowired
    private LocationService locationService;

    @GetMapping
    public ResponseEntity<List<Location>> listar(@RequestParam(value = "q", required = false) String q) {
        List<Location> base = locationService.findAll();
        if (q != null && !q.isBlank()) {
            String term = q.toLowerCase(Locale.ROOT).trim();
            base = base.stream()
                    .filter(l -> safe(l.getNombre()).contains(term) || safe(l.getDescripcion()).contains(term))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(base);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Location> obtener(@PathVariable Integer id) {
        Optional<Location> opt = locationService.findById(id);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Location body) {
        if (body.getNombre() == null || body.getNombre().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Nombre requerido");
        }
        if (locationService.existsByNombre(body.getNombre().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("La ubicación ya existe");
        }
        Location saved = locationService.save(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Location body) {
        Optional<Location> existente = locationService.findById(id);
        if (existente.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No encontrada");
        if (body.getNombre() != null && locationService.existsByNombreAndIdNot(body.getNombre().trim(), id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Nombre en uso");
        }
        body.setId(id);
        Location updated = locationService.update(body);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        Optional<Location> existente = locationService.findById(id);
        if (existente.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        locationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String safe(String v) { return v == null ? "" : v.toLowerCase(Locale.ROOT); }
}

