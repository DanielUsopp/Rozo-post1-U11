package com.universidad.refactoringu11.inventario;

/**
 * Tipos de producto con lo que varía entre ellos. La etiqueta solo cambia en
 * sus textos, así que se guarda como datos de cada constante; el recargo de
 * envío cambia de fórmula (porcentaje del precio o monto fijo), así que cada
 * constante implementa su propio cálculo.
 */
public enum TipoProducto {

    LIBRO("LIBRO", "Exento de IVA") {
        @Override
        public double recargoEnvio(double precio) {
            return 0.0;
        }
    },
    ELECTRONICO("ELECTRO", "Garantia 1 ano") {
        @Override
        public double recargoEnvio(double precio) {
            return precio * 0.08;
        }
    },
    ROPA("ROPA", "Ver talla") {
        @Override
        public double recargoEnvio(double precio) {
            return precio * 0.03;
        }
    },
    ALIMENTO("ALIMENTO", "Perecedero") {
        @Override
        public double recargoEnvio(double precio) {
            return 2.50;
        }
    };

    private final String prefijoEtiqueta;
    private final String leyendaEtiqueta;

    TipoProducto(String prefijoEtiqueta, String leyendaEtiqueta) {
        this.prefijoEtiqueta = prefijoEtiqueta;
        this.leyendaEtiqueta = leyendaEtiqueta;
    }

    public abstract double recargoEnvio(double precio);

    public String etiquetaPara(String nombreProducto) {
        return "[" + prefijoEtiqueta + "] " + nombreProducto + " - " + leyendaEtiqueta;
    }
}
