package jd.piano.pruebas;

import jd.piano.programa.AnalisisMidi;
import jd.piano.programa.MultiPiano;
import jd.piano.programa.NotaMidi;
import jd.piano.programa.Partitura;
import jd.piano.programa.Recursos;
import jd.piano.teclas.LayoutTeclados;
import jd.piano.teclas.Piano;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * La lectura de los ficheros MIDI: que el tiempo cuadre y que no se pierdan
 * notas por el camino.
 */
public final class PruebaMidi {

    private static final int NOTA_INICIAL = 24;
    private static final int NOTA_FINAL = 108;

    private PruebaMidi() {
    }

    public static Comprobador ejecutar() {
        Comprobador c = new Comprobador("lectura de los MIDI");
        File[] canciones = canciones();
        if (canciones.length == 0) {
            c.comprobar("hay canciones para probar", false);
            return c;
        }
        c.dato(canciones.length + " canciones en la carpeta");
        tempo(c, canciones);
        notas(c, canciones);
        cobertura(c, canciones);
        return c;
    }

    /**
     * Pasar pulsos a tiempo no es una regla de tres: un MIDI puede cambiar de
     * tempo por el camino. Si el mapa de tempo estuviera mal, la duracion
     * calculada se separaria de la que declara Java y las notas cayendo se
     * irian desincronizando conforme avanza la cancion.
     */
    private static void tempo(Comprobador c, File[] canciones) {
        double peorError = 0;
        String peor = "";
        boolean ok = true;
        for (File f : canciones) {
            try {
                Sequence sec = MidiSystem.getSequence(f);
                long declarada = sec.getMicrosecondLength();
                long calculada = Partitura.de(f.getPath()).getDuracionUs();
                double error = declarada == 0 ? 0
                        : 1000.0 * Math.abs(calculada - declarada) / declarada;
                if (error > peorError) {
                    peorError = error;
                    peor = f.getName();
                }
                ok = ok && error < 1.0;
            } catch (Exception error) {
                ok = false;
            }
        }
        c.dato(String.format("peor desvio: %.4f por mil (%s)", peorError, peor));
        c.comprobar("la duracion calculada cuadra con la que declara Java", ok);
    }

    /** Cada NOTE_ON del fichero debe salir como una nota de la partitura. */
    private static void notas(Comprobador c, File[] canciones) {
        boolean cuadran = true;
        boolean ordenadas = true;
        boolean sinPercusion = true;
        boolean conDuracion = true;
        for (File f : canciones) {
            try {
                Partitura p = Partitura.de(f.getPath());
                List<NotaMidi> notas = p.getNotas();
                cuadran = cuadran && notas.size() == contarNoteOn(f);
                long anterior = -1;
                for (NotaMidi n : notas) {
                    ordenadas = ordenadas && n.inicioUs() >= anterior;
                    anterior = n.inicioUs();
                    sinPercusion = sinPercusion && n.canal() != 9;
                    conDuracion = conDuracion && n.duracionUs() > 0;
                }
            } catch (Exception error) {
                cuadran = false;
            }
        }
        c.comprobar("el recuento de notas cuadra con los NOTE_ON del fichero", cuadran);
        c.comprobar("las notas salen ordenadas por instante de inicio", ordenadas);
        c.comprobar("la percusion queda fuera", sinPercusion);
        c.comprobar("ninguna nota dura cero", conDuracion);
    }

    /**
     * Con el rango completo, el multipiano tiene que ver casi todas las notas.
     * Con la rejilla fija de dos octavas que habia antes se perdia un cuarto.
     */
    private static void cobertura(Comprobador c, File[] canciones) {
        long total = 0;
        long fuera = 0;
        for (File f : canciones) {
            try {
                AnalisisMidi a = AnalisisMidi.de(f.getPath());
                List<Integer> canales = a.getCanalesUsados();
                MultiPiano mp = new MultiPiano(canales, NOTA_INICIAL, NOTA_FINAL,
                        LayoutTeclados.calcular(canales.size(),
                                Piano.contarBlancas(NOTA_INICIAL, NOTA_FINAL), 1300, 700));
                mp.setPosicion(0, 0);
                for (Track t : MidiSystem.getSequence(f).getTracks()) {
                    for (int i = 0; i < t.size(); i++) {
                        if (t.get(i).getMessage() instanceof ShortMessage sm
                                && sm.getCommand() == ShortMessage.NOTE_ON
                                && sm.getData2() > 0 && sm.getChannel() != 9) {
                            total++;
                            if (mp.getTecla(sm.getChannel(), sm.getData1()) == null) {
                                fuera++;
                            }
                        }
                    }
                }
            } catch (Exception error) {
                total = Math.max(total, 1);
                fuera = total;
            }
        }
        double visibles = 100.0 * (total - fuera) / Math.max(1, total);
        c.dato(String.format("notas visibles en el multipiano: %.2f%%", visibles));
        c.comprobar("el multipiano ve mas del 99% de las notas", visibles > 99.0);
    }

    private static int contarNoteOn(File f) throws Exception {
        int n = 0;
        for (Track t : MidiSystem.getSequence(f).getTracks()) {
            for (int i = 0; i < t.size(); i++) {
                if (t.get(i).getMessage() instanceof ShortMessage sm
                        && sm.getCommand() == ShortMessage.NOTE_ON
                        && sm.getData2() > 0 && sm.getChannel() != 9) {
                    n++;
                }
            }
        }
        return n;
    }

    static File[] canciones() {
        File carpeta = Recursos.buscar("canciones");
        List<File> lista = new ArrayList<>();
        if (carpeta != null && carpeta.isDirectory()) {
            File[] hijos = carpeta.listFiles(
                    (d, n) -> n.toLowerCase().endsWith(".mid"));
            if (hijos != null) {
                lista.addAll(Arrays.asList(hijos));
            }
        }
        lista.sort(Comparator.comparing(File::getName));
        return lista.toArray(new File[0]);
    }
}
