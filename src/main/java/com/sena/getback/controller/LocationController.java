package com.sena.getback.controller;

import com.sena.getback.model.Location;
import com.sena.getback.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class LocationController {

    @Autowired
    private LocationService locationService;

    @GetMapping("/locations")
    public String showLocationsPage(Model model) {
        List<Location> locations = locationService.findAll();
        model.addAttribute("locations", locations);
        model.addAttribute("location", new Location());
        model.addAttribute("totalUbicaciones", locations.size());
        model.addAttribute("activeSection", "locations");
        model.addAttribute("title", "Gestión de Ubicaciones");
        return "admin";
    }

    @PostMapping("/locations/save")
    public String saveLocation(@ModelAttribute Location location, RedirectAttributes redirectAttributes) {
        try {
            // Validar si ya existe una ubicación con el mismo nombre
            if (location.getId() == null) {
                // Creación nueva
                if (locationService.existsByNombre(location.getNombre())) {
                    redirectAttributes.addFlashAttribute("error", "Ya existe una ubicación con el nombre: " + location.getNombre());
                    return "redirect:/admin?activeSection=locations";
                }
            } else {
                // Actualización
                if (locationService.existsByNombreAndIdNot(location.getNombre(), location.getId())) {
                    redirectAttributes.addFlashAttribute("error", "Ya existe otra ubicación con el nombre: " + location.getNombre());
                    return "redirect:/admin?activeSection=locations";
                }
            }

            locationService.save(location);
            redirectAttributes.addFlashAttribute("success", 
                location.getId() == null ? "Ubicación creada exitosamente" : "Ubicación actualizada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar la ubicación: " + e.getMessage());
        }
        return "redirect:/admin?activeSection=locations";
    }

    @GetMapping("/locations/edit/{id}")
    public String editLocation(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Optional<Location> location = locationService.findById(id);
        if (location.isPresent()) {
            redirectAttributes.addFlashAttribute("location", location.get());
        } else {
            redirectAttributes.addFlashAttribute("error", "Ubicación no encontrada");
        }
        return "redirect:/admin?activeSection=locations";
    }

    @GetMapping("/locations/delete/{id}")
    public String deleteLocation(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Location> location = locationService.findById(id);
            if (location.isPresent()) {
                locationService.deleteById(id);
                redirectAttributes.addFlashAttribute("success", "Ubicación eliminada exitosamente: " + location.get().getNombre());
            } else {
                redirectAttributes.addFlashAttribute("error", "Ubicación no encontrada");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la ubicación: " + e.getMessage());
        }
        return "redirect:/admin?activeSection=locations";
    }

    @GetMapping("/locations/cancel")
    public String cancelEdit(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("location", new Location());
        return "redirect:/admin?activeSection=locations";
    }
}