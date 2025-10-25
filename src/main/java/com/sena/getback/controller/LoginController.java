package com.sena.getback.controller;

import com.sena.getback.model.Usuario;
import com.sena.getback.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class LoginController {

	// no tocar ni monda de este controller
	@Autowired
	private UsuarioService usuarioService;

	@GetMapping("/login")
	public String mostrarLogin(Model model) {
		model.addAttribute("usuario", new Usuario());
		return "login/index";
	}

	@PostMapping("/login")
	public String procesarLogin(@RequestParam("correo") String correo, @RequestParam("password") String password,
			RedirectAttributes redirectAttributes, Model model, HttpSession session) {

		Optional<Usuario> usuarioOpt = usuarioService.findByCorreo(correo);

		if (usuarioOpt.isPresent()) {
			Usuario usuario = usuarioOpt.get();

			if (usuario.getClave().equals(password)) {
				if ("ACTIVO".equalsIgnoreCase(usuario.getEstado())) {

					// Guardar el usuario logueado en la sesión
					session.setAttribute("usuarioLogueado", usuario);

					// Mostrar mensaje de bienvenida
					redirectAttributes.addFlashAttribute("message", "Bienvenido " + usuario.getNombre());

					// Redirigir según el rol
					// Redirigir según el rol
					if (usuario.getRol() != null && usuario.getRol().getNombre() != null) {
						if ("ADMIN".equalsIgnoreCase(usuario.getRol().getNombre())) {
							return "redirect:/admin";
						} else if ("MESERO".equalsIgnoreCase(usuario.getRol().getNombre())) {
							return "redirect:/configuracion";
						} else {
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
}
