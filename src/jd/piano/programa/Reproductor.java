package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import java.io.File;

/**
 * Reproduce un MIDI y va encendiendo las teclas del piano.
 *
 * Antes esto era un metodo que llamaba a sequencer.start() y se quedaba
 * durmiendo la duracion entera de la cancion: no se podia pausar, ni buscar,
 * ni cambiar el tempo, ni saber por donde iba. Ahora no bloquea y expone el
 * transporte completo.
 */
public class Reproductor implements Receiver {

    /** El canal 10 en General MIDI es percusion: sus notas no son alturas. */
    private static final int CANAL_PERCUSION = 9;

    /** Controladores que apagan todo lo que este sonando. */
    private static final int CC_TODAS_LAS_NOTAS_OFF = 123;
    private static final int CC_TODO_EL_SONIDO_OFF = 120;

    private Sequencer secuenciador;
    private Transmitter transmisor;
    private Piano piano;
    private long duracionUs;

    /** Notas que han sonado, para el contador de la barra. */
    private int notasTocadas;

    /** Canales silenciados: no suenan ni encienden teclas. */
    private final boolean[] silenciado = new boolean[16];

    /**
     * El sintetizador donde suena la cancion. No lo abre este objeto: se le
     * pasa el que ya esta abierto, para que solo haya un dueno del dispositivo.
     */
    private Synthesizer sintetizador;

    public Reproductor() {
        this.secuenciador = null;
        this.transmisor = null;
        this.piano = null;
        this.duracionUs = 0;
        this.sintetizador = null;
        this.notasTocadas = 0;
    }

    public void setSintetizador(Synthesizer sintetizador) {
        this.sintetizador = sintetizador;
        for (int canal = 0; canal < this.silenciado.length; canal++) {
            aplicarSilencio(canal);
        }
    }

    /** Silencia o reactiva un canal, en sonido y en imagen. */
    public void setSilenciado(int canal, boolean silencio) {
        if (canal < 0 || canal >= this.silenciado.length) {
            return;
        }
        this.silenciado[canal] = silencio;
        aplicarSilencio(canal);
        if (silencio && this.piano != null) {
            // Las teclas que estuvieran encendidas de ese canal se apagan ya
            this.piano.soltarTodas();
        }
    }

    public boolean estaSilenciado(int canal) {
        return canal >= 0 && canal < this.silenciado.length && this.silenciado[canal];
    }

    private void aplicarSilencio(int canal) {
        if (this.sintetizador != null) {
            MidiChannel[] canales = this.sintetizador.getChannels();
            if (canal < canales.length && canales[canal] != null) {
                canales[canal].setMute(this.silenciado[canal]);
            }
        }
    }

    public void conectar(Piano piano) {
        this.piano = piano;
    }

    /** Deja una cancion lista para sonar, sin arrancarla. */
    public void cargar(String ruta) throws Exception {
        cerrar();
        Sequence secuencia = MidiSystem.getSequence(new File(ruta));
        this.secuenciador = MidiSystem.getSequencer();
        this.secuenciador.open();
        this.secuenciador.setSequence(secuencia);
        this.transmisor = this.secuenciador.getTransmitter();
        this.transmisor.setReceiver(this);
        this.duracionUs = secuencia.getMicrosecondLength();
        this.notasTocadas = 0;
    }

    /** Cuantas notas han sonado desde que se cargo la cancion. */
    public int getNotasTocadas() {
        return this.notasTocadas;
    }

    public boolean hayCancion() {
        return this.secuenciador != null;
    }

    public void reproducir() {
        if (this.secuenciador != null && !this.secuenciador.isRunning()) {
            this.secuenciador.start();
        }
    }

    public void pausar() {
        if (this.secuenciador != null && this.secuenciador.isRunning()) {
            this.secuenciador.stop();
            apagarTeclado();
        }
    }

    public void alternarPausa() {
        if (estaSonando()) {
            pausar();
        } else {
            reproducir();
        }
    }

    public void parar() {
        if (this.secuenciador != null) {
            this.secuenciador.stop();
            this.secuenciador.setMicrosecondPosition(0);
            apagarTeclado();
        }
    }

    /** Salta a un punto de la cancion, en microsegundos. */
    public void buscar(long microsegundos) {
        if (this.secuenciador != null) {
            long destino = Math.max(0, Math.min(this.duracionUs, microsegundos));
            this.secuenciador.setMicrosecondPosition(destino);
            apagarTeclado();
        }
    }

    public void setFactorTempo(float factor) {
        if (this.secuenciador != null) {
            this.secuenciador.setTempoFactor(Math.max(0.25f, Math.min(3f, factor)));
        }
    }

    public float getFactorTempo() {
        return this.secuenciador == null ? 1f : this.secuenciador.getTempoFactor();
    }

    public long getPosicionUs() {
        return this.secuenciador == null ? 0 : this.secuenciador.getMicrosecondPosition();
    }

    public long getDuracionUs() {
        return this.duracionUs;
    }

    public boolean estaSonando() {
        return this.secuenciador != null && this.secuenciador.isRunning();
    }

    /** Si ha llegado al final. Sirve para no dejar la barra clavada al 99%. */
    public boolean haTerminado() {
        return this.secuenciador != null && !this.secuenciador.isRunning()
                && getPosicionUs() >= this.duracionUs;
    }

    public void cerrar() {
        if (this.transmisor != null) {
            this.transmisor.close();
            this.transmisor = null;
        }
        if (this.secuenciador != null) {
            this.secuenciador.stop();
            this.secuenciador.close();
            this.secuenciador = null;
        }
        apagarTeclado();
    }

    private void apagarTeclado() {
        if (this.piano != null) {
            this.piano.soltarTodas();
        }
    }

    @Override
    public void send(MidiMessage mensaje, long marcaDeTiempo) {
        if (this.piano == null || !(mensaje instanceof ShortMessage sm)) {
            return;
        }
        int canal = sm.getChannel();
        if (canal == CANAL_PERCUSION || this.silenciado[canal]) {
            return;
        }
        int comando = sm.getCommand();

        // Muchos MIDI apagan las notas con este controlador en vez de con
        // NOTE_OFF. Sin atenderlo se quedaban teclas encendidas para siempre.
        if (comando == ShortMessage.CONTROL_CHANGE) {
            int cc = sm.getData1();
            if (cc == CC_TODAS_LAS_NOTAS_OFF || cc == CC_TODO_EL_SONIDO_OFF) {
                apagarTeclado();
            }
            return;
        }

        Tecla tecla = this.piano.getTecla(canal, sm.getData1());
        if (tecla == null) {
            return;
        }
        if (comando == ShortMessage.NOTE_ON) {
            int velocidad = sm.getData2();
            if (velocidad > 0) {
                this.notasTocadas++;
                tecla.setColorPulsado(Estilo.colorDeCanal(canal));
                tecla.pulsar(velocidad);
            } else {
                tecla.soltar();
            }
        } else if (comando == ShortMessage.NOTE_OFF) {
            tecla.soltar();
        }
    }

    @Override
    public void close() {
        cerrar();
    }
}
