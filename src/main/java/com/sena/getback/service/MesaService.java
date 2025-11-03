package com.sena.getback.service;

import com.sena.getback.model.Mesa;
import com.sena.getback.repository.MesaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MesaService {

    @Autowired
    private MesaRepository mesaRepository;

    // 🔹 Listar todas las mesas
    public List<Mesa> findAll() {
        return mesaRepository.findAll();
    }

    // 🔹 Buscar mesa por ID
    public Optional<Mesa> findById(Integer id) {
        return mesaRepository.findById(id);
    }

    // 🔹 Guardar o actualizar mesa
    public Mesa save(Mesa mesa) {
        return mesaRepository.save(mesa);
    }

    // 🔹 Eliminar mesa por ID
    public void deleteById(Integer id) {
        mesaRepository.deleteById(id);
    }

    // 🔹 Validar existencia por número y ubicación
    public boolean existsByNumeroAndUbicacion(String numero, Integer ubicacionId) {
        if (ubicacionId == null) {
            // Si no tiene ubicación asignada
            return mesaRepository.existsByNumeroAndUbicacionIsNull(numero);
        }
        return mesaRepository.existsByNumeroAndUbicacion_Id(numero, ubicacionId);
    }

    // 🔹 Validar existencia por número, ubicación e ID (para edición)
    public boolean existsByNumeroAndUbicacionAndIdNot(String numero, Integer ubicacionId, Integer id) {
        if (ubicacionId == null) {
            // Si no tiene ubicación asignada
            return mesaRepository.existsByNumeroAndUbicacionIsNull(numero);
        }
        return mesaRepository.existsByNumeroAndUbicacion_IdAndIdNot(numero, ubicacionId, id);
    }
    public void ocuparMesa(Integer mesaId) {
        mesaRepository.findById(mesaId).ifPresent(mesa -> {
            mesa.setEstado("OCUPADA");
            mesaRepository.save(mesa);
        });
    }

    public void liberarMesa(Integer mesaId) {
        mesaRepository.findById(mesaId).ifPresent(mesa -> {
            mesa.setEstado("DISPONIBLE");
            mesaRepository.save(mesa);
        });
    }

}
