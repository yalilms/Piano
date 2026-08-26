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

    private AnalisisMidi(List<Integer> canalesUsados, int notaMinima,
                         int notaMaxima, int totalNotas) {
        this.canalesUsados = canalesUsados;
        this.notaMinima = notaMinima;
        this.notaMaxima = notaMaxima;
        this.totalNotas = totalNotas;
    }

    public static AnalisisMidi de(String ruta) throws Exception {
        Sequence secuencia = MidiSystem.getSequence(new File(ruta));
        boolean[] vistos = new boolean[CANALES];
        int minima = Integer.MAX_VALUE;
        int maxima = Integer.MIN_VALUE;
        int total = 0;

        for (Track pista : secuencia.getTracks()) {
            for (int i = 0; i < pista.size(); i++) {
                MidiEvent evento = pista.get(i);
                if (!(evento.getMessage() instanceof ShortMessage sm)) {
                    continue;
                }
                boolean esNota = sm.getCommand() == ShortMessage.NOTE_ON
                        && sm.getData2() > 0
                        && sm.getChannel() != CANAL_PERCUSION;
                if (esNota) {
                    vistos[sm.getChannel()] = true;
                    minima = Math.min(minima, sm.getData1());
                    maxima = Math.max(maxima, sm.getData1());
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
        return new AnalisisMidi(canales, minima, maxima, total);
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
