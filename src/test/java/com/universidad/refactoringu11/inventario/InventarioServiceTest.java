package com.universidad.refactoringu11.inventario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventarioServiceTest {

    private InventarioService service;
    private Producto libro;

    @BeforeEach
    void setUp() {
        service = new InventarioService();
        libro = new Producto(1L, "Clean Code", TipoProducto.LIBRO, 45.0, 20, 5);
        service.agregarProducto(libro);
    }

    @Test
    void entrada_incrementaStockYRetornaOk() {
        assertEquals("OK_30", service.actualizarInventario(1L, 10, "ENTRADA"));
        assertEquals(30, libro.getStock());
    }

    @Test
    void salida_conStockSuficiente_descuentaYRetornaOk() {
        assertEquals("OK_12", service.actualizarInventario(1L, 8, "SALIDA"));
    }

    @Test
    void salida_conStockInsuficiente_retornaErrorYNoModificaStock() {
        assertEquals("ERROR_STOCK_INSUFICIENTE", service.actualizarInventario(1L, 50, "SALIDA"));
        assertEquals(20, libro.getStock());
    }

    @Test
    void salida_dejaStockBajoElMinimo_activaReordenConCantidadCalculada() {
        // stockMinimo=5, stock inicial=20, salida de 17 -> nuevoStock=3 (<5)
        // cantidadReorden = stockMinimo*3 - nuevoStock = 15 - 3 = 12
        assertEquals("OK_BAJO_STOCK_REORDEN_12", service.actualizarInventario(1L, 17, "SALIDA"));
        assertEquals(3, libro.getStock());
    }

    @Test
    void tipoMovimientoInvalido_retornaError() {
        assertEquals("ERROR_DATOS_INVALIDOS", service.actualizarInventario(1L, 5, "TRANSFERENCIA"));
    }

    @Test
    void productoInexistente_retornaError() {
        assertEquals("ERROR_PRODUCTO_NO_EXISTE", service.actualizarInventario(99L, 5, "ENTRADA"));
    }

    // --- Casos limite agregados para fijar el comportamiento actual ---

    @Test
    void salida_dejaStockExactamenteEnElMinimo_noActivaReorden() {
        // 20 - 15 = 5: la comparacion es "menor que", no "menor o igual"
        assertEquals("OK_5", service.actualizarInventario(1L, 15, "SALIDA"));
    }

    @Test
    void salida_dejaStockEnCero_reordenaElTripleDelMinimo() {
        assertEquals("OK_BAJO_STOCK_REORDEN_15", service.actualizarInventario(1L, 20, "SALIDA"));
        assertEquals(0, libro.getStock());
    }

    @Test
    void entrada_queNoAlcanzaElMinimo_tambienActivaReorden() {
        Producto cafe = new Producto(2L, "Cafe molido", TipoProducto.ALIMENTO, 15.0, 1, 5);
        service.agregarProducto(cafe);
        assertEquals("OK_BAJO_STOCK_REORDEN_12", service.actualizarInventario(2L, 2, "ENTRADA"));
        assertEquals(3, cafe.getStock());
    }

    @Test
    void cantidadCero_retornaErrorDatosInvalidos() {
        assertEquals("ERROR_DATOS_INVALIDOS", service.actualizarInventario(1L, 0, "ENTRADA"));
        assertEquals(20, libro.getStock());
    }

    @Test
    void productoIdNulo_retornaErrorDatosInvalidos() {
        assertEquals("ERROR_DATOS_INVALIDOS", service.actualizarInventario(null, 5, "ENTRADA"));
    }

    @Test
    void tipoMovimientoNulo_lanzaNullPointerException() {
        // Comportamiento actual (no deseable, pero real): no se valida null
        assertThrows(NullPointerException.class,
            () -> service.actualizarInventario(1L, 5, null));
    }

    @Test
    void entrada_conCantidadNegativa_noSeValidaYPuedeDejarStockNegativo() {
        // Comportamiento actual: solo SALIDA revisa stock negativo.
        // 20 + (-30) = -10 -> reorden = 5*3 - (-10) = 25
        assertEquals("OK_BAJO_STOCK_REORDEN_25", service.actualizarInventario(1L, -30, "ENTRADA"));
        assertEquals(-10, libro.getStock());
    }
}
