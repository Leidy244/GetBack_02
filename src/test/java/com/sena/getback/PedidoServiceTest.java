package com.sena.getback;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

public class PedidoServiceTest {

    public static void main(String[] args) throws Exception {
        String itemsJson = "{\"items\":[{\"productoId\":1,\"productoNombre\":\"Cerveza\",\"cantidad\":2,\"precio\":5000,\"subtotal\":10000,\"comentarios\":\"\",\"tipo\":\"BAR\"}],\"total\":10000}";
        
        System.out.println("Testing with JSON: " + itemsJson);
        
        Map<String, Integer> requeridos = extraerCantidadesPorProducto(itemsJson);
        System.out.println("Requeridos: " + requeridos);
        
        if (requeridos.containsKey("Cerveza") && requeridos.get("Cerveza") == 2) {
            System.out.println("SUCCESS: Extracted correctly");
        } else {
            System.out.println("FAILURE: Failed to extract");
        }
    }

    private static Map<String, Integer> extraerCantidadesPorProducto(String itemsJson) throws Exception {
        Map<String, Integer> requeridos = new HashMap<>();
        if (itemsJson == null || itemsJson.isBlank()) {
            return requeridos;
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> root = mapper.readValue(itemsJson, Map.class);
        Object itemsObj = root.get("items");
        if (!(itemsObj instanceof List<?> itemsList)) {
            return requeridos;
        }

        for (Object o : itemsList) {
            if (!(o instanceof Map<?, ?> itemMap)) continue;
            Object nombreObj = itemMap.get("productoNombre");
            Object cantidadObj = itemMap.get("cantidad");
            if (nombreObj == null || cantidadObj == null) continue;
            String nombre = String.valueOf(nombreObj);
            int cant = (cantidadObj instanceof Number)
                    ? ((Number) cantidadObj).intValue()
                    : Integer.parseInt(String.valueOf(cantidadObj));
            if (cant <= 0) continue;
            requeridos.merge(nombre, cant, Integer::sum);
        }

        return requeridos;
    }
}
