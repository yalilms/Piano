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

    /**
     * Un color por canal MIDI. Nada de amarillo puro, que se pierde sobre
     * marfil.
     *
     * No van en orden de tono a proposito: recorrer la rueda de color dejaba
     * los canales 0 a 4 todos calidos y se confundian entre si. Como casi
     * ninguna cancion pasa de nueve canales, los primeros indices son los que
     * importan y aqui van alternando frio y calido para separarse al maximo.
     */
    private static final Color[] COLORES_CANAL = {
            new Color(232, 69, 60),    new Color(46, 143, 212),
            new Color(53, 168, 91),    new Color(201, 75, 201),
            new Color(240, 168, 30),   new Color(23, 165, 151),
            new Color(123, 79, 209),   new Color(242, 118, 43),
            new Color(224, 71, 155),   new Color(127, 191, 63),
            new Color(63, 95, 207),    new Color(181, 118, 106),
            new Color(143, 163, 60),   new Color(111, 143, 168),
            new Color(201, 193, 31),   new Color(154, 154, 154)
    };

    public static Color colorDeCanal(int canal) {
        return COLORES_CANAL[((canal % COLORES_CANAL.length) + COLORES_CANAL.length)
                % COLORES_CANAL.length];
    }

    /** Nota MIDI del Do central. */
    public static final int DO_CENTRAL = 60;

    private static final String[] NOMBRES_NOTA =
            {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    /** Se pueden apagar desde la ventana; a escalas pequenas no caben. */
    private static boolean mostrarNombres = true;

    public static void setMostrarNombres(boolean mostrar) {
        mostrarNombres = mostrar;
    }

    public static boolean isMostrarNombres() {
        return mostrarNombres;
    }

    /** Nombre de la nota sin la octava: C, C#, D... */
    public static String nombreDeNota(int nota) {
        return NOMBRES_NOTA[((nota % 12) + 12) % 12];
    }

    /** Octava en la convencion cientifica, donde el Do central es C4. */
    public static int octavaDe(int nota) {
        return nota / 12 - 1;
    }

    /**
     * En los Do se pone tambien el numero de octava: son la referencia con la
     * que uno se orienta en un teclado largo.
     */
    public static String etiquetaDe(int nota) {
        String nombre = nombreDeNota(nota);
        if (nombre.equals("C")) {
            nombre = nombre + octavaDe(nota);
        }
        return nombre;
    }

    public static final Color TEXTO_TECLA = new Color(120, 108, 92);
    public static final Color MARCA_DO_CENTRAL = new Color(196, 84, 52);

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
