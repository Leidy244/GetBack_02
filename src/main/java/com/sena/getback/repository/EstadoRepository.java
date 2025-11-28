package com.sena.getback.repository;

import com.sena.getback.model.Estado;
import com.sena.getback.model.Mesa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Integer> {

	Optional<Mesa> findByNombreEstado(String string);
}
