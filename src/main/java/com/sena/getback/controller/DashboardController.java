package com.sena.getback.controller;

import com.sena.getback.service.*;
import org.knowm.xchart.*;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Controller
public class DashboardController {

    @Autowired private MenuService menuService;
    @Autowired private CategoriaService categoriaService;
    @Autowired private EventoService eventoService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private MesaService mesaService;
    @Autowired private FacturaService facturaService;
    @Autowired private PedidoService pedidoService;
    

    @GetMapping("/dashboard")
    public String mostrarDashboard(Model model, jakarta.servlet.http.HttpSession session) {
        System.out.println("=== INICIANDO DASHBOARD SPRING ===");
        
        try {
            // =======================
            // 1️⃣ CONFIGURAR RUTAS SPRING
            // =======================
            String staticImgPath = System.getProperty("user.dir") + java.io.File.separator + "images";
            System.out.println("📁 Ruta externa images: " + staticImgPath);
            
            // Crear directorio si no existe
            Files.createDirectories(Paths.get(staticImgPath));
            
            // =======================
            // 2️⃣ CREAR PLACEHOLDER SI NO EXISTE
            // =======================
            crearPlaceholderSpring(staticImgPath);
            
            // =======================
            // 3️⃣ MÉTRICAS PRINCIPALES
            // =======================
            long ventasHoyVal = facturaService.obtenerVentasHoy();
            double ingresosHoyVal = facturaService.obtenerIngresosHoy();
            Object estadoCaja = session != null ? session.getAttribute("cajaEstado") : null;
            boolean abiertaCaja = false;
            if (estadoCaja instanceof java.util.Map<?, ?> map) {
                Object abiertaVal = map.get("abierta");
                abiertaCaja = (abiertaVal instanceof Boolean) ? (Boolean) abiertaVal : false;
            }
            if (!abiertaCaja) {
                ventasHoyVal = 0;
                ingresosHoyVal = 0.0;
            }
            model.addAttribute("ventasHoy", ventasHoyVal);
            model.addAttribute("ingresosHoy", ingresosHoyVal);
            model.addAttribute("ingresosTotales", facturaService.obtenerIngresosTotales());
            model.addAttribute("totalVentas", facturaService.contarFacturas());
            model.addAttribute("pedidosPendientes", pedidoService.contarPedidosPendientes());
            model.addAttribute("totalProductos", menuService.count());
            model.addAttribute("totalCategorias", categoriaService.count());
            model.addAttribute("totalEventos", eventoService.count());
            model.addAttribute("totalUsuarios", usuarioService.countActiveUsers());
            model.addAttribute("totalMesas", mesaService.count());

            // =======================
            // 4️⃣ GENERAR GRÁFICAS
            // =======================
            boolean graficasGeneradas = generarTodasLasGraficasSpring(staticImgPath);
            
            if (graficasGeneradas) {
                System.out.println("✅ Gráficas generadas en: " + staticImgPath);
                model.addAttribute("chartCategorias", "/images/chart-categorias.png");
                model.addAttribute("chartEventos", "/images/chart-eventos.png");
                model.addAttribute("chartUsuarios", "/images/chart-usuarios.png");
                model.addAttribute("chartMesas", "/images/chart-mesas.png");
            } else {
                System.out.println("🔄 Usando placeholders");
                model.addAttribute("chartCategorias", "/images/placeholder-chart.png");
                model.addAttribute("chartEventos", "/images/placeholder-chart.png");
                model.addAttribute("chartUsuarios", "/images/placeholder-chart.png");
                model.addAttribute("chartMesas", "/images/placeholder-chart.png");
            }

        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("chartCategorias", "/images/placeholder-chart.png");
            model.addAttribute("chartEventos", "/images/placeholder-chart.png");
            model.addAttribute("chartUsuarios", "/images/placeholder-chart.png");
            model.addAttribute("chartMesas", "/images/placeholder-chart.png");
        }

        model.addAttribute("activeSection", "dashboard");
        return "admin";
    }

