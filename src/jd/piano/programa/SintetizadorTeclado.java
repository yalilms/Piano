package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.instrumentos.InstrumentoVisual;
import jd.piano.instrumentos.PiezaPulsable;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Permite tocar el piano con el teclado del ordenador y con el raton.
 *
 * Reparte dos filas del teclado como si fueran dos octavas, igual que hacen
 * los secuenciadores: las letras de abajo son las blancas y las de encima las
 * negras.
 *
 * Suena por el canal 16 (indice 15) a proposito: es el que menos usan los
 * ficheros MIDI, asi que tocar encima de una cancion no le cambia el
 * instrumento a ninguna de sus pistas.
 */
public class SintetizadorTeclado {

    /** Canal reservado para lo que toca el usuario. */
    public static final int CANAL = 15;

    /** Controlador del pedal de resonancia en MIDI. */
    private static final int CC_SOSTENIDO = 64;

    private static final int NOTA_BASE = 60;
    private static final int VELOCIDAD = 96;
    private static final int OCTAVA_MINIMA = -3;
    private static final int OCTAVA_MAXIMA = 3;

    /** Tecla del ordenador a semitono, contando desde NOTA_BASE. */
    private static final Map<Integer, Integer> MAPA = new HashMap<>();

    static {
        // Fila de abajo: una octava
        int[] abajo = {KeyEvent.VK_Z, KeyEvent.VK_S, KeyEvent.VK_X, KeyEvent.VK_D,
                KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_G, KeyEvent.VK_B,
                KeyEvent.VK_H, KeyEvent.VK_N, KeyEvent.VK_J, KeyEvent.VK_M};
        int[] semitonosAbajo = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        for (int i = 0; i < abajo.length; i++) {
            MAPA.put(abajo[i], semitonosAbajo[i]);
        }
        // Fila de arriba: la octava siguiente
        int[] arriba = {KeyEvent.VK_Q, KeyEvent.VK_2, KeyEvent.VK_W, KeyEvent.VK_3,
                KeyEvent.VK_E, KeyEvent.VK_R, KeyEvent.VK_5, KeyEvent.VK_T,
                KeyEvent.VK_6, KeyEvent.VK_Y, KeyEvent.VK_7, KeyEvent.VK_U,
                KeyEvent.VK_I};
        for (int i = 0; i < arriba.length; i++) {
            MAPA.put(arriba[i], 12 + i);
        }
    }

    private Synthesizer sintetizador;
    private MidiChannel canal;
    private InstrumentoVisual instrumento;
    private int octava;
    private int programa;
    private int banco;
    private boolean sostenido;

    /** Banco de sonidos externo, si se ha cargado alguno. */
    private javax.sound.midi.Soundbank bancoCargado;

    /** Que nota esta sonando por cada tecla del ordenador pulsada. */
    private final Map<Integer, Integer> sonando = new HashMap<>();

    public SintetizadorTeclado() {
        this.sintetizador = null;
        this.canal = null;
        this.instrumento = null;
        this.octava = 0;
        this.programa = 0;
        this.sostenido = false;
        this.banco = 0;
        this.bancoCargado = null;
    }

    public void conectar(InstrumentoVisual instrumento) {
        this.instrumento = instrumento;
    }

    /**
     * Abre el sintetizador. Si no se puede, el teclado sigue funcionando pero
     * solo se vera en pantalla, sin sonar.
     */
    public void abrir() {
        try {
            this.sintetizador = MidiSystem.getSynthesizer();
            this.sintetizador.open();
            this.canal = this.sintetizador.getChannels()[CANAL];
            this.canal.programChange(this.programa);
        } catch (Exception error) {
            System.out.println("Aviso: sin sintetizador, el teclado no sonara ("
                    + error.getMessage() + ")");
            this.sintetizador = null;
            this.canal = null;
        }
    }

    public void cerrar() {
        soltarTodo();
        if (this.sintetizador != null) {
            this.sintetizador.close();
            this.sintetizador = null;
            this.canal = null;
        }
    }

    /**
     * Con que instrumento se toca, dentro del banco de siempre.
     * Es lo mismo que setSonido con el banco 0.
     */
    public void setPrograma(int programa) {
        setSonido(0, programa);
    }

    /**
     * Con que instrumento se toca, diciendo tambien de que banco.
     *
     * El banco es lo que abre la puerta a todo lo que no son los 128 del
     * estandar: las variaciones y los efectos de sonido del GS estan en los
     * bancos 128, 256, 384 y siguientes. En MIDI el banco son catorce bits, y se
     * pasa tal cual lo da el sintetizador.
     */
    public void setSonido(int banco, int programa) {
        this.banco = Math.max(0, banco);
        this.programa = Math.max(0, Math.min(127, programa));
        soltarTodo();
        if (this.canal != null) {
            this.canal.programChange(this.banco, this.programa);
        }
    }

