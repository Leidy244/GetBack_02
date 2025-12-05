package com.sena.getback.controller.api;

import com.sena.getback.model.Inventario;
import com.sena.getback.service.InventarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventario")
public class InventarioApiController {

    private final InventarioService inventarioService;

    public InventarioApiController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping("/ingresos")
    public List<Inventario> listarIngresos() {
        return inventarioService.listarIngresosRecientes();
    }

    @GetMapping("/ingresos/{id}")
    public ResponseEntity<Inventario> obtenerIngreso(@PathVariable Long id) {
        return inventarioService.obtenerPorId(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/ingresos")
    public ResponseEntity<Inventario> crearIngreso(@RequestBody Inventario ingreso) {
        if (ingreso.getFechaIngreso() == null) ingreso.setFechaIngreso(java.time.LocalDateTime.now());
        Inventario saved = inventarioService.registrarIngreso(ingreso);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/ingresos/{id}")
    public ResponseEntity<Inventario> actualizarIngreso(@PathVariable Long id, @RequestBody Inventario ingreso) {
        Inventario base = inventarioService.obtenerPorId(id).orElse(null);
        if (base == null) return ResponseEntity.notFound().build();
        ingreso.setId(id);
        if (ingreso.getFechaIngreso() == null) ingreso.setFechaIngreso(base.getFechaIngreso());
        Inventario saved = inventarioService.actualizarIngreso(ingreso);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/ingresos/{id}")
    public ResponseEntity<Void> eliminarIngreso(@PathVariable Long id) {
        inventarioService.eliminarIngreso(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stock")
    public Map<String,Integer> stockPorProducto() {
        return inventarioService.calcularStockPorProducto();
    }

    @GetMapping("/stock/{producto}")
    public Map<String,Integer> stockDe(@PathVariable String producto) {
        int val = inventarioService.obtenerStockDisponible(producto);
        return java.util.Collections.singletonMap("stock", val);
    }

    @PostMapping("/consumo")
    public ResponseEntity<Map<String,Integer>> registrarConsumo(@RequestBody Map<String,Object> payload) {
        Object p = payload.get("producto");
        Object c = payload.get("cantidad");
        if (!(p instanceof String) || !(c instanceof Number)) return ResponseEntity.badRequest().build();
        String producto = ((String)p).trim();
        int cantidad = ((Number)c).intValue();
        inventarioService.registrarConsumo(producto, cantidad);
        int nuevo = inventarioService.obtenerStockDisponible(producto);
        return ResponseEntity.ok(java.util.Collections.singletonMap("stock", nuevo));
    }

    @GetMapping("/alertas/sin-rotacion")
    public Map<String,Integer> alertasSinRotacion(@RequestParam(name = "dias", defaultValue = "30") int dias) {
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        java.util.Map<String, java.time.LocalDateTime> ultima = new java.util.HashMap<>();
        for (Inventario inv : inventarioService.listarIngresosRecientes()) {
            String prod = inv.getProducto();
            java.time.LocalDateTime fi = inv.getFechaIngreso();
            if (prod != null && fi != null) {
                String canon = prod.trim().toLowerCase();
                java.time.LocalDateTime prev = ultima.get(canon);
                if (prev == null || fi.isAfter(prev)) ultima.put(canon, fi);
            }
        }
        java.util.Map<String,Integer> out = new java.util.LinkedHashMap<>();
        ultima.forEach((canon, last) -> {
            long diff = java.time.Duration.between(last, ahora).toDays();
            if (diff >= dias) out.put(canon, (int) diff);
        });
        return out;
    }
}

