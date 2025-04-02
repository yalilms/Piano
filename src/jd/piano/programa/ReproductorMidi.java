package jd.piano.programa;

import jd.piano.teclas.Piano;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import java.awt.*;

public class ReproductorMidi implements Receiver{

    private static final Color[] COLORES = new Color[16];
    // private PianoMidi piano; --> PianoMidi no existe

    public ReproductorMidi(){
        throw new NullPointerException();
    }

    public void reproducir(String ruta){
        throw new NullPointerException();
    }

    public void conectar(Piano p){
        throw new NullPointerException();
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        throw new NullPointerException();
    }

    @Override
    public void close() {
        throw new NullPointerException();
    }
}
