package com.sena.getback.controller;

import com.sena.getback.model.Rol;
import com.sena.getback.model.Usuario;
import com.sena.getback.repository.RolRepository;
import com.sena.getback.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Optional;

@Controller
@RequestMapping("/users")
public class UsuarioController {

	// NO MOVER NI CHIMBO DE ESTE CONTROLADOR att:juan
	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private RolRepository rolRepository;

	@GetMapping
	public String listUsers(Model model) {
		model.addAttribute("users", usuarioService.findAllUsers());
		model.addAttribute("newUser", new Usuario());
		model.addAttribute("activeSection", "users");
		model.addAttribute("title", "Gestión de Usuarios");
		model.addAttribute("roles", rolRepository.findAll());

		return "admin";
	}

	@PostMapping("/save")
	public String saveUser(@ModelAttribute("newUser") Usuario usuario, RedirectAttributes redirectAttrs) {
		try {
			usuarioService.saveUser(usuario);
			redirectAttrs.addFlashAttribute("success", "Usuario guardado exitosamente");
		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al guardar usuario: " + e.getMessage());
		}
		return "redirect:/users";
	}

	@GetMapping("/edit/{id}")
	public String editUser(@PathVariable Long id, Model model, RedirectAttributes redirectAttrs) {
		Optional<Usuario> user = usuarioService.findUserById(id);
		if (user.isPresent()) {
			model.addAttribute("newUser", user.get());
			model.addAttribute("users", usuarioService.findAllUsers());
			model.addAttribute("activeSection", "users");
			model.addAttribute("title", "Editar Usuario");
			model.addAttribute("roles", rolRepository.findAll());
			return "admin";
		} else {
			redirectAttrs.addFlashAttribute("error", "Usuario no encontrado");
			return "redirect:/users";
		}
	}

	@PostMapping("/update")
	public String updateUser(@ModelAttribute("newUser") Usuario usuario, RedirectAttributes redirectAttrs) {
		try {
			usuarioService.updateUser(usuario);
			redirectAttrs.addFlashAttribute("success", "Usuario actualizado exitosamente");
		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al actualizar usuario: " + e.getMessage());
		}
		return "redirect:/users";
	}

	@GetMapping("/toggle-status/{id}")
	public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttrs) {
		try {
			usuarioService.toggleUserStatus(id);
			redirectAttrs.addFlashAttribute("success", "Estado del usuario actualizado");
		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al actualizar estado: " + e.getMessage());
		}
		return "redirect:/users";
	}

	@GetMapping("/delete/{id}")
	public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttrs) {
		try {
			usuarioService.deleteUser(id);
			redirectAttrs.addFlashAttribute("success", "Usuario eliminado exitosamente");
		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al eliminar usuario: " + e.getMessage());
		}
		return "redirect:/users";
	}

	@GetMapping("/registro")
	public String mostrarFormularioRegistro(Model model) {
		model.addAttribute("usuario", new Usuario());
		return "login/registro";
	}

	@PostMapping("/registro")
	public String registrarUsuario(@ModelAttribute("usuario") Usuario usuario, RedirectAttributes redirectAttributes) {
		try {
			Rol rolMesero = rolRepository.findByNombre("MESERO").orElseGet(() -> {
				Rol nuevoRol = new Rol("MESERO");
				return rolRepository.save(nuevoRol);
			});

			usuario.setRol(rolMesero);
			usuario.setEstado("ACTIVO");

			usuarioService.saveUser(usuario);

			redirectAttributes.addFlashAttribute("message", "Registro exitoso. Ahora puede iniciar sesión.");
			return "redirect:/login";

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error en el registro: " + e.getMessage());
			return "login/index";
		}
	}

	@GetMapping("/perfil")
	public String mostrarPerfilUsuario(HttpSession session, Model model, RedirectAttributes redirectAttrs) {
		Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");

		if (usuarioLogueado == null) {
			redirectAttrs.addFlashAttribute("error", "Debe iniciar sesión para acceder a su perfil.");
			return "redirect:/login";
		}

		model.addAttribute("usuario", usuarioLogueado);
		return "/mesero/configuracion";
	}

}