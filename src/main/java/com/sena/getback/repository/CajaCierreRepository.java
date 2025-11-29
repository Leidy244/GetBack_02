package com.sena.getback.repository;

import com.sena.getback.model.CajaCierre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CajaCierreRepository extends JpaRepository<CajaCierre, Long> {
    List<CajaCierre> findByFechaBetween(LocalDate start, LocalDate end);
}
