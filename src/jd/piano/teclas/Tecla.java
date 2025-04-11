package jd.piano.teclas;

import java.awt.*;

public abstract class Tecla implements Pulsable{
    protected Point posicion;
    private int nota;
    private boolean pulsada;
    private Color colorPulsada;
    private Graphics graphics;

    public Tecla(int n) {
        this.nota = n;
        this.posicion = null;
        this.pulsada = false;
        this.colorPulsada = null;
        this.graphics = null;
    }

    public int getNumeroNota() {
        return this.nota;
    }

    @Override
    public void setGraphics(Graphics graphics) {
        this.graphics = graphics;
    }

    @Override
    public void setPosicion(int x, int y) {
        this.posicion = new Point(x, y);
    }

    @Override
    public void pulsar() {
        this.pulsada = true;
    }

    @Override
    public void soltar() {
        this.pulsada = false;
    }

    @Override
    public void dibujar() {
        if (this.posicion == null || this.graphics == null) {
            throw new IllegalStateException("Hay que llamar a setPosicion y setGraphics "+
                    "antes de llamar al metodo dibujar");
        } 
        int[] verticesX = this.getVerticesX();
        int[] verticesY = this.getVerticesY();
        
        this.graphics.setColor(this.getColor());
        this.graphics.fillPolygon(verticesX, verticesY, verticesX.length);
        this.graphics.setColor(Color.BLACK);
        this.graphics.drawPolygon(verticesX, verticesY, verticesX.length);
    }

    @Override
    public void setColorPulsado(Color c) {
        this.colorPulsada = c;
    }

    @Override
    public boolean estaPulsado() {
        return this.pulsada;
    }

    @Override
    public Color getColorPulsado() {
        return this.colorPulsada;
    }

    protected abstract int[] getVerticesX();
    protected abstract int[] getVerticesY();
}
