package com.sena.getback.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/caja")
public class CajaController {

    @GetMapping
    public String panelCaja(Model model) {
        model.addAttribute("title", "Panel de Cajero");
        return "caja/panel_caja";
    }
}