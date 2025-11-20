package com.sena.getback.controller; 

import com.sena.getback.model.Factura;
import com.sena.getback.model.Pedido;
import com.sena.getback.model.Usuario;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    // Pedidos marcados como "completados" visualmente en el inicio de caja (pero aún PENDIENTES de pago)
    private final Set<Integer> pedidosCompletadosInicioCaja = ConcurrentHashMap.newKeySet();

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
            @RequestParam(required = false) String filtro,
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

            // Ventas del día basadas en los pedidos pagados que genera caja
            List<Pedido> pedidosPagados = pedidoService.obtenerPedidosPagados();
            LocalDate hoy = LocalDate.now();

            long ventasHoy = pedidosPagados.stream()
                    .filter(p -> p.getFechaCreacion() != null
                            && p.getFechaCreacion().toLocalDate().equals(hoy))
                    .count();
            model.addAttribute("ventasHoy", ventasHoy);

            double ingresosHoy = pedidosPagados.stream()
                    .filter(p -> p.getFechaCreacion() != null
                            && p.getFechaCreacion().toLocalDate().equals(hoy))
                    .mapToDouble(p -> p.getTotal() != null ? p.getTotal() : 0.0)
                    .sum();
            model.addAttribute("ingresosHoy", ingresosHoy);

            double ingresosTotales = facturas.stream()
                    .filter(f -> "PAGADO".equals(f.getEstadoPago()))
                    .mapToDouble(f -> f.getTotalPagar().doubleValue())
                    .sum();
            model.addAttribute("ingresosTotales", ingresosTotales);

            // Cantidad de pedidos pendientes de cobro (se usa en la tarjeta verde del dashboard y en la sección Pagos)
            long pedidosPendientesPago = pedidoService.obtenerPedidosPendientes().size();
            model.addAttribute("pedidosPendientesPago", pedidosPendientesPago);

            long ventasCanceladas = facturas.stream()
                    .filter(f -> "CANCELADO".equals(f.getEstadoPago()))
                    .count();
            model.addAttribute("ventasCanceladas", ventasCanceladas);

            List<Factura> actividadReciente = facturas.stream()
                    .sorted(Comparator.comparing(Factura::getFechaEmision).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
            model.addAttribute("actividadReciente", actividadReciente);

            // Pedidos para mostrar en cards en el inicio
            // Ambos grupos se basan en pedidos PENDIENTES (no pagados todavía):
            //  - pedidosPendientesBar: aún no marcados como completados en el panel
            //  - pedidosPagadosBar: marcados como "completados" visualmente (pero siguen pendientes de pago)
            List<Pedido> pedidosPendientesBarTodos = pedidoService.obtenerPedidosPendientesBar();

            List<Pedido> pedidosPendientesBar = pedidosPendientesBarTodos.stream()
                    .filter(p -> p.getId() != null && !pedidosCompletadosInicioCaja.contains(p.getId()))
                    .collect(Collectors.toList());

            List<Pedido> pedidosCompletadosBar = pedidosPendientesBarTodos.stream()
                    .filter(p -> p.getId() != null && pedidosCompletadosInicioCaja.contains(p.getId()))
                    .collect(Collectors.toList());

            model.addAttribute("pedidosPendientesBar", pedidosPendientesBar);
            model.addAttribute("pedidosPagadosBar", pedidosCompletadosBar);

            // Para el dashboard, "Pedidos Pendientes" refleja los pedidos de BAR aún no marcados como completados en el inicio
            long pedidosPendientes = pedidosPendientesBar.size();
            model.addAttribute("pedidosPendientes", pedidosPendientes);
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
            List<Pedido> pagosPagados = pedidoService.obtenerPedidosPagados();

            if (filtro != null && filtro.equalsIgnoreCase("hoy")) {
                LocalDate hoy = LocalDate.now();
                pagosPagados = pagosPagados.stream()
                        .filter(p -> p.getFechaCreacion() != null
                                && p.getFechaCreacion().toLocalDate().equals(hoy))
                        .collect(Collectors.toList());
            }

            model.addAttribute("pagosPagados", pagosPagados);
            model.addAttribute("filtro", filtro);
        }

        if ("corte-caja".equals(activeSection)) {
            LocalDate hoy = LocalDate.now();

            List<Pedido> pagosPagadosHoy = pedidoService.obtenerPedidosPagados().stream()
                    .filter(p -> p.getFechaCreacion() != null
                            && p.getFechaCreacion().toLocalDate().equals(hoy))
                    .collect(Collectors.toList());

            long ventasDia = pagosPagadosHoy.size();
            double ingresosDia = pagosPagadosHoy.stream()
                    .mapToDouble(p -> p.getTotal() != null ? p.getTotal() : 0.0)
                    .sum();

            model.addAttribute("fechaCorte", hoy);
            model.addAttribute("ventasDia", ventasDia);
            model.addAttribute("ingresosDia", ingresosDia);
        }

        return "caja/panel_caja";
    }

    @PostMapping("/pagar")
    public String pagarPedido(@RequestParam("pedidoId") Integer pedidoId,
                              @RequestParam(value = "montoRecibido", required = false) Double montoRecibido) {
        pedidoService.marcarPedidoComoPagado(pedidoId, montoRecibido);
        return "redirect:/caja?section=pagos";
    }

    @PostMapping("/marcar-completado")
    public String marcarPedidoComoCompletadoDesdeInicio(@RequestParam("pedidoId") Integer pedidoId) {
        // Solo marcar visualmente como "completado" en el panel de inicio de caja.
        // El estado de pago (PAGADO) se gestiona exclusivamente desde la vista de pagos (/caja?section=pagos).
        if (pedidoId != null) {
            pedidosCompletadosInicioCaja.add(pedidoId);
        }
        return "redirect:/caja";
    }
 
}

