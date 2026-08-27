package jd.piano.programa;

/**
 * Para que se esta usando el programa. Antes habia un desplegable con dos
 * modos de dibujado y una casilla de aprendizaje aparte, que se podia marcar
 * incluso cuando no hacia nada. Asi queda claro que son cuatro formas de usarlo
 * y solo una a la vez.
 */
public enum ModoDeUso {

    VER_CANCION("Ver una cancion",
            "Un teclado con las notas cayendo"),

    VER_CANALES("Ver por canales",
            "Un teclado por cada canal de la cancion"),

    APRENDER("Aprender a tocarla",
            "Se para y espera a que toques la nota que viene"),

    TOCAR_LIBRE("Tocar libre",
            "Sin cancion: el teclado para ti");

    private final String nombre;
    private final String explicacion;

    ModoDeUso(String nombre, String explicacion) {
        this.nombre = nombre;
        this.explicacion = explicacion;
    }

    public String getNombre() {
        return this.nombre;
    }

    public String getExplicacion() {
        return this.explicacion;
    }

    /** Si hace falta una cancion cargada para que el modo tenga sentido. */
    public boolean necesitaCancion() {
        return this != TOCAR_LIBRE;
    }

    /** Si se dibuja un teclado por canal en vez de uno solo. */
    public boolean esPorCanales() {
        return this == VER_CANALES;
    }

    /** Si las notas caen desde arriba. */
    public boolean tieneCascada() {
        return this == VER_CANCION || this == APRENDER;
    }

    @Override
    public String toString() {
        return this.nombre;
    }
}
