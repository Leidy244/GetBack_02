package com.sena.getback.service;

import com.sena.getback.model.ActivityLog;
import com.sena.getback.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpSession;
import com.sena.getback.repository.UsuarioRepository;
import com.sena.getback.model.Usuario;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public ActivityLog log(String type, String message, String username, String metadata) {
        String finalUser = resolveUsername(username);
        ActivityLog a = new ActivityLog();
        a.setTimestamp(LocalDateTime.now());
        a.setType(type);
        a.setMessage(message);
        a.setUsername(finalUser);
        a.setMetadata(metadata);
        return repository.save(a);
    }

    public String resolveDisplayUsername(String username) {
        return resolveUsername(username);
    }

    private String resolveUsername(String provided) {
        String p = provided != null ? provided.trim() : "";
        if (p.isEmpty() || p.equalsIgnoreCase("anonymousUser") || p.equalsIgnoreCase("system")) {
            String fromSec = fromSecurityContext();
            if (fromSec != null && !fromSec.isBlank()) return fromSec;
            String fromSession = fromHttpSession();
            if (fromSession != null && !fromSession.isBlank()) return fromSession;
            return "Sistema";
        }
        // Si viene correo/username, convertirlo a nombre completo si existe
        return usuarioRepository.findByCorreo(p)
                .map(this::displayName)
                .orElse(p);
    }

    private String fromSecurityContext() {
        try {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            String name = (a != null) ? a.getName() : null;
            if (name == null || name.equalsIgnoreCase("anonymousUser")) return null;
            return usuarioRepository.findByCorreo(name)
                    .map(this::displayName)
                    .orElse(name);
        } catch (Exception e) { return null; }
    }

    private String fromHttpSession() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpSession session = attrs.getRequest().getSession(false);
            if (session == null) return null;
            Object obj = session.getAttribute("usuarioLogueado");
            if (obj instanceof Usuario u) return displayName(u);
            return null;
        } catch (Exception e) { return null; }
    }

    private String displayName(Usuario u) {
        String n = u.getNombre() != null ? u.getNombre().trim() : "";
        String a = u.getApellido() != null ? (" " + u.getApellido().trim()) : "";
        String full = (n + a).trim();
        return full.isEmpty() ? (u.getCorreo() != null ? u.getCorreo() : "Usuario") : full;
    }

    public List<ActivityLog> getRecent(int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        return repository.findRecent(PageRequest.of(0, size));
    }

    public List<ActivityLog> getPage(int page, int size) {
        int p = Math.max(0, page);
        int s = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(p, s);
        return repository.findRecent(pageable);
    }
}
