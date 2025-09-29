package com.sena.getback.repository;

import com.sena.getback.model.Usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

	Optional<Usuario> findByCorreo(String correo);

	Optional<Usuario> findByRol_Nombre(String nombreRol);

	// Buscar por rol Admin
	List<Usuario> findByRolId(Integer rolId);

}
