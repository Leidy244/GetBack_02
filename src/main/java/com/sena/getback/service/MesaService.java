package com.sena.getback.service;

import com.sena.getback.model.Mesa;
import com.sena.getback.repository.MesaRepository;
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
public class MesaService {

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private ActivityLogService activityLogService;

    // 🔹 Listar todas las mesas
    public List<Mesa> findAll() {
        return mesaRepository.findAll();
    }

    // 🔹 Buscar mesa por ID
    public Optional<Mesa> findById(Integer id) {
        return mesaRepository.findById(id);
    }

    // 🔹 Guardar o actualizar mesa
    public Mesa save(Mesa mesa) {
        boolean isUpdate = (mesa.getId() != null);
        Mesa saved = mesaRepository.save(mesa);
        try {
            String nombre = saved.getNumero() != null ? saved.getNumero() : ("#" + saved.getId());
            String user = currentUser();
            String msg = (isUpdate ? "Se actualizó la mesa \"" : "Se creó la mesa \"") + nombre + "\"";
            activityLogService.log("TABLE", msg, user, null);
        } catch (Exception ignored) {}
        return saved;
    }

    private String currentUser() {
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
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            return (a != null && a.getName() != null) ? a.getName() : "system";
        } catch (Exception e) { return "system"; }
    }

    // 🔹 Eliminar mesa por ID
    public void deleteById(Integer id) {
        mesaRepository.deleteById(id);
    }

    // 🔹 Validar existencia por número y ubicación
    public boolean existsByNumeroAndUbicacion(String numero, Integer ubicacionId) {
        if (ubicacionId == null) {
            // Si no tiene ubicación asignada
            return mesaRepository.existsByNumeroAndUbicacionIsNull(numero);
        }
        return mesaRepository.existsByNumeroAndUbicacion_Id(numero, ubicacionId);
    }

    // 🔹 Validar existencia por número, ubicación e ID (para edición)
    public boolean existsByNumeroAndUbicacionAndIdNot(String numero, Integer ubicacionId, Integer id) {
        if (ubicacionId == null) {
            // Si no tiene ubicación asignada
            return mesaRepository.existsByNumeroAndUbicacionIsNull(numero);
        }
        return mesaRepository.existsByNumeroAndUbicacion_IdAndIdNot(numero, ubicacionId, id);
    }
    public void ocuparMesa(Integer mesaId) {
        mesaRepository.findById(mesaId).ifPresent(mesa -> {
            mesa.setEstado("OCUPADA");
            mesaRepository.save(mesa);
            try {
                String nombre = mesa.getNumero() != null ? mesa.getNumero() : ("#" + mesa.getId());
                activityLogService.log("TABLE", "Mesa " + nombre + " marcada como OCUPADA", currentUser(), null);
            } catch (Exception ignored) {}
        });
    }

    public void liberarMesa(Integer mesaId) {
        mesaRepository.findById(mesaId).ifPresent(mesa -> {
            mesa.setEstado("DISPONIBLE");
            mesaRepository.save(mesa);
            try {
                String nombre = mesa.getNumero() != null ? mesa.getNumero() : ("#" + mesa.getId());
                activityLogService.log("TABLE", "Mesa " + nombre + " marcada como DISPONIBLE", currentUser(), null);
            } catch (Exception ignored) {}
        });
    }
 // 🔹 Contar todas las mesas registradas
    public long count() {
        return mesaRepository.count();
    }


}
