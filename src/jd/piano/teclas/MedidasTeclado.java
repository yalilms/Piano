package jd.piano.teclas;

/**
 * Tamano de las teclas. Antes eran constantes fijas en TeclaBlanca y
 * TeclaNegra, lo que impedia dibujar un teclado mas pequeno: por eso el
 * MultiPiano tenia que recortar el rango de notas y se perdia un cuarto de
 * las notas de la cancion.
 *
 * Todas las medidas salen de un factor de escala sobre las de un piano
 * normal, y se redondean a enteros una sola vez aqui.
 */
public final class MedidasTeclado {

    public static final int ANCHURA_BLANCA_BASE = 25;
    public static final int ALTURA_BLANCA_BASE = 100;
    public static final int ANCHURA_NEGRA_BASE = 15;
    public static final int ALTURA_NEGRA_BASE = 50;

    /** Por debajo de esto las teclas dejan de distinguirse. */
    private static final int ANCHURA_BLANCA_MINIMA = 4;

    /**
     * Holgura para que las escalas exactas no se caigan al entero de abajo.
     * 25 * 0,6 en coma flotante da 14,999999999999998, y truncar eso daria 14.
     */
    private static final double PELIN = 1e-9;

    public static final MedidasTeclado NORMAL = new MedidasTeclado(1.0);

    private final double escala;
    private final int anchuraBlanca;
    private final int alturaBlanca;
    private final int anchuraNegra;
    private final int alturaNegra;

    public MedidasTeclado(double escala) {
        // Truncado y no redondeado, a proposito. Quien pide la escala la ha
        // calculado para que el teclado quepa justo en el hueco; si el ancho de
        // tecla se redondea hacia arriba, cada tecla se pasa hasta un pixel y
        // con 52 blancas el teclado acaba 52 px mas ancho de lo que cabe. Se
        // centraba, asi que las teclas de los extremos salian cortadas.
        this.anchuraBlanca = Math.max(ANCHURA_BLANCA_MINIMA,
                (int) Math.floor(ANCHURA_BLANCA_BASE * escala + PELIN));
        // La escala efectiva se recalcula desde el ancho ya redondeado, para que
        // el resto de medidas sean coherentes con lo que de verdad se dibuja.
        this.escala = (double) this.anchuraBlanca / ANCHURA_BLANCA_BASE;
        this.alturaBlanca = (int) Math.round(ALTURA_BLANCA_BASE * this.escala);
        this.anchuraNegra = Math.max(3, (int) Math.round(ANCHURA_NEGRA_BASE * this.escala));
        this.alturaNegra = (int) Math.round(ALTURA_NEGRA_BASE * this.escala);
    }

    public double getEscala() {
        return this.escala;
    }

    public int getAnchuraBlanca() {
        return this.anchuraBlanca;
    }

    public int getAlturaBlanca() {
        return this.alturaBlanca;
    }

    public int getAnchuraNegra() {
        return this.anchuraNegra;
    }

    public int getAlturaNegra() {
        return this.alturaNegra;
    }

    /**
     * Cuanto se desplaza la negra a la izquierda de la blanca que le sigue.
     *
     * Este valor y las dos invasiones de abajo son la razon de que las teclas
     * encajen sin costuras: la negra se corre getDesplazamientoNegra() px, asi
     * que sobresale justo eso por la derecha de la blanca anterior y el resto
     * por la izquierda de la siguiente. Mientras los tres salgan de la misma
     * division entera, la suma es exacta aunque la anchura sea impar.
     */
    public int getDesplazamientoNegra() {
        return this.anchuraNegra / 2;
    }

    /** Lo que invade la negra de la izquierda sobre esta blanca. */
    public int getInvasionIzquierda() {
        return this.anchuraNegra - this.anchuraNegra / 2;
    }

    /** Lo que invade la negra de la derecha sobre esta blanca. */
    public int getInvasionDerecha() {
        return this.anchuraNegra / 2;
    }

    /** Radio de las esquinas de abajo de las teclas. */
    public double getRadioEsquina() {
        return Math.max(1, 3 * this.escala);
    }

    /** Cuanto baja la tecla al pulsarla. */
    public int getHundimiento() {
        return Math.max(1, (int) Math.round(2 * this.escala));
    }

    /** Alto de la franja de fieltro sobre el teclado. */
    public int getAlturaFieltro() {
        return Math.max(2, (int) Math.round(6 * this.escala));
    }

    /** Anchura que ocupa un teclado con ese numero de teclas blancas. */
    public int anchuraDe(int teclasBlancas) {
        return teclasBlancas * this.anchuraBlanca;
    }
}
