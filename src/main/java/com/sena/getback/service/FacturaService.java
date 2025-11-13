package com.sena.getback.service;

import com.sena.getback.model.Factura;
import com.sena.getback.repository.FacturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FacturaService {

    @Autowired
    private FacturaRepository facturaRepository;

    // 📊 Ventas agrupadas por día
    public Map<String, Double> obtenerVentasPorDia() {
        List<Factura> facturas = facturaRepository.findAll();

        return facturas.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getFechaEmision().toLocalDate().toString(),
                        TreeMap::new, // mantiene orden cronológico
                        Collectors.summingDouble(f -> f.getTotalPagar().doubleValue())
                ));
    }

    // 📅 Ventas de hoy
    public long obtenerVentasHoy() {
        LocalDate hoy = LocalDate.now();
        return facturaRepository.findAll().stream()
                .filter(f -> f.getFechaEmision().toLocalDate().isEqual(hoy))
                .count();
    }

    // 💰 Total de ingresos
    public double obtenerIngresosTotales() {
        return facturaRepository.findAll().stream()
                .mapToDouble(f -> f.getTotalPagar().doubleValue())
                .sum();
    }

    // 💵 Ingresos de hoy
    public double obtenerIngresosHoy() {
        LocalDate hoy = LocalDate.now();
        return facturaRepository.findAll().stream()
                .filter(f -> f.getFechaEmision() != null && f.getFechaEmision().toLocalDate().isEqual(hoy))
                .mapToDouble(f -> f.getTotalPagar().doubleValue())
                .sum();
    }

    // 🧾 Cantidad total de facturas (ventas)
    public long contarFacturas() {
        return facturaRepository.count();
    }
}
