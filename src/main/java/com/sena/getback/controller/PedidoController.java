package com.sena.getback.controller;

import com.sena.getback.model.*;
import com.sena.getback.repository.*;
import com.sena.getback.service.PedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

        // En este flujo de administración usamos el campo "orden" para guardar un comentario simple
        if (comentario != null && !comentario.isBlank()) {
            pedido.setOrden(comentario.trim());
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
