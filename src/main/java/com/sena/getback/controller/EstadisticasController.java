package com.sena.getback.controller;

import com.sena.getback.model.Usuario;
import com.sena.getback.repository.UsuarioRepository;
import com.sena.getback.service.PedidoService;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.style.Styler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/estadisticas")
public class EstadisticasController {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String vistaEstadisticas(HttpSession session) {
        return "mesero/estadisticas";
    }

    @GetMapping("/datos-meseros")
    @ResponseBody
    public Map<String, Object> datosMeseros(HttpSession session) {
        Integer usuarioId = null;
        Object u = session != null ? session.getAttribute("usuarioLogueado") : null;
        if (u instanceof Usuario usr && usr.getId() != null && usr.getRol() != null && usr.getRol().getNombre() != null) {
            boolean esAdmin = "ADMIN".equalsIgnoreCase(usr.getRol().getNombre());
            if (!esAdmin) {
                usuarioId = usr.getId().intValue();
            }
        }

        Map<String, Long> estadisticas = pedidoService.obtenerEstadisticasPorMesero(usuarioId);
        return Map.of(
                "labels", List.copyOf(estadisticas.keySet()),
                "data", List.copyOf(estadisticas.values())
        );
    }

    @GetMapping(value = "/grafico-meseros", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> generarGraficoMeseros(HttpSession session) {
        Integer usuarioId = null;
        Object u = session != null ? session.getAttribute("usuarioLogueado") : null;
        if (u instanceof Usuario usr && usr.getId() != null && usr.getRol() != null && usr.getRol().getNombre() != null) {
            boolean esAdmin = "ADMIN".equalsIgnoreCase(usr.getRol().getNombre());
            if (!esAdmin) {
                usuarioId = usr.getId().intValue();
            }
        }
        
        // Obtener estadísticas de pedidos por mesero
        Map<String, Long> estadisticas = pedidoService.obtenerEstadisticasPorMesero(usuarioId);
        
        // Crear el gráfico
        CategoryChart chart = new CategoryChartBuilder()
                .width(800)
                .height(420)
                .title("Pedidos por mesero")
                .xAxisTitle("Mesero")
                .yAxisTitle("Número de pedidos")
                .build();

        // Personalizar el gráfico para que se vea integrado con el tema oscuro
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setPlotGridLinesVisible(false);
        chart.getStyler().setXAxisLabelRotation(45);
        chart.getStyler().setPlotBorderVisible(false);
        chart.getStyler().setChartBackgroundColor(new Color(17, 17, 17));
        chart.getStyler().setPlotBackgroundColor(new Color(17, 17, 17));
        chart.getStyler().setChartFontColor(Color.WHITE);
        chart.getStyler().setAxisTickLabelsColor(Color.WHITE);
        chart.getStyler().setChartTitleBoxBackgroundColor(new Color(17, 17, 17));
        chart.getStyler().setChartTitleBoxVisible(false);
        chart.getStyler().setPlotContentSize(0.9);

        // Colores de barras inspirados en tu paleta morado / turquesa
        Color[] sliceColors = new Color[]{
                new Color(0, 198, 255),      // celeste
                new Color(123, 47, 247),     // morado
                new Color(0, 230, 180),      // verde agua
                new Color(255, 179, 0),      // naranja
                new Color(255, 99, 132)      // rojo suave
        };
        chart.getStyler().setSeriesColors(sliceColors);

        // Agregar datos al gráfico
        List<String> meseros = List.copyOf(estadisticas.keySet());
        List<Long> pedidos = List.copyOf(estadisticas.values());
        
        chart.addSeries("Pedidos", meseros, pedidos);

        // Convertir el gráfico a imagen
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            BitmapEncoder.saveBitmap(chart, os, BitmapEncoder.BitmapFormat.PNG);
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(os.toByteArray());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
