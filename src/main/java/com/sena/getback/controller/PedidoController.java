package com.sena.getback.controller;

import com.sena.getback.model.*;
import com.sena.getback.repository.*;
import com.sena.getback.service.PedidoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final UsuarioRepository usuarioRepository;
    private final MenuRepository menuRepository;
    private final MesaRepository mesaRepository;
    private final EstadoRepository estadoRepository;

    public PedidoController(PedidoService pedidoService,
                            UsuarioRepository usuarioRepository,
                            MenuRepository menuRepository,
                            MesaRepository mesaRepository,
                            EstadoRepository estadoRepository) {
        this.pedidoService = pedidoService;
        this.usuarioRepository = usuarioRepository;
        this.menuRepository = menuRepository;
        this.mesaRepository = mesaRepository;
        this.estadoRepository = estadoRepository;
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Integer id,
                          @RequestParam(required = false) String comentario,
                          @RequestParam Long usuarioId,
                          @RequestParam Long menuId,
                          @RequestParam Integer mesaId,
                          @RequestParam Integer estadoId,
                          RedirectAttributes redirect) {

        Pedido pedido = (id != null) ? pedidoService.findById(id) : new Pedido();
        if (pedido == null) {
            pedido = new Pedido();
        }

        // En este flujo de administración, si hay comentario, lo integramos en un JSON estándar
        // para el campo "orden" con la estructura { "items": [...], "total": ..., "comentarios": "..." }
        if (comentario != null && !comentario.isBlank()) {
            String trimmed = comentario.trim();
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> data;

                // Si ya existe un JSON en orden (por ejemplo creado desde el flujo de mesero),
                // lo reutilizamos y solo añadimos/actualizamos la clave "comentarios".
                if (pedido.getOrden() != null && !pedido.getOrden().isBlank()) {
                    Object root = mapper.readValue(pedido.getOrden(), Object.class);
                    if (root instanceof Map<?, ?> rootMap) {
                        data = new HashMap<>();
                        for (Map.Entry<?, ?> e : rootMap.entrySet()) {
                            if (e.getKey() != null) {
                                data.put(String.valueOf(e.getKey()), e.getValue());
                            }
                        }
                    } else {
                        data = new HashMap<>();
                        data.put("items", root);
                    }
                } else {
                    data = new HashMap<>();
                }

                // Asegurar estructura mínima
                if (!data.containsKey("items")) {
                    data.put("items", java.util.Collections.emptyList());
                }
                if (!data.containsKey("total")) {
                    data.put("total", 0);
                }

                data.put("comentarios", trimmed);
                String ordenJson = mapper.writeValueAsString(data);
                pedido.setOrden(ordenJson);
            } catch (Exception e) {
                // Fallback sencillo en caso de error al parsear/serializar
                String safeComment = trimmed.replace("\"", "\\\"");
                String fallbackJson = "{\"items\":[],\"total\":0,\"comentarios\":\"" + safeComment + "\"}";
                pedido.setOrden(fallbackJson);
            }

            // También reflejamos el comentario en el campo dedicado de la entidad
            pedido.setComentariosGenerales(trimmed);
        }

        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        Menu menu = menuRepository.findById(menuId).orElse(null);
        Mesa mesa = mesaRepository.findById(mesaId).orElse(null);
        Estado estado = estadoRepository.findById(estadoId).orElse(null);

        pedido.setUsuario(usuario);
        pedido.setMenu(menu);
        pedido.setMesa(mesa);
        pedido.setEstado(estado);

        boolean nuevo = (pedido.getId() == null);
        pedidoService.save(pedido);

        redirect.addFlashAttribute("success", nuevo ? "Pedido creado correctamente" : "Pedido actualizado correctamente ✏️");
        return "redirect:/admin?activeSection=pedidos";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirect) {
        pedidoService.delete(id);
        redirect.addFlashAttribute("success", "Pedido eliminado correctamente 🗑️");
        return "redirect:/admin?activeSection=pedidos";
    }
}
