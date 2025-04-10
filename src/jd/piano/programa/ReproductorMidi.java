package jd.piano.programa;

import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

import javax.sound.midi.*;
import java.awt.*;
import java.io.File;

public class ReproductorMidi implements Receiver{

    private static final Color[] COLORES = new Color[16];
    private Piano piano; //--> PianoMidi no existe

    public ReproductorMidi(){
        this.piano = null;
    }

    public void reproducir(String ruta) throws Exception{
        File f = new File(ruta);
        Sequence sequence = MidiSystem.getSequence(f);
        Sequencer sequencer = MidiSystem.getSequencer();
        sequencer.open();

        Transmitter transmitter = sequencer.getTransmitter();
        transmitter.setReceiver(this);

        sequencer.setSequence(sequence);
        sequencer.start();

        long duracion = sequence.getMicrosecondLength();
        Thread.sleep(duracion / 1000);

        transmitter.close();
        sequencer.close();

    }

    public void conectar(Piano p){
        this.piano = piano;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (!(message instanceof ShortMessage)) return;

        ShortMessage sm = (ShortMessage) message;
        int canal = sm.getChannel();
        if (canal == 9) return; // canal de percusión

        int nota = sm.getData1();
        Tecla tecla = piano.getTecla(canal, nota);
        if (tecla == null) return;

        int comando = sm.getCommand();

        if (comando == ShortMessage.NOTE_ON) {
            int volumen = sm.getData2();
            if (volumen > 0) {
                tecla.setColorPulsado(COLORES[canal]);
                tecla.pulsar();
            } else {
                tecla.soltar();
            }
        } else if (comando == ShortMessage.NOTE_OFF) {
            tecla.soltar();
        }

        tecla.dibujar();
    }

    @Override
    public void close() {

    }
}
