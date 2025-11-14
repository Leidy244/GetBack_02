package com.sena.getback.service;

import com.sena.getback.model.Menu;
import com.sena.getback.model.Categoria;
import com.sena.getback.repository.MenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final ActivityLogService activityLogService;

    // Inyección por constructor
    public MenuService(MenuRepository menuRepository, ActivityLogService activityLogService) {
        this.menuRepository = menuRepository;
        this.activityLogService = activityLogService;
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
        menuRepository.deleteById(id);
        try {
            String user = getCurrentUsername();
            activityLogService.log("PRODUCT", "Se eliminó el producto #" + id, user, null);
        } catch (Exception ignored) {}
    }
}