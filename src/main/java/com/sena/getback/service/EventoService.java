package com.sena.getback.service;

import com.sena.getback.model.Evento;
import com.sena.getback.repository.EventoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventoService {

    @Autowired
    private EventoRepository eventoRepository;

    public List<Evento> findAll() {
        return eventoRepository.findAll();
    }

    public Evento findById(Integer id) {
        return eventoRepository.findById(id).orElse(null);
    }

    public Evento save(Evento evento) {
        return eventoRepository.save(evento);
    }

    public void delete(Integer id) {
        eventoRepository.deleteById(id);
    }
}
