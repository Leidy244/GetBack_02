package com.sena.getback.controller;

import com.sena.getback.model.Evento;
import com.sena.getback.model.Menu; // tu modelo de productos
import com.sena.getback.service.EventoService;
import com.sena.getback.service.MenuService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ClienteController {

    private final EventoService eventoService;
    private final MenuService menuService; 

    public ClienteController(EventoService eventoService, MenuService menuService) {
        this.eventoService = eventoService;
        this.menuService = menuService;
    }

    // Página cliente (vista pública)
    @GetMapping("/cliente")
    public String mostrarPaginaCliente(Model model) {
        // Eventos
        List<Evento> eventos = eventoService.findAll();
        model.addAttribute("eventos", eventos);

        // Productos (Menú)
        List<Menu> productos = menuService.findAll(); 
        model.addAttribute("productos", productos);

        return "cliente/pagina_cliente";
    }
}
