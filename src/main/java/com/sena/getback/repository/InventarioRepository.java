package com.sena.getback.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sena.getback.model.Inventario;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    List<Inventario> findAllByOrderByFechaIngresoDesc();

    List<Inventario> findByMenu_Id(Long menuId);

    @Modifying
    @Query("DELETE FROM Inventario i WHERE LOWER(i.producto) = LOWER(?1)")
    void deleteByProductoCaseInsensitive(String producto);

    @Modifying
    @Query("DELETE FROM Inventario i WHERE i.menu.id = ?1")
    void deleteByMenuId(Long menuId);
}
