package com.sena.getback.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sena.getback.model.Rol;

public interface RolRepository extends JpaRepository<Rol, Integer> {

	Optional<Rol> findByNombre(String nombre);
}
