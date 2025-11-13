package com.sena.getback.service;

import com.sena.getback.model.Usuario;
import com.sena.getback.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ActivityLogService activityLogService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public List<Usuario> findAllUsers() {
        return usuarioRepository.findAll();
    }

// helpers
private String currentUser() {
    try {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.getName() != null) ? a.getName() : "system";
    } catch (Exception e) { return "system"; }
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
        if (usuario.getClave() != null && !usuario.getClave().isEmpty()) {
            usuario.setClave(passwordEncoder.encode(usuario.getClave()));
        }

        boolean isUpdate = usuario.getId() != null;
        Usuario saved = usuarioRepository.save(usuario);
        try {
            String name = (saved.getNombre() != null ? saved.getNombre() : "Usuario") +
                    (saved.getApellido() != null ? (" " + saved.getApellido()) : "");
            String user = currentUser();
            String msg = (isUpdate ? "Se actualizó el usuario \"" : "Se creó el usuario \"") + name.trim() + "\"";
            activityLogService.log("USER", msg, user, null);
        } catch (Exception ignored) {}
        return saved;
    }

    @Override
    public Usuario updateUser(Usuario usuario) {
        Usuario existente = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (usuario.getClave() != null && !usuario.getClave().isEmpty()) {
            existente.setClave(passwordEncoder.encode(usuario.getClave()));
        }

        existente.setNombre(usuario.getNombre());
        existente.setApellido(usuario.getApellido());
        existente.setCorreo(usuario.getCorreo());
        existente.setTelefono(usuario.getTelefono());
        existente.setDireccion(usuario.getDireccion());
        existente.setEstado(usuario.getEstado());
        existente.setRol(usuario.getRol());

        Usuario saved = usuarioRepository.save(existente);
        try {
            String name = (saved.getNombre() != null ? saved.getNombre() : "Usuario") +
                    (saved.getApellido() != null ? (" " + saved.getApellido()) : "");
            activityLogService.log("USER", "Se actualizó el usuario \"" + name.trim() + "\"", currentUser(), null);
        } catch (Exception ignored) {}
        return saved;
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
        Usuario existente = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.getNombre() != null) existente.setNombre(usuario.getNombre());
        if (usuario.getApellido() != null) existente.setApellido(usuario.getApellido());
        if (usuario.getTelefono() != null) existente.setTelefono(usuario.getTelefono());
        if (usuario.getDireccion() != null) existente.setDireccion(usuario.getDireccion());

        if (usuario.getClave() != null && !usuario.getClave().isEmpty()) {
            existente.setClave(passwordEncoder.encode(usuario.getClave()));
        }

        Usuario saved = usuarioRepository.save(existente);
        try {
            String name = (saved.getNombre() != null ? saved.getNombre() : "Usuario") +
                    (saved.getApellido() != null ? (" " + saved.getApellido()) : "");
            activityLogService.log("PROFILE", "Se actualizó el perfil de \"" + name.trim() + "\"", currentUser(), null);
        } catch (Exception ignored) {}
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

    @Override
    public List<Usuario> findByRol(String rol) {
        return usuarioRepository.findByRol_Nombre(rol);
    }

    @Override
    public List<Usuario> findByEstado(String estado) {
        return usuarioRepository.findByEstado(estado);
    }

    @Override
    public long countActiveUsers() {
        return usuarioRepository.countByEstado("ACTIVO");
    }

    @Override
    public long countByRol(String rol) {
        return usuarioRepository.countByRol_Nombre(rol);
    }

    @Override
    public List<Usuario> searchUsers(String termino) {
        return null;
    }
}
