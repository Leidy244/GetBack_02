package com.sena.getback.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sena.getback.model.Inventario;
import com.sena.getback.repository.InventarioRepository;
import com.sena.getback.repository.MenuRepository;
import com.sena.getback.model.Menu;

@Service
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final MenuRepository menuRepository;

    public InventarioService(InventarioRepository inventarioRepository, MenuRepository menuRepository) {
        this.inventarioRepository = inventarioRepository;
        this.menuRepository = menuRepository;
    }

    public List<Inventario> listarIngresosRecientes() {
        return inventarioRepository.findAllByOrderByFechaIngresoDesc();
    }

    /**
     * Devuelve una lista de nombres de productos distintos registrados en inventario,
     * ordenados alfabéticamente. Se usa para sugerir nombres al crear/editar productos de menú.
     */
    public List<String> listarNombresProductosInventario() {
        return inventarioRepository.findAll().stream()
            .map(Inventario::getProducto)
            .filter(p -> p != null && !p.isBlank())
            .map(String::trim)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    /**
     * Calcula el stock total por producto (campo producto en Inventario).
     */
    public Map<String, Integer> calcularStockPorProducto() {
        Map<String, Integer> sumados = inventarioRepository.findAll().stream()
            .filter(i -> i.getProducto() != null && !i.getProducto().isBlank())
            .collect(Collectors.groupingBy(
                    Inventario::getProducto,
                    Collectors.summingInt(i -> i.getCantidad() != null ? i.getCantidad() : 0)));
        return sumados.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> Math.max(0, e.getValue())));
    }

    /**
     * Devuelve un mapa con los productos cuyo stock es menor o igual al umbral dado.
     */
    public Map<String, Integer> obtenerProductosBajoStock(int umbral) {
        return calcularStockPorProducto().entrySet().stream()
            .filter(e -> e.getValue() <= umbral)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Obtiene el stock disponible de un producto por nombre exacto (ignorando mayúsculas/minúsculas).
     */
    public int obtenerStockDisponible(String nombreProducto) {
        if (nombreProducto == null || nombreProducto.isBlank()) {
            return 0;
        }
        String clave = nombreProducto.trim().toLowerCase();
        return calcularStockPorProducto().entrySet().stream()
            .filter(e -> e.getKey() != null && e.getKey().trim().toLowerCase().equals(clave))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(0);
    }

    public Optional<Inventario> obtenerPorId(Long id) {
        return inventarioRepository.findById(id);
    }

    @Transactional
    public Inventario registrarIngreso(Inventario inventario) {
        Inventario saved = inventarioRepository.save(inventario);
        syncMenuStockByProductName(inventario.getProducto());
        return saved;
    }

    @Transactional
    public Inventario actualizarIngreso(Inventario inventario) {
        Inventario saved = inventarioRepository.save(inventario);
        syncMenuStockByProductName(inventario.getProducto());
        return saved;
    }

    @Transactional
    public void eliminarIngreso(Long id) {
        inventarioRepository.findById(id).ifPresent(inv -> {
            String nombre = inv.getProducto();
            inventarioRepository.deleteById(id);
            syncMenuStockByProductName(nombre);
        });
    }

    /**
     * Desvincula el menú de los movimientos de inventario que lo referencian
     * para permitir eliminar el producto sin violar la FK.
     */
    @Transactional
    public void detachMenuReferences(Long menuId) {
        if (menuId == null) return;
        List<Inventario> ref = inventarioRepository.findByMenu_Id(menuId);
        if (ref == null || ref.isEmpty()) return;
        ref.forEach(i -> i.setMenu(null));
        inventarioRepository.saveAll(ref);
        // opcional: también sincronizar stock de menú para productos asociados a este menú
        ref.stream().map(Inventario::getProducto).filter(p -> p != null && !p.isBlank()).distinct()
                .forEach(this::syncMenuStockByProductName);
    }

    @Transactional
    public void purgeByProductName(String nombreProducto) {
        if (nombreProducto == null || nombreProducto.isBlank()) return;
        inventarioRepository.deleteByProductoIgnoreCase(nombreProducto.trim());
        syncMenuStockByProductName(nombreProducto);
    }

    @Transactional
    public void purgeByMenuId(Long menuId) {
        if (menuId == null) return;
        List<Inventario> ref = inventarioRepository.findByMenu_Id(menuId);
        if (ref == null || ref.isEmpty()) return;
        // Capturar nombres antes de borrar para sincronizar stock
        List<String> nombres = ref.stream()
                .map(Inventario::getProducto)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .toList();
        inventarioRepository.deleteAll(ref);
        nombres.forEach(this::syncMenuStockByProductName);
    }

    /**
     * Registra una salida de inventario (consumo) para el producto indicado.
     * La cantidad se almacena como negativa para que calcularStockPorProducto la descuente.
     */
    @Transactional
    public void registrarConsumo(String nombreProducto, int cantidadConsumida) {
        if (nombreProducto == null || nombreProducto.isBlank()) {
            return;
        }
        if (cantidadConsumida <= 0) {
            return;
        }

        int disponible = obtenerStockDisponible(nombreProducto);
        int consumoReal = Math.min(cantidadConsumida, Math.max(0, disponible));
        if (consumoReal <= 0) {
            return;
        }

        Inventario movimiento = new Inventario();
        movimiento.setRemision("CONSUMO PEDIDO");
        movimiento.setProducto(nombreProducto.trim());
        movimiento.setCantidad(-consumoReal);
        movimiento.setObservaciones("Consumo automático por pedido");
        movimiento.setFechaIngreso(java.time.LocalDateTime.now());
        inventarioRepository.save(movimiento);
        syncMenuStockByProductName(nombreProducto);
    }

    /**
     * Sincroniza el campo stock del menú para productos del área BAR
     * con el stock calculado en inventario por nombre.
     */
    @Transactional
    public void syncMenuStockByProductName(String nombreProducto) {
        if (nombreProducto == null || nombreProducto.isBlank()) return;
        int disponible = obtenerStockDisponible(nombreProducto);
        List<Menu> menus = menuRepository.findByNombreProductoIgnoreCase(nombreProducto.trim());
        if (menus == null || menus.isEmpty()) return;
        for (Menu m : menus) {
            m.setStock(disponible);
        }
        menuRepository.saveAll(menus);
    }
}
