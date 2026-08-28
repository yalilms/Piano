package jd.piano.programa;

import jd.piano.instrumentos.CirculoCromatico;
import jd.piano.instrumentos.InstrumentoVisual;
import jd.piano.instrumentos.Mastil;
import jd.piano.instrumentos.Xilofono;
import jd.piano.teclas.MedidasTeclado;

/**
 * Con que instrumento se puede ver una cancion.
 *
 * Cada uno abarca un rango distinto: el teclado las 88 teclas de un piano, la
 * guitarra 40 notas, el bajo 31 y el xilofono 61. Las que se salen no se
 * pintan, asi que conviene saber cuanto se pierde antes de elegir. El circulo
 * cromatico es el unico que llega a todo, porque todas las octavas de la misma
 * nota caen en el mismo sitio.
 */
public enum TipoInstrumento {

    TECLADO("Teclado", "Las 88 teclas de un piano"),
    GUITARRA("Guitarra", "Seis cuerdas y quince trastes"),
    BAJO("Bajo", "Cuatro cuerdas, una octava mas grave"),
    XILOFONO("Xilofono", "Barras de madera, de mayor a menor"),
    CIRCULO("Circulo cromatico", "Las doce notas en rueda, con sus intervalos");

    /** La0 a Do8: las 88 teclas de un piano de cola. */
    private static final int NOTA_INICIAL = 21;
    private static final int NOTA_FINAL = 108;

    private final String nombre;
    private final String explicacion;

    TipoInstrumento(String nombre, String explicacion) {
        this.nombre = nombre;
        this.explicacion = explicacion;
    }

    public String getNombre() {
        return this.nombre;
    }

    public String getExplicacion() {
        return this.explicacion;
    }

    public InstrumentoVisual crear() {
        return switch (this) {
            case GUITARRA -> Mastil.guitarra();
            case BAJO -> Mastil.bajo();
            case XILOFONO -> new Xilofono();
            case CIRCULO -> new CirculoCromatico();
            default -> new PianoSencillo(NOTA_INICIAL, NOTA_FINAL,
                    MedidasTeclado.NORMAL);
        };
    }

    /** Nota mas grave que representa. */
    public int getNotaMinima() {
        return switch (this) {
            case GUITARRA -> Mastil.AFINACION_GUITARRA[0];
            case BAJO -> Mastil.AFINACION_BAJO[0];
            case XILOFONO -> Xilofono.NOTA_MINIMA;
            // Todas las octavas del mismo Do caen en el mismo nodo, asi que la
            // rueda representa cualquier nota que le echen
            case CIRCULO -> 0;
            default -> NOTA_INICIAL;
        };
    }

    public int getNotaMaxima() {
        return switch (this) {
            case GUITARRA -> Mastil.AFINACION_GUITARRA[
                    Mastil.AFINACION_GUITARRA.length - 1] + 15;
            case BAJO -> Mastil.AFINACION_BAJO[
                    Mastil.AFINACION_BAJO.length - 1] + 15;
            case XILOFONO -> Xilofono.NOTA_MAXIMA;
            case CIRCULO -> 127;
            default -> NOTA_FINAL;
        };
    }

    /**
     * Que porcentaje de las notas de la cancion cabe en este instrumento.
     * Sirve para avisar antes de que alguien vea media cancion en blanco.
     */
    public double coberturaDe(Partitura partitura) {
        return coberturaDe(partitura, 0);
    }

    /**
     * Lo mismo contando la transposicion. Cambia mucho: una cancion que en un
     * mastil de guitarra se ve a medias puede caber entera subida una octava.
     */
    public double coberturaDe(Partitura partitura, int semitonos) {
        long total = 0;
        long caben = 0;
        for (NotaMidi nota : partitura.getNotas()) {
            total++;
            int altura = nota.nota() + semitonos;
            if (altura >= getNotaMinima() && altura <= getNotaMaxima()) {
                caben++;
            }
        }
        return total == 0 ? 100.0 : 100.0 * caben / total;
    }

    @Override
    public String toString() {
        return this.nombre;
    }
}
