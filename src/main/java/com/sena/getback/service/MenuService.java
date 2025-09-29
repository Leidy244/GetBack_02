package com.sena.getback.service;

import com.sena.getback.model.Menu;
import com.sena.getback.repository.MenuRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {

	private final MenuRepository menuRepository;

	public MenuService(MenuRepository menuRepository) {
		this.menuRepository = menuRepository;
	}

	public List<Menu> listarTodos() {
		return menuRepository.findAll();
	}

	public List<Menu> listarDisponibles() {
		return menuRepository.findByDisponibleTrue();
	}

	public Menu guardar(Menu menu) {
		return menuRepository.save(menu);
	}

	public void eliminar(Integer id) {
		menuRepository.deleteById(id);
	}

	public Menu obtenerPorId(Integer id) {
		return menuRepository.findById(id).orElse(null);
	}

	public List<Menu> buscarPorCategoria(Integer categoriaId) {
		return menuRepository.findByCategoriaIdAndDisponibleTrue(categoriaId);
	}
}
