package jd.piano.programa;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee un fichero MIDI antes de reproducirlo para saber que canales usa y en
 * que notas se mueve. Con eso el MultiPiano monta solo los teclados que hacen
 * falta y les da el rango justo.
 *
 * El canal 10 (indice 9) es percusion en General MIDI y se ignora: sus notas
 * son instrumentos, no alturas, y pintarlas en un piano no significa nada.
 */
public final class AnalisisMidi {

    private static final int CANAL_PERCUSION = 9;
    private static final int CANALES = 16;

    private final List<Integer> canalesUsados;
    private final int notaMinima;
    private final int notaMaxima;
    private final int totalNotas;
    private final int[] programaPorCanal;
    private final List<Integer> percusionUsada;
    private final int totalPercusion;
    private final int[] minimaPorCanal;
    private final int[] maximaPorCanal;
    private final int[] notasPorCanal;

    private AnalisisMidi(List<Integer> canalesUsados, int notaMinima,
                         int notaMaxima, int totalNotas, int[] programaPorCanal,
                         List<Integer> percusionUsada, int totalPercusion,
                         int[] minimaPorCanal, int[] maximaPorCanal,
                         int[] notasPorCanal) {
        this.canalesUsados = canalesUsados;
        this.notaMinima = notaMinima;
        this.notaMaxima = notaMaxima;
        this.totalNotas = totalNotas;
        this.programaPorCanal = programaPorCanal;
        this.percusionUsada = percusionUsada;
        this.totalPercusion = totalPercusion;
        this.minimaPorCanal = minimaPorCanal;
        this.maximaPorCanal = maximaPorCanal;
        this.notasPorCanal = notasPorCanal;
    }

    public static AnalisisMidi de(String ruta) throws Exception {
        Sequence secuencia = MidiSystem.getSequence(new File(ruta));
        boolean[] vistos = new boolean[CANALES];
        int[] programa = new int[CANALES];
        boolean[] percusion = new boolean[128];
        // Rango de cada canal por separado: hace falta para repartir los
        // canales entre varios instrumentos y saber si a cada uno le cabe el suyo
        int[] minimaCanal = new int[CANALES];
        int[] maximaCanal = new int[CANALES];
        int[] notasCanal = new int[CANALES];
        java.util.Arrays.fill(minimaCanal, Integer.MAX_VALUE);
        java.util.Arrays.fill(maximaCanal, Integer.MIN_VALUE);
        int golpes = 0;
        int minima = Integer.MAX_VALUE;
        int maxima = Integer.MIN_VALUE;
        int total = 0;

        for (Track pista : secuencia.getTracks()) {
            for (int i = 0; i < pista.size(); i++) {
                MidiEvent evento = pista.get(i);
                if (!(evento.getMessage() instanceof ShortMessage sm)) {
                    continue;
                }
                // El primer PROGRAM_CHANGE de cada canal dice que instrumento es
                if (sm.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                    programa[sm.getChannel()] = sm.getData1();
                    continue;
                }
                boolean suena = sm.getCommand() == ShortMessage.NOTE_ON
                        && sm.getData2() > 0;
                if (suena && sm.getChannel() == CANAL_PERCUSION) {
                    // La percusion se cuenta aparte: en ese canal la nota es un
                    // instrumento, no una altura
                    percusion[sm.getData1()] = true;
                    golpes++;
                }
                boolean esNota = suena && sm.getChannel() != CANAL_PERCUSION;
                if (esNota) {
                    int canal = sm.getChannel();
                    vistos[canal] = true;
                    minima = Math.min(minima, sm.getData1());
                    maxima = Math.max(maxima, sm.getData1());
                    minimaCanal[canal] = Math.min(minimaCanal[canal], sm.getData1());
                    maximaCanal[canal] = Math.max(maximaCanal[canal], sm.getData1());
                    notasCanal[canal]++;
                    total++;
                }
            }
        }

        List<Integer> canales = new ArrayList<>();
        for (int c = 0; c < CANALES; c++) {
            if (vistos[c]) {
                canales.add(c);
            }
        }
        if (canales.isEmpty()) {
            // Un MIDI solo de percusion: se deja un teclado para no quedarse en blanco
            canales.add(0);
            minima = 48;
            maxima = 72;
        }
        List<Integer> piezas = new ArrayList<>();
        for (int nota = 0; nota < percusion.length; nota++) {
            if (percusion[nota]) {
                piezas.add(nota);
            }
        }
        return new AnalisisMidi(canales, minima, maxima, total, programa,
                piezas, golpes, minimaCanal, maximaCanal, notasCanal);
    }

    public List<Integer> getCanalesUsados() {
        return new ArrayList<>(this.canalesUsados);
    }

    public int getNotaMinima() {
        return this.notaMinima;
    }

    public int getNotaMaxima() {
        return this.notaMaxima;
    }

    public int getTotalNotas() {
        return this.totalNotas;
    }

    /** Numero de programa General MIDI de ese canal, 0 si no lo dice. */
    public int getPrograma(int canal) {
        int programa = 0;
        if (canal >= 0 && canal < this.programaPorCanal.length) {
            programa = this.programaPorCanal[canal];
        }
        return programa;
    }

    /**
     * Nota mas grave de ese canal, o -1 si el canal no toca nada. Con el rango
     * por canal se puede decidir si a una pista le cabe una guitarra o hay que
     * darle un teclado.
     */
    public int getNotaMinimaDe(int canal) {
        return valido(canal) && this.notasPorCanal[canal] > 0
                ? this.minimaPorCanal[canal] : -1;
    }

    public int getNotaMaximaDe(int canal) {
        return valido(canal) && this.notasPorCanal[canal] > 0
                ? this.maximaPorCanal[canal] : -1;
    }

    public int getNotasDe(int canal) {
        return valido(canal) ? this.notasPorCanal[canal] : 0;
    }

    private boolean valido(int canal) {
        return canal >= 0 && canal < CANALES;
    }

    /** Notas de percusion que usa la cancion, o vacia si no tiene bateria. */
    public List<Integer> getPercusionUsada() {
        return new ArrayList<>(this.percusionUsada);
    }

    public int getTotalPercusion() {
        return this.totalPercusion;
    }

    public String getInstrumento(int canal) {
        return InstrumentosGM.nombre(getPrograma(canal));
    }

    /**
     * Rango redondeado a octavas completas, empezando en Do, para que el
     * teclado no acabe cortado por la mitad de una octava.
     */
    public int getRangoInicial() {
        return Math.max(0, (this.notaMinima / 12) * 12);
    }

    public int getRangoFinal() {
        return Math.min(127, (this.notaMaxima / 12) * 12 + 12);
    }
}
