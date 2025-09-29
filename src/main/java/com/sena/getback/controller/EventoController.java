package com.sena.getback.controller;

import com.sena.getback.model.Evento;
import com.sena.getback.repository.EventoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/eventos")
public class EventoController {

    private final EventoRepository eventoRepository;

    public EventoController(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Evento evento, RedirectAttributes redirect) {
        try {
            eventoRepository.save(evento);
            redirect.addFlashAttribute("success", "Evento guardado correctamente");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al guardar evento: " + e.getMessage());
        }
        return "redirect:/admin?activeSection=events";
    }

    @PostMapping("/editar/{id}")
    public String editar(@PathVariable Integer id,
                         @ModelAttribute Evento evento,
                         RedirectAttributes redirect) {
        try {
            evento.setId(id);
            eventoRepository.save(evento);
            redirect.addFlashAttribute("success", "Evento actualizado correctamente");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al actualizar evento: " + e.getMessage());
        }
        return "redirect:/admin?activeSection=events";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            eventoRepository.deleteById(id);
            redirect.addFlashAttribute("success", "Evento eliminado correctamente");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al eliminar evento: " + e.getMessage());
        }
        return "redirect:/admin?activeSection=events";
    }
}