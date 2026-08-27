package jd.piano.programa;

import jd.piano.instrumentos.InstrumentoVisual;
import jd.piano.instrumentos.Mastil;
import jd.piano.teclas.MedidasTeclado;

/**
 * Con que instrumento se puede ver una cancion.
 *
 * Cada uno abarca un rango distinto: el teclado llega a las 85 teclas, la
 * guitarra a 40 notas y el bajo a 31. Las que se salen no se pintan, asi que
 * conviene saber cuanto se pierde antes de elegir.
 */
public enum TipoInstrumento {

    TECLADO("Teclado", "Las 85 teclas de un piano"),
    GUITARRA("Guitarra", "Seis cuerdas y quince trastes"),
    BAJO("Bajo", "Cuatro cuerdas, una octava mas grave");

    private static final int NOTA_INICIAL = 24;
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
            default -> new PianoSencillo(NOTA_INICIAL, NOTA_FINAL,
                    MedidasTeclado.NORMAL);
        };
    }

    /** Nota mas grave que representa. */
    public int getNotaMinima() {
        return switch (this) {
            case GUITARRA -> Mastil.AFINACION_GUITARRA[0];
            case BAJO -> Mastil.AFINACION_BAJO[0];
            default -> NOTA_INICIAL;
        };
    }

    public int getNotaMaxima() {
        return switch (this) {
            case GUITARRA -> Mastil.AFINACION_GUITARRA[
                    Mastil.AFINACION_GUITARRA.length - 1] + 15;
            case BAJO -> Mastil.AFINACION_BAJO[
                    Mastil.AFINACION_BAJO.length - 1] + 15;
            default -> NOTA_FINAL;
        };
    }

    /**
     * Que porcentaje de las notas de la cancion cabe en este instrumento.
     * Sirve para avisar antes de que alguien vea media cancion en blanco.
     */
    public double coberturaDe(Partitura partitura) {
        long total = 0;
        long caben = 0;
        for (NotaMidi nota : partitura.getNotas()) {
            total++;
            if (nota.nota() >= getNotaMinima() && nota.nota() <= getNotaMaxima()) {
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
