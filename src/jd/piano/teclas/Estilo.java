package jd.piano.teclas;

import java.awt.Color;

/**
 * Paleta y medidas del acabado visual: madera y marfil.
 * Centralizado aqui para que cambiar el aspecto sea tocar un sitio.
 */
public final class Estilo {

    // Teclas blancas: marfil calido, mas claro arriba que abajo
    public static final Color MARFIL_ALTO = new Color(252, 249, 240);
    public static final Color MARFIL_BAJO = new Color(222, 214, 195);
    /** Canto frontal de la tecla, el que mira al que toca. */
    public static final Color MARFIL_CANTO = new Color(255, 255, 252);

    // Teclas negras: grafito, no negro puro
    public static final Color GRAFITO_ALTO = new Color(74, 70, 64);
    public static final Color GRAFITO_BAJO = new Color(20, 18, 14);
    public static final Color BRILLO_NEGRA = new Color(255, 255, 255, 70);

    /** Separacion entre teclas: gris calido, nunca negro puro. */
    public static final Color SEPARADOR = new Color(128, 116, 96);
    public static final Color CONTORNO_NEGRA = new Color(12, 10, 8);

    /** Fieltro sobre el teclado, como en un piano de verdad. */
    public static final Color FIELTRO_ALTO = new Color(122, 31, 43);
    public static final Color FIELTRO_BAJO = new Color(74, 16, 24);
    public static final int FIELTRO_ALTURA = 6;

    public static final Color SOMBRA = new Color(0, 0, 0, 70);

    /** Cuanto se hunde la tecla al pulsarla, en pixeles. */
    public static final int HUNDIMIENTO = 2;

    /** Duracion del desvanecido al soltar, en milisegundos. */
    public static final long FUNDIDO_MS = 260;

    private Estilo() {
    }

    /** Interpola entre dos colores. t=0 devuelve a, t=1 devuelve b. */
    public static Color mezclar(Color a, Color b, double t) {
        double f = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * f),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * f),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * f));
    }

    public static Color conAlfa(Color c, int alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
                Math.max(0, Math.min(255, alfa)));
    }
}
