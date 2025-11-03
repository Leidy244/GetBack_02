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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Controller
@RequestMapping("/users")
public class UsuarioController {

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
	public String saveUser(@ModelAttribute("newUser") Usuario usuario,
			@RequestParam(value = "fecha", required = false) String fecha,
			@RequestParam(value = "horaInicio", required = false) String horaInicio,
			@RequestParam(value = "horaFin", required = false) String horaFin, RedirectAttributes redirectAttrs) {

		try {
			// Convertir strings a LocalDate y LocalTime
			if (fecha != null && !fecha.isEmpty()) {
				usuario.setFecha(LocalDate.parse(fecha));
			}
			if (horaInicio != null && !horaInicio.isEmpty()) {
				usuario.setHoraInicio(LocalTime.parse(horaInicio));
			}
			if (horaFin != null && !horaFin.isEmpty()) {
				usuario.setHoraFin(LocalTime.parse(horaFin));
			}

			usuarioService.saveUser(usuario);
			redirectAttrs.addFlashAttribute("success", "Usuario guardado exitosamente");
		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al guardar usuario: " + e.getMessage());
		}
		return "redirect:/users";
	}

	@PostMapping("/update")
	public String updateUser(@RequestParam("id") Long id, @RequestParam("nombre") String nombre,
			@RequestParam("apellido") String apellido, @RequestParam("correo") String correo,
			@RequestParam(value = "telefono", required = false) String telefono,
			@RequestParam(value = "direccion", required = false) String direccion,
			@RequestParam("estado") String estado,
			@RequestParam("rol.id") Integer rolId,
			@RequestParam(value = "clave", required = false) String clave,
			@RequestParam(value = "fecha", required = false) String fecha,
			@RequestParam(value = "horaInicio", required = false) String horaInicio,
			@RequestParam(value = "horaFin", required = false) String horaFin, RedirectAttributes ra) {

		try {
			Optional<Usuario> usuarioOpt = usuarioService.findUserById(id);
			if (usuarioOpt.isPresent()) {
				Usuario usuario = usuarioOpt.get();

				// Actualizar campos básicos
				usuario.setNombre(nombre);
				usuario.setApellido(apellido);
				usuario.setCorreo(correo);
				usuario.setTelefono(telefono);
				usuario.setDireccion(direccion);
				usuario.setEstado(estado);

				// Actualizar rol
				Optional<Rol> rolOpt = rolRepository.findById(rolId);
				rolOpt.ifPresent(usuario::setRol);

				// Actualizar contraseña solo si se proporciona
				if (clave != null && !clave.trim().isEmpty()) {
					usuario.setClave(clave);
				}

				// Actualizar fecha y hora del turno
				if (fecha != null && !fecha.isEmpty()) {
					usuario.setFecha(LocalDate.parse(fecha));
				} else {
					usuario.setFecha(null);
				}

				if (horaInicio != null && !horaInicio.isEmpty()) {
					usuario.setHoraInicio(LocalTime.parse(horaInicio));
				} else {
					usuario.setHoraInicio(null);
				}

				if (horaFin != null && !horaFin.isEmpty()) {
					usuario.setHoraFin(LocalTime.parse(horaFin));
				} else {
					usuario.setHoraFin(null);
				}

				usuarioService.updateUser(usuario);
				ra.addFlashAttribute("success", "Usuario actualizado exitosamente");
			} else {
				ra.addFlashAttribute("error", "Usuario no encontrado");
			}
		} catch (Exception e) {
			ra.addFlashAttribute("error", "Error al actualizar usuario: " + e.getMessage());
			e.printStackTrace(); // Para debugging
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