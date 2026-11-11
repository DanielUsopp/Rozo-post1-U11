package com.universidad.refactoringu11.reportes;

import com.universidad.refactoringu11.inventario.Producto;
import com.universidad.refactoringu11.inventario.TipoProducto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReporteBajoStockServiceTest {

    private final ReporteBajoStockService reporte = new ReporteBajoStockService();

    @Test
    void generarReporte_sinNotificaciones_indicaQueNoHayProductos() {
        assertEquals("Reporte de bajo stock: sin productos bajo el minimo", reporte.generarReporte());
    }

    @Test
    void generarReporte_dosAvisosDelMismoProducto_conservaSoloElUltimo() {
        Producto mouse = new Producto(7L, "Mouse", TipoProducto.ELECTRONICO, 30.0, 3, 4);
        reporte.notificar(mouse, 9);
        mouse.setStock(1);
        reporte.notificar(mouse, 11);

        assertEquals("Reporte de bajo stock:\n- Mouse: stock 1 de minimo 4, reordenar 11",
                reporte.generarReporte());
    }
}
