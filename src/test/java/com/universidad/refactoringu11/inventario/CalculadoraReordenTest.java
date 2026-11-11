package com.universidad.refactoringu11.inventario;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculadoraReordenTest {

    private final CalculadoraReorden calculadora = new CalculadoraReorden();

    @Test
    void stockMenorAlMinimo_necesitaReordenHastaElTripleDelMinimo() {
        Producto p = new Producto(1L, "Mouse", TipoProducto.ELECTRONICO, 30.0, 2, 4);
        assertTrue(calculadora.necesitaReorden(p));
        assertEquals(10, calculadora.unidadesAReordenar(p));   // 4*3 - 2
    }

    @Test
    void stockIgualAlMinimo_noNecesitaReorden() {
        Producto p = new Producto(2L, "Cuaderno", TipoProducto.LIBRO, 8.0, 4, 4);
        assertFalse(calculadora.necesitaReorden(p));
        assertEquals(0, calculadora.unidadesAReordenar(p));
    }
}
