package com.sena.getback.service;

import com.sena.getback.model.Evento;
import com.sena.getback.repository.EventoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Collectors;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final ActivityLogService activityLogService;

    public EventoService(EventoRepository eventoRepository, ActivityLogService activityLogService) {
        this.eventoRepository = eventoRepository;
        this.activityLogService = activityLogService;
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
        boolean isUpdate = (evento.getId() != null);
        Evento saved = eventoRepository.save(evento);
        try {
            String nombre = saved.getTitulo() != null ? saved.getTitulo() : ("#" + saved.getId());
            String user = currentUser();
            String msg = (isUpdate ? "Se actualizó el evento \"" : "Se creó el evento \"") + nombre + "\"";
            activityLogService.log("EVENT", msg, user, null);
        } catch (Exception ignored) {}
        return saved;
    }

    private String currentUser() {
        try {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            return (a != null && a.getName() != null) ? a.getName() : "system";
        } catch (Exception e) { return "system"; }
    }

    // Eliminar un evento por ID (con verificación)
    public void deleteById(Long id) {
        if (eventoRepository.existsById(id)) {
            eventoRepository.deleteById(id);
        }
    }

    public void toggleEventoEstado(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        String actual = evento.getEstado();
        if (actual == null || actual.equalsIgnoreCase("ACTIVO")) {
            evento.setEstado("INACTIVO");
        } else {
            evento.setEstado("ACTIVO");
        }

        eventoRepository.save(evento);
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

    public List<Evento> findEventosUltimas24Horas() {
        LocalDateTime limite = LocalDateTime.now().minusHours(24);

        return eventoRepository.findAll().stream()
                .filter(evento -> evento.getFecha() != null)
                .filter(evento -> "ACTIVO".equalsIgnoreCase(evento.getEstado()))
                .filter(evento -> {
                    LocalTime hora = evento.getHora() != null ? evento.getHora() : LocalTime.MIDNIGHT;
                    LocalDateTime fechaHoraEvento = LocalDateTime.of(evento.getFecha(), hora);
                    return fechaHoraEvento.isAfter(limite);
                })
                .collect(Collectors.toList());
    }

}
