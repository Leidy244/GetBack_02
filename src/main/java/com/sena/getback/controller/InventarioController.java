package com.sena.getback.controller;

import java.time.LocalDate;

import com.sena.getback.model.Inventario;
import com.sena.getback.model.Menu;
import com.sena.getback.service.InventarioService;
import com.sena.getback.service.MenuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventario")
public class InventarioController {

    private final InventarioService inventarioService;
    private final MenuService menuService;

    public InventarioController(InventarioService inventarioService, MenuService menuService) {
        this.inventarioService = inventarioService;
        this.menuService = menuService;
    }

    @GetMapping
    public String listarInventario(Model model) {
        model.addAttribute("ingresosInventario", inventarioService.listarIngresosRecientes());
        model.addAttribute("nuevoIngreso", new Inventario());
        // Proveer lista de productos del menú para autocompletar y nombres existentes en inventario
        model.addAttribute("products", menuService.findAll());
        model.addAttribute("nombresInventario", inventarioService.listarNombresProductosInventario());

        // Construir una lista unificada y deduplicada para el datalist (conserva orden: menú luego inventario)
        java.util.LinkedHashSet<String> nombresSet = new java.util.LinkedHashSet<>();
        menuService.findAll().forEach(m -> {
            if (m.getNombreProducto() != null && !m.getNombreProducto().isBlank()) {
                nombresSet.add(m.getNombreProducto().trim());
            }
        });
        inventarioService.listarNombresProductosInventario().forEach(n -> {
            if (n != null && !n.isBlank()) nombresSet.add(n.trim());
        });
        model.addAttribute("inventarioNombres", new java.util.ArrayList<>(nombresSet));

		// Stock total por producto e items en bajo stock (umbral fijo por ahora)
		model.addAttribute("stockPorProducto", inventarioService.calcularStockPorProducto());
		model.addAttribute("bajoStock", inventarioService.obtenerProductosBajoStock(5));
        model.addAttribute("activeSection", "inventario");
        model.addAttribute("title", "Gestión de Inventario");
        return "admin";
    }

    @PostMapping("/registrar")
    public String registrarIngresoInventario(@RequestParam("remision") String remision,
                                             @RequestParam("producto") String producto,
                                             @RequestParam(value = "productoId", required = false) Long productoId,
                                             @RequestParam("cantidad") Integer cantidad,
                                             @RequestParam(value = "fechaRemision", required = false) String fechaRemision,
                                             @RequestParam(value = "proveedor", required = false) String proveedor,
                                             @RequestParam(value = "telefonoProveedor", required = false) String telefonoProveedor,
                                             @RequestParam(value = "observaciones", required = false) String observaciones,
                                             RedirectAttributes redirectAttributes) {
        try {
            try {
                Inventario ingreso = new Inventario();
                ingreso.setRemision(remision);
                // Si vino un productoId válido preferimos ligar la entidad Menu y usar su nombre
                if (productoId != null && productoId > 0 && menuService.existsById(productoId)) {
                    Menu m = menuService.findById(productoId);
                    ingreso.setMenu(m);
                    ingreso.setProducto(m != null ? m.getNombreProducto() : producto);
                } else {
                    ingreso.setMenu(null);
                    ingreso.setProducto(producto);
                }
                ingreso.setCantidad(cantidad);
                if (fechaRemision != null && !fechaRemision.isBlank()) {
                    ingreso.setFechaRemision(LocalDate.parse(fechaRemision));
                }
                ingreso.setProveedor(proveedor);
                ingreso.setTelefonoProveedor(telefonoProveedor);
                ingreso.setObservaciones(observaciones);
                inventarioService.registrarIngreso(ingreso);

                redirectAttributes.addFlashAttribute("success", "Ingreso de inventario registrado correctamente");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Error al registrar inventario: " + e.getMessage());
            }

            return "redirect:/inventario";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al registrar inventario: " + e.getMessage());
        }

        return "redirect:/inventario";
    }

    @PostMapping("/actualizar")
    public String actualizarIngresoInventario(@RequestParam("id") Long id,
                                              @RequestParam("remision") String remision,
                                              @RequestParam("producto") String producto,
                                              @RequestParam(value = "productoId", required = false) Long productoId,
                                              @RequestParam("cantidad") Integer cantidad,
                                              @RequestParam(value = "fechaRemision", required = false) String fechaRemision,
                                              @RequestParam(value = "proveedor", required = false) String proveedor,
                                              @RequestParam(value = "telefonoProveedor", required = false) String telefonoProveedor,
                                              @RequestParam(value = "observaciones", required = false) String observaciones,
                                              RedirectAttributes redirectAttributes) {

        try {
            Inventario ingreso = inventarioService.obtenerPorId(id).orElse(null);
            if (ingreso == null) {
                redirectAttributes.addFlashAttribute("error", "Registro de inventario no encontrado");
                return "redirect:/inventario";
            }

            ingreso.setRemision(remision);
            if (productoId != null && productoId > 0 && menuService.existsById(productoId)) {
                Menu m = menuService.findById(productoId);
                ingreso.setMenu(m);
                ingreso.setProducto(m != null ? m.getNombreProducto() : producto);
            } else {
                ingreso.setMenu(null);
                ingreso.setProducto(producto);
            }
            ingreso.setCantidad(cantidad);
            if (fechaRemision != null && !fechaRemision.isBlank()) {
                ingreso.setFechaRemision(LocalDate.parse(fechaRemision));
            }
            ingreso.setProveedor(proveedor);
            ingreso.setTelefonoProveedor(telefonoProveedor);
            ingreso.setObservaciones(observaciones);
            inventarioService.actualizarIngreso(ingreso);

            redirectAttributes.addFlashAttribute("success", "Ingreso de inventario actualizado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar inventario: " + e.getMessage());
        }

        return "redirect:/inventario";
    }

    @PostMapping("/eliminar")
    public String eliminarIngresoInventario(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            inventarioService.eliminarIngreso(id);
            redirectAttributes.addFlashAttribute("success", "Registro de inventario eliminado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar inventario: " + e.getMessage());
        }
        return "redirect:/inventario";
    }
}
