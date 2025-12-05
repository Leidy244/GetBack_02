package com.sena.getback.controller.api;

import com.sena.getback.model.Location;
import com.sena.getback.model.Mesa;
import com.sena.getback.service.LocationService;
import com.sena.getback.service.MesaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/mesas")
public class MesaApiController {

    private final MesaService mesaService;
    private final LocationService locationService;

    public MesaApiController(MesaService mesaService, LocationService locationService) {
        this.mesaService = mesaService;
        this.locationService = locationService;
    }

    @GetMapping
    public ResponseEntity<List<Mesa>> listAll() {
        return ResponseEntity.ok(mesaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mesa> getOne(@PathVariable Integer id) {
        Optional<Mesa> opt = mesaService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(opt.get());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Mesa body,
                                    @RequestParam(value = "ubicacionId", required = false) Integer ubicacionId) {
        if (body == null || body.getNumero() == null || body.getNumero().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El número de mesa es obligatorio");
        }
        if (body.getCapacidad() == null || body.getCapacidad() < 1 || body.getCapacidad() > 50) {
            return ResponseEntity.badRequest().body("La capacidad debe ser entre 1 y 50 personas");
        }

        Location ubicacion = null;
        if (ubicacionId != null) {
            ubicacion = locationService.findById(ubicacionId).orElse(null);
            if (ubicacion == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ubicación no encontrada");
        } else if (body.getUbicacion() != null && body.getUbicacion().getId() != null) {
            ubicacion = locationService.findById(body.getUbicacion().getId()).orElse(null);
            if (ubicacion == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ubicación no válida");
        }

        String numeroMesa = body.getNumero().trim().toUpperCase();
        if (mesaService.existsByNumeroAndUbicacion(numeroMesa, ubicacion != null ? ubicacion.getId() : null)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe una mesa con ese número en la ubicación seleccionada");
        }

        body.setId(null);
        body.setNumero(numeroMesa);
        body.setEstado(body.getEstado() != null ? body.getEstado() : "DISPONIBLE");
        body.setUbicacion(ubicacion);
        Mesa saved = mesaService.save(body);
        return ResponseEntity.created(URI.create("/api/mesas/" + saved.getId())).body(saved);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(@PathVariable Integer id,
                                   @RequestBody Mesa body,
                                   @RequestParam(value = "ubicacionId", required = false) Integer ubicacionId) {
        Optional<Mesa> opt = mesaService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No encontrada");
        Mesa mesa = opt.get();

        Location ubicacion = null;
        if (ubicacionId != null) {
            ubicacion = locationService.findById(ubicacionId).orElse(null);
            if (ubicacion == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ubicación no encontrada");
        } else if (body.getUbicacion() != null && body.getUbicacion().getId() != null) {
            ubicacion = locationService.findById(body.getUbicacion().getId()).orElse(null);
            if (ubicacion == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ubicación no válida");
        }

        if (body.getNumero() != null && !body.getNumero().trim().isEmpty()) {
            String numeroNuevo = body.getNumero().trim().toUpperCase();
            Integer ubicacionIdCheck = ubicacion != null ? ubicacion.getId() : (mesa.getUbicacion() != null ? mesa.getUbicacion().getId() : null);
            if (mesaService.existsByNumeroAndUbicacionAndIdNot(numeroNuevo, ubicacionIdCheck, mesa.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Otra mesa con ese número ya existe en esa ubicación");
            }
            mesa.setNumero(numeroNuevo);
        }
        if (body.getCapacidad() != null) {
            Integer cap = body.getCapacidad();
            if (cap < 1 || cap > 50) return ResponseEntity.badRequest().body("La capacidad debe ser entre 1 y 50");
            mesa.setCapacidad(cap);
        }
        if (body.getEstado() != null && !body.getEstado().trim().isEmpty()) {
            mesa.setEstado(body.getEstado().trim().toUpperCase());
        }
        if (body.getDescripcion() != null) {
            mesa.setDescripcion(body.getDescripcion());
        }
        if (ubicacion != null) {
            mesa.setUbicacion(ubicacion);
        }

        return ResponseEntity.ok(mesaService.save(mesa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> put(@PathVariable Integer id,
                                 @RequestBody Mesa body,
                                 @RequestParam(value = "ubicacionId", required = false) Integer ubicacionId) {
        Optional<Mesa> opt = mesaService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No encontrada");
        if (body == null || body.getNumero() == null || body.getNumero().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El número de mesa es obligatorio");
        }
        if (body.getCapacidad() == null || body.getCapacidad() < 1 || body.getCapacidad() > 50) {
            return ResponseEntity.badRequest().body("La capacidad debe ser entre 1 y 50 personas");
        }

        Location ubicacion = null;
        if (ubicacionId != null) {
            ubicacion = locationService.findById(ubicacionId).orElse(null);
            if (ubicacion == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ubicación no encontrada");
        } else if (body.getUbicacion() != null && body.getUbicacion().getId() != null) {
            ubicacion = locationService.findById(body.getUbicacion().getId()).orElse(null);
            if (ubicacion == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ubicación no válida");
        }

        String numeroMesa = body.getNumero().trim().toUpperCase();
        Integer ubicacionIdCheck = ubicacion != null ? ubicacion.getId() : (opt.get().getUbicacion() != null ? opt.get().getUbicacion().getId() : null);
        if (mesaService.existsByNumeroAndUbicacionAndIdNot(numeroMesa, ubicacionIdCheck, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Otra mesa con ese número ya existe en esa ubicación");
        }

        Mesa mesa = opt.get();
        mesa.setNumero(numeroMesa);
        mesa.setCapacidad(body.getCapacidad());
        mesa.setEstado(body.getEstado() != null ? body.getEstado() : "DISPONIBLE");
        mesa.setDescripcion(body.getDescripcion());
        mesa.setUbicacion(ubicacion);
        return ResponseEntity.ok(mesaService.save(mesa));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        Optional<Mesa> opt = mesaService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        mesaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ocupar")
    public ResponseEntity<?> ocupar(@PathVariable Integer id) {
        Optional<Mesa> opt = mesaService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        mesaService.ocuparMesa(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/liberar")
    public ResponseEntity<?> liberar(@PathVariable Integer id) {
        Optional<Mesa> opt = mesaService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        mesaService.liberarMesa(id);
        return ResponseEntity.ok().build();
    }
}

