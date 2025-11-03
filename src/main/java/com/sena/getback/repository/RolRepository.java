package com.sena.getback.repository;

import com.sena.getback.model.Rol;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {

	Optional<Rol> findByNombre(String string);
}
