package com.sena.getback.service;

import com.sena.getback.model.Mesa;
import com.sena.getback.repository.MesaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesaService {

	private final MesaRepository mesaRepository;

	public MesaService(MesaRepository mesaRepository) {
		this.mesaRepository = mesaRepository;
	}

	// Listar todas las mesas
	public List<Mesa> findAll() {
		return mesaRepository.findAll();
	}

	// Buscar mesa por id
	public Mesa findById(Integer id) {
		return mesaRepository.findById(id).orElse(null);
	}

	// Guardar o editar mesa
	public Mesa save(Mesa mesa) {
		return mesaRepository.save(mesa);
	}

	// Eliminar mesa por id
	public void delete(Integer id) {
		mesaRepository.deleteById(id);
	}

	// Buscar mesas por ubicación
	public List<Mesa> findByUbicacion(String ubicacion) {
		return mesaRepository.findByUbicacionContainingIgnoreCase(ubicacion);
	}
}