package jd.piano.programa;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Convierte un fichero MIDI en una lista de notas con inicio y duracion en
 * microsegundos, para poder dibujarlas cayendo antes de que suenen.
 *
 * La conversion de pulsos a tiempo no es una simple regla de tres: un MIDI
 * puede cambiar de tempo por el camino. Se construye un mapa con todos los
 * cambios y se acumula tramo a tramo.
 */
public class Partitura {

    private static final int META_CAMBIO_DE_TEMPO = 0x51;
    private static final int CANAL_PERCUSION = 9;
    private static final long US_POR_PULSO_POR_DEFECTO = 500_000;

    private final List<NotaMidi> notas;
    private final long duracionUs;

    private Partitura(List<NotaMidi> notas, long duracionUs) {
        this.notas = notas;
        this.duracionUs = duracionUs;
    }

    /** Notas ordenadas por instante de inicio. */
    public List<NotaMidi> getNotas() {
        return this.notas;
    }

    public long getDuracionUs() {
        return this.duracionUs;
    }

    public static Partitura de(String ruta) throws Exception {
        Sequence secuencia = MidiSystem.getSequence(new File(ruta));
        MapaTempo tempo = new MapaTempo(secuencia);

        // Nota abierta por canal y altura: el tick en que empezo y su velocidad
        long[][] inicio = new long[16][128];
        int[][] velocidad = new int[16][128];
        for (long[] fila : inicio) {
            java.util.Arrays.fill(fila, -1);
        }

        List<NotaMidi> notas = new ArrayList<>();
        for (Track pista : secuencia.getTracks()) {
            for (int i = 0; i < pista.size(); i++) {
                MidiEvent evento = pista.get(i);
                if (!(evento.getMessage() instanceof ShortMessage sm)) {
                    continue;
                }
                int canal = sm.getChannel();
                int altura = sm.getData1();
                if (canal == CANAL_PERCUSION || altura < 0 || altura > 127) {
                    continue;
                }
                boolean enciende = sm.getCommand() == ShortMessage.NOTE_ON
                        && sm.getData2() > 0;
                boolean apaga = sm.getCommand() == ShortMessage.NOTE_OFF
                        || (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0);

                if (enciende) {
                    // Si esa misma nota ya estaba abierta es que se vuelve a
                    // pulsar sin haberla soltado: se cierra aqui la anterior.
                    // Sobrescribir sin mas perdia una nota por cada repique.
                    if (inicio[canal][altura] >= 0) {
                        notas.add(crear(tempo, canal, altura,
                                velocidad[canal][altura],
                                inicio[canal][altura], evento.getTick()));
                    }
                    inicio[canal][altura] = evento.getTick();
                    velocidad[canal][altura] = sm.getData2();
                } else if (apaga && inicio[canal][altura] >= 0) {
                    notas.add(crear(tempo, canal, altura, velocidad[canal][altura],
                            inicio[canal][altura], evento.getTick()));
                    inicio[canal][altura] = -1;
                }
            }
        }

        // Notas que se quedaron sin NOTE_OFF: se cierran al final de la cancion
        long finTick = secuencia.getTickLength();
        for (int canal = 0; canal < 16; canal++) {
            for (int altura = 0; altura < 128; altura++) {
                if (inicio[canal][altura] >= 0) {
                    notas.add(crear(tempo, canal, altura, velocidad[canal][altura],
                            inicio[canal][altura], finTick));
                }
            }
        }
        long finUs = tempo.aMicrosegundos(finTick);

        notas.sort(Comparator.comparingLong(NotaMidi::inicioUs));
        return new Partitura(notas, finUs);
    }

    /** Una nota a partir de sus pulsos de inicio y fin. */
    private static NotaMidi crear(MapaTempo tempo, int canal, int altura,
                                  int velocidad, long tickInicio, long tickFin) {
        long desde = tempo.aMicrosegundos(tickInicio);
        long hasta = tempo.aMicrosegundos(tickFin);
        // Suelo de 1 ms: hay ficheros que apagan la nota en el mismo pulso en
        // que la encienden, y una barra de alto cero no se veria.
        return new NotaMidi(canal, altura, velocidad, desde,
                Math.max(1000, hasta - desde));
    }

    /**
     * Traduce pulsos a microsegundos teniendo en cuenta los cambios de tempo.
     */
    private static final class MapaTempo {

        private final long[] ticks;
        private final long[] usAcumulados;
        private final double[] usPorTick;

        MapaTempo(Sequence secuencia) {
            int resolucion = Math.max(1, secuencia.getResolution());
            boolean porPulsos = secuencia.getDivisionType() == Sequence.PPQ;

            List<long[]> cambios = new ArrayList<>();
            if (porPulsos) {
                for (Track pista : secuencia.getTracks()) {
                    for (int i = 0; i < pista.size(); i++) {
                        MidiEvent evento = pista.get(i);
                        if (evento.getMessage() instanceof MetaMessage mm
                                && mm.getType() == META_CAMBIO_DE_TEMPO
                                && mm.getData().length >= 3) {
                            byte[] d = mm.getData();
                            long usPorPulso = ((d[0] & 0xFFL) << 16)
                                    | ((d[1] & 0xFFL) << 8) | (d[2] & 0xFFL);
                            cambios.add(new long[]{evento.getTick(), usPorPulso});
                        }
                    }
                }
            }
            cambios.sort(Comparator.comparingLong(c -> c[0]));

            // Siempre hay un tramo inicial: 120 pulsos por minuto si el fichero
            // no dice otra cosa antes del primer evento
            List<long[]> tramos = new ArrayList<>();
            if (cambios.isEmpty() || cambios.get(0)[0] > 0) {
                tramos.add(new long[]{0, US_POR_PULSO_POR_DEFECTO});
            }
            tramos.addAll(cambios);

            this.ticks = new long[tramos.size()];
            this.usAcumulados = new long[tramos.size()];
            this.usPorTick = new double[tramos.size()];

            double acumulado = 0;
            for (int i = 0; i < tramos.size(); i++) {
                long tick = tramos.get(i)[0];
                double porTick = porPulsos
                        ? (double) tramos.get(i)[1] / resolucion
                        : 1_000_000.0 / (secuencia.getDivisionType() * resolucion);
                if (i > 0) {
                    acumulado += (tick - this.ticks[i - 1]) * this.usPorTick[i - 1];
                }
                this.ticks[i] = tick;
                this.usAcumulados[i] = (long) acumulado;
                this.usPorTick[i] = porTick;
            }
        }

        long aMicrosegundos(long tick) {
            int i = this.ticks.length - 1;
            while (i > 0 && this.ticks[i] > tick) {
                i--;
            }
            return this.usAcumulados[i]
                    + (long) ((tick - this.ticks[i]) * this.usPorTick[i]);
        }
    }
}
