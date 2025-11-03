package com.sena.getback.service;

import com.sena.getback.model.Menu;
import com.sena.getback.model.Categoria;
import com.sena.getback.repository.MenuRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MenuService {

	private final MenuRepository menuRepository;

	// Inyección por constructor
	public MenuService(MenuRepository menuRepository) {
		this.menuRepository = menuRepository;
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
		return menuRepository.save(producto);
	}

	// Eliminar producto por id
	public void delete(Long id) {
		menuRepository.deleteById(id);
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


}