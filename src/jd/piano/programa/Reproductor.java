package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.instrumentos.InstrumentoVisual;

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
    private InstrumentoVisual instrumento;
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

    /**
     * Por donde salen los mensajes hacia el sintetizador. Antes el secuenciador
     * iba conectado por su cuenta y no se podia intervenir lo que llegaba;
     * ahora pasa todo por aqui, y eso es lo que permite cambiar el instrumento
     * de un canal o cortarlo de verdad.
     */
    private Receiver salidaAlSintetizador;

    /** Instrumento forzado por canal, o -1 si manda la cancion. */
    private final int[] programaForzado = new int[16];

    private Bateria bateria;

    public Reproductor() {
        this.secuenciador = null;
        this.transmisor = null;
        this.instrumento = null;
        this.duracionUs = 0;
        this.sintetizador = null;
        this.notasTocadas = 0;
        this.salidaAlSintetizador = null;
        this.bateria = null;
        java.util.Arrays.fill(this.programaForzado, -1);
    }

    public void setSintetizador(Synthesizer sintetizador) {
        this.sintetizador = sintetizador;
        this.salidaAlSintetizador = null;
        if (sintetizador != null) {
            try {
                this.salidaAlSintetizador = sintetizador.getReceiver();
            } catch (Exception error) {
                this.salidaAlSintetizador = null;
            }
        }
        for (int canal = 0; canal < this.silenciado.length; canal++) {
            aplicarSilencio(canal);
        }
    }

    /** Donde se pintan los golpes de bateria del canal 10. */
    public void setBateria(Bateria bateria) {
        this.bateria = bateria;
    }

    /**
     * Fuerza el instrumento de un canal, o lo devuelve a lo que diga la
     * cancion con -1. Se reaplica cada vez que el fichero manda su propio
     * cambio de programa, que si no lo pisaria.
     */
    public void setProgramaDeCanal(int canal, int programa) {
        if (canal < 0 || canal >= this.programaForzado.length) {
            return;
        }
        this.programaForzado[canal] = programa;
        if (programa >= 0) {
            enviarCambioDePrograma(canal, programa);
        }
    }

    public int getProgramaForzado(int canal) {
        return canal >= 0 && canal < this.programaForzado.length
                ? this.programaForzado[canal] : -1;
    }

    private void enviarCambioDePrograma(int canal, int programa) {
        if (this.sintetizador != null) {
            MidiChannel[] canales = this.sintetizador.getChannels();
            if (canal < canales.length && canales[canal] != null) {
                canales[canal].programChange(programa);
            }
        }
    }

    /** Silencia o reactiva un canal, en sonido y en imagen. */
    public void setSilenciado(int canal, boolean silencio) {
        if (canal < 0 || canal >= this.silenciado.length) {
            return;
        }
        this.silenciado[canal] = silencio;
        aplicarSilencio(canal);
        if (silencio && this.instrumento != null) {
            // Solo las de ese canal: soltarTodas apagaba tambien las de los demas
            this.instrumento.soltarCanal(canal);
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

    public void conectar(InstrumentoVisual instrumento) {
        this.instrumento = instrumento;
    }

    /** Deja una cancion lista para sonar, sin arrancarla. */
    public void cargar(String ruta) throws Exception {
        cerrar();
        Sequence secuencia = MidiSystem.getSequence(new File(ruta));
        // Sin conectar al dispositivo por defecto: los mensajes pasan por este
        // receptor y de aqui al sintetizador, para poder intervenirlos. Si no
        // hay sintetizador se pide conectado, o no sonaria nada.
        this.secuenciador = MidiSystem.getSequencer(this.salidaAlSintetizador == null);
        this.secuenciador.open();
        this.secuenciador.setSequence(secuencia);
        this.transmisor = this.secuenciador.getTransmitter();
        this.transmisor.setReceiver(this);
        this.duracionUs = secuencia.getMicrosecondLength();
        this.notasTocadas = 0;
        // Los silenciados no se arrastran de una cancion a otra: el panel de
        // canales se rehace con todo activado y quedarian descuadrados.
        for (int canal = 0; canal < this.silenciado.length; canal++) {
            this.silenciado[canal] = false;
            aplicarSilencio(canal);
            this.programaForzado[canal] = -1;
        }
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

    /**
     * Salta a un punto de la cancion, en microsegundos.
     *
     * Hay que pararlo, mover la posicion y volver a arrancarlo: el
     * secuenciador de Java ignora setMicrosecondPosition mientras esta en
     * marcha, asi que pinchar en la barra de progreso no hacia nada.
     */
    public void buscar(long microsegundos) {
        if (this.secuenciador == null) {
            return;
        }
        long destino = Math.max(0, Math.min(this.duracionUs, microsegundos));
        boolean sonaba = this.secuenciador.isRunning();
        if (sonaba) {
            this.secuenciador.stop();
        }
        this.secuenciador.setMicrosecondPosition(destino);
        apagarTeclado();
        if (sonaba) {
            this.secuenciador.start();
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
        if (this.instrumento != null) {
            this.instrumento.soltarTodas();
        }
        if (this.bateria != null) {
            this.bateria.apagarTodo();
        }
    }

    @Override
    public void send(MidiMessage mensaje, long marcaDeTiempo) {
        if (!(mensaje instanceof ShortMessage sm)) {
            reenviar(mensaje, marcaDeTiempo);
            return;
        }
        int canal = sm.getChannel();
        int comando = sm.getCommand();

        if (canal == CANAL_PERCUSION) {
            atenderPercusion(sm, marcaDeTiempo);
            return;
        }

        // Un canal silenciado no enciende nada, pero los apagados si pasan: si
        // no, una nota que estuviera sonando al silenciarlo se quedaria
        // encendida para siempre.
        boolean apaga = comando == ShortMessage.NOTE_OFF
                || (comando == ShortMessage.NOTE_ON && sm.getData2() == 0);
        if (this.silenciado[canal] && !apaga) {
            return;
        }

        // Si el usuario ha forzado el instrumento de este canal, el cambio de
        // programa de la cancion no debe pisarlo
        if (comando == ShortMessage.PROGRAM_CHANGE
                && this.programaForzado[canal] >= 0) {
            enviarCambioDePrograma(canal, this.programaForzado[canal]);
            return;
        }

        reenviar(mensaje, marcaDeTiempo);
        actualizarTeclado(sm, canal, comando);
    }

    /**
     * En el canal 10 la nota es una pieza de bateria, no una altura, asi que va
     * a los pads y no al piano. Antes se descartaba: eran el 35% de las notas.
     */
    private void atenderPercusion(ShortMessage sm, long marcaDeTiempo) {
        if (this.silenciado[CANAL_PERCUSION]
                && sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0) {
            return;
        }
        reenviar(sm, marcaDeTiempo);
        if (this.bateria != null && sm.getCommand() == ShortMessage.NOTE_ON
                && sm.getData2() > 0) {
            this.notasTocadas++;
            this.bateria.golpear(sm.getData1(), sm.getData2());
        }
    }

    private void actualizarTeclado(ShortMessage sm, int canal, int comando) {
        if (this.instrumento == null) {
            return;
        }
        // Muchos MIDI apagan las notas con este controlador en vez de con
        // NOTE_OFF. Sin atenderlo se quedaban teclas encendidas para siempre.
        if (comando == ShortMessage.CONTROL_CHANGE) {
            int cc = sm.getData1();
            if (cc == CC_TODAS_LAS_NOTAS_OFF || cc == CC_TODO_EL_SONIDO_OFF) {
                apagarTeclado();
            }
            return;
        }

        // El instrumento decide donde cae la nota y con que color: en un piano
        // es una tecla, en el mastil un traste. Si no la representa, se pierde.
        if (comando == ShortMessage.NOTE_ON) {
            int velocidad = sm.getData2();
            if (velocidad > 0) {
                if (this.instrumento.pulsar(canal, sm.getData1(), velocidad)) {
                    this.notasTocadas++;
                }
            } else {
                this.instrumento.soltar(canal, sm.getData1());
            }
        } else if (comando == ShortMessage.NOTE_OFF) {
            this.instrumento.soltar(canal, sm.getData1());
        }
    }

    /** Deja pasar el mensaje hacia el sintetizador, si lo hay. */
    private void reenviar(MidiMessage mensaje, long marcaDeTiempo) {
        if (this.salidaAlSintetizador != null) {
            this.salidaAlSintetizador.send(mensaje, marcaDeTiempo);
        }
    }

    @Override
    public void close() {
        cerrar();
    }
}
