package jd.piano.programa;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Todos los sonidos que el sintetizador sabe hacer de verdad.
 *
 * Hasta ahora el programa ofrecia 128, los del estandar General MIDI, porque los
 * leia de una lista escrita a mano. Pero el banco que trae Java es el GS de
 * Roland y tiene 235 repartidos en trece bancos, de los que 226 se pueden
 * elegir: ademas de los 128 de siempre estan las variaciones (Ukulele, guitarra
 * de doce cuerdas, pianos desafinados, campanas de iglesia) y, sobre todo, los
 * efectos de sonido, que casi no se podian elegir: lluvia, trueno, perro,
 * telefono, risas, ametralladora, galope, puerta chirriando, grito, pistola
 * laser, explosion, latido, sirena, pasos, tren y avion. De 8 efectos elegibles
 * se pasa a 41.
 *
 * Se le pregunta al sintetizador en vez de suponerlo, asi que si se carga un
 * banco de sonidos externo (un fichero .sf2 o .dls) la lista crece con lo que
 * traiga: ahi es donde estan los miles de sonidos.
 */
public final class BancoDeSonidos {

    /**
     * Un sonido concreto: su nombre, y donde encontrarlo.
     *
     * El banco no es un numero del 0 al 12: en MIDI son catorce bits, asi que
     * las variaciones del GS salen como 128, 256, 384... Se guarda tal cual lo
     * da el sintetizador para poder pedirselo igual.
     */
    public record Sonido(String nombre, int banco, int programa,
                         String etiqueta) {

        @Override
        public String toString() {
            return this.etiqueta;
        }
    }

    /** Las dieciseis familias del estandar, en el orden que las numera. */
    private static final String[] FAMILIAS = {
            "pianos", "percusion afinada", "organos", "guitarras", "bajos",
            "cuerdas", "conjuntos", "metales", "canas", "flautas",
            "sinte solista", "sinte de fondo", "efectos de sinte", "etnicos",
            "percusion", "efectos de sonido",
    };

    private final List<Sonido> sonidos;
    private final String nombreDelBanco;

    private BancoDeSonidos(List<Sonido> sonidos, String nombreDelBanco) {
        this.sonidos = sonidos;
        this.nombreDelBanco = nombreDelBanco;
    }

    /**
     * Le pregunta al sintetizador que tiene. Si no hay sintetizador o no
     * contesta, se cae a los 128 nombres del estandar, que es lo que habia
     * antes: mas vale una lista corta que ninguna.
     */
    public static BancoDeSonidos de(Synthesizer sintetizador) {
        if (sintetizador != null) {
            try {
                Instrument[] disponibles = sintetizador.getAvailableInstruments();
                if (disponibles != null && disponibles.length > 0) {
                    Soundbank banco = sintetizador.getDefaultSoundbank();
                    return new BancoDeSonidos(catalogar(disponibles),
                            banco == null ? "el del sintetizador"
                                    : banco.getName().trim());
                }
            } catch (RuntimeException error) {
                // se cae al estandar
            }
        }
        return elEstandar();
    }

    /** Los 128 de General MIDI, sin preguntarle a nadie. */
    public static BancoDeSonidos elEstandar() {
        List<Sonido> lista = new ArrayList<>();
        for (int programa = 0; programa < InstrumentosGM.cuantos(); programa++) {
            String nombre = InstrumentosGM.nombre(programa);
            lista.add(new Sonido(nombre, 0, programa,
                    etiquetaDe(nombre, programa, 0, false)));
        }
        return new BancoDeSonidos(List.copyOf(lista), "General MIDI");
    }

    /**
     * Ordena y etiqueta lo que da el sintetizador.
     *
     * El banco 0 va primero y en su orden, que es el de siempre y el que la
     * gente reconoce; detras las variaciones y los efectos. A los nombres que
     * aparecen en varios bancos se les pone de que variacion son, que si no
     * salen ocho entradas identicas y no hay forma de saber cual es cual.
     */
    private static List<Sonido> catalogar(Instrument[] disponibles) {
        List<Instrument> elegibles = unoPorSitio(disponibles);

        Map<String, Integer> cuantasVeces = new HashMap<>();
        for (Instrument instrumento : elegibles) {
            cuantasVeces.merge(instrumento.getName().trim(), 1, Integer::sum);
        }

        List<Instrument> ordenados = new ArrayList<>(elegibles);
        ordenados.sort(Comparator
                .comparingInt((Instrument i) -> i.getPatch().getBank() == 0 ? 0 : 1)
                .thenComparingInt(i -> i.getPatch().getBank() == 0
                        ? i.getPatch().getProgram() : i.getPatch().getBank())
                .thenComparingInt(i -> i.getPatch().getProgram()));

        List<Sonido> lista = new ArrayList<>();
        for (Instrument instrumento : ordenados) {
            Patch sitio = instrumento.getPatch();
            String nombre = instrumento.getName().trim();
            boolean repetido = cuantasVeces.getOrDefault(nombre, 1) > 1;
            lista.add(new Sonido(nombre, sitio.getBank(), sitio.getProgram(),
                    etiquetaDe(nombre, sitio.getProgram(), sitio.getBank(),
                            repetido)));
        }
        return List.copyOf(lista);
    }

