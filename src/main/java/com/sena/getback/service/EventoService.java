package com.sena.getback.service;

import com.sena.getback.model.Evento;
import com.sena.getback.repository.EventoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;

    public EventoService(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    // Listar todos los eventos
    public List<Evento> listarEventos() {
        return eventoRepository.findAll();
    }

    // Guardar o actualizar evento
    public Evento guardar(Evento evento) {
        return eventoRepository.save(evento);
    }

    // Buscar evento por id
    public Evento buscarPorId(Long id) {
        return eventoRepository.findById(id).orElse(null);
    }

    // Eliminar evento
    public void eliminar(Long id) {
        eventoRepository.deleteById(id);
    }
}
