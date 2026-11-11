package com.universidad.refactoringu11.inventario;

/**
 * Lo que el inventario necesita de quien quiera enterarse de un bajo stock.
 * Vive en inventario (el paquete de mayor nivel); quien la implementa depende
 * de inventario, nunca al revés.
 */
public interface NotificadorBajoStock {
    void notificar(Producto producto, int cantidadReorden);
}
