package com.sena.getback.repository;

import com.sena.getback.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
	Optional<Usuario> findByCorreo(String correo);

	List<Usuario> findByEstado(String estado);

	List<Usuario> findByRol_Nombre(String nombre);

	boolean existsByCorreo(String correo);

	List<Usuario> findByEstadoOrderByNombreAsc(String estado);

	List<Usuario> findByNombre(String nombre);

	long countByEstado(String estado);

	long countByRol_Nombre(String nombre);
}