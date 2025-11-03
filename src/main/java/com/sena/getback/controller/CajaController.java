package com.sena.getback.controller;

import com.sena.getback.service.MenuService;
import com.sena.getback.service.CategoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/caja")
public class CajaController {

    private final MenuService menuService;
    private final CategoriaService categoriaService;

    public CajaController(MenuService menuService, CategoriaService categoriaService) {
        this.menuService = menuService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String panelCaja(
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String categoria,
            Model model) {

        String activeSection = (section != null && !section.isEmpty()) ? section : "inicio-caja";
        model.addAttribute("activeSection", activeSection);
        model.addAttribute("title", "Panel de Cajero - GETBACK");

        if ("punto-venta".equals(activeSection)) {
            model.addAttribute("categorias", categoriaService.findAll());

            if (categoria != null && !categoria.isEmpty()) {
                model.addAttribute("menus", menuService.findByCategoriaNombre(categoria));
            } else {
                model.addAttribute("menus", menuService.findAll());
            }
        }

        return "caja/panel_caja";
    }


}
