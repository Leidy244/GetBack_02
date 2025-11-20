package com.sena.getback.config;

import com.sena.getback.model.Estado;
import com.sena.getback.model.Rol;
import com.sena.getback.repository.EstadoRepository;
import com.sena.getback.repository.RolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final EstadoRepository estadoRepository;
    private final RolRepository rolRepository;

    public DataInitializer(EstadoRepository estadoRepository, RolRepository rolRepository) {
        this.estadoRepository = estadoRepository;
        this.rolRepository = rolRepository;
    }

    @Override
    public void run(String... args) {
        inicializarEstados();
        inicializarRoles();
    }

    private void inicializarEstados() {
        // 1 = PENDIENTE (PEDIDO)
        crearOActualizarEstado(1, "PENDIENTE", "PEDIDO");
        // 2 = PAGADO (PAGADO)
        crearOActualizarEstado(2, "PAGADO", "PAGADO");
        // 3 = COMPLETADO (COMPLETADO)
        crearOActualizarEstado(3, "COMPLETADO", "COMPLETADO");
    }

    private void crearOActualizarEstado(Integer id, String nombre, String tipo) {
        Estado estado = estadoRepository.findById(id).orElseGet(() -> {
            Estado e = new Estado();
            e.setId(id);
            return e;
        });
        estado.setNombreEstado(nombre);
        estado.setTipoEstado(tipo);
        estadoRepository.save(estado);
    }

    private void inicializarRoles() {
        // 1 = ADMIN, 2 = CAJA, 3 = MESERO
        crearOActualizarRol(1, "ADMIN");
        crearOActualizarRol(2, "CAJA");
        crearOActualizarRol(3, "MESERO");
    }

    private void crearOActualizarRol(Integer id, String nombre) {
        Rol rol = rolRepository.findById(id).orElseGet(() -> {
            Rol r = new Rol();
            r.setId(id);
            return r;
        });
        rol.setNombre(nombre);
        rolRepository.save(rol);
    }
}
