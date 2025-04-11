package jd.piano.programa;

import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

import javax.sound.midi.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class ReproductorMidi implements Receiver {

    private static final Color[] COLORES = {
            Color.RED,
            Color.BLUE,
            Color.GREEN,
            Color.YELLOW,
            Color.MAGENTA,
            Color.ORANGE,
            Color.CYAN,
            Color.PINK,
            new Color(128, 0, 128),
            new Color(0, 128, 128),
            new Color(255, 165, 0),
            new Color(0, 255, 127),
            new Color(70, 130, 180),
            new Color(210, 105, 30),
            new Color(255, 20, 147),
            new Color(105, 105, 105)
    };
    private Piano piano;

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
        this.piano = p;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        boolean continuar = message instanceof ShortMessage;

        if (continuar) {
            ShortMessage sm = (ShortMessage) message;
            int canal = sm.getChannel();
            int nota = sm.getData1();
            int comando = sm.getCommand();

            continuar = canal != 9;

            if (continuar) {
                Tecla tecla = piano.getTecla(canal, nota);
                continuar = tecla != null;

                if (continuar) {
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
            }
        }
    }


    @Override
    public void close() {
    }
}
