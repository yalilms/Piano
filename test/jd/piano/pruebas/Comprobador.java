package jd.piano.pruebas;

/**
 * Lo minimo para escribir comprobaciones legibles sin arrastrar JUnit al
 * proyecto, que no tiene sistema de construccion.
 */
public class Comprobador {

    private final String titulo;
    private int aciertos;
    private int fallos;

    public Comprobador(String titulo) {
        this.titulo = titulo;
        this.aciertos = 0;
        this.fallos = 0;
        System.out.println();
        System.out.println("== " + titulo.toUpperCase() + " ==");
    }

    public void comprobar(String que, boolean condicion) {
        if (condicion) {
            this.aciertos++;
        } else {
            this.fallos++;
        }
        System.out.println("  " + (condicion ? "OK  " : "MAL ") + que);
    }

    /** Un dato de contexto, sin contar como comprobacion. */
    public void dato(String texto) {
        System.out.println("       " + texto);
    }

    public int getFallos() {
        return this.fallos;
    }

    public int getAciertos() {
        return this.aciertos;
    }

    public String getTitulo() {
        return this.titulo;
    }
}
