package com.sena.getback.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sena.getback.model.Inventario;
import com.sena.getback.repository.InventarioRepository;

@Service
public class InventarioService {

    private final InventarioRepository inventarioRepository;

    public InventarioService(InventarioRepository inventarioRepository) {
        this.inventarioRepository = inventarioRepository;
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
        return inventarioRepository.findAll().stream()
            .filter(i -> i.getProducto() != null && !i.getProducto().isBlank())
            .collect(Collectors.groupingBy(
                    Inventario::getProducto,
                    Collectors.summingInt(i -> i.getCantidad() != null ? i.getCantidad() : 0)));
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
        return inventarioRepository.save(inventario);
    }

    @Transactional
    public Inventario actualizarIngreso(Inventario inventario) {
        return inventarioRepository.save(inventario);
    }

    @Transactional
    public void eliminarIngreso(Long id) {
        inventarioRepository.deleteById(id);
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

        Inventario movimiento = new Inventario();
        movimiento.setRemision("CONSUMO PEDIDO");
        movimiento.setProducto(nombreProducto.trim());
        movimiento.setCantidad(-cantidadConsumida);
        movimiento.setObservaciones("Consumo automático por pedido");

        inventarioRepository.save(movimiento);
    }
}
