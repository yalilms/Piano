package jd.piano.teclas;

/**
 * Reparte varios teclados en una rejilla, eligiendo cuantas columnas y a que
 * escala para aprovechar todo el espacio disponible.
 *
 * Antes la rejilla era fija de 4x4 con teclas de tamano fijo, y para que
 * cupiera habia que recortar el rango de notas a dos octavas: se perdia casi
 * un cuarto de las notas de la cancion. Ahora se adapta.
 */
public final class LayoutTeclados {

    /** Separacion entre teclados, en unidades de escala 1. */
    private static final int ESPACIO_X_BASE = 10;
    private static final int ESPACIO_Y_BASE = 20;

    /** No se agrandan las teclas mas alla de un piano normal. */
    private static final double ESCALA_MAXIMA = 1.0;

    private final int filas;
    private final int columnas;
    private final MedidasTeclado medidas;
    private final int espacioX;
    private final int espacioY;

    private LayoutTeclados(int filas, int columnas, double escala) {
        this.filas = filas;
        this.columnas = columnas;
        this.medidas = new MedidasTeclado(escala);
        double efectiva = this.medidas.getEscala();
        this.espacioX = Math.max(2, (int) Math.round(ESPACIO_X_BASE * efectiva));
        this.espacioY = Math.max(4, (int) Math.round(ESPACIO_Y_BASE * efectiva));
    }

    /**
     * Prueba todos los repartos en columnas y se queda con el que permite las
     * teclas mas grandes.
     */
    public static LayoutTeclados calcular(int cuantosTeclados, int blancasPorTeclado,
                                          int anchoDisponible, int altoDisponible) {
        int mejorColumnas = 1;
        double mejorEscala = 0;

        for (int columnas = 1; columnas <= cuantosTeclados; columnas++) {
            int filas = (cuantosTeclados + columnas - 1) / columnas;

            // Ancho y alto que ocuparia la rejilla a escala 1
            double anchoBase = columnas * blancasPorTeclado * MedidasTeclado.ANCHURA_BLANCA_BASE
                    + (columnas - 1) * ESPACIO_X_BASE;
            // Cada teclado carga con su franja de fieltro encima
            double altoBase = filas * (MedidasTeclado.ALTURA_BLANCA_BASE + 6)
                    + (filas - 1) * ESPACIO_Y_BASE;

            double escala = Math.min(anchoDisponible / anchoBase,
                    altoDisponible / altoBase);
            if (escala > mejorEscala) {
                mejorEscala = escala;
                mejorColumnas = columnas;
            }
        }

        int filas = (cuantosTeclados + mejorColumnas - 1) / mejorColumnas;
        return new LayoutTeclados(filas, mejorColumnas,
                Math.min(ESCALA_MAXIMA, mejorEscala));
    }

    public int getFilas() {
        return this.filas;
    }

    public int getColumnas() {
        return this.columnas;
    }

    public MedidasTeclado getMedidas() {
        return this.medidas;
    }

    public int getEspacioX() {
        return this.espacioX;
    }

    public int getEspacioY() {
        return this.espacioY;
    }
}
