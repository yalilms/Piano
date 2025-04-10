package jd.piano.programa;

import jd.piano.teclas.*;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PianoSencillo extends Piano {

    private Map<Integer, Tecla> teclas;

    public PianoSencillo(int teclaInicial, int teclaFinal){
        this.teclas = new HashMap<>();
        for (int i = teclaInicial; i<=teclaFinal; i++) {
            this.teclas.put(i, TeclaFactory.crearTecla(i));
        }
    }

    @Override
    public Tecla getTecla(int canal, int nota) {
        return this.teclas.get(nota);
    }

    @Override
    public void setPosicion(int x, int y) {
        int x1 = x;
        int y1 = y;
        for (Tecla i : this.teclas.values()) {
            if (i instanceof TeclaBlanca teclaBlanca) {
                teclaBlanca.setPosicion(x1, y1);
                x1 += teclaBlanca.getAnchura();
            }
            if (i instanceof TeclaNegra teclaNegra) {
                teclaNegra.setPosicion(x1-TeclaNegra.ANCHURA/2, y1);
            }
        }
        this.dibujar();
    }

    @Override
    public void setGraphics(Graphics g) {
        for (Tecla i : this.teclas.values()) {
            i.setGraphics(g);
        }
    }

    @Override
    public void dibujar() {
        for (Tecla i : this.teclas.values()) {
            i.dibujar();
        }
    }

    @Override
    public int getAnchura() {
        int totalAnchura = 0;
        for (Tecla i : this.teclas.values()) {
            if (i instanceof TeclaBlanca teclaBlanca) {
                totalAnchura += teclaBlanca.getAnchura();
            }
        }
        return totalAnchura;
    }

    @Override
    public int getAltura() {
        return TeclaBlanca.ALTURA;
    }
}
