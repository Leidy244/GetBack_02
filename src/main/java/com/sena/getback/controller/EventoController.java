package com.sena.getback.controller;

import com.sena.getback.model.Evento;
import com.sena.getback.service.EventoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    // Listar eventos
    @GetMapping
    public String listarEventos(Model model) {
        model.addAttribute("eventos", eventoService.listarEventos());
        return "admin/fragments/events"; // tu fragmento de eventos
    }

    // Guardar nuevo evento
    @PostMapping("/guardar")
    public String guardarEvento(@ModelAttribute Evento evento,
                                RedirectAttributes redirectAttributes) {
        try {
            eventoService.guardar(evento);
            redirectAttributes.addFlashAttribute("success", "Evento guardado correctamente ✅");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar el evento ❌");
        }
        return "redirect:/admin/eventos";
    }

    // Editar evento
    @PostMapping("/editar/{id}")
    public String editarEvento(@PathVariable Long id,
                               @ModelAttribute Evento evento,
                               RedirectAttributes redirectAttributes) {
        try {
            evento.setId(id);
            eventoService.guardar(evento);
            redirectAttributes.addFlashAttribute("success", "Evento actualizado correctamente ✏️");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el evento ❌");
        }
        return "redirect:/admin/eventos";
    }

    // Eliminar evento
    @GetMapping("/eliminar/{id}")
    public String eliminarEvento(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            eventoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Evento eliminado correctamente 🗑️");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el evento ❌");
        }
        return "redirect:/admin/eventos";
    }
}
