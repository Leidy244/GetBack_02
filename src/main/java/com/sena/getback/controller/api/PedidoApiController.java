package com.sena.getback.controller.api;

import com.sena.getback.model.Pedido;
import com.sena.getback.service.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoApiController {

    private final PedidoService pedidoService;

    public PedidoApiController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // Listar todos los pedidos
    @GetMapping
    public ResponseEntity<List<Pedido>> listAll() {
        return ResponseEntity.ok(pedidoService.findAll());
    }

    // Obtener un pedido por id
    @GetMapping("/{id}")
    public ResponseEntity<Pedido> getOne(@PathVariable Integer id) {
        Pedido pedido = pedidoService.findById(id);
        if (pedido == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(pedido);
    }

    // Crear un pedido básico a partir de mesaId, itemsJson, comentarios y total
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        if (body == null || !body.containsKey("mesaId")) {
            return ResponseEntity.badRequest().body("mesaId es obligatorio");
        }
        Object mesaIdObj = body.get("mesaId");
        Integer mesaId;
        try {
            if (mesaIdObj instanceof Number n) {
                mesaId = n.intValue();
            } else {
                mesaId = Integer.parseInt(String.valueOf(mesaIdObj));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("mesaId inválido");
        }

        String itemsJson = body.get("itemsJson") != null ? String.valueOf(body.get("itemsJson")) : null;
        String comentarios = body.get("comentarios") != null ? String.valueOf(body.get("comentarios")) : null;

        Double total = null;
        Object totalObj = body.get("total");
        if (totalObj instanceof Number n) {
            total = n.doubleValue();
        } else if (totalObj != null) {
            try {
                total = Double.parseDouble(String.valueOf(totalObj));
            } catch (Exception ignored) {}
        }

        try {
            Pedido saved = pedidoService.crearPedido(mesaId, itemsJson, comentarios, total);
            return ResponseEntity
                    .created(URI.create("/api/pedidos/" + saved.getId()))
                    .body(saved);
        } catch (IllegalStateException e) {
            // Errores de stock u otras validaciones de negocio
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear el pedido: " + e.getMessage());
        }
    }

    // Actualizar parcialmente algunos campos simples del pedido
    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Pedido pedido = pedidoService.findById(id);
        if (pedido == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Pedido no encontrado");

        if (body.containsKey("comentariosGenerales")) {
            Object c = body.get("comentariosGenerales");
            pedido.setComentariosGenerales(c != null ? String.valueOf(c) : null);
        }
        if (body.containsKey("orden")) {
            Object o = body.get("orden");
            pedido.setOrden(o != null ? String.valueOf(o) : null);
        }
        if (body.containsKey("total")) {
            Object t = body.get("total");
            if (t instanceof Number n) pedido.setTotal(n.doubleValue());
            else if (t != null) {
                try {
                    pedido.setTotal(Double.parseDouble(String.valueOf(t)));
                } catch (Exception ignored) {}
            }
        }

        Pedido saved = pedidoService.save(pedido);
        return ResponseEntity.ok(saved);
    }

    // Eliminar un pedido
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        Pedido pedido = pedidoService.findById(id);
        if (pedido == null) return ResponseEntity.notFound().build();
        pedidoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Pedidos pendientes (para panel bar/caja)
    @GetMapping("/pendientes")
    public ResponseEntity<List<Pedido>> pendientes() {
        return ResponseEntity.ok(pedidoService.obtenerPedidosPendientesBar());
    }

    // Pedidos completados
    @GetMapping("/completados")
    public ResponseEntity<List<Pedido>> completados() {
        return ResponseEntity.ok(pedidoService.obtenerPedidosCompletados());
    }

    // Pedidos completados no recogidos (campanita mesero)
    @GetMapping("/completados/no-recogidos")
    public ResponseEntity<List<Pedido>> completadosNoRecogidos() {
        return ResponseEntity.ok(pedidoService.obtenerPedidosCompletadosNoRecogidos());
    }

    // Marcar pedido como COMPLETADO (flujo BAR/CAJA)
    @PostMapping("/{id}/completar")
    public ResponseEntity<Void> marcarCompletado(@PathVariable Integer id) {
        pedidoService.marcarPedidoComoCompletadoBar(id);
        return ResponseEntity.ok().build();
    }

    // Revertir pedido a PENDIENTE (flujo BAR/CAJA)
    @PostMapping("/{id}/pendiente")
    public ResponseEntity<Void> marcarPendiente(@PathVariable Integer id) {
        pedidoService.marcarPedidoComoPendienteBar(id);
        return ResponseEntity.ok().build();
    }

    // Marcar pedido como recogido (solo flag booleano)
    @PostMapping("/{id}/recogido")
    public ResponseEntity<Void> marcarRecogido(@PathVariable Integer id) {
        pedidoService.marcarPedidoComoRecogido(id);
        return ResponseEntity.ok().build();
    }

    // Marcar pedido como pagado de forma sencilla (método por defecto)
    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> pagar(@PathVariable Integer id, @RequestBody(required = false) Map<String, Object> body) {
        Double monto = null;
        if (body != null) {
            Object m = body.get("montoRecibido");
            if (m instanceof Number n) monto = n.doubleValue();
            else if (m != null) {
                try {
                    monto = Double.parseDouble(String.valueOf(m));
                } catch (Exception ignored) {}
            }
        }
        try {
            pedidoService.marcarPedidoComoPagado(id, monto);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al pagar el pedido: " + e.getMessage());
        }
    }
}
