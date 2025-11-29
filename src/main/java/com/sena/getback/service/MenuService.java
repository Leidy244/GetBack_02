package com.sena.getback.service;

import com.sena.getback.model.Menu;
import com.sena.getback.model.Categoria;
import com.sena.getback.repository.MenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpSession;
import com.sena.getback.model.Usuario;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final ActivityLogService activityLogService;
    private final InventarioService inventarioService;

    // Inyección por constructor
    public MenuService(MenuRepository menuRepository, ActivityLogService activityLogService,
                       InventarioService inventarioService) {
        this.menuRepository = menuRepository;
        this.activityLogService = activityLogService;
        this.inventarioService = inventarioService;
    }

    // Listar todos los productos
    public List<Menu> findAll() {
        return menuRepository.findAll();
    }

    // Listar productos disponibles
    public List<Menu> listarDisponibles() {
        return menuRepository.findAll().stream().filter(menu -> menu.getDisponible() != null && menu.getDisponible())
                .collect(Collectors.toList());
    }

    // Buscar producto por id
    public Menu findById(Long id) {
        return menuRepository.findById(id).orElse(null);
    }

    // Buscar producto por id retornando Optional
    public Optional<Menu> findByIdOptional(Long id) {
        return menuRepository.findById(id);
    }

    // Guardar o editar producto
    public Menu save(Menu producto) {
        boolean isUpdate = (producto.getId() != null);
        Menu saved = menuRepository.save(producto);
        try {
            String name = saved.getNombreProducto() != null ? saved.getNombreProducto() : ("#" + saved.getId());
            String user = getCurrentUsername();
            String msg = (isUpdate ? "Se actualizó el producto \"" : "Se creó el producto \"") + name + "\"";
            activityLogService.log("PRODUCT", msg, user, null);
        } catch (Exception ignored) {}
        return saved;
    }

    private String getCurrentUsername() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpSession session = attrs.getRequest().getSession(false);
                if (session != null) {
                    Object obj = session.getAttribute("usuarioLogueado");
                    if (obj instanceof Usuario u) {
                        String n = u.getNombre() != null ? u.getNombre().trim() : "";
                        String a = u.getApellido() != null ? (" " + u.getApellido().trim()) : "";
                        String full = (n + a).trim();
                        return full.isEmpty() ? (u.getCorreo() != null ? u.getCorreo() : "system") : full;
                    }
                }
            }
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return (auth != null && auth.getName() != null) ? auth.getName() : "system";
        } catch (Exception e) { return "system"; }
    }

    // Verificar si un producto existe por id
    public boolean existsById(Long id) {
        return menuRepository.existsById(id);
    }

    // Buscar productos por categoría
    public List<Menu> findByCategoria(Categoria categoria) {
        return menuRepository.findAll().stream().filter(menu -> categoria.equals(menu.getCategoria()))
                .collect(Collectors.toList());
    }

    // Buscar productos por nombre (búsqueda parcial)
    public List<Menu> findByNombreContaining(String nombre) {
        return menuRepository.findAll().stream()
                .filter(menu -> menu.getNombreProducto() != null
                        && menu.getNombreProducto().toLowerCase().contains(nombre.toLowerCase()))
                .collect(Collectors.toList());
    }

    // Buscar producto por nombre exacto (case-insensitive)
    public Menu findByNombreExact(String nombre) {
        if (nombre == null) return null;
        String target = nombre.trim().toLowerCase();
        return menuRepository.findAll().stream()
                .filter(menu -> menu.getNombreProducto() != null
                        && menu.getNombreProducto().trim().toLowerCase().equals(target))
                .findFirst()
                .orElse(null);
    }

    // Buscar productos por precio menor o igual
    public List<Menu> findByPrecioLessThanEqual(Double precio) {
        return menuRepository.findAll().stream().filter(menu -> menu.getPrecio() != null && menu.getPrecio() <= precio)
                .collect(Collectors.toList());
    }

    // Contar total de productos
    public long count() {
        return menuRepository.count();
    }

    // Contar productos disponibles
    public long countDisponibles() {
        return menuRepository.findAll().stream().filter(menu -> menu.getDisponible() != null && menu.getDisponible())
                .count();
    }

    public List<Menu> findByCategoriaNombre(String nombreCategoria) {
        if (nombreCategoria == null || nombreCategoria.equalsIgnoreCase("todos")) {
            return findAll();
        }

        return menuRepository.findAll().stream()
                .filter(menu -> menu.getCategoria() != null
                        && menu.getCategoria().getNombre() != null
                        && menu.getCategoria().getNombre().equalsIgnoreCase(nombreCategoria))
                .collect(Collectors.toList());
    }

    // 🔹 Contar cuántos productos hay por categoría
    public Map<String, Long> contarProductosPorCategoria() {
        return menuRepository.findAll().stream()
                .filter(menu -> menu.getCategoria() != null && menu.getCategoria().getNombre() != null)
                .collect(Collectors.groupingBy(
                        menu -> menu.getCategoria().getNombre(), // agrupa por nombre de categoría
                        Collectors.counting() // cuenta los productos de cada grupo
                ));
    }

    // Eliminar producto por id
    public void delete(Long id) {
        Menu m = findById(id);
        try { inventarioService.purgeByMenuId(id); } catch (Exception ignored) {}
        try { inventarioService.detachMenuReferences(id); } catch (Exception ignored) {}
        try { if (m != null && m.getNombreProducto() != null) inventarioService.purgeByProductName(m.getNombreProducto()); } catch (Exception ignored) {}
        menuRepository.deleteById(id);
        try {
            String user = getCurrentUsername();
            activityLogService.log("PRODUCT", "Se eliminó el producto #" + id, user, null);
        } catch (Exception ignored) {}
    }

    public void decrementarStockPorNombre(String nombreProducto, int cantidad) {
        if (nombreProducto == null || nombreProducto.isBlank() || cantidad <= 0) return;
        List<Menu> menus = menuRepository.findByNombreProductoIgnoreCase(nombreProducto.trim());
        if (menus == null || menus.isEmpty()) return;
        menus.forEach(m -> {
            boolean esBar = m.getCategoria() != null && m.getCategoria().getArea() != null &&
                    m.getCategoria().getArea().trim().equalsIgnoreCase("Bar");
            if (!esBar) return;
            int actual = m.getStock() != null ? m.getStock() : 0;
            int nuevo = Math.max(0, actual - cantidad);
            m.setStock(nuevo);
        });
        menuRepository.saveAll(menus);
    }
}
