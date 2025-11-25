package com.sena.getback.repository;

import com.sena.getback.model.ClienteFrecuente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClienteFrecuenteRepository extends JpaRepository<ClienteFrecuente, Long> {

    List<ClienteFrecuente> findByNombreContainingIgnoreCase(String nombre);
}
