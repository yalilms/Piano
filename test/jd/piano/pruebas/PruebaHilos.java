package jd.piano.pruebas;

import jd.piano.instrumentos.PiezaPulsable;
import jd.piano.programa.AnalisisMidi;
import jd.piano.programa.Conjunto;
import jd.piano.programa.PianoSencillo;
import jd.piano.programa.Reproductor;
import jd.piano.programa.TipoInstrumento;
import jd.piano.teclas.Piano;

import javax.sound.midi.ShortMessage;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Los dos hilos.
 *
 * El secuenciador entrega los mensajes MIDI en su propio hilo, cientos por
 * segundo, mientras el de Swing pinta y atiende los controles. Todo lo que
 * cruza entre los dos tiene que aguantarlo.
 *
 * Esto no se comprobaba, y habia un fallo de verdad: mover un canal a otro
 * instrumento con la cancion sonando vaciaba y rellenaba la misma lista que el
 * hilo del MIDI estaba recorriendo, y los dos hilos morian con
 * ConcurrentModificationException. Como el que muere es el del secuenciador, no
 * salta ningun dialogo: la cancion sigue oyendose y las notas dejan de pintarse.
 *
 * La prueba es corta a proposito, pero el fallo saltaba en menos de un segundo.
 */
public final class PruebaHilos {

    private static final long DURACION_MS = 1500;

    private PruebaHilos() {
    }

    public static Comprobador ejecutar() {
        Comprobador c = new Comprobador("los dos hilos");
        File[] canciones = PruebaMidi.canciones();
        if (canciones.length == 0) {
            c.comprobar("hay canciones para probar", false);
            return c;
        }
        repartoEnCaliente(c, canciones[0].getPath());
        estadoDeLasPiezas(c);
        return c;
    }

    /**
     * Mover canales de instrumento mientras entran notas y mientras se pinta.
     * Es lo que pasa al tocar el desplegable "se ve en" con la cancion sonando.
     */
    private static void repartoEnCaliente(Comprobador c, String cancion) {
        Conjunto conjunto;
        try {
            conjunto = Conjunto.para(AnalisisMidi.de(cancion));
        } catch (Exception error) {
            c.comprobar("se puede montar el conjunto: " + error, false);
            return;
        }
        conjunto.ajustarA(900, 460);
        conjunto.setPosicion(0, 0);

        AtomicBoolean seguir = new AtomicBoolean(true);
        AtomicReference<Throwable> falloMidi = new AtomicReference<>();
        AtomicReference<Throwable> falloPintado = new AtomicReference<>();
        AtomicInteger rondas = new AtomicInteger();
        AtomicInteger cambios = new AtomicInteger();

        Thread midi = new Thread(() -> {
            try {
                int n = 0;
                while (seguir.get()) {
                    for (int canal = 0; canal < 8; canal++) {
                        conjunto.pulsar(canal, 40 + (n % 40), 100);
                        conjunto.soltar(canal, 40 + (n % 40));
                    }
                    n++;
                    rondas.incrementAndGet();
                }
            } catch (Throwable error) {
                falloMidi.set(error);
            }
        }, "prueba-midi");

        Thread pintado = new Thread(() -> {
            try {
                List<Rectangle> regiones = new ArrayList<>();
                while (seguir.get()) {
                    for (int canal = 0; canal < 16; canal++) {
                        for (int nota = 21; nota <= 108; nota++) {
                            PiezaPulsable pieza = conjunto.piezaDe(canal, nota);
                            if (pieza != null) {
                                pieza.getIntensidad();
                            }
                        }
                    }
                    regiones.clear();
                    conjunto.anadirRegionesActivas(regiones);
                    conjunto.getAnchura();
                    conjunto.getAltura();
                }
            } catch (Throwable error) {
                falloPintado.set(error);
            }
        }, "prueba-pintado");

        midi.start();
        pintado.start();

        Throwable falloUsuario = null;
        TipoInstrumento[] tipos = TipoInstrumento.values();
        long hasta = System.currentTimeMillis() + DURACION_MS;
        try {
            int i = 0;
            while (System.currentTimeMillis() < hasta) {
                conjunto.asignar(i % 8, tipos[(i / 8) % tipos.length]);
                cambios.incrementAndGet();
                i++;
                Thread.sleep(3);
            }
        } catch (Throwable error) {
            falloUsuario = error;
        }
        seguir.set(false);
        esperar(midi);
        esperar(pintado);

        c.dato(rondas.get() + " rondas de notas y " + cambios.get()
                + " cambios de instrumento en " + DURACION_MS + " ms");
        if (falloMidi.get() != null) {
            c.dato("MIDI: " + falloMidi.get());
        }
        if (falloPintado.get() != null) {
            c.dato("pintado: " + falloPintado.get());
        }
        c.comprobar("el hilo del MIDI aguanta que se reparta en caliente",
                falloMidi.get() == null);
        c.comprobar("el hilo que pinta aguanta que se reparta en caliente",
                falloPintado.get() == null);
        c.comprobar("mover canales no lanza nada", falloUsuario == null);
        c.comprobar("de verdad ha habido notas y cambios a la vez",
                rondas.get() > 100 && cambios.get() > 20);
    }

    /**
     * La caja que hay que repintar de cada pieza se cachea, y quien recoloca el
     * instrumento la invalida. Comprobar el campo y devolverlo despues dejaba
     * una rendija por la que se colaba un null justo entre las dos cosas.
     */
    private static void estadoDeLasPiezas(Comprobador c) {
        Piano piano = new PianoSencillo(21, 108);
        piano.setPosicion(0, 0);
        Reproductor r = new Reproductor();
        r.conectar(piano);

        AtomicBoolean seguir = new AtomicBoolean(true);
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        AtomicInteger nulos = new AtomicInteger();

        Thread lector = new Thread(() -> {
            try {
                while (seguir.get()) {
                    for (int nota = 21; nota <= 108; nota++) {
                        PiezaPulsable pieza = piano.getTecla(0, nota);
                        if (pieza.getForma() == null
                                || pieza.getCajaRepintado() == null) {
                            nulos.incrementAndGet();
                        }
                    }
                }
            } catch (Throwable error) {
                fallo.set(error);
            }
        }, "prueba-lector");
        lector.start();

        Throwable falloEscritor = null;
        long hasta = System.currentTimeMillis() + DURACION_MS / 2;
        try {
            int i = 0;
            while (System.currentTimeMillis() < hasta) {
                // Recolocar invalida las formas cacheadas de las 88 teclas
                piano.setPosicion(i % 20, i % 20);
                r.send(mensaje(ShortMessage.NOTE_ON, 0, 60, 100), -1);
                r.send(mensaje(ShortMessage.NOTE_OFF, 0, 60, 0), -1);
                i++;
            }
        } catch (Throwable error) {
            falloEscritor = error;
        }
        seguir.set(false);
        esperar(lector);
        r.cerrar();

        if (nulos.get() > 0) {
            c.dato(nulos.get() + " veces la forma o la caja salieron null");
        }
        c.comprobar("recolocar mientras se lee no devuelve nunca null",
                nulos.get() == 0);
        c.comprobar("leer las formas mientras se recoloca no lanza nada",
                fallo.get() == null);
        c.comprobar("recolocar mientras se lee no lanza nada",
                falloEscritor == null);
    }

    private static ShortMessage mensaje(int comando, int canal, int dato1,
                                        int dato2) {
        try {
            ShortMessage m = new ShortMessage();
            m.setMessage(comando, canal, dato1, dato2);
            return m;
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static void esperar(Thread hilo) {
        try {
            hilo.join(3000);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
