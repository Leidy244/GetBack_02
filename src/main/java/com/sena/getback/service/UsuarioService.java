package com.sena.getback.service;

import com.sena.getback.model.Usuario;
import com.sena.getback.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

	@Autowired
	private UsuarioRepository usuarioRepository;

	private final String UPLOAD_DIR = System.getProperty("user.dir") + "/images/"; // carpeta donde se guardarán las

	public List<Usuario> getAllUsers() {
		return usuarioRepository.findAll();
	}

	public Optional<Usuario> getUsuarioById(Integer id) {
		return usuarioRepository.findById(id);
	}

	public void createUsuario(Usuario usuario) {
		usuarioRepository.save(usuario);
	}

	public void updateUsuario(Integer id, Usuario usuario) {
		usuario.setId(id);
		usuarioRepository.save(usuario);
	}

	public boolean deleteUsuario(Integer id) {
		if (usuarioRepository.existsById(id)) {
			usuarioRepository.deleteById(id);
			return true;
		}
		return false;
	}

	// ADMINISTRADOR
	public Optional<Usuario> obtenerAdmin() {
		return usuarioRepository.findByRol_Nombre("ADMIN"); // traer el admin
	}

	public Usuario actualizarAdmin(Usuario adminActualizado) {
		return usuarioRepository.save(adminActualizado); // guardar cambios
	}

	// ACTUALIZAR FOTO
	public void actualizarFoto(Integer id, MultipartFile foto) {
		if (!foto.isEmpty()) {
			try {
				// Asegurar carpeta
				File carpeta = new File(UPLOAD_DIR);
				if (!carpeta.exists()) {
					carpeta.mkdirs();
				}

				// Buscar admin por ID
				Usuario admin = usuarioRepository.findById(id).orElse(null);
				if (admin != null) {

					// Borrar foto anterior si existe
					if (admin.getFoto() != null && !admin.getFoto().isEmpty()) {
						File archivoAnterior = new File(UPLOAD_DIR + admin.getFoto());
						if (archivoAnterior.exists()) {
							boolean borrado = archivoAnterior.delete();
							if (!borrado) {
								System.out
										.println("⚠️ No se pudo borrar la foto anterior: " + archivoAnterior.getName());
							}
						}
					}

					// Guardar nueva foto
					String nombreArchivo = System.currentTimeMillis() + "_" + foto.getOriginalFilename();
					Path ruta = Paths.get(UPLOAD_DIR + nombreArchivo);
					Files.write(ruta, foto.getBytes());

					// Actualizar campo en BD
					admin.setFoto(nombreArchivo);
					usuarioRepository.save(admin);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// Métodos adicionales

	// Cambiar estado de usuario (toggle entre ACTIVO / INACTIVO)
	public void toggleUserStatus(Integer id) {
		Optional<Usuario> usuarioOptional = usuarioRepository.findById(id);

		if (usuarioOptional.isPresent()) {
			Usuario usuario = usuarioOptional.get();

			if ("ACTIVO".equals(usuario.getEstado())) {
				usuario.setEstado("INACTIVO");
			} else {
				usuario.setEstado("ACTIVO");
			}

			usuarioRepository.save(usuario);
		} else {
			throw new RuntimeException("Usuario no encontrado con ID: " + id);
		}
	}

	// Obtener el primer usuario
	public Optional<Usuario> getFirstUser() {
		List<Usuario> usuarios = usuarioRepository.findAll();
		return usuarios.isEmpty() ? Optional.empty() : Optional.of(usuarios.get(0));
	}

	// Actualizar solo el perfil (sin tocar rol ni estado)
	public void updateProfile(Usuario usuario) {
		Optional<Usuario> usuarioOptional = usuarioRepository.findById(usuario.getId());

		if (usuarioOptional.isPresent()) {
			Usuario usuarioExistente = usuarioOptional.get();

			usuarioExistente.setNombre(usuario.getNombre());
			usuarioExistente.setApellido(usuario.getApellido());
			usuarioExistente.setTelefono(usuario.getTelefono());
			usuarioExistente.setDireccion(usuario.getDireccion());
			usuarioExistente.setCorreo(usuario.getCorreo());

			if (usuario.getClave() != null && !usuario.getClave().trim().isEmpty()) {
				usuarioExistente.setClave(usuario.getClave());
			}

			usuarioRepository.save(usuarioExistente);
		} else {
			throw new RuntimeException("Usuario no encontrado con ID: " + usuario.getId());
		}
	}

	// Buscar por correo
	public Optional<Usuario> findByCorreo(String correo) {
		return usuarioRepository.findByCorreo(correo);
	}
}
