package jd.piano.pruebas;

import jd.piano.programa.PianoSencillo;
import jd.piano.programa.Reproductor;
import jd.piano.teclas.Piano;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Que no se quede ninguna nota sonando.
 *
 * Es el fallo que se noto usando el programa: al cambiar de cancion en medio de
 * un acorde, una nota se quedaba sonando para siempre y habia que darle a Parar
 * antes de cambiar. La causa era que al parar el secuenciador solo se apagaban
 * las teclas dibujadas; la nota la sostiene el sintetizador hasta que le llega
 * su NOTE_OFF, y ese NOTE_OFF ya no llegaba nunca. Y encima cerrar() cerraba el
 * camino de salida antes de intentar apagar nada.
 *
 * Lo que se cuenta aqui son las notas abiertas: un NOTE_ON al que nadie le ha
 * mandado su NOTE_OFF ni un apagado general de su canal. Es el unico observable
 * que sirve. Se probo antes con getVoiceStatus del sintetizador y no vale:
 * Gervill acumula voces que ya no suenan, dice el canal equivocado, y no retira
 * nada cuando se le llama por los metodos de MidiChannel. Contando las notas
 * abiertas se comprueba lo que de verdad depende de nosotros, que es mandar el
 * MIDI correcto, y da igual el sintetizador que haya detras.
 *
 * No hace falta ningun sintetizador ni suena nada: el espia se cuela como
 * salida, asi que el secuenciador se pide sin conectar a nada.
 */
public final class PruebaSilencio {

    /** Cuanto se espera a que la cancion tenga notas abiertas. */
    private static final long ESPERA_MS = 12000;

    private PruebaSilencio() {
    }

    public static Comprobador ejecutar() {
        Comprobador c = new Comprobador("nada se queda sonando");
        File[] canciones = PruebaMidi.canciones();
        if (canciones.length == 0) {
            c.comprobar("hay canciones para probar", false);
            return c;
        }
        String cancion = canciones[0].getPath();

        cierraLasNotas(c, cancion, "al cambiar de cancion",
                Reproductor::cerrar);
        cierraLasNotas(c, cancion, "al parar", Reproductor::parar);
        cierraLasNotas(c, cancion, "al pausar", Reproductor::pausar);
        // Estas dos dejan la cancion sonando, asi que apareceran notas nuevas:
        // lo que se comprueba es que no quede huerfana ninguna de las de antes
        sinHuerfanas(c, cancion, "al saltar a otro punto",
                r -> r.buscar(4_000_000));
        sinHuerfanas(c, cancion, "al cambiar el tono",
                r -> r.setTransposicion(5));

        estadoEntreCanciones(c, cancion);
        return c;
    }

    /**
     * Lleva la cuenta de las notas abiertas segun lo que sale hacia el
     * sintetizador. Se pone en medio y deja pasar todo.
     */
    private static final class Espia implements Receiver {
        private final Receiver siguiente;

        /**
         * Nota abierta -> numero de la pulsacion que la abrio. El numero hace
         * falta para distinguir una nota que se ha quedado huerfana de la misma
         * nota vuelta a pulsar despues: en un conjunto de claves las dos se ven
         * igual.
         */
        private final Map<String, Long> abiertas = new HashMap<>();
        private long pulsaciones;
        private final Set<Integer> controladores = new TreeSet<>();
        private boolean afinacionCentrada;

        Espia(Receiver siguiente) {
            this.siguiente = siguiente;
        }

        @Override
        public synchronized void send(MidiMessage mensaje, long marca) {
            if (mensaje instanceof ShortMessage sm) {
                anotar(sm);
            }
            if (this.siguiente != null) {
                this.siguiente.send(mensaje, marca);
            }
        }

        private void anotar(ShortMessage sm) {
            int canal = sm.getChannel();
            int comando = sm.getCommand();
            String clave = canal + ":" + sm.getData1();
            if (comando == ShortMessage.NOTE_ON && sm.getData2() > 0) {
                this.pulsaciones++;
                this.abiertas.put(clave, this.pulsaciones);
            } else if (comando == ShortMessage.NOTE_OFF
                    || (comando == ShortMessage.NOTE_ON && sm.getData2() == 0)) {
                this.abiertas.remove(clave);
            } else if (comando == ShortMessage.CONTROL_CHANGE) {
                this.controladores.add(sm.getData1());
                if (sm.getData1() == 120 || sm.getData1() == 123) {
                    // Apagado general: se cierra todo lo de ese canal
                    this.abiertas.keySet()
                            .removeIf(k -> k.startsWith(canal + ":"));
                }
            } else if (comando == ShortMessage.PITCH_BEND) {
                this.afinacionCentrada =
                        sm.getData1() == 0 && sm.getData2() == 64;
            }
        }

        synchronized Map<String, Long> abiertas() {
            return new HashMap<>(this.abiertas);
        }

        synchronized boolean vio(int controlador) {
            return this.controladores.contains(controlador);
        }

