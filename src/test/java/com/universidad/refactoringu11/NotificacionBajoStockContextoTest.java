package com.universidad.refactoringu11;

import com.universidad.refactoringu11.inventario.InventarioService;
import com.universidad.refactoringu11.inventario.Producto;
import com.universidad.refactoringu11.inventario.TipoProducto;
import com.universidad.refactoringu11.reportes.ReporteBajoStockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NotificacionBajoStockContextoTest {

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private ReporteBajoStockService reporteBajoStockService;

    @Test
    void salidaBajoElMinimo_conElContextoDeSpring_llegaAlReporte() {
        inventarioService.agregarProducto(
                new Producto(50L, "Monitor 24", TipoProducto.ELECTRONICO, 700.0, 6, 4));

        String resultado = inventarioService.actualizarInventario(50L, 3, "SALIDA");

        assertEquals("OK_BAJO_STOCK_REORDEN_9", resultado);
        assertTrue(reporteBajoStockService.generarReporte()
                .contains("- Monitor 24: stock 3 de minimo 4, reordenar 9"));
    }
}
