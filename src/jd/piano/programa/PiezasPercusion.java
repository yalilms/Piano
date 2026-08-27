package jd.piano.programa;

/**
 * Las piezas de bateria del canal 10 en General MIDI.
 *
 * En ese canal la nota no es una altura sino un instrumento: el 36 es el
 * bombo y el 42 el charles cerrado. Por eso no tiene sentido pintarlas en un
 * piano, y por eso hasta ahora se descartaban enteras: eran el 35% de todas
 * las notas de las canciones del proyecto.
 */
public final class PiezasPercusion {

    /** Rango que cubre General MIDI en el canal de percusion. */
    public static final int NOTA_MINIMA = 27;
    public static final int NOTA_MAXIMA = 87;

    /** Familias, para poder ordenar los pads de forma sensata. */
    public enum Familia {
        BOMBO("Bombos"),
        CAJA("Cajas"),
        CHARLES("Charles"),
        PLATO("Platos"),
        TOM("Toms"),
        LATINA("Percusion latina"),
        OTRA("Otros");

        private final String nombre;

        Familia(String nombre) {
            this.nombre = nombre;
        }

        public String getNombre() {
            return this.nombre;
        }
    }

    private record Pieza(String nombre, Familia familia) {
    }

    private static final Pieza[] PIEZAS = new Pieza[NOTA_MAXIMA - NOTA_MINIMA + 1];

    static {
        poner(27, "Q agudo", Familia.OTRA);
        poner(28, "Slap", Familia.OTRA);
        poner(29, "Scratch adelante", Familia.OTRA);
        poner(30, "Scratch atras", Familia.OTRA);
        poner(31, "Baquetas", Familia.OTRA);
        poner(32, "Clic cuadrado", Familia.OTRA);
        poner(33, "Clic de metronomo", Familia.OTRA);
        poner(34, "Campana de metronomo", Familia.OTRA);
        poner(35, "Bombo grave", Familia.BOMBO);
        poner(36, "Bombo", Familia.BOMBO);
        poner(37, "Aro de caja", Familia.CAJA);
        poner(38, "Caja", Familia.CAJA);
        poner(39, "Palmada", Familia.CAJA);
        poner(40, "Caja electrica", Familia.CAJA);
        poner(41, "Tom suelo grave", Familia.TOM);
        poner(42, "Charles cerrado", Familia.CHARLES);
        poner(43, "Tom suelo", Familia.TOM);
        poner(44, "Charles con pedal", Familia.CHARLES);
        poner(45, "Tom grave", Familia.TOM);
        poner(46, "Charles abierto", Familia.CHARLES);
        poner(47, "Tom medio grave", Familia.TOM);
        poner(48, "Tom medio", Familia.TOM);
        poner(49, "Crash 1", Familia.PLATO);
        poner(50, "Tom agudo", Familia.TOM);
        poner(51, "Ride 1", Familia.PLATO);
        poner(52, "Plato chino", Familia.PLATO);
        poner(53, "Campana del ride", Familia.PLATO);
        poner(54, "Pandereta", Familia.LATINA);
        poner(55, "Splash", Familia.PLATO);
        poner(56, "Cencerro", Familia.LATINA);
        poner(57, "Crash 2", Familia.PLATO);
        poner(58, "Vibraslap", Familia.OTRA);
        poner(59, "Ride 2", Familia.PLATO);
        poner(60, "Bongo agudo", Familia.LATINA);
        poner(61, "Bongo grave", Familia.LATINA);
        poner(62, "Conga aguda apagada", Familia.LATINA);
        poner(63, "Conga aguda", Familia.LATINA);
        poner(64, "Conga grave", Familia.LATINA);
        poner(65, "Timbal agudo", Familia.LATINA);
        poner(66, "Timbal grave", Familia.LATINA);
        poner(67, "Agogo agudo", Familia.LATINA);
        poner(68, "Agogo grave", Familia.LATINA);
        poner(69, "Cabasa", Familia.LATINA);
        poner(70, "Maracas", Familia.LATINA);
        poner(71, "Silbato corto", Familia.OTRA);
        poner(72, "Silbato largo", Familia.OTRA);
        poner(73, "Guiro corto", Familia.LATINA);
        poner(74, "Guiro largo", Familia.LATINA);
        poner(75, "Claves", Familia.LATINA);
        poner(76, "Caja china aguda", Familia.LATINA);
        poner(77, "Caja china grave", Familia.LATINA);
        poner(78, "Cuica apagada", Familia.LATINA);
        poner(79, "Cuica", Familia.LATINA);
        poner(80, "Triangulo apagado", Familia.LATINA);
        poner(81, "Triangulo", Familia.LATINA);
        poner(82, "Shaker", Familia.LATINA);
        poner(83, "Cascabel", Familia.OTRA);
        poner(84, "Arbol de campanas", Familia.OTRA);
        poner(85, "Castanuelas", Familia.LATINA);
        poner(86, "Surdo apagado", Familia.LATINA);
        poner(87, "Surdo", Familia.LATINA);
    }

    private static void poner(int nota, String nombre, Familia familia) {
        PIEZAS[nota - NOTA_MINIMA] = new Pieza(nombre, familia);
    }

    private PiezasPercusion() {
    }

    public static boolean esConocida(int nota) {
        return nota >= NOTA_MINIMA && nota <= NOTA_MAXIMA
                && PIEZAS[nota - NOTA_MINIMA] != null;
    }

    public static String nombre(int nota) {
        String nombre = "Percusion " + nota;
        if (esConocida(nota)) {
            nombre = PIEZAS[nota - NOTA_MINIMA].nombre();
        }
        return nombre;
    }

    public static Familia familia(int nota) {
        Familia familia = Familia.OTRA;
        if (esConocida(nota)) {
            familia = PIEZAS[nota - NOTA_MINIMA].familia();
        }
        return familia;
    }

    /**
     * Orden en el que conviene mostrar los pads: primero el bombo y la caja,
     * que son los que marcan el ritmo, y al final lo accesorio.
     */
    public static int ordenDe(int nota) {
        return familia(nota).ordinal() * 1000 + nota;
    }
}
