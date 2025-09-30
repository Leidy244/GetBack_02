package com.sena.getback.controller;


import com.sena.getback.service.EventoService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClienteController {

    private final EventoService eventoService;

    public ClienteController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    // Página pública (landing con eventos)
    @GetMapping("/cliente")
    public String paginaCliente(Model model) {
        model.addAttribute("eventos", eventoService.listarEventos());
        return "cliente/pagina_cliente";  
        // 👆 apunta a templates/cliente/pagina_cliente.html
    }
}
