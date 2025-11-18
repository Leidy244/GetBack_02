package com.sena.getback.controller;

import com.sena.getback.model.Evento;
import com.sena.getback.service.EventoService;
import com.sena.getback.service.UploadFileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/eventos")
public class EventoController {

	private final EventoService eventoService;
	private final UploadFileService uploadFileService;

	public EventoController(EventoService eventoService, UploadFileService uploadFileService) {
		this.eventoService = eventoService;
		this.uploadFileService = uploadFileService;
	}

	// Listar eventos en admin
	@GetMapping
	public String listarEventos(Model model) {
		List<Evento> eventos = eventoService.findAll();
		model.addAttribute("eventos", eventos);
		model.addAttribute("activeSection", "events");
		return "admin";
	}

	// Guardar nuevo evento
	@PostMapping("/guardar")
	public String guardarEvento(@ModelAttribute Evento evento, @RequestParam("file") MultipartFile file,
			RedirectAttributes redirectAttributes) {
		try {
			if (!file.isEmpty()) {
				String fileName = uploadFileService.saveImages(file, evento.getTitulo());
				evento.setImagen("/images/" + fileName);
			}
			eventoService.save(evento);
			redirectAttributes.addFlashAttribute("success", "Evento guardado correctamente");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al guardar el evento");
		}
		return "redirect:/admin/eventos";
	}

	@GetMapping("/toggle-estado/{id}")
	public String toggleEstadoEvento(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			eventoService.toggleEventoEstado(id);
			redirectAttributes.addFlashAttribute("success", "Estado del evento actualizado correctamente");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al actualizar estado del evento");
		}
		return "redirect:/admin/eventos";
	}

	// Editar evento existente
	@PostMapping("/editar/{id}")
	public String editarEvento(@PathVariable Long id, @ModelAttribute Evento evento,
			@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
		try {
			evento.setId(id);

			if (!file.isEmpty()) {
				// guardar nueva imagen
				String fileName = uploadFileService.saveImages(file, evento.getTitulo());
				evento.setImagen("/images/" + fileName);
			} else {
				// mantener imagen anterior
				Evento existente = eventoService.findById(id).orElse(null);
				if (existente != null) {
					evento.setImagen(existente.getImagen());
				}
			}

			eventoService.save(evento);
			redirectAttributes.addFlashAttribute("success", "Evento actualizado correctamente");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al actualizar el evento");
		}
		return "redirect:/admin/eventos";
	}

	// Eliminar evento
	@GetMapping("/eliminar/{id}")
	public String eliminarEvento(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			Evento evento = eventoService.findById(id).orElse(null);
			if (evento != null) {
				if (evento.getImagen() != null) {
					String nombreArchivo = evento.getImagen().replace("/images/", "");
					uploadFileService.deleteImage(nombreArchivo);
				}
				eventoService.deleteById(id);
			}
			redirectAttributes.addFlashAttribute("success", "Evento eliminado correctamente");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al eliminar el evento");
		}
		return "redirect:/admin/eventos";
	}

	// Ver página cliente
	@GetMapping("/ver-cliente")
	public String verCliente() {
		return "redirect:/cliente";
	}
}
