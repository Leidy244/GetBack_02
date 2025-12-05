package com.sena.getback.controller.api;

import com.sena.getback.model.Evento;
import com.sena.getback.service.EventoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoApiController {

    private final EventoService eventoService;

    public EventoApiController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    public List<Evento> listar() {
        return eventoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evento> obtener(@PathVariable Long id) {
        return eventoService.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Evento> crear(@RequestBody Evento evento) {
        evento.setId(null);
        if (evento.getEstado() == null || evento.getEstado().isBlank()) evento.setEstado("ACTIVO");
        Evento saved = eventoService.save(evento);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Evento> actualizar(@PathVariable Long id, @RequestBody Evento evento) {
        Evento base = eventoService.findById(id).orElse(null);
        if (base == null) return ResponseEntity.notFound().build();
        evento.setId(id);
        if (evento.getImagen() == null) evento.setImagen(base.getImagen());
        if (evento.getEstado() == null) evento.setEstado(base.getEstado());
        Evento saved = eventoService.save(evento);
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Void> toggle(@PathVariable Long id) {
        eventoService.toggleEventoEstado(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        eventoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

