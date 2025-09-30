package com.sena.getback.repository;

import com.sena.getback.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {
    // Si más adelante quieres consultas personalizadas, las defines aquí
}
