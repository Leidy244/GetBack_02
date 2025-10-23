package com.sena.getback.service;

import com.sena.getback.model.Usuario;
import com.sena.getback.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Override
	public List<Usuario> findAllUsers() {
		return usuarioRepository.findAll();
	}

	@Override
	public Optional<Usuario> findUserById(Long id) {
		return usuarioRepository.findById(id);
	}

	@Override
	public Usuario saveUser(Usuario usuario) {
		if (usuario.getId() == null && usuarioRepository.existsByCorreo(usuario.getCorreo())) {
			throw new RuntimeException("El correo ya está registrado");
		}

		if (usuario.getId() != null) {
			Optional<Usuario> usuarioExistente = usuarioRepository.findByCorreo(usuario.getCorreo());
			if (usuarioExistente.isPresent() && !usuarioExistente.get().getId().equals(usuario.getId())) {
				throw new RuntimeException("El correo ya está en uso por otro usuario");
			}
		}

		if (usuario.getEstado() == null) {
			usuario.setEstado("ACTIVO");
		}

		return usuarioRepository.save(usuario);
	}

	@Override
	public Usuario updateUser(Usuario usuario) {
		return saveUser(usuario);
	}

	@Override
	public void deleteUser(Long id) {
		if (!usuarioRepository.existsById(id)) {
			throw new RuntimeException("Usuario no encontrado");
		}
		usuarioRepository.deleteById(id);
	}

	@Override
	public void toggleUserStatus(Long id) {
		Usuario usuario = usuarioRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

		usuario.setEstado(usuario.getEstado().equals("ACTIVO") ? "INACTIVO" : "ACTIVO");
		usuarioRepository.save(usuario);
	}

	@Override
	public Optional<Usuario> getFirstUser() {
		return usuarioRepository.findAll().stream().findFirst();
	}

	@Override
	public void updateProfile(Usuario usuario) {
		Usuario usuarioExistente = usuarioRepository.findById(usuario.getId())
				.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

		if (usuario.getNombre() != null) {
			usuarioExistente.setNombre(usuario.getNombre());
		}
		if (usuario.getApellido() != null) {
			usuarioExistente.setApellido(usuario.getApellido());
		}
		if (usuario.getTelefono() != null) {
			usuarioExistente.setTelefono(usuario.getTelefono());
		}
		if (usuario.getDireccion() != null) {
			usuarioExistente.setDireccion(usuario.getDireccion());
		}
		if (usuario.getClave() != null && !usuario.getClave().isEmpty()) {
			usuarioExistente.setClave(usuario.getClave());
		}

		usuarioRepository.save(usuarioExistente);
	}

	@Override
	public Optional<Usuario> findByCorreo(String correo) {
		return usuarioRepository.findByCorreo(correo);
	}

	@Override
	public List<Usuario> findActiveUsers() {
		return usuarioRepository.findByEstadoOrderByNombreAsc("ACTIVO");
	}

	@Override
	public boolean existsByCorreo(String correo) {
		return usuarioRepository.existsByCorreo(correo);
	}

	public List<Usuario> findByRol(String rol) {
		return usuarioRepository.findByRol_Nombre(rol);
	}

	public List<Usuario> findByEstado(String estado) {
		return usuarioRepository.findByEstado(estado);
	}

	public long countActiveUsers() {
		return usuarioRepository.countByEstado("ACTIVO");
	}

	public long countByRol(String rol) {
		return usuarioRepository.countByRol_Nombre(rol);
	}

	@Override
	public List<Usuario> searchUsers(String termino) {
		return null;
	}
}