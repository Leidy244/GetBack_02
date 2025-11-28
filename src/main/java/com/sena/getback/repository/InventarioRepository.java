package com.sena.getback.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sena.getback.model.Inventario;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    List<Inventario> findAllByOrderByFechaIngresoDesc();

    List<Inventario> findByMenu_Id(Long menuId);
}
