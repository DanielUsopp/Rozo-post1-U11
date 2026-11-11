package com.universidad.refactoringu11.inventario;

/**
 * Política de reabastecimiento: cuándo un producto debe reordenarse y cuántas
 * unidades pedir. Hoy se pide hasta cubrir el triple del stock mínimo.
 */
public class CalculadoraReorden {

    private static final int MULTIPLO_STOCK_MINIMO = 3;

    public boolean necesitaReorden(Producto producto) {
        return producto.getStock() < producto.getStockMinimo();
    }

    public int unidadesAReordenar(Producto producto) {
        if (!necesitaReorden(producto)) {
            return 0;
        }
        return producto.getStockMinimo() * MULTIPLO_STOCK_MINIMO - producto.getStock();
    }
}