    public int getPrograma() {
        return this.programa;
    }

    public int getBanco() {
        return this.banco;
    }

    /**
     * Carga un banco de sonidos externo y deja puesto el sonido equivalente.
     *
     * Se descarga el que se hubiera cargado antes, que si no cada fichero se
     * queda en memoria para siempre. El que trae Java no se toca: es el que
     * queda debajo si el nuevo no cubre algun sitio.
     *
     * @return cuantos instrumentos hay disponibles despues de cargarlo
     */
    public int cargarBanco(java.io.File fichero) throws Exception {
        soltarTodo();
        javax.sound.midi.Soundbank nuevo =
                BancoDeSonidos.cargarFichero(this.sintetizador, fichero);
        if (this.bancoCargado != null) {
            this.sintetizador.unloadAllInstruments(this.bancoCargado);
        }
        this.bancoCargado = nuevo;
        // El sonido que estaba puesto puede no existir en el banco nuevo: se
        // vuelve a pedir, y si no esta el sintetizador pone lo que tenga
        setSonido(this.banco, this.programa);
        return this.sintetizador.getAvailableInstruments().length;
    }

    /** Si se ha cargado un banco de sonidos externo. */
    public boolean tieneBancoPropio() {
        return this.bancoCargado != null;
    }

    /**
     * Pedal de resonancia. Se manda como el controlador 64, que el
     * sintetizador ya sabe interpretar: mantiene el sonido de las notas
     * soltadas hasta que se levanta.
     */
    public void setSostenido(boolean pisado) {
        this.sostenido = pisado;
        if (this.canal != null) {
            this.canal.controlChange(CC_SOSTENIDO, pisado ? 127 : 0);
        }
    }

    public boolean isSostenido() {
        return this.sostenido;
    }

    /** El sintetizador abierto, para que el reproductor pueda silenciar canales. */
    public Synthesizer getSintetizador() {
        return this.sintetizador;
    }

    public int getOctava() {
        return this.octava;
    }

    /** Devuelve true si la tecla del ordenador se ha usado para algo. */
    public boolean pulsarTecla(int codigo) {
        boolean atendida = false;
        if (codigo == KeyEvent.VK_LEFT) {
            atendida = cambiarOctava(-1);
        } else if (codigo == KeyEvent.VK_RIGHT) {
            atendida = cambiarOctava(1);
        } else {
            Integer semitono = MAPA.get(codigo);
            // Se ignora si ya estaba sonando: al mantener pulsada una tecla, el
            // sistema repite el evento muchas veces por segundo
            if (semitono != null && !this.sonando.containsKey(codigo)) {
                int nota = NOTA_BASE + semitono + this.octava * 12;
                this.sonando.put(codigo, nota);
                encender(nota);
                atendida = true;
            }
        }
        return atendida;
    }

    public boolean soltarTecla(int codigo) {
        Integer nota = this.sonando.remove(codigo);
        if (nota != null) {
            apagar(nota);
        }
        return nota != null;
    }

    /** Para el raton: enciende una nota concreta. */
    public void encender(int nota) {
        if (nota < 0 || nota > 127) {
            return;
        }
        if (this.canal != null) {
            this.canal.noteOn(nota, VELOCIDAD);
        }
        PiezaPulsable pieza = piezaDe(nota);
        if (pieza != null) {
            pieza.setColorPulsado(Estilo.COLOR_USUARIO);
            pieza.pulsar(VELOCIDAD);
        }
    }

    public void apagar(int nota) {
        if (nota < 0 || nota > 127) {
            return;
        }
        if (this.canal != null) {
            this.canal.noteOff(nota);
        }
        PiezaPulsable pieza = piezaDe(nota);
        if (pieza != null) {
            pieza.soltar();
        }
    }

    public void soltarTodo() {
        for (Integer nota : new HashMap<>(this.sonando).values()) {
            apagar(nota);
        }
        this.sonando.clear();
        if (this.canal != null) {
            this.canal.allNotesOff();
        }
    }

    /**
     * En el modo por canales puede que el canal reservado no tenga teclado;
     * entonces se usa el primero que haya, para que lo que se toca se vea.
     */
    private PiezaPulsable piezaDe(int nota) {
        PiezaPulsable pieza = null;
        if (this.instrumento != null) {
            pieza = this.instrumento.piezaDe(CANAL, nota);
            if (pieza == null && this.instrumento instanceof MultiPiano mp
                    && !mp.getCanales().isEmpty()) {
                pieza = this.instrumento.piezaDe(mp.getCanales().get(0), nota);
            }
        }
        return pieza;
    }

    private boolean cambiarOctava(int delta) {
        int nueva = Math.max(OCTAVA_MINIMA, Math.min(OCTAVA_MAXIMA, this.octava + delta));
        boolean cambia = nueva != this.octava;
        if (cambia) {
            soltarTodo();
            this.octava = nueva;
        }
        return true;
    }
}
