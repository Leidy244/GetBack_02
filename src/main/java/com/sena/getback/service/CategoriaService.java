package com.sena.getback.service;

import com.sena.getback.model.Categoria;
import com.sena.getback.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

	@Autowired
	private CategoriaRepository categoriaRepository;

	// Listar todas las categorías
	public List<Categoria> findAll() {
		return categoriaRepository.findAll();
	}

	// Alias para compatibilidad con diferentes nombres
	public List<Categoria> listar() {
		return findAll();
	}

	// Buscar categoría por id
	public Categoria findById(Integer id) {
		return categoriaRepository.findById(id).orElse(null);
	}

	// Buscar categoría por id retornando Optional
	public Optional<Categoria> findByIdOptional(Integer id) {
		return categoriaRepository.findById(id);
	}

	// Guardar o actualizar categoría
	public Categoria save(Categoria categoria) {
		return categoriaRepository.save(categoria);
	}

	// Eliminar categoría por id
	public void delete(Integer id) {
		categoriaRepository.deleteById(id);
	}

	// Verificar si existe categoría por id
	public boolean existsById(Integer id) {
		return categoriaRepository.existsById(id);
	}

	// Buscar categoría por nombre
	public Categoria findByNombre(String nombre) {
		return categoriaRepository.findAll().stream()
				.filter(categoria -> nombre.equalsIgnoreCase(categoria.getNombre())).findFirst().orElse(null);
	}

	// Contar total de categorías
	public long count() {
		return categoriaRepository.count();
	}
}