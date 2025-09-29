package com.sena.getback.repository;

import com.sena.getback.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Integer> {
	List<Menu> findByDisponibleTrue();

	List<Menu> findByCategoriaIdAndDisponibleTrue(Integer categoriaId);
}
