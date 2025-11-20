package com.sena.getback.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sena.getback.model.Usuario;
import com.sena.getback.repository.UsuarioRepository;
import com.sena.getback.service.UploadFileService;
import com.sena.getback.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@Controller
public class PerfilesController {

	@Autowired
	private UploadFileService uploadFileService;

	private final UsuarioService usuarioService;
	private final UsuarioRepository usuarioRepository;

	@Autowired
	public PerfilesController(UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
		this.usuarioService = usuarioService;
		this.usuarioRepository = usuarioRepository;

	}

	/** PERFIL DEL ADMIN */
	@GetMapping("/perfil")
	public String mostrarPerfil(Model model) {
		Usuario admin = usuarioService.getFirstUser().orElse(new Usuario());
		model.addAttribute("usuario", admin);
		model.addAttribute("activeSection", "perfil");
		return "admin";
	}

	@PostMapping("/actualizar-datos")
	public String actualizarPerfil(@ModelAttribute Usuario adminActualizado, HttpSession session,
			RedirectAttributes redirectAttrs) {
		try {
			// Obtener el usuario logueado desde la sesión
			Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
			if (usuarioLogueado == null) {
				redirectAttrs.addFlashAttribute("error", "Sesión expirada. Por favor inicia sesión de nuevo.");
				return "redirect:/login";
			}

			// Actualizar los campos que vienen del formulario
			if (adminActualizado.getNombre() != null && !adminActualizado.getNombre().isEmpty()) {
				usuarioLogueado.setNombre(adminActualizado.getNombre());
			}
			if (adminActualizado.getApellido() != null && !adminActualizado.getApellido().isEmpty()) {
				usuarioLogueado.setApellido(adminActualizado.getApellido());
			}
			if (adminActualizado.getCorreo() != null && !adminActualizado.getCorreo().isEmpty()) {
				usuarioLogueado.setCorreo(adminActualizado.getCorreo());
			}
			if (adminActualizado.getDireccion() != null) {
				usuarioLogueado.setDireccion(adminActualizado.getDireccion());
			}
			if (adminActualizado.getTelefono() != null) {
				usuarioLogueado.setTelefono(adminActualizado.getTelefono());
			}
			if (adminActualizado.getClave() != null && !adminActualizado.getClave().isEmpty()) {
				usuarioLogueado.setClave(adminActualizado.getClave());
			}

			// Guardar cambios en la BD
			usuarioService.updateUser(usuarioLogueado);

			// Actualizar la sesión con el usuario modificado
			session.setAttribute("usuarioLogueado", usuarioLogueado);

			redirectAttrs.addFlashAttribute("success", "Perfil actualizado correctamente");

		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al actualizar perfil: " + e.getMessage());
		}

		return "redirect:/admin?activeSection=perfil";
	}

	/** ACTUALIZAR FOTO */
	@PostMapping("/actualizar-foto")
	public String actualizarFoto(@RequestParam("id") Long id, @RequestParam("foto") MultipartFile foto,
			RedirectAttributes redirectAttrs, HttpSession session) {

		try {
			// Buscar el usuario
			Usuario admin = usuarioRepository.findById(id)
					.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

			// Validar que el archivo no esté vacío
			if (foto.isEmpty()) {
				redirectAttrs.addFlashAttribute("error", "Por favor seleccione una foto");
				return "redirect:/admin?activeSection=perfil";
			}

			// Eliminar foto anterior si existe
			if (admin.getFoto() != null && !admin.getFoto().isEmpty()) {
				try {
					uploadFileService.deleteImage(admin.getFoto());
				} catch (Exception e) {
					System.err.println("⚠️ No se pudo borrar la foto anterior: " + e.getMessage());
				}
			}

			// Guardar nueva imagen
			String fileName = uploadFileService.saveImages(foto, admin.getNombre());
			admin.setFoto(fileName);
			usuarioRepository.save(admin);

			session.setAttribute("usuarioLogueado", admin);

			redirectAttrs.addFlashAttribute("success", "Foto actualizada correctamente");

		} catch (IOException e) {
			redirectAttrs.addFlashAttribute("error", "Error al guardar la foto: " + e.getMessage());
		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al actualizar foto: " + e.getMessage());
		}

		return "redirect:/admin?activeSection=perfil";
	}

	// CONFIGURACION DE MESERO

	@PostMapping("/actualizar-datos-mesero")
	public String actualizarPerfilMesero(@ModelAttribute Usuario adminActualizado, HttpSession session,
			RedirectAttributes redirectAttrs) {
		try {
			// Obtener el usuario logueado desde la sesión
			Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
			if (usuarioLogueado == null) {
				redirectAttrs.addFlashAttribute("error", "Sesión expirada. Por favor inicia sesión de nuevo.");
				return "redirect:/configuracion";
			}

			// Actualizar los campos que vienen del formulario
			if (adminActualizado.getNombre() != null && !adminActualizado.getNombre().isEmpty()) {
				usuarioLogueado.setNombre(adminActualizado.getNombre());
			}
			if (adminActualizado.getApellido() != null && !adminActualizado.getApellido().isEmpty()) {
				usuarioLogueado.setApellido(adminActualizado.getApellido());
			}
			if (adminActualizado.getCorreo() != null && !adminActualizado.getCorreo().isEmpty()) {
				usuarioLogueado.setCorreo(adminActualizado.getCorreo());
			}
			if (adminActualizado.getDireccion() != null) {
				usuarioLogueado.setDireccion(adminActualizado.getDireccion());
			}
			if (adminActualizado.getTelefono() != null) {
				usuarioLogueado.setTelefono(adminActualizado.getTelefono());
			}
			if (adminActualizado.getClave() != null && !adminActualizado.getClave().isEmpty()) {
				usuarioLogueado.setClave(adminActualizado.getClave());
			}

			// Guardar cambios en la BD
			usuarioService.updateUser(usuarioLogueado);

			// Actualizar la sesión con el usuario modificado
			session.setAttribute("usuarioLogueado", usuarioLogueado);

			redirectAttrs.addFlashAttribute("success", "Perfil actualizado correctamente");

		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al actualizar perfil: " + e.getMessage());
		}

		return "redirect:/configuracion";
	}

