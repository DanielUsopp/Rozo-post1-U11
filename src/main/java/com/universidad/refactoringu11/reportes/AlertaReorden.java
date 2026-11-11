package com.universidad.refactoringu11.reportes;

/** Lo que llegó en una notificación de bajo stock, tal como estaba en ese momento. */
record AlertaReorden(String producto, int stock, int stockMinimo, int cantidadReorden) {

    String comoLinea() {
        return "- " + producto + ": stock " + stock + " de minimo " + stockMinimo
                + ", reordenar " + cantidadReorden;
    }
}
