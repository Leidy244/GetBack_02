package com.sena.getback.controller;

import com.sena.getback.model.Rol;
import com.sena.getback.model.Usuario;
import com.sena.getback.repository.RolRepository;
import com.sena.getback.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Optional;

@Controller
@RequestMapping("/users")
public class UsuarioController {

	// No tocar ni monda tambien de este controlador att:juan :P

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private RolRepository rolRepository;

	@GetMapping
	public String listUsers(Model model) {
		model.addAttribute("users", usuarioService.getAllUsers());
		model.addAttribute("newUser", new Usuario());
		model.addAttribute("activeSection", "users");
		model.addAttribute("title", "Gestión de Usuarios");

		return "admin";
	}

	@PostMapping("/save")
	public String saveUser(@ModelAttribute("newUser") Usuario usuario, RedirectAttributes redirectAttrs) {
		try {
			usuarioService.createUsuario(usuario);
			redirectAttrs.addFlashAttribute("success", "Usuario guardado exitosamente");
		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al guardar usuario: " + e.getMessage());
		}
		return "redirect:/users";
	}

	@GetMapping("/edit/{id}")
	public String editUser(@PathVariable Integer id, Model model, RedirectAttributes redirectAttrs) {
		Optional<Usuario> user = usuarioService.getUsuarioById(id);
		if (user.isPresent()) {
			model.addAttribute("newUser", user.get());
			model.addAttribute("users", usuarioService.getAllUsers());
			model.addAttribute("activeSection", "users");
			model.addAttribute("title", "Editar Usuario");
			return "admin";
		} else {
			redirectAttrs.addFlashAttribute("error", "Usuario no encontrado");
			return "redirect:/users";
		}
	}

	@PostMapping("/update")
	public String updateUser(@ModelAttribute("newUser") Usuario usuario, RedirectAttributes redirectAttrs) {
		try {
			usuarioService.updateUsuario(usuario.getId(), usuario);
			redirectAttrs.addFlashAttribute("success", "Usuario actualizado exitosamente");
		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al actualizar usuario: " + e.getMessage());
		}
		return "redirect:/users";
	}

	@GetMapping("/toggle-status/{id}")
	public String toggleUserStatus(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
		try {
			usuarioService.toggleUserStatus(id);
			redirectAttrs.addFlashAttribute("success", "Estado del usuario actualizado");
		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al actualizar estado: " + e.getMessage());
		}
		return "redirect:/users";
	}

	@GetMapping("/delete/{id}")
	public String deleteUser(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
		try {
			if (usuarioService.getUsuarioById(id).isPresent()) {
				usuarioService.deleteUsuario(id);
				redirectAttrs.addFlashAttribute("success", "Usuario eliminado exitosamente");
			} else {
				redirectAttrs.addFlashAttribute("error", "Usuario no encontrado");
			}
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
			// Buscar el rol USER en la BD
			Rol rolUser = rolRepository.findByNombre("USER")
					.orElseThrow(() -> new RuntimeException("Rol USER no encontrado"));

			usuario.setRol(rolUser);
			usuario.setEstado("ACTIVO");

			usuarioService.createUsuario(usuario);

			redirectAttributes.addFlashAttribute("message", "Registro exitoso. Ahora puede iniciar sesión.");
			return "redirect:/login";

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error en el registro: " + e.getMessage());
			return "redirect:/users/registro";
		}
	}

}
