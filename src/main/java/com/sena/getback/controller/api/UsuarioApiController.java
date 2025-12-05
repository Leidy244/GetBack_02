package com.sena.getback.controller.api;

import com.sena.getback.model.Rol;
import com.sena.getback.model.Usuario;
import com.sena.getback.repository.RolRepository;
import com.sena.getback.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioApiController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private RolRepository rolRepository;

    @GetMapping
    public ResponseEntity<List<Usuario>> listar(
            @RequestParam(value = "rol", required = false) String rol,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "q", required = false) String q
    ) {
        List<Usuario> base;
        if (rol != null && !rol.isBlank()) {
            base = usuarioService.findByRol(normalizarRol(rol));
        } else if (estado != null && !estado.isBlank()) {
            base = usuarioService.findByEstado(estado.toUpperCase(Locale.ROOT));
        } else {
            base = usuarioService.findAllUsers();
        }
        if (q != null && !q.isBlank()) {
            String term = q.toLowerCase(Locale.ROOT).trim();
            base = base.stream().filter(u -> {
                String nombre = safe(u.getNombre());
                String apellido = safe(u.getApellido());
                String correo = safe(u.getCorreo());
                return nombre.contains(term) || apellido.contains(term) || correo.contains(term);
            }).collect(Collectors.toList());
        }
        return ResponseEntity.ok(base);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtener(@PathVariable Long id) {
        return usuarioService.findUserById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<Usuario> crear(@RequestBody Usuario usuario,
                                         @RequestParam(value = "rol", required = false) String rolNombre,
                                         @RequestParam(value = "rolId", required = false) Integer rolId) {
        asignarRol(usuario, rolNombre, rolId);
        Usuario saved = usuarioService.saveUser(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Long id,
                                              @RequestBody Usuario usuario,
                                              @RequestParam(value = "rol", required = false) String rolNombre,
                                              @RequestParam(value = "rolId", required = false) Integer rolId) {
        Optional<Usuario> existente = usuarioService.findUserById(id);
        if (existente.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        usuario.setId(id);
        asignarRol(usuario, rolNombre, rolId);
        Usuario updated = usuarioService.updateUser(usuario);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> toggleEstado(@PathVariable Long id) {
        usuarioService.toggleUserStatus(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Rol>> roles() {
        return ResponseEntity.ok(rolRepository.findAll());
    }

    private void asignarRol(Usuario usuario, String rolNombre, Integer rolId) {
        if (usuario.getRol() != null && usuario.getRol().getId() != null) return;
        Rol rol = null;
        if (rolId != null) {
            rol = rolRepository.findById(rolId).orElse(null);
        }
        if (rol == null && rolNombre != null && !rolNombre.isBlank()) {
            rol = rolRepository.findByNombre(normalizarRol(rolNombre))
                    .orElseGet(() -> rolRepository.save(new Rol(normalizarRol(rolNombre))));
        }
        if (rol != null) usuario.setRol(rol);
    }

    private String normalizarRol(String r) {
        return r == null ? null : r.trim().toUpperCase(Locale.ROOT);
    }

    private String safe(String v) { return v == null ? "" : v.toLowerCase(Locale.ROOT); }
}