	/** ACTUALIZAR FOTO */
	@PostMapping("/actualizar-foto-mesero")
	public String actualizarFotoMesero(@RequestParam("id") Long id, @RequestParam("foto") MultipartFile foto,
			RedirectAttributes redirectAttrs, HttpSession session) {

		try {
			// Buscar el usuario
			Usuario mesero = usuarioRepository.findById(id)
					.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

			// Validar que el archivo no esté vacío
			if (foto.isEmpty()) {
				redirectAttrs.addFlashAttribute("error", "Por favor seleccione una foto");
				return "redirect:/configuracion";
			}

			// Eliminar foto anterior si existe
			if (mesero.getFoto() != null && !mesero.getFoto().isEmpty()) {
				try {
					uploadFileService.deleteImage(mesero.getFoto());
				} catch (Exception e) {
					System.err.println("⚠️ No se pudo borrar la foto anterior: " + e.getMessage());
				}
			}

			// Guardar nueva imagen
			String fileName = uploadFileService.saveImages(foto, mesero.getNombre());
			mesero.setFoto(fileName);
			usuarioRepository.save(mesero);

			session.setAttribute("usuarioLogueado", mesero);

			redirectAttrs.addFlashAttribute("success", "Foto actualizada correctamente");

		} catch (IOException e) {
			redirectAttrs.addFlashAttribute("error", "Error al guardar la foto: " + e.getMessage());
		} catch (Exception e) {
			redirectAttrs.addFlashAttribute("error", "Error al actualizar foto: " + e.getMessage());
		}

		return "redirect:/configuracion";
	}

	// CONFIGURACION DEL CAJERO

		@PostMapping("/actualizar-datos-caja")
		public String actualizarPerfilCajero(@ModelAttribute Usuario adminActualizado, HttpSession session,
				RedirectAttributes redirectAttrs) {
			try {
				// Obtener el usuario logueado desde la sesión
				Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
				if (usuarioLogueado == null) {
					redirectAttrs.addFlashAttribute("error", "Sesión expirada. Por favor inicia sesión de nuevo.");
					return "redirect:/caja?section=configuracion";
				}

				// Actualizar los campos que vienen del formulario
				if (adminActualizado.getNombre() != null && !adminActualizado.getNombre().isEmpty()) {
					usuarioLogueado.setNombre(adminActualizado.getNombre());
				}
				if (adminActualizado.getApellido() != null && !adminActualizado.getApellido().isEmpty()) {
					usuarioLogueado.setApellido(adminActualizado.getApellido());
				}
				if (adminActualizado.getCorreo() != null && !adminActualizado.getCorreo().isEmpty()) {
					usuarioLogueado.setCorreo(adminActualizado.getCorreo());
				}
				if (adminActualizado.getDireccion() != null) {
					usuarioLogueado.setDireccion(adminActualizado.getDireccion());
				}
				if (adminActualizado.getTelefono() != null) {
					usuarioLogueado.setTelefono(adminActualizado.getTelefono());
				}
				if (adminActualizado.getClave() != null && !adminActualizado.getClave().isEmpty()) {
					usuarioLogueado.setClave(adminActualizado.getClave());
				}

				// Guardar cambios en la BD
				usuarioService.updateUser(usuarioLogueado);

				// Actualizar la sesión con el usuario modificado
				session.setAttribute("usuarioLogueado", usuarioLogueado);

				redirectAttrs.addFlashAttribute("success", "Perfil actualizado correctamente");

			} catch (Exception e) {
				redirectAttrs.addFlashAttribute("error", "Error al actualizar perfil: " + e.getMessage());
			}

			return "redirect:/caja?section=configuracion";
		}

		/** ACTUALIZAR FOTO */
		@PostMapping("/actualizar-foto-caja")
		public String actualizarFotoCajero(@RequestParam("id") Long id, @RequestParam("foto") MultipartFile foto,
				RedirectAttributes redirectAttrs, HttpSession session) {

			try {
				// Buscar el usuario
				Usuario caja = usuarioRepository.findById(id)
						.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

				// Validar que el archivo no esté vacío
				if (foto.isEmpty()) {
					redirectAttrs.addFlashAttribute("error", "Por favor seleccione una foto");
					return "redirect:/caja?section=configuracion";
				}

				// Eliminar foto anterior si existe
				if (caja.getFoto() != null && !caja.getFoto().isEmpty()) {
					try {
						uploadFileService.deleteImage(caja.getFoto());
					} catch (Exception e) {
						System.err.println("⚠️ No se pudo borrar la foto anterior: " + e.getMessage());
					}
				}

				// Guardar nueva imagen
				String fileName = uploadFileService.saveImages(foto, caja.getNombre());
				caja.setFoto(fileName);
				usuarioRepository.save(caja);

				session.setAttribute("usuarioLogueado", caja);

				redirectAttrs.addFlashAttribute("success", "Foto actualizada correctamente");

			} catch (IOException e) {
				redirectAttrs.addFlashAttribute("error", "Error al guardar la foto: " + e.getMessage());
			} catch (Exception e) {
				redirectAttrs.addFlashAttribute("error", "Error al actualizar foto: " + e.getMessage());
			}

			return "redirect:/caja?section=configuracion";
		}

}