    private void crearPlaceholderSpring(String basePath) throws IOException {
        File placeholderFile = new File(basePath + "/placeholder-chart.png");
        if (!placeholderFile.exists()) {
            System.out.println("🔄 Creando placeholder...");
            
            PieChart chart = new PieChartBuilder()
                    .width(600)
                    .height(400)
                    .title("Datos no disponibles")
                    .build();

            chart.addSeries("Cargando...", 1);
            BitmapEncoder.saveBitmap(chart, basePath + "/placeholder-chart", BitmapFormat.PNG);
            System.out.println("✅ Placeholder creado: " + placeholderFile.getAbsolutePath());
        }
    }

    private boolean generarTodasLasGraficasSpring(String basePath) {
        try {
            // 1. Productos por categoría
            Map<String, Long> datosCategorias = menuService.contarProductosPorCategoria();
            if (datosCategorias == null || datosCategorias.isEmpty()) {
                datosCategorias = new HashMap<>();
                datosCategorias.put("Bebidas", 15L);
                datosCategorias.put("Platos", 12L);
                datosCategorias.put("Postres", 8L);
            }
            generarGraficoProductosPorCategoria(datosCategorias, basePath);

            // 2. Eventos por mes
            List<Integer> eventosMes = eventoService.obtenerEventosPorMes();
            generarGraficoEventosPorMes(eventosMes, basePath);

            // 3. Usuarios por rol
            generarGraficoUsuariosPorRol(basePath);

            // 4. Estado de mesas
            generarGraficoEstadoMesas(basePath);

            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error generando gráficas: " + e.getMessage());
            return false;
        }
    }

    private void generarGraficoProductosPorCategoria(Map<String, Long> datos, String basePath) throws IOException {
        PieChart chart = new PieChartBuilder()
                .width(600)
                .height(400)
                .title("Productos por Categoría")
                .build();

        datos.forEach(chart::addSeries);
        BitmapEncoder.saveBitmap(chart, basePath + "/chart-categorias", BitmapFormat.PNG);
    }

    private void generarGraficoEventosPorMes(List<Integer> eventosPorMes, String basePath) throws IOException {
        CategoryChart chart = new CategoryChartBuilder()
                .width(700)
                .height(400)
                .title("Eventos por Mes")
                .xAxisTitle("Mes")
                .yAxisTitle("Cantidad")
                .build();

        List<String> meses = Arrays.asList("Ene", "Feb", "Mar", "Abr", "May", "Jun",
                                         "Jul", "Ago", "Sep", "Oct", "Nov", "Dic");

        if (eventosPorMes == null || eventosPorMes.isEmpty() || eventosPorMes.size() != 12) {
            eventosPorMes = Arrays.asList(2, 3, 5, 4, 6, 8, 7, 6, 5, 4, 3, 2);
        }

        chart.addSeries("Eventos", meses, eventosPorMes);
        BitmapEncoder.saveBitmap(chart, basePath + "/chart-eventos", BitmapFormat.PNG);
    }

    private void generarGraficoUsuariosPorRol(String basePath) throws IOException {
        PieChart chart = new PieChartBuilder()
                .width(600)
                .height(400)
                .title("Usuarios por Rol")
                .build();

        try {
            long adminCount = usuarioService.countByRol("ADMIN");
            long meseroCount = usuarioService.countByRol("MESERO"); 
            long cajeroCount = usuarioService.countByRol("CAJERO");
            
            chart.addSeries("Administradores", adminCount);
            chart.addSeries("Meseros", meseroCount);
            chart.addSeries("Cajeros", cajeroCount);
            
        } catch (Exception e) {
            chart.addSeries("Administradores", 3);
            chart.addSeries("Meseros", 8);
            chart.addSeries("Cajeros", 4);
        }

        BitmapEncoder.saveBitmap(chart, basePath + "/chart-usuarios", BitmapFormat.PNG);
    }

