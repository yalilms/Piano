package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.instrumentos.InstrumentoVisual;

import javax.sound.midi.InvalidMidiDataException;
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

    private static final int CANALES = 16;

    /** Controladores que apagan todo lo que este sonando. */
    private static final int CC_TODAS_LAS_NOTAS_OFF = 123;
    private static final int CC_TODO_EL_SONIDO_OFF = 120;

    /** El pedal de resonancia. */
    private static final int CC_SOSTENIDO = 64;

    /** Devuelve los controladores del canal a sus valores de partida. */
    private static final int CC_REINICIAR_CONTROLADORES = 121;

    /** La rueda de afinacion tiene 14 bits: el centro es la mitad. */
    private static final int AFINACION_CENTRADA = 8192;

    private Sequencer secuenciador;
    private Transmitter transmisor;
    private long duracionUs;

    /*
     * De aqui abajo, todo lo que cruza entre el hilo de Swing y el del
     * secuenciador va volatile. Swing los escribe al cargar una cancion o al
     * tocar un control; el secuenciador los lee en cada mensaje MIDI, que son
     * cientos por segundo.
     */

    private volatile InstrumentoVisual instrumento;

    /** Notas que han sonado, para el contador de la barra. */
    private volatile int notasTocadas;

    /**
     * Canales silenciados, un bit por canal. Es una mascara y no un array
     * porque los elementos de un array no pueden ser volatiles, y con un int
     * cabe entero: dieciseis canales, dieciseis bits.
     */
    private volatile int silenciados;

    /**
     * El sintetizador donde suena la cancion. No lo abre este objeto: se le
     * pasa el que ya esta abierto, para que solo haya un dueno del dispositivo.
     */
    private volatile Synthesizer sintetizador;

    /**
     * Por donde salen los mensajes hacia el sintetizador. Antes el secuenciador
     * iba conectado por su cuenta y no se podia intervenir lo que llegaba;
     * ahora pasa todo por aqui, y eso es lo que permite cambiar el instrumento
     * de un canal o cortarlo de verdad.
     */
    private volatile Receiver salidaAlSintetizador;

    /** Instrumento forzado por canal, o -1 si manda la cancion. */
    private final java.util.concurrent.atomic.AtomicIntegerArray programaForzado =
            new java.util.concurrent.atomic.AtomicIntegerArray(16);

    private volatile Bateria bateria;

    /** Tope de la transposicion, en semitonos: dos octavas a cada lado. */
    public static final int TONO_MAXIMO = 12;

    /** Un bucle mas corto que esto no sirve para practicar y solo da tirones. */
    private static final long BUCLE_MINIMO_US = 500_000;

    /*
     * Los lee el hilo del secuenciador en cada mensaje y los escribe el de
     * Swing cuando se toca el control, asi que van volatiles.
     */
    private volatile int semitonos;

    /** -1 en inicio: sin bucle. -1 solo en fin: la A puesta, falta la B. */
    private volatile long bucleInicioUs;
    private volatile long bucleFinUs;


    public Reproductor() {
        this.secuenciador = null;
        this.transmisor = null;
        this.instrumento = null;
        this.duracionUs = 0;
        this.sintetizador = null;
        this.notasTocadas = 0;
        this.salidaAlSintetizador = null;
        this.bateria = null;
        this.semitonos = 0;
        this.silenciados = 0;
        this.bucleInicioUs = -1;
        this.bucleFinUs = -1;
        for (int canal = 0; canal < 16; canal++) {
            this.programaForzado.set(canal, -1);
        }
    }

    /**
     * Cuantos semitonos se sube o baja la cancion. Sirve para pasarla a un tono
     * mas facil de tocar, o para meterla en el rango de un instrumento que no
     * le llega: una linea de bajo subida una octava cabe en una guitarra.
     *
     * La percusion no se transpone: en el canal 10 la nota es una pieza, no una
     * altura, y subirla convertiria el bombo en un platillo.
     */
    public void setTransposicion(int semitonos) {
        int nuevo = Math.max(-TONO_MAXIMO, Math.min(TONO_MAXIMO, semitonos));
        if (nuevo == this.semitonos) {
            return;
        }
        // Lo que este sonando se encendio con el tono viejo, asi que su
        // NOTE_OFF llegaria transpuesto de otra forma y no apagaria nada
        apagarNotas();
        this.semitonos = nuevo;
    }

    public int getTransposicion() {
        return this.semitonos;
    }

    /** La nota tal y como se ve y se oye, ya transpuesta. */
    public int notaMostrada(int nota) {
        return nota + this.semitonos;
    }

    // ---------- bucle para practicar un trozo ----------

    /**
     * Marca el bucle en tres pasos con el mismo boton: la primera vez pone la
     * A donde va la cancion, la segunda la B, y la tercera lo quita.
     *
     * Si la B cae antes que la A o demasiado cerca, se entiende que uno se ha
     * pasado y se recoloca la A ahi, en vez de dejar un bucle inservible.
     */
    public void marcarBucle() {
        long ahora = getPosicionUs();
        if (this.bucleInicioUs < 0) {
            this.bucleInicioUs = ahora;
            this.bucleFinUs = -1;
        } else if (this.bucleFinUs < 0) {
            if (ahora > this.bucleInicioUs + BUCLE_MINIMO_US) {
                this.bucleFinUs = ahora;
            } else {
                this.bucleInicioUs = ahora;
            }
        } else {
            quitarBucle();
        }
    }

    public void quitarBucle() {
        this.bucleInicioUs = -1;
        this.bucleFinUs = -1;
    }

    /** Si hay un bucle completo, con sus dos extremos puestos. */
    public boolean tieneBucle() {
        return this.bucleInicioUs >= 0 && this.bucleFinUs > this.bucleInicioUs;
    }

    /** Si esta puesta la A y falta la B. */
    public boolean esperaFinDeBucle() {
        return this.bucleInicioUs >= 0 && this.bucleFinUs < 0;
    }

    public long getBucleInicioUs() {
        return this.bucleInicioUs;
    }

    public long getBucleFinUs() {
        return this.bucleFinUs;
    }

    /**
     * Vuelve al principio del bucle si la cancion ya ha pasado del final.
     *
     * Lo llama el temporizador de los controles y no el propio send: buscar
     * para y arranca el secuenciador, y hacerlo desde dentro del hilo que esta
     * entregando los mensajes es pedir un bloqueo.
     */
    public void atenderBucle() {
        if (tieneBucle() && estaSonando() && getPosicionUs() >= this.bucleFinUs) {
            buscar(this.bucleInicioUs);
        }
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
        for (int canal = 0; canal < CANALES; canal++) {
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
        if (canal < 0 || canal >= CANALES) {
            return;
        }
        this.programaForzado.set(canal, programa);
        if (programa >= 0) {
            enviarCambioDePrograma(canal, programa);
        }
    }

    public int getProgramaForzado(int canal) {
        return canal >= 0 && canal < CANALES
                ? this.programaForzado.get(canal) : -1;
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
        if (canal < 0 || canal >= CANALES) {
            return;
        }
        this.silenciados = silencio
                ? this.silenciados | (1 << canal)
                : this.silenciados & ~(1 << canal);
        aplicarSilencio(canal);
        if (silencio && this.instrumento != null) {
            // Solo las de ese canal: soltarTodas apagaba tambien las de los demas
            this.instrumento.soltarCanal(canal);
        }
    }

    public boolean estaSilenciado(int canal) {
        return canal >= 0 && canal < CANALES
                && (this.silenciados & (1 << canal)) != 0;
    }

    private void aplicarSilencio(int canal) {
        if (this.sintetizador != null) {
            MidiChannel[] canales = this.sintetizador.getChannels();
            if (canal < canales.length && canales[canal] != null) {
                canales[canal].setMute(estaSilenciado(canal));
            }
        }
    }

    public void conectar(InstrumentoVisual instrumento) {
        this.instrumento = instrumento;
    }

    /** Deja una cancion lista para sonar, sin arrancarla. */
    /**
     * Corta todo lo que este sonando, en el teclado y en el sintetizador. Hace
     * falta antes de cambiar el tono: si no, las notas abiertas no encuentran
     * su apagado.
     */
    /**
     * Corta lo que este sonando: las teclas dibujadas y las voces del
     * sintetizador.
     *
     * Lo segundo es lo que faltaba. Apagar las teclas solo cambia el dibujo; la
     * nota la sigue sosteniendo el sintetizador hasta que le llega su NOTE_OFF,
     * y si se para el secuenciador en medio de un acorde ese NOTE_OFF no llega
     * nunca. Por eso al cambiar de cancion se quedaba una nota sonando para
     * siempre y habia que darle a Parar antes.
     *
     * Se silencia mandando mensajes MIDI de verdad por la misma salida por la
     * que va la cancion, y no llamando a los metodos de MidiChannel. Medido:
     * con Gervill, el sintetizador que trae Java, allSoundOff y hasta noteOff
     * sobre el MidiChannel no retiran la voz; el controlador 120 mandado por el
     * receptor si. Y de paso funciona igual cuando la salida no es un
     * Synthesizer que tengamos a mano, sino el dispositivo por defecto.
     */
    private void apagarNotas() {
        apagarTeclado();
        for (int canal = 0; canal < CANALES; canal++) {
            enviarControlador(canal, CC_TODO_EL_SONIDO_OFF, 0);
            enviarControlador(canal, CC_TODAS_LAS_NOTAS_OFF, 0);
        }
        // Por si la salida ya esta cerrada: no retira voces en Gervill, pero es
        // lo unico que queda cuando no hay receptor y no cuesta nada
        Synthesizer s = this.sintetizador;
        if (s != null) {
            for (MidiChannel canal : s.getChannels()) {
                if (canal != null) {
                    canal.allSoundOff();
                }
            }
        }
    }

    /**
     * Deja los dieciseis canales como recien abiertos, ademas de cortar las
     * notas. Solo entre canciones.
     *
     * Sin esto una cancion le dejaba a la siguiente el pedal pisado y la rueda
     * de afinacion donde la hubiera puesto, y la siguiente sonaba desafinada
     * sin que nada lo explicara. No se hace al pausar ni al buscar: ahi la
     * cancion sigue siendo la misma y su volumen, su panorama y su pedal tienen
     * que quedarse como estaban.
     */
    private void reiniciarCanales() {
        apagarNotas();
        for (int canal = 0; canal < CANALES; canal++) {
            enviarControlador(canal, CC_SOSTENIDO, 0);
            enviarControlador(canal, CC_REINICIAR_CONTROLADORES, 0);
            enviarAfinacionCentrada(canal);
        }
        Synthesizer s = this.sintetizador;
        if (s != null) {
            for (MidiChannel canal : s.getChannels()) {
                if (canal != null) {
                    canal.resetAllControllers();
                    canal.controlChange(CC_SOSTENIDO, 0);
                    canal.setPitchBend(AFINACION_CENTRADA);
                }
            }
        }
    }

    /** Un cambio de controlador por la salida, si sigue abierta. */
    private void enviarControlador(int canal, int controlador, int valor) {
        Receiver salida = this.salidaAlSintetizador;
        if (salida == null) {
            return;
        }
        try {
            ShortMessage mensaje = new ShortMessage();
            mensaje.setMessage(ShortMessage.CONTROL_CHANGE, canal, controlador,
                    valor);
            salida.send(mensaje, -1);
        } catch (InvalidMidiDataException | IllegalStateException error) {
            // La salida ya estaba cerrada: no hay nada que silenciar por ahi
        }
    }

    /** La rueda de afinacion al centro: 8192 en catorce bits. */
    private void enviarAfinacionCentrada(int canal) {
        Receiver salida = this.salidaAlSintetizador;
        if (salida == null) {
            return;
        }
        try {
            ShortMessage mensaje = new ShortMessage();
            mensaje.setMessage(ShortMessage.PITCH_BEND, canal,
                    AFINACION_CENTRADA & 0x7F, AFINACION_CENTRADA >> 7);
            salida.send(mensaje, -1);
        } catch (InvalidMidiDataException | IllegalStateException error) {
            // idem
        }
    }

    public void cargar(String ruta) throws Exception {
        // El bucle de la cancion anterior no significa nada en esta
        quitarBucle();
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
        this.silenciados = 0;
        for (int canal = 0; canal < CANALES; canal++) {
            aplicarSilencio(canal);
            this.programaForzado.set(canal, -1);
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
            // Se cortan las voces, no solo las teclas: en pausa no puede quedar
            // un acorde sonando. Los controladores se dejan como estan, que la
            // cancion es la misma y al seguir tiene que sonar igual.
            apagarNotas();
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
            // Parar es acabar con esta cancion: los canales quedan a cero
            reiniciarCanales();
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
        apagarNotas();
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
        // Primero se para y se silencia, y despues se cierra. Al reves no
        // servia de nada: cerrando el transmisor se corta el camino por el que
        // habria que mandar el apagado, y la nota se quedaba sonando.
        if (this.secuenciador != null) {
            this.secuenciador.stop();
        }
        reiniciarCanales();

        if (this.transmisor != null) {
            this.transmisor.close();
            this.transmisor = null;
        }
        if (this.secuenciador != null) {
            this.secuenciador.close();
            this.secuenciador = null;
        }
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
        if (estaSilenciado(canal) && !apaga) {
            return;
        }

        // Si el usuario ha forzado el instrumento de este canal, el cambio de
        // programa de la cancion no debe pisarlo
        if (comando == ShortMessage.PROGRAM_CHANGE
                && this.programaForzado.get(canal) >= 0) {
            enviarCambioDePrograma(canal, this.programaForzado.get(canal));
            return;
        }

        ShortMessage transpuesto = transponer(sm, comando);
        if (transpuesto == null) {
            // La transposicion la saca del rango MIDI: se descarta entera, el
            // encendido y el apagado, para no dejar notas colgadas
            return;
        }
        reenviar(transpuesto, marcaDeTiempo);
        actualizarTeclado(transpuesto, canal, comando);
    }

    /**
     * La misma nota subida o bajada. Devuelve el mensaje original cuando no hay
     * nada que transponer, y null si la nota se sale de las 128 del MIDI.
     */
    private ShortMessage transponer(ShortMessage sm, int comando) {
        boolean esNota = comando == ShortMessage.NOTE_ON
                || comando == ShortMessage.NOTE_OFF;
        if (this.semitonos == 0 || !esNota) {
            return sm;
        }
        int nota = sm.getData1() + this.semitonos;
        if (nota < 0 || nota > 127) {
            return null;
        }
        try {
            ShortMessage nuevo = new ShortMessage();
            nuevo.setMessage(comando, sm.getChannel(), nota, sm.getData2());
            return nuevo;
        } catch (javax.sound.midi.InvalidMidiDataException error) {
            return null;
        }
    }

    /**
     * En el canal 10 la nota es una pieza de bateria, no una altura, asi que va
     * a los pads y no al piano. Antes se descartaba: eran el 35% de las notas.
     */
    private void atenderPercusion(ShortMessage sm, long marcaDeTiempo) {
        if (estaSilenciado(CANAL_PERCUSION)
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
