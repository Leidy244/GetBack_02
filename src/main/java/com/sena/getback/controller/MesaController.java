package com.sena.getback.controller;

import com.sena.getback.model.Mesa;
import com.sena.getback.model.Location;
import com.sena.getback.service.MesaService;
import com.sena.getback.service.LocationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class MesaController {

    private static final Logger logger = LoggerFactory.getLogger(MesaController.class);

    @Autowired
    private MesaService mesaService;

    @Autowired
    private LocationService locationService;

    // 📌 Mostrar página de gestión de mesas
    @GetMapping("/mesas")
    public String showMesasPage(Model model) {
        logger.info("Cargando página de gestión de mesas");
        try {
            cargarDatosMesas(model);
            model.addAttribute("activeSection", "mesas");
            model.addAttribute("title", "Gestión de Mesas");

            if (!model.containsAttribute("mesa")) {
                model.addAttribute("mesa", new Mesa());
            }
        } catch (Exception e) {
            logger.error("Error al cargar página de mesas: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar las mesas: " + e.getMessage());
            cargarDatosPorDefecto(model);
        }
        return "admin";
    }

    // 📌 Guardar o actualizar mesa
    @PostMapping("/mesas/save")
    public String saveMesa(@ModelAttribute("mesa") Mesa mesa,
                          RedirectAttributes redirectAttributes) {
        logger.info("Intentando guardar mesa: {}", mesa.getNumero());

        try {
            // 🧩 Validaciones básicas
            if (mesa.getNumero() == null || mesa.getNumero().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El número de mesa es obligatorio");
                redirectAttributes.addFlashAttribute("mesa", mesa);
                return "redirect:/admin/mesas";
            }

            if (mesa.getCapacidad() == null || mesa.getCapacidad() < 1 || mesa.getCapacidad() > 50) {
                redirectAttributes.addFlashAttribute("error", "La capacidad debe ser entre 1 y 50 personas");
                redirectAttributes.addFlashAttribute("mesa", mesa);
                return "redirect:/admin/mesas";
            }

            // 🧩 Limpiar y normalizar datos
            String numeroMesa = mesa.getNumero().trim().toUpperCase();
            mesa.setNumero(numeroMesa);
            if (mesa.getDescripcion() != null) {
                mesa.setDescripcion(mesa.getDescripcion().trim());
            }

            // 🧩 Estado por defecto o mantener actual
            if (mesa.getId() == null) {
                mesa.setEstado("DISPONIBLE");
            } else {
                mesaService.findById(mesa.getId()).ifPresent(m -> mesa.setEstado(m.getEstado()));
            }

            // 🧩 Validar número único por ubicación
            Integer ubicacionId = (mesa.getUbicacion() != null) ? mesa.getUbicacion().getId() : null;

            if (mesa.getId() == null) {
                if (mesaService.existsByNumeroAndUbicacion(mesa.getNumero(), ubicacionId)) {
                    String errorMsg = "Ya existe una mesa con el número " + mesa.getNumero() +
                            (mesa.getUbicacion() != null ? " en " + mesa.getUbicacion().getNombre() : "");
                    redirectAttributes.addFlashAttribute("error", errorMsg);
                    redirectAttributes.addFlashAttribute("mesa", mesa);
                    return "redirect:/admin/mesas";
                }
            } else {
                if (mesaService.existsByNumeroAndUbicacionAndIdNot(mesa.getNumero(), ubicacionId, mesa.getId())) {
                    String errorMsg = "Ya existe otra mesa con el número " + mesa.getNumero() +
                            (mesa.getUbicacion() != null ? " en " + mesa.getUbicacion().getNombre() : "");
                    redirectAttributes.addFlashAttribute("error", errorMsg);
                    redirectAttributes.addFlashAttribute("mesa", mesa);
                    return "redirect:/admin/mesas";
                }
            }

            // 🧩 Guardar mesa
            Mesa mesaGuardada = mesaService.save(mesa);
            String ubicacionNombre = (mesaGuardada.getUbicacion() != null) ? mesaGuardada.getUbicacion().getNombre() : null;
            String ubicacionTexto = (ubicacionNombre != null && !ubicacionNombre.trim().isEmpty()) ? (" en " + ubicacionNombre) : "";
            String mensaje = (mesa.getId() == null ? "Mesa creada exitosamente: " : "Mesa actualizada exitosamente: ")
                    + mesaGuardada.getNumero()
                    + ubicacionTexto;

            redirectAttributes.addFlashAttribute("success", mensaje);
            logger.info(mensaje);

        } catch (Exception e) {
            logger.error("Error al guardar mesa: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al guardar la mesa: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mesa", mesa);
        }

        return "redirect:/admin/mesas";
    }

    // 📌 Editar mesa
    @GetMapping("/mesas/edit/{id}")
    public String editMesa(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Mesa> mesaOpt = mesaService.findById(id);
            if (mesaOpt.isPresent()) {
                Mesa mesa = mesaOpt.get();
                if (mesa.estaOcupada()) {
                    redirectAttributes.addFlashAttribute("error",
                            "No se puede editar la mesa " + mesa.getNumero() + " porque está OCUPADA");
                } else {
                    redirectAttributes.addFlashAttribute("mesa", mesa);
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Mesa no encontrada");
            }
        } catch (Exception e) {
            logger.error("Error al editar mesa: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al editar la mesa: " + e.getMessage());
        }
        return "redirect:/admin/mesas";
    }

    // 📌 Eliminar mesa
    @GetMapping("/mesas/delete/{id}")
    public String deleteMesa(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Mesa> mesaOpt = mesaService.findById(id);
            if (mesaOpt.isPresent()) {
                Mesa mesa = mesaOpt.get();
                if (mesa.estaOcupada()) {
                    redirectAttributes.addFlashAttribute("error",
                            "No se puede eliminar la mesa " + mesa.getNumero() + " porque está OCUPADA");
                } else {
                    mesaService.deleteById(id);
                    redirectAttributes.addFlashAttribute("success",
                            "Mesa " + mesa.getNumero() + " eliminada exitosamente");
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Mesa no encontrada");
            }
        } catch (Exception e) {
            logger.error("Error al eliminar mesa: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al eliminar mesa: " + e.getMessage());
        }
        return "redirect:/admin/mesas";
    }

    // 📌 Cancelar edición
    @GetMapping("/mesas/cancel")
    public String cancelEdit(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("mesa", new Mesa());
        return "redirect:/admin/mesas";
    }

    // 📌 Cambiar estado de mesa
    @GetMapping("/mesas/ocupar/{id}")
    public String ocuparMesa(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        return cambiarEstadoMesa(id, "OCUPADA", redirectAttributes);
    }

    @GetMapping("/mesas/liberar/{id}")
    public String liberarMesa(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        return cambiarEstadoMesa(id, "DISPONIBLE", redirectAttributes);
    }

    private String cambiarEstadoMesa(Integer id, String nuevoEstado, RedirectAttributes redirectAttributes) {
        try {
            Optional<Mesa> mesaOpt = mesaService.findById(id);
            if (mesaOpt.isPresent()) {
                Mesa mesa = mesaOpt.get();
                if (nuevoEstado.equals(mesa.getEstado())) {
                    redirectAttributes.addFlashAttribute("error",
                            "La mesa ya está en estado " + nuevoEstado.toLowerCase());
                    return "redirect:/admin/mesas";
                }
                mesa.setEstado(nuevoEstado);
                mesaService.save(mesa);
                redirectAttributes.addFlashAttribute("success",
                        "Mesa " + mesa.getNumero() + " marcada como " + nuevoEstado);
            } else {
                redirectAttributes.addFlashAttribute("error", "Mesa no encontrada");
            }
        } catch (Exception e) {
            logger.error("Error al cambiar estado de mesa: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al cambiar estado: " + e.getMessage());
        }
        return "redirect:/admin/mesas";
    }

    // 📌 Cargar datos
    private void cargarDatosMesas(Model model) {
        List<Mesa> mesas = mesaService.findAll();
        List<Location> ubicaciones = locationService.findAll();

        mesas.sort((m1, m2) -> {
            if (m1.getNumero() == null) return 1;
            if (m2.getNumero() == null) return -1;
            return m1.getNumero().compareToIgnoreCase(m2.getNumero());
        });

        model.addAttribute("mesas", mesas);
        model.addAttribute("ubicaciones", ubicaciones);
        model.addAttribute("totalMesas", mesas.size());
        model.addAttribute("mesasDisponibles", contarMesasPorEstado(mesas, "DISPONIBLE"));
        model.addAttribute("mesasOcupadas", contarMesasPorEstado(mesas, "OCUPADA"));
    }

    private void cargarDatosPorDefecto(Model model) {
        model.addAttribute("mesas", Collections.emptyList());
        model.addAttribute("ubicaciones", Collections.emptyList());
        model.addAttribute("totalMesas", 0);
        model.addAttribute("mesasDisponibles", 0);
        model.addAttribute("mesasOcupadas", 0);
        model.addAttribute("mesa", new Mesa());
    }

    private long contarMesasPorEstado(List<Mesa> mesas, String estado) {
        return mesas.stream()
                .filter(m -> m != null && estado.equals(m.getEstado()))
                .count();
    }
}