package com.sena.getback.controller;

import com.sena.getback.model.Usuario;
import com.sena.getback.service.EmailService;
import com.sena.getback.service.UsuarioService;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class LoginController {

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private EmailService emailService;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/login")
    public String mostrarLogin(Model model, HttpSession session) {
        Object logged = session != null ? session.getAttribute("usuarioLogueado") : null;
        if (logged instanceof Usuario usuario && usuario.getRol() != null && usuario.getRol().getNombre() != null) {
            String rol = usuario.getRol().getNombre().toUpperCase();
            switch (rol) {
                case "ADMIN":
                    return "redirect:/admin";
                case "MESERO":
                    return "redirect:/configuracion";
                case "CAJA":
                    return "redirect:/caja";
                default:
                    return "redirect:/";
            }
        }
        model.addAttribute("usuario", new Usuario());
        return "login/index";
    }

	@PostMapping("/login")
	public String procesarLogin(@RequestParam("correo") String correo,
	                            @RequestParam("password") String password,
	                            RedirectAttributes redirectAttributes,
	                            Model model,
	                            HttpSession session) {

	    Optional<Usuario> usuarioOpt = usuarioService.findByCorreo(correo);

	    if (usuarioOpt.isPresent()) {
	        Usuario usuario = usuarioOpt.get();
	        if (passwordEncoder.matches(password, usuario.getClave())) {
	            if ("ACTIVO".equalsIgnoreCase(usuario.getEstado())) {
	                session.setAttribute("usuarioLogueado", usuario);
	                redirectAttributes.addFlashAttribute("message", "Bienvenido " + usuario.getNombre());
	                if (usuario.getRol() != null && usuario.getRol().getNombre() != null) {
	                    switch (usuario.getRol().getNombre().toUpperCase()) {
	                        case "ADMIN":
	                            return "redirect:/admin";
	                        case "MESERO":
	                            return "redirect:/configuracion";
	                        case "CAJA":
	                            return "redirect:/caja";
	                        default:
	                            return "redirect:/";
	                    }
	                } else {
	                    model.addAttribute("error", "El usuario no tiene un rol asignado, contacta al administrador.");
	                    return "login/index";
	                }

	            } else {
	                model.addAttribute("error", "Tu cuenta está inactiva, contacta al administrador.");
	                return "login/index";
	            }

	        } else {
	            model.addAttribute("error", "Contraseña incorrecta.");
	            return "login/index";
	        }

	    } else {
	        model.addAttribute("error", "Usuario no encontrado.");
	        return "login/index";
	    }
	}

    @GetMapping("/login-error")
    public String loginError(Model model) {
        model.addAttribute("error", "Credenciales inválidas.");
        return "login/index";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }

	@GetMapping("/forgot-password")
	public String mostrarForgotPassword(Model model) {
		return "login/forgot-password";
	}

	@PostMapping("/forgot-password")
	public String procesarForgotPassword(@RequestParam("correo") String correo,
	                                     Model model,
	                                     RedirectAttributes redirectAttributes) {
		Optional<Usuario> usuarioOpt = usuarioService.findByCorreo(correo);
		
		if (usuarioOpt.isPresent()) {
			Usuario usuario = usuarioOpt.get();
			if ("ACTIVO".equalsIgnoreCase(usuario.getEstado())) {
			
				String resetToken = generateResetToken();
				
				
				try {
					emailService.enviarCorreoRecuperacion(correo, resetToken);
					redirectAttributes.addFlashAttribute("message", "Se ha enviado un enlace de recuperación a tu correo.");
				} catch (Exception e) {
					model.addAttribute("error", "Error al enviar el correo. Por favor intenta más tarde.");
					return "login/forgot-password";
				}
				
				return "redirect:/forgot-password";
			} else {
				model.addAttribute("error", "La cuenta está inactiva. Contacta al administrador.");
				return "login/forgot-password";
			}
		} else {
			model.addAttribute("error", "No existe una cuenta con ese correo electrónico.");
			return "login/forgot-password";
		}
	}

	@GetMapping("/reset-password")
	public String mostrarResetPassword(@RequestParam(value = "correo", required = false) String correo,
	                                  @RequestParam(value = "token", required = false) String token,
	                                  Model model) {
		if (correo == null || token == null || correo.isEmpty() || token.isEmpty()) {
			return "redirect:/reset-password";
		}
		
		model.addAttribute("correo", correo);
		model.addAttribute("token", token);
		return "login/reset-password";
	}

	@PostMapping("/reset-password")
	public String procesarResetPassword(@RequestParam("correo") String correo,
	                                   @RequestParam("token") String token,
	                                   @RequestParam("nuevaPassword") String nuevaPassword,
	                                   @RequestParam("confirmarPassword") String confirmarPassword,
	                                   Model model,
	                                   RedirectAttributes redirectAttributes) {
		
		if (!nuevaPassword.equals(confirmarPassword)) {
			model.addAttribute("error", "Las contraseñas no coinciden.");
			model.addAttribute("correo", correo);
			model.addAttribute("token", token);
			return "login/reset-password";
		}
		
		// Validar longitud mínima
		if (nuevaPassword.length() < 6) {
			model.addAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
			model.addAttribute("correo", correo);
			model.addAttribute("token", token);
			return "login/reset-password";
		}
		
		Optional<Usuario> usuarioOpt = usuarioService.findByCorreo(correo);
		
		if (usuarioOpt.isPresent()) {
			Usuario usuario = usuarioOpt.get();
			usuario.setClave(nuevaPassword);
			
			usuarioService.saveUser(usuario);
			
			redirectAttributes.addFlashAttribute("success", "Contraseña actualizada correctamente. Por favor inicia sesión.");
			return "redirect:/login";
		} else {
			model.addAttribute("error", "Usuario no encontrado.");
			return "login/reset-password";
		}
	}

	private String generateResetToken() {
		return "RESET-" + System.currentTimeMillis();
	}
}
