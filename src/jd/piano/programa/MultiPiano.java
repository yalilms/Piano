package jd.piano.programa;

import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;
import jd.piano.teclas.TeclaBlanca;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MultiPiano extends Piano {

    private List<PianoSencillo> pianos;

    public MultiPiano(int ti, int tf) {
        setTeclaInicial(ti);
        setTeclaFinal(tf);
        this.pianos = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            this.pianos.add(new PianoSencillo(ti, tf));
        }
    }

    @Override
    public Tecla getTecla(int canal, int nota) {
        Tecla tecla = null;
        if (canal >= 0 && canal < pianos.size()) {
            tecla = this.pianos.get(canal).getTecla(0, nota);
        }
        return tecla;
    }

    @Override
    public void setPosicion(int x, int y) {
        super.setPosicion(x, y);
        int filas = 8;
        int columnas = 2;
        int espacioEntreFilas = 30;
        int espacioEntreColumnas = 40;
        
        int anchoUnPiano = this.pianos.get(0).getAnchura();
        int altoUnPiano = this.pianos.get(0).getAltura();
        
        for (int i = 0; i < pianos.size(); i++) {
            int fila = i / columnas;
            int columna = i % columnas;
            
            int posX = x + columna * (anchoUnPiano + espacioEntreColumnas);
            int posY = y + fila * (altoUnPiano + espacioEntreFilas);
            
            PianoSencillo piano = this.pianos.get(i);
            piano.setPosicion(posX, posY);
        }
    }

    @Override
    public void setGraphics(Graphics g) {
        for (PianoSencillo piano : pianos) {
            piano.setGraphics(g);
        }
    }

    @Override
    public void dibujar() {
        for (PianoSencillo piano : this.pianos) {
            piano.dibujar();
        }
    }

    @Override
    public int getAnchura() {
        int anchoUnPiano = this.pianos.getFirst().getAnchura();
        int espacioEntreColumnas = 40;
        return 2 * anchoUnPiano + espacioEntreColumnas;
    }

    @Override
    public int getAltura() {
        int altoUnPiano = this.pianos.getFirst().getAltura();
        int espacioEntreFilas = 30;
        return 8 * altoUnPiano + 7 * espacioEntreFilas;
    }
}
