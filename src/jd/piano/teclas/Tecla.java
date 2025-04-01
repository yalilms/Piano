package jd.piano.teclas;

import java.awt.*;

public abstract class Tecla implements Pulsable{
    protected Point posicion;
    private int nota;
    private boolean pulsada;
    private Color colorPulsada;
    private Graphics graphics;

    Tecla(int n) {}


    public int getNumeroNota() {
        throw new UnsupportedOperationException();
    }

    protected abstract int[] getVerticesX();
    protected abstract int[] getVerticesY();

}
