package com.sena.getback.controller;

import com.sena.getback.model.*;
import com.sena.getback.repository.*;
import com.sena.getback.service.*;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final MenuService menuService;
    private final CategoriaService categoriaService;
    private final UsuarioService usuarioService;
    private final LocationService locationService;
    private final MesaService mesaService;

    @Autowired
    private UploadFileService uploadFileService;

    private final CategoriaRepository categoriaRepository;
    private final MenuRepository menuRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LocationRepository locationRepository;
    private final MesaRepository mesaRepository;
    private final RolRepository rolRepository;

    @Autowired
    public AdminController(
            CategoriaRepository categoriaRepository,
            MenuRepository menuRepository,
            EventoRepository eventoRepository,
            UsuarioRepository usuarioRepository,
            UsuarioService usuarioService,
            MenuService menuService,
            CategoriaService categoriaService,
            UploadFileService uploadFileService,
            LocationService locationService,
            LocationRepository locationRepository,
            MesaService mesaService,
            MesaRepository mesaRepository,
            RolRepository rolRepository) {

        this.menuService = menuService;
        this.categoriaService = categoriaService;
        this.usuarioService = usuarioService;
        this.uploadFileService = uploadFileService;
        this.locationService = locationService;
        this.mesaService = mesaService;

        this.categoriaRepository = categoriaRepository;
        this.menuRepository = menuRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.locationRepository = locationRepository;
        this.mesaRepository = mesaRepository;
        this.rolRepository = rolRepository;
    }

    /** ==================== PANEL PRINCIPAL ==================== **/
    @GetMapping
    public String panel(@RequestParam(value = "activeSection", required = false) String activeSection, Model model) {

        String section = (activeSection != null && !activeSection.isEmpty()) ? activeSection : "dashboard";
        model.addAttribute("activeSection", section);
        model.addAttribute("title", "Panel de Administración");

        try {
            // DASHBOARD
            if ("dashboard".equals(section)) {
                model.addAttribute("totalCategorias", categoriaRepository.count());
                model.addAttribute("totalProductos", menuRepository.count());
                model.addAttribute("totalEventos", eventoRepository.count());
                model.addAttribute("totalUsuarios", usuarioRepository.count());
                model.addAttribute("totalUbicaciones", locationRepository.count());
                model.addAttribute("totalMesas", mesaRepository.count());
            }

            // LOCATIONS
            if ("locations".equals(section)) {
                model.addAttribute("locations", locationRepository.findAll());
                model.addAttribute("totalUbicaciones", locationRepository.count());
                model.addAttribute("location", model.containsAttribute("location") ? model.getAttribute("location") : new Location());
            }

            // MESAS
            if ("mesas".equals(section)) {
                model.addAttribute("mesas", mesaRepository.findAll());
                model.addAttribute("totalMesas", mesaRepository.count());
                model.addAttribute("ubicaciones", locationRepository.findAll());
                model.addAttribute("mesa", model.containsAttribute("mesa") ? model.getAttribute("mesa") : new Mesa());
            }

            // PRODUCTOS
            if ("products".equals(section)) {
                model.addAttribute("products", menuRepository.findAll());
                model.addAttribute("newProduct", new Menu());
                model.addAttribute("categorias", categoriaRepository.findAll());
            }

            // CATEGORÍAS
            if ("categories".equals(section)) {
                model.addAttribute("categorias", categoriaRepository.findAll());
                model.addAttribute("categoria", new Categoria());
            }

            // EVENTOS
            if ("events".equals(section)) {
                model.addAttribute("eventos", eventoRepository.findAll());
                model.addAttribute("evento", new Evento());
            }

            // USUARIOS
            if ("users".equals(section)) {
                model.addAttribute("users", usuarioService.findAllUsers());
                model.addAttribute("newUser", new Usuario());
                model.addAttribute("roles", rolRepository.findAll());
            }

            // PERFIL ADMIN
            Usuario admin = usuarioService.getFirstUser().orElse(new Usuario());
            model.addAttribute("usuario", admin);

        } catch (Exception e) {
            System.err.println("❌ Error cargando datos: " + e.getMessage());
            model.addAttribute("categorias", java.util.Collections.emptyList());
            model.addAttribute("products", java.util.Collections.emptyList());
            model.addAttribute("eventos", java.util.Collections.emptyList());
            model.addAttribute("users", java.util.Collections.emptyList());
            model.addAttribute("locations", java.util.Collections.emptyList());
            model.addAttribute("mesas", java.util.Collections.emptyList());
            model.addAttribute("roles", java.util.Collections.emptyList());

            model.addAttribute("newProduct", new Menu());
            model.addAttribute("categoria", new Categoria());
            model.addAttribute("evento", new Evento());
            model.addAttribute("location", new Location());
            model.addAttribute("mesa", new Mesa());
            model.addAttribute("usuario", new Usuario());
        }

        return "admin";
    }

    /** ==================== PERFIL ADMIN ==================== **/
    @GetMapping("/perfil")
    public String mostrarPerfil(Model model) {
        Usuario admin = usuarioService.getFirstUser().orElse(new Usuario());
        model.addAttribute("usuario", admin);
        model.addAttribute("activeSection", "perfil");
        return "admin";
    }

    @PostMapping("/actualizar-datos")
    public String actualizarPerfil(@ModelAttribute("usuario") Usuario adminActualizado,
                                   RedirectAttributes redirectAttrs) {
        try {
            Usuario admin = usuarioService.getFirstUser()
                    .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

            if (adminActualizado.getNombre() != null && !adminActualizado.getNombre().isEmpty())
                admin.setNombre(adminActualizado.getNombre());
            if (adminActualizado.getApellido() != null && !adminActualizado.getApellido().isEmpty())
                admin.setApellido(adminActualizado.getApellido());
            if (adminActualizado.getCorreo() != null && !adminActualizado.getCorreo().isEmpty())
                admin.setCorreo(adminActualizado.getCorreo());
            if (adminActualizado.getDireccion() != null)
                admin.setDireccion(adminActualizado.getDireccion());
            if (adminActualizado.getTelefono() != null)
                admin.setTelefono(adminActualizado.getTelefono());
            if (adminActualizado.getClave() != null && !adminActualizado.getClave().isEmpty())
                admin.setClave(adminActualizado.getClave());

            usuarioService.updateUser(admin);
            redirectAttrs.addFlashAttribute("success", "Perfil actualizado correctamente");

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Error al actualizar perfil: " + e.getMessage());
        }
        return "redirect:/admin?activeSection=perfil";
    }

    /** ==================== ACTUALIZAR FOTO ==================== **/
    @PostMapping("/actualizar-foto")
    public String actualizarFoto(@RequestParam("id") Long id, @RequestParam("foto") MultipartFile foto,
                                 RedirectAttributes redirectAttrs) {
        try {
            Usuario admin = usuarioRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (foto.isEmpty()) {
                redirectAttrs.addFlashAttribute("error", "Por favor seleccione una foto");
                return "redirect:/admin?activeSection=perfil";
            }

            // Validar tipo MIME
            if (!foto.getContentType().startsWith("image/")) {
                redirectAttrs.addFlashAttribute("error", "El archivo debe ser una imagen válida");
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

            String fileName = uploadFileService.saveImages(foto, admin.getNombre());
            admin.setFoto(fileName);
            usuarioRepository.save(admin);

            redirectAttrs.addFlashAttribute("success", "Foto actualizada correctamente");

        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("error", "Error al guardar la foto: " + e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Error al actualizar foto: " + e.getMessage());
        }

        return "redirect:/admin?activeSection=perfil";
    }}
    
