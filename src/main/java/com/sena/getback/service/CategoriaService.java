package com.sena.getback.service;

import com.sena.getback.model.Categoria;
import com.sena.getback.repository.CategoriaRepository;
import com.sena.getback.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpSession;
import com.sena.getback.model.Usuario;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

	@Autowired
	private CategoriaRepository categoriaRepository;

	@Autowired
	private ActivityLogService activityLogService;

	// Listar todas las categorías
	public List<Categoria> findAll() {
		return categoriaRepository.findAll();
	}

	// Alias para compatibilidad con diferentes nombres
	public List<Categoria> listar() {
		return findAll();
	}

	// Buscar categoría por id
	public Categoria findById(Integer id) {
		return categoriaRepository.findById(id).orElse(null);
	}

	// Buscar categoría por id retornando Optional
	public Optional<Categoria> findByIdOptional(Integer id) {
		return categoriaRepository.findById(id);
	}

	// Guardar o actualizar categoría
	public Categoria save(Categoria categoria) {
		boolean isUpdate = (categoria.getId() != null);
		Categoria saved = categoriaRepository.save(categoria);
		try {
			String nombre = saved.getNombre() != null ? saved.getNombre() : ("#" + saved.getId());
			String user = getCurrentUsername();
			String msg = (isUpdate ? "Se actualizó la categoría \"" : "Se creó la categoría \"") + nombre + "\"";
			activityLogService.log("CATEGORY", msg, user, null);
		} catch (Exception ignored) {}
		return saved;
	}

    private String getCurrentUsername() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpSession session = attrs.getRequest().getSession(false);
                if (session != null) {
                    Object obj = session.getAttribute("usuarioLogueado");
                    if (obj instanceof Usuario u) {
                        String n = u.getNombre() != null ? u.getNombre().trim() : "";
                        String a = u.getApellido() != null ? (" " + u.getApellido().trim()) : "";
                        String full = (n + a).trim();
                        return full.isEmpty() ? (u.getCorreo() != null ? u.getCorreo() : "system") : full;
                    }
                }
            }
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return (auth != null && auth.getName() != null) ? auth.getName() : "system";
        } catch (Exception e) { return "system"; }
    }

	// Eliminar categoría por id
	public void delete(Integer id) {
		categoriaRepository.deleteById(id);
	}

	// Verificar si existe categoría por id
	public boolean existsById(Integer id) {
		return categoriaRepository.existsById(id);
	}

	// Buscar categoría por nombre
	public Categoria findByNombre(String nombre) {
		return categoriaRepository.findAll().stream()
				.filter(categoria -> nombre.equalsIgnoreCase(categoria.getNombre())).findFirst().orElse(null);
	}

	// Contar total de categorías
	public long count() {
		return categoriaRepository.count();
	}
}
