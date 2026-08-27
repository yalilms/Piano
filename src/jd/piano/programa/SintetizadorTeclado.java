package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

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
    private Piano piano;
    private int octava;
    private int programa;

    /** Que nota esta sonando por cada tecla del ordenador pulsada. */
    private final Map<Integer, Integer> sonando = new HashMap<>();

    public SintetizadorTeclado() {
        this.sintetizador = null;
        this.canal = null;
        this.piano = null;
        this.octava = 0;
        this.programa = 0;
    }

    public void conectar(Piano piano) {
        this.piano = piano;
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
     * Con que instrumento se toca. El banco que trae Java tiene los 128 de
     * General MIDI cargados desde el arranque, asi que solo hace falta pedir
     * el cambio de programa.
     */
    public void setPrograma(int programa) {
        this.programa = Math.max(0, Math.min(127, programa));
        soltarTodo();
        if (this.canal != null) {
            this.canal.programChange(this.programa);
        }
    }

    public int getPrograma() {
        return this.programa;
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
        Tecla tecla = teclaDe(nota);
        if (tecla != null) {
            tecla.setColorPulsado(Estilo.colorDeCanal(CANAL));
            tecla.pulsar(VELOCIDAD);
        }
    }

    public void apagar(int nota) {
        if (nota < 0 || nota > 127) {
            return;
        }
        if (this.canal != null) {
            this.canal.noteOff(nota);
        }
        Tecla tecla = teclaDe(nota);
        if (tecla != null) {
            tecla.soltar();
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
     * En el multipiano puede que el canal reservado no tenga teclado; entonces
     * se usa el primero que haya, para que lo que se toca se vea igual.
     */
    private Tecla teclaDe(int nota) {
        Tecla tecla = null;
        if (this.piano != null) {
            tecla = this.piano.getTecla(CANAL, nota);
            if (tecla == null && this.piano instanceof MultiPiano mp
                    && !mp.getCanales().isEmpty()) {
                tecla = this.piano.getTecla(mp.getCanales().get(0), nota);
            }
        }
        return tecla;
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