    private void generarGraficoEstadoMesas(String basePath) throws IOException {
        PieChart chart = new PieChartBuilder()
                .width(600)
                .height(400)
                .title("Estado de Mesas")
                .build();

        try {
            long disponibles = mesaService.findAll().stream()
                    .filter(m -> m.getEstado() != null && "DISPONIBLE".equalsIgnoreCase(m.getEstado()))
                    .count();
            long ocupadas = mesaService.findAll().stream()
                    .filter(m -> m.getEstado() != null && "OCUPADA".equalsIgnoreCase(m.getEstado()))
                    .count();

            chart.addSeries("Disponibles", disponibles);
            chart.addSeries("Ocupadas", ocupadas);
            
        } catch (Exception e) {
            chart.addSeries("Disponibles", 12);
            chart.addSeries("Ocupadas", 8);
        }

        BitmapEncoder.saveBitmap(chart, basePath + "/chart-mesas", BitmapFormat.PNG);
    }

    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public Map<String, Object> obtenerEstadisticas(jakarta.servlet.http.HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        // Métricas principales
        long ventasHoyVal = facturaService.obtenerVentasHoy();
        double ingresosHoyVal = facturaService.obtenerIngresosHoy();
        Object estadoCaja = session != null ? session.getAttribute("cajaEstado") : null;
        boolean abiertaCaja = false;
        if (estadoCaja instanceof java.util.Map<?, ?> map) {
            Object abiertaVal = map.get("abierta");
            abiertaCaja = (abiertaVal instanceof Boolean) ? (Boolean) abiertaVal : false;
        }
        if (!abiertaCaja) {
            ventasHoyVal = 0;
            ingresosHoyVal = 0.0;
        }
        result.put("ventasHoy", ventasHoyVal);
        result.put("ingresosHoy", ingresosHoyVal);
        result.put("ingresosTotales", facturaService.obtenerIngresosTotales());
        result.put("totalVentas", facturaService.contarFacturas());
        result.put("pedidosPendientes", pedidoService.contarPedidosPendientes());
        result.put("totalProductos", menuService.count());
        result.put("totalCategorias", categoriaService.count());
        result.put("totalEventos", eventoService.count());
        result.put("totalUsuarios", usuarioService.countActiveUsers());
        result.put("totalMesas", mesaService.count());

        // Productos por categoría
        Map<String, Long> catMap = menuService.contarProductosPorCategoria();
        result.put("productosPorCategoria", Map.of(
                "labels", new ArrayList<>(catMap.keySet()),
                "data", new ArrayList<>(catMap.values())
        ));

        // Eventos por mes
        result.put("eventosPorMes", eventoService.obtenerEventosPorMes());

        // Usuarios por rol
        long admin = usuarioService.countByRol("ADMIN");
        long mesero = usuarioService.countByRol("MESERO");
        long cajero = usuarioService.countByRol("CAJERO");
        result.put("usuariosPorRol", Map.of(
                "labels", Arrays.asList("Administradores", "Meseros", "Cajeros"),
                "data", Arrays.asList(admin, mesero, cajero)
        ));

        // Estado de mesas
        long disponibles = mesaService.findAll().stream()
                .filter(m -> m.getEstado() != null && "DISPONIBLE".equalsIgnoreCase(m.getEstado()))
                .count();
        long ocupadas = mesaService.findAll().stream()
                .filter(m -> m.getEstado() != null && "OCUPADA".equalsIgnoreCase(m.getEstado()))
                .count();
        result.put("estadoMesas", Map.of(
                "labels", Arrays.asList("Disponibles", "Ocupadas"),
                "data", Arrays.asList(disponibles, ocupadas)
        ));

        // Ventas del día (detalle)
        List<com.sena.getback.model.Factura> hoy = facturaService.obtenerFacturasHoy();
        List<Map<String, Object>> ventasDelDia = hoy.stream()
                .map(f -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("numero", f.getNumeroFactura());
                    m.put("hora", f.getFechaEmision() != null ? f.getFechaEmision().toString() : null);
                    m.put("cliente", f.getClienteNombre());
                    m.put("mesa", f.getNumeroMesa());
                    m.put("total", f.getTotalPagar());
                    m.put("metodo", f.getMetodoPago());
                    return m;
                })
                .toList();
        result.put("ventasDelDia", ventasDelDia);

        return result;
    }

    @GetMapping("/api/dashboard/ventas-mes")
    @ResponseBody
    public Map<String, Object> obtenerVentasMes(@org.springframework.web.bind.annotation.RequestParam("period") String period) {
        try {
            String[] parts = period.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            return facturaService.obtenerResumenVentasMes(year, month);
        } catch (Exception e) {
            Map<String, Object> res = new HashMap<>();
            res.put("ventasMes", 0);
            res.put("ingresosMes", 0.0);
            return res;
        }
    }
}
