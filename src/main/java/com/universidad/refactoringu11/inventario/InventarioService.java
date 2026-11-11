package com.universidad.refactoringu11.inventario;

import com.universidad.refactoringu11.reportes.ReporteBajoStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class InventarioService {

    private static final String ENTRADA = "ENTRADA";
    private static final String SALIDA = "SALIDA";

    private final List<Producto> productos = new ArrayList<>();
    private final List<String> movimientos = new ArrayList<>();
    private final CalculadoraReorden calculadoraReorden = new CalculadoraReorden();
    private ReporteBajoStockService reporteBajoStockService;

    @Autowired(required = false)
    public void setReporteBajoStockService(ReporteBajoStockService reporteBajoStockService) {
        this.reporteBajoStockService = reporteBajoStockService;
    }

    public void agregarProducto(Producto producto) {
        productos.add(producto);
    }

    public List<Producto> productosBajoStockMinimo() {
        return productos.stream()
                .filter(calculadoraReorden::necesitaReorden)
                .toList();
    }

    public String actualizarInventario(Long productoId, int cantidad, String tipoMovimiento) {
        if (datosDeEntradaInvalidos(productoId, cantidad, tipoMovimiento)) {
            return "ERROR_DATOS_INVALIDOS";
        }
        Optional<Producto> encontrado = buscarPorId(productoId);
        if (encontrado.isEmpty()) {
            return "ERROR_PRODUCTO_NO_EXISTE";
        }
        return moverStock(encontrado.get(), cantidad, tipoMovimiento);
    }

    private boolean datosDeEntradaInvalidos(Long productoId, int cantidad, String tipoMovimiento) {
        // tipoMovimiento.equals(...) conserva el comportamiento original ante null
        return productoId == null || cantidad == 0
                || !(tipoMovimiento.equals(ENTRADA) || tipoMovimiento.equals(SALIDA));
    }

    private Optional<Producto> buscarPorId(Long productoId) {
        return productos.stream()
                .filter(p -> p.getId().equals(productoId))
                .findFirst();
    }

    private String moverStock(Producto producto, int cantidad, String tipoMovimiento) {
        boolean esSalida = tipoMovimiento.equals(SALIDA);
        int nuevoStock = esSalida ? producto.getStock() - cantidad : producto.getStock() + cantidad;
        if (esSalida && nuevoStock < 0) {
            return "ERROR_STOCK_INSUFICIENTE";
        }
        producto.setStock(nuevoStock);
        registrarMovimiento(tipoMovimiento, cantidad, producto);
        if (calculadoraReorden.necesitaReorden(producto)) {
            return alertarBajoStock(producto);
        }
        return "OK_" + nuevoStock;
    }

    private void registrarMovimiento(String tipoMovimiento, int cantidad, Producto producto) {
        String registro = tipoMovimiento + " de " + cantidad + " unidades de "
                + producto.getNombre() + " -> stock final: " + producto.getStock();
        movimientos.add(registro);
        System.out.println(registro);
    }

    private String alertarBajoStock(Producto producto) {
        int cantidadReorden = calculadoraReorden.unidadesAReordenar(producto);
        if (reporteBajoStockService != null) {
            System.out.println(reporteBajoStockService.generarReporte());
        }
        System.out.println("ALERTA: " + producto.getNombre()
                + " bajo stock minimo. Reordenar " + cantidadReorden + " unidades.");
        return "OK_BAJO_STOCK_REORDEN_" + cantidadReorden;
    }
}
