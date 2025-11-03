package com.sena.getback.service;

import com.sena.getback.model.Evento;
import com.sena.getback.repository.EventoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;

    public EventoService(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    // Listar todos los eventos
    public List<Evento> findAll() {
        return eventoRepository.findAll();
    }

    // Buscar evento por ID
    public Optional<Evento> findById(Long id) {
        return eventoRepository.findById(id);
    }

    // Guardar o actualizar un evento
    public Evento save(Evento evento) {
        return eventoRepository.save(evento);
    }

    // Eliminar un evento por ID (con verificación)
    public void deleteById(Long id) {
        if (eventoRepository.existsById(id)) {
            eventoRepository.deleteById(id);
        }
    }
}
