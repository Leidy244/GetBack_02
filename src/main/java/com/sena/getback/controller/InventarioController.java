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
    public String listarInventario(Model model,
                                   @RequestParam(name = "desde", required = false) LocalDate desde,
                                   @RequestParam(name = "hasta", required = false) LocalDate hasta,
                                   @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
                                   @RequestParam(name = "size", required = false, defaultValue = "10") Integer size) {
        var ingresosAll = inventarioService.listarIngresosRecientes()
                .stream()
                .filter(i -> i.getRemision() == null
                        || !"CONSUMO PEDIDO".equalsIgnoreCase(i.getRemision()))
                .toList();

        var ingresosFiltrados = ingresosAll.stream()
                .filter(i -> {
                    if (desde != null && i.getFechaRemision() != null && i.getFechaRemision().isBefore(desde)) return false;
                    if (hasta != null && i.getFechaRemision() != null && i.getFechaRemision().isAfter(hasta)) return false;
                    return true;
                })
                .sorted((a,b) -> {
                    var fa = a.getFechaIngreso();
                    var fb = b.getFechaIngreso();
                    if (fa == null && fb == null) return 0;
                    if (fa == null) return 1;
                    if (fb == null) return -1;
                    return fb.compareTo(fa);
                })
                .toList();

        int totalItems = ingresosFiltrados.size();
        int safeSize = (size != null && size > 0) ? size : 10;
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) safeSize));
        int safePage = (page != null && page > 0) ? page : 1;
        if (safePage > totalPages) safePage = totalPages;
        int startIdx = Math.min((safePage - 1) * safeSize, Math.max(0, totalItems));
        int endIdx = Math.min(startIdx + safeSize, totalItems);
        var pageContent = totalItems > 0 ? ingresosFiltrados.subList(startIdx, endIdx) : java.util.List.<Inventario>of();

        model.addAttribute("ingresosInventario", pageContent);
        model.addAttribute("historialInventario", ingresosFiltrados);
        model.addAttribute("ingresosTodos", ingresosAll);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("page", safePage);
        model.addAttribute("size", safeSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("nuevoIngreso", new Inventario());
        // Proveer lista de productos del menú SOLO área BAR para autocompletar
        var productosBar = menuService.findAll().stream()
                .filter(m -> {
                    var cat = m.getCategoria();
                    String area = cat != null ? cat.getArea() : null;
                    String nombreCat = cat != null ? cat.getNombre() : null;
                    boolean areaBar = area != null && area.trim().equalsIgnoreCase("Bar");
                    boolean nombreBarHeur = nombreCat != null && nombreCat.toLowerCase().matches(".*(bebida|bar|licor).*");
                    return areaBar || (area == null && nombreBarHeur);
                })
                .toList();
        model.addAttribute("products", productosBar);
        // Evitar sugerencias desde inventario para mantener nombres consistentes, usar solo menú BAR
        model.addAttribute("nombresInventario", java.util.Collections.emptyList());

        // Construir una lista unificada y deduplicada para el datalist (conserva orden: menú luego inventario)
        java.util.LinkedHashSet<String> nombresSet = new java.util.LinkedHashSet<>();
        menuService.findAll().forEach(m -> {
            boolean esBar = m.getCategoria() != null && m.getCategoria().getArea() != null &&
                    m.getCategoria().getArea().trim().equalsIgnoreCase("Bar");
            if (esBar && m.getNombreProducto() != null && !m.getNombreProducto().isBlank()) {
                nombresSet.add(m.getNombreProducto().trim());
            }
        });
        // No mezclar nombres desde inventario: datalist solo con nombres del menú BAR
        model.addAttribute("inventarioNombres", new java.util.ArrayList<>(nombresSet));

		// Stock total por producto e items en bajo stock (umbral fijo por ahora)
        int stockThreshold = 5;
        var stockPorProducto = inventarioService.calcularStockPorProducto();
        // Canonical: clave en minúsculas y trim y SUMA valores por clave canónica
        java.util.Map<String,Integer> stockCanon = new java.util.HashMap<>();
        stockPorProducto.forEach((k,v) -> {
            if (k != null) {
                String canon = k.trim().toLowerCase();
                int val = v != null ? v : 0;
                stockCanon.merge(canon, val, Integer::sum);
            }
        });
        // Clamp a >= 0
        stockCanon.replaceAll((k,v) -> Math.max(0, v));

        // Filtrar bajo stock sólo para productos existentes en menú (evita mostrar los eliminados)
        java.util.Set<String> menuCanon = new java.util.HashSet<>();
        menuService.findAll().forEach(m -> {
            boolean esBar = m.getCategoria() != null && m.getCategoria().getArea() != null &&
                    m.getCategoria().getArea().trim().equalsIgnoreCase("Bar");
            if (esBar && m.getNombreProducto() != null && !m.getNombreProducto().isBlank()) {
                menuCanon.add(m.getNombreProducto().trim().toLowerCase());
            }
        });
        java.util.Map<String,Integer> bajoStockDisplay = new java.util.LinkedHashMap<>();
        java.util.Map<String,Integer> bajoStockCanon = new java.util.HashMap<>();
        // Construir mapa de nombre mostrado -> stock
        java.util.Map<String,String> canonToDisplay = new java.util.HashMap<>();
        menuService.findAll().forEach(m -> {
            boolean esBar = m.getCategoria() != null && m.getCategoria().getArea() != null &&
                    m.getCategoria().getArea().trim().equalsIgnoreCase("Bar");
            if (esBar && m.getNombreProducto() != null && !m.getNombreProducto().isBlank()) {
                canonToDisplay.put(m.getNombreProducto().trim().toLowerCase(), m.getNombreProducto().trim());
            }
        });
        stockCanon.forEach((k,v) -> {
            if (menuCanon.contains(k) && v <= stockThreshold) {
                String display = canonToDisplay.getOrDefault(k, k);
                bajoStockDisplay.put(display, v);
                bajoStockCanon.put(k, v);
            }
        });

        model.addAttribute("stockPorProducto", stockPorProducto);
        model.addAttribute("stockCanon", stockCanon);
        model.addAttribute("bajoStock", bajoStockDisplay);
        model.addAttribute("bajoStockCanon", bajoStockCanon);
        model.addAttribute("stockThreshold", stockThreshold);

        // Alerta: productos sin rotación (sin movimientos) por más de X días
        int rotacionThresholdDias = 30;
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        java.util.Map<String, java.time.LocalDateTime> ultimaFechaCanon = new java.util.HashMap<>();
        ingresosAll.forEach(inv -> {
            String prod = inv.getProducto();
            java.time.LocalDateTime fi = inv.getFechaIngreso();
            if (prod != null && fi != null) {
                String canon = prod.trim().toLowerCase();
                java.time.LocalDateTime prev = ultimaFechaCanon.get(canon);
                if (prev == null || fi.isAfter(prev)) {
                    ultimaFechaCanon.put(canon, fi);
                }
            }
        });
        java.util.Map<String, Integer> sinRotacionDisplay = new java.util.LinkedHashMap<>();
        ultimaFechaCanon.forEach((canon, last) -> {
            long dias = java.time.Duration.between(last, ahora).toDays();
            if (dias >= rotacionThresholdDias && menuCanon.contains(canon)) {
                String display = canonToDisplay.getOrDefault(canon, canon);
                sinRotacionDisplay.put(display, (int) dias);
            }
        });
        model.addAttribute("sinRotacion", sinRotacionDisplay);
        model.addAttribute("rotacionThresholdDias", rotacionThresholdDias);
        model.addAttribute("activeSection", "inventario");
        model.addAttribute("title", "Gestión de Inventario");
        return "admin";
    }

    @GetMapping("/movimientos")
    public String listarMovimientos(Model model,
                                    @RequestParam(name = "desde", required = false) LocalDate desde,
                                    @RequestParam(name = "hasta", required = false) LocalDate hasta,
                                    @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
                                    @RequestParam(name = "size", required = false, defaultValue = "10") Integer size) {
        var ingresosAll = inventarioService.listarIngresosRecientes();

        var ingresosFiltrados = ingresosAll.stream()
                .filter(i -> {
                    if (desde != null && i.getFechaRemision() != null && i.getFechaRemision().isBefore(desde)) return false;
                    if (hasta != null && i.getFechaRemision() != null && i.getFechaRemision().isAfter(hasta)) return false;
                    return true;
                })
                .sorted((a,b) -> {
                    var fa = a.getFechaIngreso();
                    var fb = b.getFechaIngreso();
                    if (fa == null && fb == null) return 0;
                    if (fa == null) return 1;
                    if (fb == null) return -1;
                    return fb.compareTo(fa);
                })
                .toList();

        int totalItems = ingresosFiltrados.size();
        int safeSize = (size != null && size > 0) ? size : 10;
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) safeSize));
        int safePage = (page != null && page > 0) ? page : 1;
        if (safePage > totalPages) safePage = totalPages;
        int startIdx = Math.min((safePage - 1) * safeSize, Math.max(0, totalItems));
        int endIdx = Math.min(startIdx + safeSize, totalItems);
        var pageContent = totalItems > 0 ? ingresosFiltrados.subList(startIdx, endIdx) : java.util.List.<Inventario>of();

        model.addAttribute("ingresosInventario", pageContent);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("page", safePage);
        model.addAttribute("size", safeSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("activeSection", "movimientosInventario");
        model.addAttribute("title", "Movimientos de Inventario");
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
                ingreso.setCantidad(cantidad != null && cantidad > 0 ? cantidad : 1);
                if (fechaRemision != null && !fechaRemision.isBlank()) {
                    ingreso.setFechaRemision(LocalDate.parse(fechaRemision));
                }
                ingreso.setProveedor(proveedor);
                ingreso.setTelefonoProveedor(telefonoProveedor);
                ingreso.setObservaciones(observaciones);
                ingreso.setFechaIngreso(java.time.LocalDateTime.now());
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
            ingreso.setCantidad(cantidad != null && cantidad > 0 ? cantidad : 1);
            if (fechaRemision != null && !fechaRemision.isBlank()) {
                ingreso.setFechaRemision(LocalDate.parse(fechaRemision));
            }
            ingreso.setProveedor(proveedor);
            ingreso.setTelefonoProveedor(telefonoProveedor);
            ingreso.setObservaciones(observaciones);
            if (ingreso.getFechaIngreso() == null) {
                ingreso.setFechaIngreso(java.time.LocalDateTime.now());
            }
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