    /**
     * Un instrumento por cada sitio, quedandose con el que de verdad suena.
     *
     * Hay sitios ocupados dos veces: en el banco GS, los nueve kits de bateria
     * (Standard, Room, Power, TR-808, Jazz...) comparten banco y programa con
     * instrumentos melodicos, asi que el sitio 0:0 es a la vez Piano 1 y el kit
     * Standard. Poner los dos en la lista seria mentir, porque en MIDI el kit no
     * se elige por programa sino por estar en el canal 10: pidiendo el sitio 0:0
     * suena el piano.
     *
     * Cual suena no se adivina: se le pregunta al banco con getInstrument, que
     * es lo mismo que resuelve el cambio de programa.
     */
    private static List<Instrument> unoPorSitio(Instrument[] disponibles) {
        Map<String, Instrument> porSitio = new LinkedHashMap<>();
        for (Instrument instrumento : disponibles) {
            Patch sitio = instrumento.getPatch();
            String clave = sitio.getBank() + ":" + sitio.getProgram();
            if (porSitio.containsKey(clave)) {
                continue;
            }
            Instrument elQueSuena = null;
            try {
                Soundbank banco = instrumento.getSoundbank();
                if (banco != null) {
                    elQueSuena = banco.getInstrument(sitio);
                }
            } catch (RuntimeException error) {
                elQueSuena = null;
            }
            porSitio.put(clave, elQueSuena != null ? elQueSuena : instrumento);
        }
        return new ArrayList<>(porSitio.values());
    }

    /**
     * Lo que se lee en el desplegable: el nombre y de que familia es. La familia
     * ayuda a orientarse cuando hay doscientos, y es lo que hace que se vea de
     * un golpe donde estan los efectos.
     */
    private static String etiquetaDe(String nombre, int programa, int banco,
                                     boolean repetido) {
        StringBuilder etiqueta = new StringBuilder(nombre);
        etiqueta.append("  -  ").append(familia(programa));
        if (banco != 0) {
            etiqueta.append(repetido ? " (variacion " + banco / 128 + ")"
                    : " (variacion)");
        }
        return etiqueta.toString();
    }

    /** A que familia del estandar pertenece ese programa. */
    public static String familia(int programa) {
        int cual = programa / 8;
        return cual >= 0 && cual < FAMILIAS.length ? FAMILIAS[cual] : "otros";
    }

    public List<Sonido> getSonidos() {
        return this.sonidos;
    }

    public int cuantos() {
        return this.sonidos.size();
    }

    public String getNombreDelBanco() {
        return this.nombreDelBanco;
    }

    /**
     * El sonido que esta en ese sitio, o null. Se busca por banco y programa y
     * no por posicion en la lista: la posicion cambia en cuanto se carga otro
     * banco de sonidos, y lo que se guarda en las preferencias tiene que seguir
     * valiendo.
     */
    public Sonido buscar(int banco, int programa) {
        for (Sonido sonido : this.sonidos) {
            if (sonido.banco() == banco && sonido.programa() == programa) {
                return sonido;
            }
        }
        return null;
    }

    /** El primero de la lista, para cuando no hay nada elegido. */
    public Sonido porDefecto() {
        Sonido piano = buscar(0, 0);
        if (piano != null) {
            return piano;
        }
        return this.sonidos.isEmpty() ? null : this.sonidos.get(0);
    }

    /**
     * Carga un fichero de banco de sonidos en el sintetizador. Es la via para
     * pasar de doscientos y pico sonidos a los miles que trae un SoundFont.
     *
     * @return el banco recien cargado, para poder descargarlo despues
     * @throws Exception si el fichero no es un banco o el sintetizador no lo
     *     admite; el mensaje se le ensena al usuario
     */
    public static Soundbank cargarFichero(Synthesizer sintetizador, File fichero)
            throws Exception {
        if (sintetizador == null) {
            throw new IllegalStateException("No hay sintetizador abierto.");
        }
        Soundbank banco = MidiSystem.getSoundbank(fichero);
        if (!sintetizador.isSoundbankSupported(banco)) {
            throw new IllegalArgumentException(
                    "El sintetizador no admite ese banco de sonidos.");
        }
        if (!sintetizador.loadAllInstruments(banco)) {
            throw new IllegalArgumentException(
                    "No se ha podido cargar ningun instrumento del fichero.");
        }
        return banco;
    }
}
