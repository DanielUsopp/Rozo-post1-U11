package com.universidad.refactoringu11.reportes;

import com.universidad.refactoringu11.inventario.NotificadorBajoStock;
import com.universidad.refactoringu11.inventario.Producto;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Arma el reporte con lo que recibe en cada notificación. No vuelve a
 * consultar al inventario: cada aviso ya trae el producto y la cantidad a
 * reordenar, y consultar de vuelta era lo que formaba el ciclo entre beans.
 */
@Service
public class ReporteBajoStockService implements NotificadorBajoStock {

    // Ultima alerta por producto, en el orden en que llegaron
    private final Map<Long, AlertaReorden> alertas = new LinkedHashMap<>();

    @Override
    public void notificar(Producto producto, int cantidadReorden) {
        alertas.put(producto.getId(), new AlertaReorden(producto.getNombre(),
                producto.getStock(), producto.getStockMinimo(), cantidadReorden));
    }

    public String generarReporte() {
        if (alertas.isEmpty()) {
            return "Reporte de bajo stock: sin productos bajo el minimo";
        }
        return alertas.values().stream()
                .map(AlertaReorden::comoLinea)
                .collect(Collectors.joining("\n", "Reporte de bajo stock:\n", ""));
    }
}