        synchronized boolean afinacionCentrada() {
            return this.afinacionCentrada;
        }

        synchronized void olvidar() {
            this.controladores.clear();
            this.afinacionCentrada = false;
        }

        @Override
        public void close() {
        }
    }

    private interface Accion {
        void hacer(Reproductor r);
    }

    /** Monta el reproductor con el espia de salida y pone la cancion a sonar. */
    private static Espia montar(Reproductor r, String cancion) throws Exception {
        Piano piano = new PianoSencillo(21, 108);
        piano.setPosicion(0, 0);
        r.conectar(piano);

        // El espia va puesto antes de cargar: asi el secuenciador se pide sin
        // conectar a ningun dispositivo y la prueba no suena
        Espia espia = new Espia(null);
        Field campo = r.getClass().getDeclaredField("salidaAlSintetizador");
        campo.setAccessible(true);
        campo.set(r, espia);

        r.cargar(cancion);
        campo.set(r, espia);
        return espia;
    }

    private static int esperarNotas(Espia espia) throws InterruptedException {
        long hasta = System.currentTimeMillis() + ESPERA_MS;
        while (espia.abiertas().isEmpty() && System.currentTimeMillis() < hasta) {
            Thread.sleep(40);
        }
        return espia.abiertas().size();
    }

    /**
     * Las notas de antes que siguen abiertas con el mismo numero de pulsacion:
     * esas son las huerfanas. Las que tienen numero nuevo son notas nuevas, que
     * es lo normal si la cancion sigue sonando.
     */
    private static Set<String> huerfanas(Map<String, Long> antes,
                                         Map<String, Long> despues) {
        Set<String> quedan = new TreeSet<>();
        for (Map.Entry<String, Long> e : antes.entrySet()) {
            if (e.getValue().equals(despues.get(e.getKey()))) {
                quedan.add(e.getKey());
            }
        }
        return quedan;
    }

    /**
     * Para lo que deja la cancion parada: no puede quedar ninguna nota abierta,
     * ni vieja ni nueva.
     */
    private static void cierraLasNotas(Comprobador c, String cancion,
                                       String cuando, Accion accion) {
        Reproductor r = new Reproductor();
        try {
            Espia espia = montar(r, cancion);
            r.reproducir();
            int antes = esperarNotas(espia);
            accion.hacer(r);
            Thread.sleep(300);
            Map<String, Long> quedan = espia.abiertas();

            if (!quedan.isEmpty()) {
                c.dato("se quedan abiertas " + new TreeSet<>(quedan.keySet()));
            }
            c.comprobar("no queda ninguna nota abierta " + cuando
                            + " (habia " + antes + ")",
                    antes > 0 && quedan.isEmpty());
        } catch (Exception error) {
            c.comprobar("silenciar " + cuando + " no lanza: " + error, false);
        } finally {
            r.cerrar();
        }
    }

    /**
     * Para lo que deja la cancion sonando: pueden aparecer notas nuevas, pero
     * ninguna de las que ya estaban puede quedarse huerfana.
     */
    private static void sinHuerfanas(Comprobador c, String cancion,
                                     String cuando, Accion accion) {
        Reproductor r = new Reproductor();
        try {
            Espia espia = montar(r, cancion);
            r.reproducir();
            int antes = esperarNotas(espia);
            Map<String, Long> antesDeLaAccion = espia.abiertas();

            accion.hacer(r);
            Thread.sleep(300);
            Set<String> quedan = huerfanas(antesDeLaAccion, espia.abiertas());

            if (!quedan.isEmpty()) {
                c.dato("huerfanas: " + quedan);
            }
            c.comprobar("ninguna nota anterior se queda huerfana " + cuando
                            + " (habia " + antes + ")",
                    antes > 0 && quedan.isEmpty());
        } catch (Exception error) {
            c.comprobar("la accion " + cuando + " no lanza: " + error, false);
        } finally {
            r.cerrar();
        }
    }

    /**
     * Y que una cancion no le deje a la siguiente el pedal pisado ni la rueda de
     * afinacion torcida: la siguiente sonaria desafinada sin que nada lo
     * explicara.
     */
    private static void estadoEntreCanciones(Comprobador c, String cancion) {
        Reproductor r = new Reproductor();
        try {
            Espia espia = montar(r, cancion);
            r.reproducir();
            esperarNotas(espia);
            espia.olvidar();

            r.cerrar();
            Thread.sleep(200);
            c.comprobar("al cambiar de cancion se suelta el pedal",
                    espia.vio(64));
            c.comprobar("al cambiar de cancion se reinician los controladores",
                    espia.vio(121));
            c.comprobar("al cambiar de cancion se centra la afinacion",
                    espia.afinacionCentrada());
            c.comprobar("al cambiar de cancion se manda el apagado general",
                    espia.vio(120) && espia.vio(123));
        } catch (Exception error) {
            c.comprobar("el estado entre canciones no lanza: " + error, false);
        } finally {
            r.cerrar();
        }
    }
}
