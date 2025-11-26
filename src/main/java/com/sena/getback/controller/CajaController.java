package com.sena.getback.controller; 

import com.sena.getback.model.Factura;
import com.sena.getback.model.Pedido;
import com.sena.getback.model.Usuario;
import com.sena.getback.repository.FacturaRepository;
import com.sena.getback.repository.PedidoRepository;
import com.sena.getback.repository.UsuarioRepository;
import com.sena.getback.service.MenuService;
import com.sena.getback.service.MesaService;
import com.sena.getback.service.CategoriaService;
import com.sena.getback.service.FacturaService;
import com.sena.getback.service.PedidoService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.sena.getback.repository.ClienteFrecuenteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/caja")
public class CajaController {
	private final MesaService mesaService;
    private final MenuService menuService;
    private final CategoriaService categoriaService;
    private final FacturaRepository facturaRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoService pedidoService;
    private final ClienteFrecuenteRepository clienteFrecuenteRepository;
    

    // Pedidos marcados como "completados" visualmente en el inicio de caja (pero aún PENDIENTES de pago)
    private final Set<Integer> pedidosCompletadosInicioCaja = ConcurrentHashMap.newKeySet();

    public CajaController(MenuService menuService,
            CategoriaService categoriaService,
            FacturaRepository facturaRepository,
            PedidoRepository pedidoRepository,
            UsuarioRepository usuarioRepository,
            PedidoService pedidoService,
            ClienteFrecuenteRepository clienteFrecuenteRepository,
            MesaService mesaService) {
this.menuService = menuService;
this.categoriaService = categoriaService;
this.facturaRepository = facturaRepository;
this.pedidoRepository = pedidoRepository;
this.usuarioRepository = usuarioRepository;
this.pedidoService = pedidoService;
this.clienteFrecuenteRepository = clienteFrecuenteRepository;
this.mesaService = mesaService;
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

        if ("inicio-caja".equals(activeSection) || "pedidos".equals(activeSection)) {
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

            model.addAttribute("clientes", clienteFrecuenteRepository.findAll());

           
            model.addAttribute("mesas", mesaService.findAll());
        }

        if ("pagos".equals(activeSection)) {
            model.addAttribute("pagosPendientes", pedidoService.obtenerPedidosPendientes());
        }

        if ("historial-pagos".equals(activeSection)) {
            List<Pedido> pagosPagados = pedidoService.obtenerPedidosPagados();

            if (filtro != null && !filtro.isEmpty()) {
                LocalDate hoy = LocalDate.now();

                if (filtro.equalsIgnoreCase("hoy")) {
                    pagosPagados = pagosPagados.stream()
                            .filter(p -> p.getFechaCreacion() != null
                                    && p.getFechaCreacion().toLocalDate().equals(hoy))
                            .collect(Collectors.toList());

                } else if (filtro.equalsIgnoreCase("semana")) {

                    // Rango de la semana actual: lunes a domingo
                    LocalDate inicioSemana = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    LocalDate finSemana = hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

                    pagosPagados = pagosPagados.stream()
                            .filter(p -> p.getFechaCreacion() != null)
                            .filter(p -> {
                                LocalDate fecha = p.getFechaCreacion().toLocalDate();
                                return (fecha.isEqual(inicioSemana) || fecha.isAfter(inicioSemana))
                                        && (fecha.isEqual(finSemana) || fecha.isBefore(finSemana));
                            })
                            .collect(Collectors.toList());

                } else if (filtro.equalsIgnoreCase("mes")) {
                    pagosPagados = pagosPagados.stream()
                            .filter(p -> p.getFechaCreacion() != null)
                            .filter(p -> {
                                LocalDate fecha = p.getFechaCreacion().toLocalDate();
                                return fecha.getYear() == hoy.getYear()
                                        && fecha.getMonth() == hoy.getMonth();
                            })
                            .collect(Collectors.toList());

                } else if (filtro.equalsIgnoreCase("anio")) {
                    pagosPagados = pagosPagados.stream()
                            .filter(p -> p.getFechaCreacion() != null)
                            .filter(p -> p.getFechaCreacion().toLocalDate().getYear() == hoy.getYear())
                            .collect(Collectors.toList());
                }
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
    public String marcarPedidoComoCompletadoDesdeInicio(@RequestParam("pedidoId") Integer pedidoId,
                                                        @RequestParam(value = "section", required = false) String section,
                                                        @RequestParam(value = "accion", required = false) String accion) {
        // Solo marcar visualmente como "completado" en el panel de inicio de caja.
        // El estado de pago (PAGADO) se gestiona exclusivamente desde la vista de pagos (/caja?section=pagos).
        if (pedidoId != null) {
            if (accion != null && accion.equalsIgnoreCase("revertir")) {
                pedidoService.marcarPedidoComoPendienteBar(pedidoId);
                pedidosCompletadosInicioCaja.remove(pedidoId);
            } else {
                pedidoService.marcarPedidoComoCompletadoBar(pedidoId);
                pedidosCompletadosInicioCaja.add(pedidoId);
            }
        }

        if (section != null && !section.isEmpty()) {
            return "redirect:/caja?section=" + section;
        }

        return "redirect:/caja";
        
        
    }
  
 // VENTA RÁPIDA DESDE PUNTO DE VENTA (POS)
    @PostMapping("/punto-venta/registrar")
    public String registrarVentaPuntoVenta(
            @RequestParam(value = "total", required = false) Double total,
            @RequestParam(value = "montoRecibido", required = false) Double montoRecibido,
            @RequestParam(value = "mesaId", required = false) Integer mesaId,
            @RequestParam(value = "metodoPago", defaultValue = "EFECTIVO") String metodoPago,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (total == null || total <= 0) {
            redirectAttributes.addFlashAttribute("mensajeError", "El total de la venta no es válido.");
            return "redirect:/caja?section=punto-venta";
        }

        try {
            BigDecimal totalBD = BigDecimal.valueOf(total);
            LocalDateTime ahora = LocalDateTime.now();

            Factura factura = new Factura();
            factura.setMonto(totalBD);
            factura.setSubtotal(totalBD);
            factura.setTotalPagar(totalBD);
            factura.setValorDescuento(BigDecimal.ZERO);
            factura.setFechaEmision(ahora);
            factura.setFechaPago(ahora);
            factura.setNumeroFactura("POS-" + ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")));
            factura.setMetodoPago(metodoPago);
            factura.setEstadoPago("PAGADO");
            factura.setEstadoFactura("GENERADA");

            // Mesa opcional
            if (mesaId != null && mesaId > 0) {
                mesaService.findById(mesaId).ifPresent(mesa -> factura.setNumeroMesa(mesa.getId()));
            }

         // CAJERO LOGUEADO (FUNCIONA EN TODOS LOS CASOS, SIN ERRORES)
            if (authentication != null && authentication.getPrincipal() != null) {
                String correoLogin = authentication.getName(); // Este es el correo del cajero

                usuarioRepository.findByCorreo(correoLogin)
                    .ifPresent(usuario -> {
                        factura.setUsuario(usuario);
                        // Opcional: para depuración
                        System.out.println("Venta registrada por: " + usuario.getNombre() + " " + usuario.getApellido());
                    });
            }

            facturaRepository.save(factura);

            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Venta registrada con éxito → Factura: " + factura.getNumeroFactura());

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("mensajeError", "Error al registrar la venta.");
        }

        return "redirect:/caja?section=punto-venta";
    }
  
}
    


