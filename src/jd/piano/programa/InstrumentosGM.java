package jd.piano.programa;

/**
 * Nombres de los 128 instrumentos de General MIDI.
 *
 * Hacen falta porque el PROGRAM_CHANGE de los ficheros MIDI dice que suena en
 * cada canal, y sin traducirlo los colores del multipiano no significan nada
 * para quien mira.
 */
public final class InstrumentosGM {

    private static final String[] NOMBRES = {
            // 0-7 Pianos
            "Piano de cola", "Piano brillante", "Piano electrico", "Piano honky-tonk",
            "Piano Rhodes", "Piano con chorus", "Clave", "Clavicordio",
            // 8-15 Percusion cromatica
            "Celesta", "Glockenspiel", "Caja de musica", "Vibrafono",
            "Marimba", "Xilofono", "Campanas tubulares", "Dulcimer",
            // 16-23 Organos
            "Organo Hammond", "Organo percusivo", "Organo de rock", "Organo de iglesia",
            "Armonio", "Acordeon", "Armonica", "Bandoneon",
            // 24-31 Guitarras
            "Guitarra espanola", "Guitarra acustica", "Guitarra electrica limpia",
            "Guitarra electrica", "Guitarra con sordina", "Guitarra con overdrive",
            "Guitarra distorsionada", "Armonicos de guitarra",
            // 32-39 Bajos
            "Bajo acustico", "Bajo electrico", "Bajo con pua", "Bajo sin trastes",
            "Bajo slap 1", "Bajo slap 2", "Bajo sintetizado 1", "Bajo sintetizado 2",
            // 40-47 Cuerdas
            "Violin", "Viola", "Violonchelo", "Contrabajo",
            "Cuerdas en tremolo", "Cuerdas en pizzicato", "Arpa", "Timbales",
            // 48-55 Conjuntos
            "Cuerdas 1", "Cuerdas 2", "Cuerdas sintetizadas 1", "Cuerdas sintetizadas 2",
            "Coro aah", "Voz ooh", "Voz sintetizada", "Golpe de orquesta",
            // 56-63 Metales
            "Trompeta", "Trombon", "Tuba", "Trompeta con sordina",
            "Corno frances", "Seccion de metales", "Metales sintetizados 1",
            "Metales sintetizados 2",
            // 64-71 Canas
            "Saxo soprano", "Saxo alto", "Saxo tenor", "Saxo baritono",
            "Oboe", "Corno ingles", "Fagot", "Clarinete",
            // 72-79 Flautas
            "Flautin", "Flauta", "Flauta dulce", "Flauta de pan",
            "Botella", "Shakuhachi", "Silbato", "Ocarina",
            // 80-87 Solistas sintetizados
            "Sinte onda cuadrada", "Sinte diente de sierra", "Sinte calliope",
            "Sinte chiff", "Sinte charang", "Sinte voz", "Sinte quintas",
            "Sinte bajo y solista",
            // 88-95 Fondos sintetizados
            "Fondo new age", "Fondo calido", "Fondo polisintetizado", "Fondo coral",
            "Fondo de arco", "Fondo metalico", "Fondo halo", "Fondo barrido",
            // 96-103 Efectos sintetizados
            "Lluvia", "Banda sonora", "Cristal", "Atmosfera",
            "Brillo", "Duendes", "Ecos", "Ciencia ficcion",
            // 104-111 Etnicos
            "Sitar", "Banjo", "Shamisen", "Koto",
            "Kalimba", "Gaita", "Violin folk", "Shanai",
            // 112-119 Percusivos
            "Campanilla", "Agogo", "Tambor metalico", "Bloque de madera",
            "Taiko", "Tom melodico", "Tambor sintetizado", "Platillo invertido",
            // 120-127 Efectos
            "Traste de guitarra", "Respiracion", "Orilla del mar", "Pajaros",
            "Telefono", "Helicoptero", "Aplausos", "Disparo"
    };

    private InstrumentosGM() {
    }

    /** @param programa numero de programa MIDI, 0-127. */
    public static String nombre(int programa) {
        String nombre = "Instrumento " + programa;
        if (programa >= 0 && programa < NOMBRES.length) {
            nombre = NOMBRES[programa];
        }
        return nombre;
    }

    public static int cuantos() {
        return NOMBRES.length;
    }
}
