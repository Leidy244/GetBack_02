// MesaRepository.java
package com.sena.getback.repository;

import com.sena.getback.model.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Integer> {
    List<Mesa> findByUbicacionContainingIgnoreCase(String ubicacion);
}