package com.sena.getback.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/caja")
public class CajaController {
    
    @GetMapping
    public String panelCaja(@RequestParam(required = false) String section, Model model) {
        String activeSection = (section != null && !section.isEmpty()) ? section : "inicio-caja";
        model.addAttribute("activeSection", activeSection);
        model.addAttribute("title", "Panel de Cajero - GETBACK");
        
        return "caja/panel_caja";
    }
}