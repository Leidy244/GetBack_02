package com.sena.getback.service;

import com.sena.getback.model.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioService {
    List<Usuario> findAllUsers();
    Optional<Usuario> findUserById(Long id);
    Usuario saveUser(Usuario usuario);
    Usuario updateUser(Usuario usuario);
    void deleteUser(Long id);
    void toggleUserStatus(Long id);
    Optional<Usuario> getFirstUser();
    void updateProfile(Usuario usuario);
    Optional<Usuario> findByCorreo(String correo);
    List<Usuario> findActiveUsers();
    List<Usuario> searchUsers(String termino);
    boolean existsByCorreo(String correo);
    List<Usuario> findByRol(String rol);
    List<Usuario> findByEstado(String estado);
    long countActiveUsers();
    long countByRol(String rol);
}