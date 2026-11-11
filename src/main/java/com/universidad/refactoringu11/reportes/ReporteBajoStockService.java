package com.universidad.refactoringu11.reportes;

import com.universidad.refactoringu11.inventario.InventarioService;
import com.universidad.refactoringu11.inventario.NotificadorBajoStock;
import com.universidad.refactoringu11.inventario.Producto;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReporteBajoStockService implements NotificadorBajoStock {

    private final InventarioService inventarioService;
    private final Map<Long, Integer> reordenPorProducto = new LinkedHashMap<>();

    public ReporteBajoStockService(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @Override
    public void notificar(Producto producto, int cantidadReorden) {
        reordenPorProducto.put(producto.getId(), cantidadReorden);
    }

    public String generarReporte() {
        List<Producto> bajoMinimo = inventarioService.productosBajoStockMinimo();
        if (bajoMinimo.isEmpty()) {
            return "Reporte de bajo stock: sin productos bajo el minimo";
        }
        return bajoMinimo.stream()
                .map(p -> "- " + p.getNombre() + ": stock " + p.getStock()
                        + " de minimo " + p.getStockMinimo()
                        + ", reordenar " + reordenPorProducto.getOrDefault(p.getId(), 0))
                .collect(Collectors.joining("\n", "Reporte de bajo stock:\n", ""));
    }
}
