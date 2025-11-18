package com.sena.getback.controller;

import com.sena.getback.model.Factura;
import com.sena.getback.model.Pedido;
import com.sena.getback.repository.FacturaRepository;
import com.sena.getback.repository.PedidoRepository;
import com.sena.getback.repository.UsuarioRepository;
import com.sena.getback.service.MenuService;
import com.sena.getback.service.CategoriaService;
import com.sena.getback.service.PedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/caja")
public class CajaController {

    private final MenuService menuService;
    private final CategoriaService categoriaService;
    private final FacturaRepository facturaRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoService pedidoService;

    public CajaController(MenuService menuService,
                          CategoriaService categoriaService,
                          FacturaRepository facturaRepository,
                          PedidoRepository pedidoRepository,
                          UsuarioRepository usuarioRepository,
                          PedidoService pedidoService) {
        this.menuService = menuService;
        this.categoriaService = categoriaService;
        this.facturaRepository = facturaRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public String panelCaja(
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String categoria,
            Model model) {

        String activeSection = (section != null && !section.isEmpty()) ? section : "inicio-caja";
        model.addAttribute("activeSection", activeSection);
        model.addAttribute("title", "Panel principal - Cajero");

        if ("inicio-caja".equals(activeSection)) {
            List<Factura> facturas = facturaRepository.findAll();
            List<Pedido> pedidos = pedidoRepository.findAll();
            long totalUsuarios = usuarioRepository.count();

            long totalVentas = facturas.size();
            model.addAttribute("totalVentas", totalVentas);
            model.addAttribute("totalUsuarios", totalUsuarios);

            long ventasHoy = facturas.stream()
                    .filter(f -> f.getFechaEmision().toLocalDate().equals(LocalDate.now()))
                    .count();
            model.addAttribute("ventasHoy", ventasHoy);

            double ingresosHoy = facturas.stream()
                    .filter(f -> f.getFechaEmision().toLocalDate().equals(LocalDate.now()))
                    .filter(f -> "PAGADO".equals(f.getEstadoPago()))
                    .mapToDouble(f -> f.getTotalPagar().doubleValue())
                    .sum();
            model.addAttribute("ingresosHoy", ingresosHoy);

            double ingresosTotales = facturas.stream()
                    .filter(f -> "PAGADO".equals(f.getEstadoPago()))
                    .mapToDouble(f -> f.getTotalPagar().doubleValue())
                    .sum();
            model.addAttribute("ingresosTotales", ingresosTotales);

            long pedidosPendientes = pedidos.stream()
                    .filter(p -> "PENDIENTE".equals(p.getEstado()))
                    .count();
            model.addAttribute("pedidosPendientes", pedidosPendientes);

            long ventasCanceladas = facturas.stream()
                    .filter(f -> "CANCELADO".equals(f.getEstadoPago()))
                    .count();
            model.addAttribute("ventasCanceladas", ventasCanceladas);

            List<Factura> actividadReciente = facturas.stream()
                    .sorted(Comparator.comparing(Factura::getFechaEmision).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
            model.addAttribute("actividadReciente", actividadReciente);
        }

        if ("punto-venta".equals(activeSection)) {
            model.addAttribute("categorias", categoriaService.findAll());

            if (categoria != null && !categoria.isEmpty()) {
                model.addAttribute("menus", menuService.findByCategoriaNombre(categoria));
            } else {
                model.addAttribute("menus", menuService.findAll());
            }
        }

        if ("pagos".equals(activeSection)) {
            model.addAttribute("pagosPendientes", pedidoService.obtenerPedidosPendientes());
        }

        if ("historial-pagos".equals(activeSection)) {
            model.addAttribute("pagosPagados", pedidoService.obtenerPedidosPagados());
        }

        return "caja/panel_caja";
    }

    @PostMapping("/pagar")
    public String pagarPedido(@RequestParam("pedidoId") Integer pedidoId,
                              @RequestParam(value = "montoRecibido", required = false) Double montoRecibido) {
        pedidoService.marcarPedidoComoPagado(pedidoId, montoRecibido);
        return "redirect:/caja?section=pagos";
    }


}
