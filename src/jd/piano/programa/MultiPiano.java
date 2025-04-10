package jd.piano.programa;

import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;
import jd.piano.teclas.TeclaBlanca;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MultiPiano extends Piano {

    private List<PianoSencillo> pianos;

    public MultiPiano(int ti, int tf){
        this.pianos = new ArrayList<>();
        for (int i = 0; i <= 16; i++) {
            this.pianos.add(i, new PianoSencillo(ti, tf));
        }
    }


    @Override
    public Tecla getTecla(int canal, int nota) {
        return this.pianos.get(canal).getTecla(0, nota);
    }

    @Override
    public void setPosicion(int x, int y) {
    }

    @Override
    public void setGraphics(Graphics g) {
    }

    @Override
    public void dibujar() {
        int y = 0;
        int x = this.pianos.getFirst().getAnchura();
        for (int i = 0; i <= 8; i++) {
            for (int j = 0; j <= 2; j++) {
                if (j == 0) {
                    this.pianos.get(i).setPosicion(0, y);
                } else {
                    this.pianos.get(i).setPosicion(x, y);
                }
                this.pianos.get(j).dibujar();
            }
            y += this.pianos.getFirst().getAltura();
        }
    }

    @Override
    public int getAnchura() {
        return 0;
    }

    @Override
    public int getAltura() {
        return TeclaBlanca.ALTURA;
    }
}
