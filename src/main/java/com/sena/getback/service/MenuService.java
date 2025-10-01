package com.sena.getback.service;

import com.sena.getback.model.Menu;
import com.sena.getback.repository.MenuRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {

    private final MenuRepository menuRepository;

    // Inyección por constructor
    public MenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    // 🔹 Listar todos los productos
    public List<Menu> findAll() {
        return menuRepository.findAll();
    }

    // 🔹 Buscar producto por id (devuelve Menu o null, no Optional)
    public Menu findById(Long id) {
        return menuRepository.findById(id).orElse(null);
    }

    // 🔹 Guardar o editar producto
    public Menu save(Menu producto) {
        return menuRepository.save(producto);
    }

    // 🔹 Eliminar producto por id
    public void delete(Long id) {
        menuRepository.deleteById(id);
    }

    // 🔹 Verificar si un producto existe por id (opcional, pero útil)
    public boolean existsById(Long id) {
        return menuRepository.existsById(id);
    }
}
