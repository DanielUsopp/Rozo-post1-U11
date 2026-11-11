package com.universidad.refactoringu11.reportes;

import com.universidad.refactoringu11.inventario.InventarioService;
import com.universidad.refactoringu11.inventario.Producto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReporteBajoStockService {

    private final InventarioService inventarioService;

    public ReporteBajoStockService(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    public String generarReporte() {
        List<Producto> bajoMinimo = inventarioService.productosBajoStockMinimo();
        if (bajoMinimo.isEmpty()) {
            return "Reporte de bajo stock: sin productos bajo el minimo";
        }
        return bajoMinimo.stream()
                .map(p -> "- " + p.getNombre() + ": stock " + p.getStock()
                        + " de minimo " + p.getStockMinimo())
                .collect(Collectors.joining("\n", "Reporte de bajo stock:\n", ""));
    }
}
