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
 // Contar todos los eventos registrados
    public long count() {
        return eventoRepository.count();
    }
 // 🔹 Obtener cantidad de eventos por mes
    public List<Integer> obtenerEventosPorMes() {
        // Creamos una lista de 12 posiciones, una por cada mes
        int[] eventosPorMes = new int[12];

        // Recorremos todos los eventos y contamos por mes
        for (Evento evento : eventoRepository.findAll()) {
            if (evento.getFecha() != null) {
                int mes = evento.getFecha().getMonthValue(); // 1 = enero ... 12 = diciembre
                eventosPorMes[mes - 1]++;
            }
        }

        // Convertimos a lista para el gráfico
        return java.util.Arrays.stream(eventosPorMes)
                .boxed()
                .toList();
    }

}
